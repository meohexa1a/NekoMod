---
name: semantic-clarity-reviewer
description: >-
  Đánh giá tính trong sáng ngữ nghĩa của code: tên hàm/biến có truyền đạt đúng ý đồ không,
  API có nói dối không, chữ ký hàm có phản ánh hành vi thật không, code đọc lên có hiểu ngay không.
  Đây là skill "mềm" — cần phán đoán, không phải regex.
---

# Semantic Clarity Reviewer

Skill này chấm điểm **tính trong sáng ngữ nghĩa** — thứ mà rule cứng không bao giờ bắt được, nhưng gây ra nhiều bug và hiểu lầm nhất.

---

## I. Tiêu Chí Đánh Giá (6 Tiêu Chí)

### 1. Tên Có Nói Dối Không? (Name Honesty)

Tên hàm, tên class, tên biến phải phản ánh CHÍNH XÁC hành vi thật sự.

- 🔴 **CRITICAL** nếu: Tên nói một đằng, code làm một nẻo
  ```kotlin
  // TỆ: Tên "scrollTo" gợi ý cuộn mượt, nhưng thực tế gán cứng
  suspend fun scrollTo(target: Float) {
      value = target.coerceIn(0f, limit)  // không animation, không delay
  }
  
  // TỆ: Tên "FILL" gợi ý lấp đầy, nhưng trên main axis hành xử như SHRINK
  SizeFlag.FILL
  ```

- 🟢 **GOOD** nếu: Tên truyền đạt chính xác
  ```kotlin
  // TỐT: Tên nói rõ là "raw delta", không hứa hẹn animation
  fun dispatchRawDelta(delta: Float): Float
  
  // TỐT: Tên nói rõ là "compute offset" — pure math, không side effect
  fun Alignment.computeOffset(allocated: Float, actual: Float): Float
  ```

---

### 2. Chữ Ký Hàm Có Phản Ánh Hành Vi? (Signature Honesty)

- 🔴 **CRITICAL** nếu: `suspend` nhưng body đồng bộ 100%
- 🔴 **CRITICAL** nếu: Tham số tồn tại nhưng không có tác dụng (phantom parameter)
- 🟡 **WARNING** nếu: Return type quá chung (trả `Any` khi có thể trả type cụ thể)

```kotlin
// TỆ: suspend nhưng không await/delay/yield bất kỳ thứ gì
suspend fun scrollTo(target: Float) { value = target }

// TỆ: shape parameter có nhánh null không làm gì, nhưng caller nghĩ nó có tác dụng
fun Modifier.background(color: Color, shape: Shape? = null)
// → nhánh null {} chỉ comment "bảo tồn bo góc" — có thật sự bảo tồn không?
```

---

### 3. API Có Side-Effect Ngầm Không? (No Hidden Side-Effects)

Hàm mang tên A tuyệt đối không được có side-effect B mà caller không biết.

- 🔴 **CRITICAL** nếu: Setter của thuộc tính X âm thầm đổi thuộc tính Y
- 🟡 **WARNING** nếu: Constructor khởi tạo resource nặng mà caller không expect

```kotlin
// TỆ: widthIn() chỉ nên đặt giới hạn biên, nhưng âm thầm đổi sizeFlag
fun Modifier.widthIn(min: Float, max: Float) = this.then(
    SizeModifier(minWidth = min, maxWidth = max, 
                 sizeFlagH = SizeFlag.SHRINK)  // ← side-effect ngầm!
)
```

---

### 4. Trừu Tượng Hóa Có Xứng Đáng Không? (Abstraction Justification)

- 🟡 **WARNING** nếu: Interface chỉ có 1 implementation và không có kế hoạch mở rộng
- 🟡 **WARNING** nếu: Sealed class/interface mà mọi consumer đều `when` rồi bóc tách cùng 1 data
- 🟡 **WARNING** nếu: Wrapper class bọc 1 primitive mà không thêm hành vi nào

```kotlin
// TỆ: sealed interface Shape rỗng, mọi consumer đều when rồi lấy 4 float
sealed interface Shape           // 0 methods, 0 properties
data object RectangleShape : Shape  // = RoundedCornerShape(0f)

// TỆ: ConsumedData bọc 1 boolean
class ConsumedData(var isConsumed: Boolean = false)

// TỐT: LayoutPolicy interface có 2 implementations với logic khác nhau
interface LayoutPolicy {
    fun computeMinSize(node: LayoutNode)
    fun arrangeChildren(...)
}
```

---

### 5. Code Đọc Lên Có Hiểu Ngay Flow Không? (Readability Flow)

- 🟡 **WARNING** nếu: Logic chính bị chôn vùi trong nested conditions >3 tầng
- 🟡 **WARNING** nếu: Hàm dài >50 dòng mà không tách helper
- 💡 **SUGGESTION** nếu: Comment giải thích "what" thay vì "why" (code tự nói what)

---

### 6. Dead Code & Phantom Properties

- 🟡 **WARNING** nếu: Property được khởi tạo nhưng 0 callers đọc/ghi
- 🟡 **WARNING** nếu: Hàm public nhưng 0 callers ngoài test

```kotlin
// TỆ: interactionSource khởi tạo MutableSharedFlow nhưng không ai dùng
val interactionSource: InteractionSource = MutableInteractionSource()

// TỆ: scrollState proxy nhưng 0 callers
var scrollState: ScrollState?
    get() = verticalScrollState ?: horizontalScrollState
```

---

## II. Quy Trình Review

1. Đọc danh sách files đã thay đổi (hoặc files cần review)
2. Với mỗi file, quét qua 6 tiêu chí trên
3. Ghi findings theo severity (CRITICAL / WARNING / GOOD / SUGGESTION)
4. Chấm điểm: Bắt đầu 10, trừ 2/CRITICAL, trừ 1/WARNING, tối thiểu 0
5. Output theo format chuẩn review-pipeline

---

## III. Output Format

```markdown
## Semantic Clarity Review — [Module/File]

### Score: X/10

### Findings:
1. 🔴 [CRITICAL] ...
2. 🟡 [WARNING] ...
3. 🟢 [GOOD] ...
4. 💡 [SUGGESTION] ...

### Summary:
1-2 câu.

### Recommendation: APPROVE | NEEDS_WORK | REJECT
```
