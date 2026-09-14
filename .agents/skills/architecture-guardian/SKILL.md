---
name: architecture-guardian
description: >-
  Tự động hóa quá trình thanh tra, siết chặt đóng gói, hoàn thiện KDoc 100%,
  kiểm thử và đóng băng kiến trúc (Architectural Lockdown) cho các module đã đạt độ ổn định.
  Kích hoạt bảo vệ bằng pre-commit hook và đồng bộ hóa bản đồ trạng thái dự án.
---

# Architecture Guardian & Lockdown Skill

Skill này đóng vai trò là **"Tổng Thanh Tra & Người Canh Gác Kiến Trúc"** cho NekoMod v3. Khi một module code đã hoàn thành tính năng và vượt qua các vòng review logic, skill này cung cấp quy chuẩn và các bước tự động để:
1. **Siết chặt quyền truy cập (Hardening)**: Biến entity thành bất biến đối với thế giới bên ngoài, đóng gói chặt chẽ các trường nháp và bộ đệm tính toán.
2. **Chuẩn hóa KDoc 100%**: Viết bổ sung KDoc tiếng Việt chuẩn mực, chi tiết và có chiều sâu cho toàn bộ public & internal APIs.
3. **Bảo vệ toàn vẹn bằng Git Hook**: Tự động đưa module vào danh sách đóng băng (`PROTECTED_PATHS` trong `.githooks/pre-commit`).
4. **Xác thực cổng kiểm thử (Zero-Regression Gate)**: Đảm bảo 100% test suite toàn dự án pass trước khi chốt sổ.

---

## I. Bộ Tiêu Chuẩn 6 Điều Kiện Khóa Cốt Lõi (Lockdown Matrix)

Một module chỉ được coi là đủ điều kiện khóa vào **🟢 Stable** khi thỏa mãn đồng thời 6 điều kiện sau:

### 1. Tính Đóng Gói Lớp (Class & Type Immutability)
- **Quy tắc**: Mọi class trong module không được phép để `open` tùy tiện. Mặc định là `final` (hoặc `class` thường trong Kotlin), `sealed class/interface`, hoặc `value class`.
- **Ngoại lệ duy nhất**: Chỉ các base class trừu tượng được thiết kế tường minh cho việc mở rộng (như `ComposeView` để Scene2D kế thừa) mới được mang từ khóa `open`.

### 2. Quyền Ghi Thuộc Tính (Computed Geometry & Buffer Mutability)
- **Quy tắc**: Mọi thuộc tính là kết quả tính toán (`x`, `y`, `width`, `height`, `contentWidth`, `contentHeight`) hoặc con trỏ bộ đệm (`vertexIndex`, `queuedQuadCount`, `clipDepth`, `clipMinX`, ...) **BẮT BUỘC** phải là `internal set` hoặc `private set`.
- **Bên ngoài (Callers/Virtual DOM)**: Chỉ có quyền đọc (`val` hoặc `var ... internal set`). Tuyệt đối cấm gán đè tọa độ từ ngoài vòng đời layout.

### 3. Đóng Gói Bộ Sưu Tập (Collection Encapsulation)
- **Quy tắc**: Tuyệt đối cấm phơi bày `ArrayList` hoặc `MutableList` có thể sửa đổi ra ngoài.
- **Chuẩn hóa**: Bắt buộc dùng cặp backing property:
  ```kotlin
  private val _children = ArrayList<LayoutNode>()
  val children: List<LayoutNode> get() = _children
  ```
- Mọi thao tác thêm/sửa/xóa phải thông qua các API chuyên biệt của entity (`addChild`, `removeChild`, `clearChildren`) để đảm bảo tính toàn vẹn 2 chiều.

### 4. Cửa Khẩu Tự Vệ (Gateway Sanitization & Never-Throw)
- **Quy tắc**: Mọi setter và constructor nhận giá trị hình học (kích thước, padding, margin, bán kính bo, alpha, độ dày viền) phải tự chuẩn hóa ngay tại cửa khẩu:
  ```kotlin
  set(value) {
      field = if (value.isNaN() || value < 0f) 0f else value
  }
  ```
- Triệt tiêu hoàn toàn nguy cơ ném ngoại lệ toán học (`IllegalArgumentException` trong `coerceIn`) hoặc làm sập game loop của Mindustry.

### 5. Độc Lập Vòng Đời Tuyệt Đối (Lifecycle Isolation — Rule 0.5)
- **Quy tắc**: Vòng đời instance của một View (`ComposeView.dispose()`) chỉ được dọn dẹp tài nguyên thuộc về chính nó (hủy Snapshot, hủy Coroutines, reset node con).
- **Cấm tiệt**: Không một View cục bộ nào được phép gọi `dispose()` lên các tài nguyên Singleton toàn cục (`UIBatch`, `UberShader`, `CompositionManager`).

### 6. Phủ KDoc Chuẩn Mực 100% (100% KDoc Coverage)
- **Quy tắc**: Không một `class`, `interface`, `object`, `fun`, hoặc `property` công khai nào được phép "trần trụi" không có tài liệu.
- **Nội dung KDoc chuẩn**:
  - Tóm tắt trách nhiệm kiến trúc bằng tiếng Việt trang trọng, dễ hiểu.
  - Chú thích rõ ràng các tham số (`@param`) và giá trị trả về (`@return`).
  - Cảnh báo Hot-Path / Zero-GC nếu hàm nằm trong vòng lặp dựng hình hoặc xử lý sự kiện frame loop.
  - Sơ đồ ASCII diagram đối với các thuật toán không gian hoặc pipeline nhiều pha.

---

## II. Quy Trình 4 Bước Khóa Module Tự Động

Khi nhận nhiệm vụ khóa một module (ví dụ: `org/hubdustry/core/graphics`), Agent thực hiện tuần tự:

```
┌─────────────────────────────────────────────────────────────────────────────┐
│ BƯỚC 1: SCAN & HARDEN                                                       │
│ 1. Đọc toàn bộ các file trong thư mục module.                               │
│ 2. Kiểm tra `open` thừa -> chuyển thành `class` hoặc `sealed`.              │
│ 3. Kiểm tra setters -> thêm `internal set` hoặc `private set`.              │
│ 4. Kiểm tra Gateway Sanitization -> thêm kiểm tra `isNaN() || < 0f`.        │
│ 5. Kiểm tra dead code & overload thừa -> dọn dẹp sạch sẽ.                   │
└──────────────────────────────────────┬──────────────────────────────────────┘
                                       │
                                       ▼
┌─────────────────────────────────────────────────────────────────────────────┐
│ BƯỚC 2: KDOC INFILLING                                                      │
│ 1. Rà soát danh sách hàm/biến/class còn thiếu KDoc.                         │
│ 2. Soạn thảo KDoc chuẩn mực theo phong cách kỹ thuật NekoMod.               │
│ 3. Chèn KDoc trực tiếp vào mã nguồn.                                        │
└──────────────────────────────────────┬──────────────────────────────────────┘
                                       │
                                       ▼
┌─────────────────────────────────────────────────────────────────────────────┐
│ BƯỚC 3: TEST VERIFICATION GATE                                              │
│ 1. Chạy lệnh: `.\gradlew.bat test --rerun-tasks`                            │
│ 2. Đảm bảo 100% test suite toàn dự án PASS, không có hồi quy nào.           │
│ 3. Nếu có test fail: Phải phân tích và khắc phục triệt để trước khi khóa.    │
└──────────────────────────────────────┬──────────────────────────────────────┘
                                       │
                                       ▼
┌─────────────────────────────────────────────────────────────────────────────┐
│ BƯỚC 4: LOCKOUT & SYNCHRONIZATION                                           │
│ 1. Thêm đường dẫn module vào `PROTECTED_PATHS` trong `.githooks/pre-commit`.│
│ 2. Cập nhật `project_status_map.md` thành 🟢 Stable (100% Ổn Định).         │
│ 3. Xuất báo cáo nghiệm thu theo format chuẩn bên dưới.                      │
└─────────────────────────────────────────────────────────────────────────────┘
```

---

## III. Mẫu Báo Cáo Nghiệm Thu Khóa Kiến Trúc (Output Format)

Khi hoàn tất khóa module, Agent xuất báo cáo theo định dạng chuẩn:

```markdown
# 🛡️ Architecture Lockdown Report — [Tên Module]

### 1. Thông Tin Chốt Sổ
- **Module Path**: `src/main/kotlin/org/hubdustry/...`
- **Số lượng files bảo vệ**: N files
- **Trạng thái**: 🟢 STABLE / LOCKED DOWN

### 2. Kết Quả Rà Soát & Bọc Giáp (Hardening Matrix)
| Tiêu chí | Trạng thái | Chi tiết thực thi |
|:---|:---:|:---|
| Class & Type Immutability | 🟢 ĐẠT | Mọi class đều là final/data class/object. |
| Computed Mutability | 🟢 ĐẠT | Toàn bộ setters tính toán đã là `internal set`. |
| Collection Encapsulation | 🟢 ĐẠT | Đóng gói qua backing property read-only. |
| Gateway Sanitization | 🟢 ĐẠT | 100% dữ liệu hình học kẹp biên $0 \le min \le max$, lọc NaN. |
| Lifecycle Isolation | 🟢 ĐẠT | Không có rò rỉ dispose giữa Singleton và View cục bộ. |
| KDoc Completeness | 🟢 ĐẠT | 100% public/internal declarations có KDoc chuẩn mực. |

### 3. Xác Thực Kiểm Thử (Verification Gate)
- **Tổng số tests chạy**: X/X tests passed (100%)
- **Hồi quy (Regression)**: 0 phát hiện

### 4. Rào Chắn Đã Kích Hoạt (Systemic Enforcement)
- [x] Đã ghi nhận vào `.githooks/pre-commit` (`PROTECTED_PATHS`)
- [x] Đã cập nhật `project_status_map.md`
- [x] Sẵn sàng commit và đóng băng
```
