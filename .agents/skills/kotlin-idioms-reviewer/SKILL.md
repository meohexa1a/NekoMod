---
name: kotlin-idioms-reviewer
description: >-
  Review Kotlin code for idiomatic style, detect Java-thinking anti-patterns,
  enforce zero-GC rules in engine hot-paths, and ensure clean architectural encapsulation.
  Use this skill whenever reviewing diffs, auditing Kotlin code, or before finalizing any task.
---

# Kotlin Idioms & Universal Architecture Reviewer Skill

Skill này cung cấp quy trình và tiêu chuẩn kỹ thuật để rà soát (review) mã nguồn Kotlin trong dự án NekoMod, triệt tiêu tận gốc tư duy viết code kiểu Java ("Java with Kotlin syntax"), loại bỏ nợ kỹ thuật tương thích ngược trong core, và đảm bảo kỷ luật hiệu năng Zero-GC trong game.

---

## I. Sổ Tay Nhận Diện 8 Căn Bệnh Anti-Pattern Trong Core Codebase

### 1. Thực Thể Thụ Động (Anemic DTO) & Thiếu Gateway Sanitization
* ❌ **Anti-Pattern**:
  - Node hoặc Component là một túi chứa dữ liệu hở (`var` công khai, `children: MutableList` lộ thiên, `parent` lộ setter).
  - Không tự bảo vệ biên tại setter, để mặc cho các tầng tính toán bên dưới phải gánh `if (isNaN() || < 0f)` hoặc bị sập vì `coerceIn(min, max)` khi `min > max`.
*  **Universal Invariant**:
  - **Active Self-Validating Entity**: Toàn bộ setter kẹp biên ngay cửa khẩu ($0 \le min \le max$, lọc `NaN`).
  - **Tree Encapsulation**: `children: List<T>` là read-only view. Mọi thay đổi cấu trúc cây phải qua API `addChild`, `removeChild`, `clearChildren` với cơ chế đồng bộ `parent` 2 chiều và chặn chu trình lặp tổ tiên.

---

### 2. Ôm Đồm Nợ Tương Thích Ngược Trong Core (Zero Deprecation Cruft)
* ❌ **Anti-Pattern**:
  - Giữ lại các class rác, method rác với `@Deprecated` hay các wrapper tương thích ngược trong các module cốt lõi (Layout, Virtual DOM, Render, Events).
*  **Universal Invariant**:
  - **Clean Break**: Core engine không mang gánh nặng lịch sử. Khi nâng cấp kiến trúc, xóa bỏ triệt để các cấu trúc cũ, refactor dứt điểm và cập nhật thẳng callers/test suite.

---

### 3. Tư duy Câu lệnh thay vì Biểu thức (Statement vs Expression)
* ❌ **Java Smell**: Coi `when` hoặc `if` như lệnh rẽ nhánh để gán biến tạm hoặc gọi lặp lại các hàm xử lý biên:
  ```kotlin
  // TỆ: Lặp lại coerceIn trong từng nhánh, cú pháp cồng kềnh
  val childW = when (child.sizeFlagHorizontal) {
      SizeFlag.EXPAND, SizeFlag.FILL -> slotW.coerceIn(child.minWidth, child.maxWidth)
      SizeFlag.SHRINK -> child.minWidth.coerceIn(child.minWidth, child.maxWidth)
  }
  ```
*  **Kotlin Idiomatic**: Coi `when` là Expression trả về giá trị thô, chain trực tiếp hàm kẹp biên `.coerceIn(min, max)` ngay sau block:
  ```kotlin
  // ĐẸP: Tách bạch rõ ràng, when chỉ chọn giá trị, coerceIn kẹp biên
  val childW = when (child.sizeFlagHorizontal) {
      SizeFlag.EXPAND, SizeFlag.FILL -> slotW
      SizeFlag.SHRINK -> child.minWidth
  }.coerceIn(child.minWidth, child.maxWidth)
  ```

---

### 4. Bóc Tách Dữ Liệu Ra Ngoài (Feature Envy & Procedural Helpers)
* ❌ **Java Smell**: Rút các trường của Object A ra để truyền vào một hàm static/helper bên ngoài:
  ```kotlin
  // TỆ: Alignment bị tước đoạt trách nhiệm, phụ thuộc hàm helper bên ngoài
  val offset = LayoutHelper.computeAlignment(child.alignHorizontal, slotW, childW)
  ```
*  **Kotlin Idiomatic**: Logic thuộc về kiểu dữ liệu nào thì gắn liền với kiểu đó qua **Extension Function** hoặc Member:
  ```kotlin
  // ĐẸP: Alignment tự biết cách tính offset của chính nó
  fun Alignment.computeOffset(allocated: Float, actual: Float): Float = when (this) {
      Alignment.START -> 0f
      Alignment.CENTER -> (allocated - actual) * 0.5f
      Alignment.END -> allocated - actual
  }
  ```

---

### 5. Nhân Đôi Code Theo Trục Đối Xứng (Symmetry Duplication)
* ❌ **Java Smell**: Rẽ nhánh `if (isHorizontal) ... else ...` lặp lại khắp nơi, hoặc viết 2 class song song chỉ đổi trục $W \leftrightarrow H$ và $X \leftrightarrow Y$.
*  **Kotlin Idiomatic**: Trừu tượng hóa theo trục qua `Orientation (HORIZONTAL, VERTICAL)`, trang bị các hàm chiếu 1D (`minSize(orientation)`, `main(x, y)`), xử lý thống nhất trên `mainAxis` và `crossAxis`.

---

### 6. Quá Tải Hàm (Method Overloading) thay vì Default Arguments
* ❌ **Java Smell**: Viết 3-4 hàm overload chỉ để truyền tham số tùy chọn:
  ```kotlin
  fun RowPolicy(): FlexLayoutPolicy = RowPolicy(0f)
  fun RowPolicy(gap: Float): FlexLayoutPolicy = ...
  ```
*  **Kotlin Idiomatic**: Sử dụng tham số mặc định (Default Parameter):
  ```kotlin
  fun RowPolicy(gap: Float = 0f): FlexLayoutPolicy = FlexLayoutPolicy(Orientation.HORIZONTAL, gap)
  ```

---

### 7. Xung Đột Giữa "Functional Idioms" và "Zero-GC Hot Path"
* **Cold-Path (DSL, Config, Navigation, Setup)**:
  -  KHUYẾN KHÍCH tối đa functional style: `map`, `filter`, `let`, `apply`, `takeIf`, `data class`.
* **Hot-Path (Frame Loop: Measure, Layout, Draw, Hit-Test)**:
  - ❌ **CẤM TUYỆT ĐỐI**: `children.filter { it.visible }.forEach { ... }` (Tạo ArrayList + Iterator + Lambda closure mỗi frame).
  -  **BẮT BUỘC**: Dùng vòng lặp chỉ mục thuần túy (`for (i in 0 until count)`), biến scratchpad trên Node hoặc `value class` để 0 byte cấp phát Heap.

---

### 8. Enterprise Over-Engineering (Bãi Rác Port/Adapter/Interface Thừa)
* ❌ **Java Smell**: Tạo một loạt Interface đơn chức năng (`WindowPort`, `InputPort`, `AssetPort`) kèm Implementation 1-1 chỉ để bọc API có sẵn của game/engine mà không bao giờ thay thế implementation thứ 2.
*  **Kotlin Idiomatic**: 
  - Gọi trực tiếp API Arc/Mindustry ở những chỗ cần thiết.
  - Sử dụng **Thin Facade** hoặc tận dụng `CompositionLocalProvider` (`LocalDensity`, `LocalAtlas`) của Compose.

---

## II. Quy Trình 6 Bước Thanh Tra Code (Review Runbook)

Khi kích hoạt skill này để review bất kỳ diff hoặc file Kotlin nào, hãy thực hiện theo đúng 6 bước sau:

### Bước 1: Quét từ khóa CẤM trong Hot-Path
Kiểm tra các file nằm trong package layout hoặc render (`policies/*`, `render/*`):
- Tìm xem có xuất hiện `.filter {`, `.map {`, `.forEach {`, `.toList()` hay không.
- Nếu có $\implies$ **FLAG NGAY**: Yêu cầu chuyển về vòng lặp `for (i in 0 until count)`.

### Bước 2: Quét Active Entity & Gateway Sanitization
- Kiểm tra các setters của domain entity: Đã kẹp biên $0 \le min \le max$ chưa? Đã lọc `NaN` chưa?
- Kiểm tra `children`: Có bị lộ `MutableList` hay setter của `parent` ra ngoài không?

### Bước 3: Quét nợ tương thích ngược (Deprecation Debt)
- Có file/class nào là wrapper cũ giữ lại chỉ để tương thích ngược (`@Deprecated`, helper shim) không?
- Nếu có trong module lõi $\implies$ Yêu cầu xóa bỏ triệt để và refactor callers trực tiếp.

### Bước 4: Quét các khối `when` và `if`
- Kiểm tra xem các nhánh `when` có đang lặp lại logic giống nhau (ví dụ: `.coerceIn()`, gán biến tạm) hay không.
- Nếu có $\implies$ Yêu cầu chuyển sang cú pháp biểu thức Expression chaining.

### Bước 5: Kiểm tra tính đóng gói và trừu tượng đối xứng
- Chuyển procedural helpers thành Extension Functions hoặc Member Methods.
- Loại bỏ rẽ nhánh ngang/dọc trùng lặp bằng `Orientation`.

### Bước 6: Chạy kiểm thử xác thực tự động
- Chạy lệnh:
  ```powershell
  ./gradlew test --rerun
  ```
- Đảm bảo 100% test pass.

---

## III. Mẫu Báo Cáo Đánh Giá (Review Output Template)

```markdown
### 📋 Kết Quả Đánh Giá Kotlin Idioms & Universal Architecture

| Tiêu chuẩn | Đánh giá | Chi tiết / Đề xuất cải thiện |
| :--- | :---: | :--- |
| **1. Active Entity & Gateway Sanitization** |  / ❌ | ... |
| **2. Zero Deprecation Debt in Core** |  / ❌ | ... |
| **3. Expression over Statement** |  / ❌ | ... |
| **4. Feature Envy / Encapsulation** |  / ❌ | ... |
| **5. Zero-GC in Hot-Paths** |  / ❌ | ... |
| **6. Symmetry Abstraction (Orientation)** |  / ❌ | ... |
| **7. Default Args / No Overload** |  / ❌ | ... |

**Kết luận**: [ĐẠT YÊU CẦU / CẦN SỬA ĐỔI]
```
