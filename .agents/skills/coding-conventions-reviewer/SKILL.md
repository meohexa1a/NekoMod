---
name: coding-conventions-reviewer
description: >-
  Rà soát các quy chuẩn lập trình Kotlin, quy tắc đặt tên (naming conventions),
  khớp package/directory, cấu trúc file, guard clauses, và giới hạn kích thước hàm trong NekoMod v3.
---

# Coding Conventions Reviewer — Quy Chuẩn Lập Trình NekoMod v3

Skill này đóng vai trò là trọng tài kiểm tra **tính kỷ luật và tính nhất quán của mã nguồn Kotlin** trong NekoMod v3. Tập trung vào các quy ước đặt tên chuẩn Kotlin, cấu trúc thư mục/tệp tin, giới hạn phạm vi hàm và triết lý phẳng hóa (Guard Clauses).

---

## I. Tiêu Chí Đánh Giá Cốt Lõi

### 1. Quy Chuẩn Đặt Tên (Kotlin Naming Conventions)
- **`PascalCase`**: Bắt buộc cho `class`, `interface`, `object`, `enum class`, `sealed interface/class`, `annotation class`, và `@Composable` functions trả về UI (như `Box`, `Column`, `TextField`).
- **`camelCase`**: Bắt buộc cho hàm thông thường (`fun`), thuộc tính (`val`, `var`), tham số hàm (`parameter`), và biến cục bộ (`local variable`).
- **`SCREAMING_SNAKE_CASE`**: Bắt buộc cho hằng số compile-time (`const val`) trong companion object hoặc top-level.
- **`lowercase.dot`**: Bắt buộc cho package declaration, viết thường hoàn toàn, phân cách bằng dấu chấm.
- **Private backing properties**: Dùng tiền tố gạch dưới `_propertyName` cho private backing field đi kèm public read-only property (ví dụ: `_children` và `children`).

### 2. Khớp Tên File & Khớp Package (File & Package Integrity)
- **File Name**: Tên tệp tin `.kt` phải phản ánh chính xác khai báo chính bên trong:
  - Nếu file chứa 1 class/object chính: Tên file phải trùng tên class (ví dụ: `ComposeView.kt` chứa `class ComposeView`).
  - Nếu file chứa extension functions theo nhóm: Đặt tên theo tiền tố nhóm (ví dụ: `ModifierScroll.kt`, `ModifierPadding.kt`).
- **Package Declaration**: Khai báo `package <name>` ở đầu file bắt buộc phải khớp 100% với đường dẫn thư mục vật lý tính từ `src/main/kotlin/` hoặc `src/test/kotlin/`.

### 3. Cấm Magic Numbers Trong Domain Logic (No Magic Numbers)
- Cấm sử dụng các số không có nguồn gốc rõ ràng (như `32f`, `16f`, `300f`, `0.94f`) rải rác trong code tính toán mà không có tên hằng số hoặc bình luận giải thích ngữ nghĩa.
- Các hằng số cấu hình vật lý, thời gian, kích thước mặc định phải được khai báo thành `private const val` hoặc hằng số có tên mô tả rõ ý đồ.

### 4. Triết Lý Phẳng Hóa & Guard Clauses (Early Return Standard)
- Bắt buộc dùng **Guard Clauses / Early Return** để phẳng hóa logic điều kiện.
- Cấm lồng `if-else` nhiều tầng khi điều kiện biên có thể được kiểm tra và return sớm:
  ```kotlin
  // ❌ Xấu: Lồng if sâu
  fun process(node: LayoutNode?) {
      if (node != null) {
          if (node.visible) {
              // ...
          }
      }
  }

  // ✅ Tốt: Guard clauses phẳng hóa
  fun process(node: LayoutNode?) {
      if (node == null || !node.visible) return
      // ...
  }
  ```

### 5. Giới Hạn Chiều Dài Hàm (Function Length Limits)
- Hàm thông thường:
  - $\le$ 50 dòng: 🟢 Lý tưởng.
  - 51 – 120 dòng: 🟡 CẢNH BÁO (Cần xem xét trích xuất hàm con hoặc local functions).
  - \> 120 dòng: 🔴 CRITICAL (Vi phạm nghiêm trọng khả năng bảo trì, bắt buộc refactor trừ trường hợp bảng switch/when ánh xạ đặc biệt).

### 6. Cấm Shadowing Biến (No Variable Shadowing)
- Tuyệt đối không đặt tên tham số hàm hoặc biến cục bộ trùng với tên trường của class (`this.x = x` chỉ dùng trong constructor của Java-thinking; trong Kotlin ưu tiên đặt tên tường minh hoặc dùng constructor properties). Cấm khai báo biến con trùng tên che khuất biến ngoài phạm vi.

### 7. Phân Tách Từ Vựng Kiến Trúc (Architectural Vocabulary Isolation)
- Cấm trộn lẫn thuật ngữ Scene2D (`Actor`, `Table`, `Stage`, `touchDown`) vào bên trong cây Virtual DOM (`LayoutNode`, `LayoutPolicy`, `PointerEvent`).
- Sự chuyển đổi thuật ngữ chỉ được phép diễn ra tại cửa khẩu duy nhất là `ComposeView` và `ArcInputAdapter`.

### 8. Quy Chuẩn Đặt Tên Unit Test (Test Naming Ergonomics)
- Tên hàm kiểm thử phải nêu bật kịch bản và kết quả kỳ vọng:
  - Cú pháp: `test<Feature>_<Scenario>_<ExpectedResult>` hoặc mô tả hành vi đầy đủ, rõ ràng bằng tiếng Anh / camelCase tiếng Anh (ví dụ: `testScrollDisabledRetainsClippingAndProgrammaticScroll`).

---

## II. Hướng Dẫn Chấm Điểm

- 🔴 **CRITICAL (-2 điểm mỗi lỗi)**:
  - Khai báo `class`, `object`, `interface` bắt đầu bằng chữ thường.
  - Package declaration sai lệch so với đường dẫn thư mục.
  - Hàm vượt quá 120 dòng mà không có lý do kiến trúc đặc biệt.
  - Trộn lẫn trực tiếp đối tượng Scene2D vào trong core Virtual DOM entity.
- 🟡 **WARNING (-1 điểm mỗi lỗi)**:
  - Sử dụng magic numbers không có named constant hoặc KDoc giải thích.
  - Hàm dài từ 51 đến 120 dòng có thể trích xuất hàm nhỏ hơn.
  - Lồng `if/else` sâu từ 3 cấp trở lên mà không dùng guard clause.
  - Biến cục bộ / tham số không tuân thủ `camelCase` hoặc hằng số không dùng `SCREAMING_SNAKE_CASE`.
- 🟢 **GOOD**: Ghi nhận các hàm ngắn gọn, phân tách trách nhiệm rõ ràng, áp dụng guard clauses xuất sắc.
- 💡 **SUGGESTION**: Đề xuất cải thiện không trừ điểm.

---

## III. Output Format Chuẩn

```markdown
## Coding Conventions Review — [Tên Module/File]

### Score: X/10

### Findings:
1. 🔴 [CRITICAL] Mô tả vi phạm — file:line — chi tiết
2. 🟡 [WARNING] Mô tả vi phạm — file:line — chi tiết
3. 🟢 [GOOD] Điểm sáng về coding convention
4. 💡 [SUGGESTION] Khuyến nghị tinh chỉnh

### Summary:
Tóm tắt mức độ tuân thủ quy chuẩn của file.

### Recommendation: APPROVE | NEEDS_WORK | REJECT
```
