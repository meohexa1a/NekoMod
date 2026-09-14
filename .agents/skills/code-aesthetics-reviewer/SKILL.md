---
name: code-aesthetics-reviewer
description: >-
  Rà soát vẻ đẹp thị giác và tính thẩm mỹ của mã nguồn Kotlin trong NekoMod:
  nhịp điệu cú pháp, phân đoạn thân hàm, thân hàm expression vs block,
  local functions, đặt tên biến kể chuyện, named arguments, và ASCII diagrams.
  KHÔNG kiểm tra logic nghiệp vụ, không kiểm tra hiệu năng — chỉ đánh giá
  "đọc lên có dễ chịu và dễ hiểu không".
---

# Code Aesthetics Reviewer Skill

Skill này đánh giá **tính thẩm mỹ và dễ đọc** của mã nguồn Kotlin theo 8 quy tắc đã được thống nhất cho dự án NekoMod.
Ranh giới đỏ tuyệt đối: **KHÔNG được thay đổi bất kỳ logic nghiệp vụ hay thuật toán nào**. Mọi đề xuất chỉ nhằm cải thiện visual rhythm, naming, và cấu trúc thị giác.

---

## I. Bộ 8 Quy Tắc Thẩm Mỹ (The Aesthetics Invariants)

---

### Quy Tắc 1 — Thân Hàm: Expression Body vs Block Body

**Nguyên tắc cốt lõi:**
```
Expression Body  =  "Hàm này LÀ một biểu thức"   → dùng =
Block Body       =  "Hàm này THỰC HIỆN các bước"  → dùng { }
```

**Bắt buộc dùng Expression Body khi:**

```kotlin
// ✅ Hàm trả về giá trị với 1 biểu thức duy nhất
fun minSize(orientation: Orientation): Float = when (orientation) {
    Orientation.HORIZONTAL -> minWidth
    Orientation.VERTICAL   -> minHeight
}

// ✅ Delegation thuần — hàm là alias cho hàm khác
fun addChild(child: LayoutNode) = addChild(child, _children.size)
fun removePointerInputFilter(filter: SuspendingPointerInputFilter): Boolean =
    _pointerInputFilters.remove(filter)
```

**Cấm tuyệt đối — Block Body bọc Return Đơn:**

```kotlin
// ❌ Anti-pattern: block body chỉ để wrap một return
fun removeChild(child: LayoutNode): Boolean {
    return _children.remove(child)   // ← bọc thừa
}

// ✅ Đúng: expression body trực tiếp
fun removeChild(child: LayoutNode): Boolean = _children.remove(child)
```

**Dùng Block Body khi:**
- Hàm có ≥2 câu lệnh / statements
- Hàm Unit-returning thể hiện side-effect có ý nghĩa riêng (`fun requestLayout() { isLayoutDirty = true }`)
- Có guard clauses (early return) trước khi đến biểu thức chính

**Chấm điểm:**
- 🔴 CRITICAL: Block body bọc `return expr` đơn trên hàm trả về giá trị
- 🟡 WARNING: `when`/`if-else` cuối hàm dùng statement thay vì expression body
- 🟡 WARNING: Delegation 1-1 vẫn dùng block body

---

### Quy Tắc 2 — `when` và `if-else` Là Biểu Thức, Không Phải Câu Lệnh

```kotlin
// ❌ Sai: return + block body + when statement
fun sizeFlag(orientation: Orientation): SizeFlag {
    return when (orientation) {
        Orientation.HORIZONTAL -> sizeFlagHorizontal
        Orientation.VERTICAL   -> sizeFlagVertical
    }
}

// ✅ Đúng: when là expression body
fun sizeFlag(orientation: Orientation): SizeFlag = when (orientation) {
    Orientation.HORIZONTAL -> sizeFlagHorizontal
    Orientation.VERTICAL   -> sizeFlagVertical
}
```

Đặc biệt: `if/else` lẻ tẻ trong các hàm chiếu trục phải được chuẩn hóa thành `when (orientation)` để nhất quán với toàn codebase:

```kotlin
// ❌ Không nhất quán — codebase còn lại dùng when
fun setContentSize(orientation: Orientation, main: Float, cross: Float) {
    if (orientation == Orientation.HORIZONTAL) { ... } else { ... }
}

// ✅ Nhất quán với pattern toàn dự án
fun setContentSize(orientation: Orientation, main: Float, cross: Float) {
    when (orientation) {
        Orientation.HORIZONTAL -> { contentWidth = main; contentHeight = cross }
        Orientation.VERTICAL   -> { contentWidth = cross; contentHeight = main }
    }
}
```

**Chấm điểm:**
- 🟡 WARNING: `if (orientation == X) ... else ...` thay vì `when (orientation)` trong bất kỳ hàm chiếu trục nào

---

### Quy Tắc 3 — Dòng Trống Là Dấu Ngắt Đoạn Ngữ Nghĩa

Mỗi cụm code giữa 2 dòng trống phải đại diện cho **đúng một pha chức năng**.
Đọc như văn xuôi: mỗi "đoạn" chỉ nói về 1 ý.

```kotlin
// ❌ Bức tường code — không biết pha nào làm gì
open fun dispose() {
    inputDispatcher.cancelAllActivePointers(System.currentTimeMillis())
    disposeNodeRecursive(rootLayoutNode)
    try { composition?.dispose() } catch (t: Throwable) { ... }
    composition = null
    inputDispatcher.dispose()
    lastLayoutW = -1f
    lastLayoutH = -1f
    rootLayoutNode.clearChildren()
}

// ✅ 3 pha rõ ràng, mỗi đoạn 1 trách nhiệm
open fun dispose() {
    // Phase 1: Cancel active interactions before tearing down
    inputDispatcher.cancelAllActivePointers(System.currentTimeMillis())
    disposeNodeRecursive(rootLayoutNode)

    // Phase 2: Dispose the Compose Runtime composition
    try { composition?.dispose() } catch (t: Throwable) { ... }
    composition = null

    // Phase 3: Reset dispatcher and layout bookkeeping
    inputDispatcher.dispose()
    lastLayoutW = -1f
    lastLayoutH = -1f
    rootLayoutNode.clearChildren()
}
```

**Quy tắc dòng trống:**
| Vị trí | Số dòng trống |
|---|---|
| Giữa top-level function/class | 2 dòng |
| Giữa các pha logic trong một hàm | 1 dòng |
| Giữa các câu lệnh cùng 1 pha | 0 dòng |
| Guard clause và thân hàm chính | 0 dòng (guard phải liền với code nó bảo vệ) |

**Chấm điểm:**
- 🔴 CRITICAL: Hàm >20 dòng không có dòng trống phân đoạn nào (bức tường code)
- 🟡 WARNING: Pha logic bị phân tách sai (dòng trống ở giữa pha thay vì giữa các pha)

---

### Quy Tắc 4 — Named Arguments Tại Call Site

Với các constructor/hàm có ≥3 tham số nguyên thủy cùng kiểu, bắt buộc dùng named arguments:

```kotlin
// ❌ Không thể đọc không cần tra signature
LayoutNodeHit(h.node, h.absX, h.absY)
PointerInputChange(pointerId, uptime, pos, pressed, prevUptime, prevPos, prevPressed, consumed, type, button, scrollDelta)

// ✅ Tự tài liệu hóa
LayoutNodeHit(node = h.node, absX = h.absX, absY = h.absY)
PointerInputChange(
    id = pointerId,
    uptimeMillis = uptime,
    position = pos,
    pressed = pressed,
    // ...
)
```

**Ngoại lệ:** Các hàm có 1-2 tham số rõ nghĩa (`Offset(x, y)`, `IntSize(w, h)`) không bắt buộc.

**Chấm điểm:**
- 🟡 WARNING: Constructor/hàm với ≥3 tham số cùng kiểu gọi theo thứ tự vị trí

---

### Quy Tắc 5 — Tên Biến Kể Chuyện (Narrative Naming)

Tên biến phải mô tả **bản chất toán học hoặc vai trò ngữ nghĩa**, không phải "nó được tạo ra như thế nào":

```kotlin
// ❌ Tên kỹ thuật mơ hồ
private var scratchTempMain = FloatArray(32)   // "temp" là gì?
private var scratchIsFrozen = BooleanArray(32) // "frozen" của cái gì?

// ✅ Tên kể đúng câu chuyện
private var allocatedMainSlots = FloatArray(32) // slot kích thước đã phân bổ cho mỗi child
private var isSlotFrozen = BooleanArray(32)     // trạng thái đông băng của slot trong Flex cycle
```

**Checklist naming:**
- Tên có trả lời câu hỏi "cái này dùng để làm gì?" không?
- Tên có khác với cách nó được tạo ra ("temp", "scratch", "buf", "tmp") không?
- Tên có nhất quán với thuật ngữ toán học/domain đã dùng ở chỗ khác không?

**Chấm điểm:**
- 🟡 WARNING: Biến có tên kỹ thuật mơ hồ (`temp`, `buf`, `tmp`, `scratch` + tên mô tả triển khai thay vì ngữ nghĩa)

---

### Quy Tắc 6 — Local Functions Cho Logic Pha Con Chặt Kết

Dùng local function khi helper:
1. Chỉ có nghĩa bên trong hàm cha cụ thể này
2. Cần truy cập nhiều (≥3) biến cục bộ / fields của hàm cha
3. Có thể đặt tên ngắn gọn phản ánh rõ pha chức năng

```kotlin
// ✅ Local functions biến orchestration thành pseudocode đọc được
override fun arrangeChildren(node: LayoutNode, ...) {
    ensureCapacity(node.children.size)

    fun collectMetrics(): Pair<Float, Float> { /* 20 dòng, đóng gói Pass 1 */ }
    fun redistributeFreeSpace(freeSpace: Float, totalRatio: Float) { /* đóng gói while loop */ }
    fun positionChildren(startMain: Float, startCross: Float) { /* đóng gói Pass 2 */ }

    // Orchestration — đọc như pseudocode
    val (totalMinMain, totalStretchRatio) = collectMetrics()
    val freeSpace = maxOf(0f, safeAvailableMain - totalMinMain - gapsTotal)
    if (freeSpace > 0f && totalStretchRatio > 0f) {
        redistributeFreeSpace(freeSpace, totalStretchRatio)
    }
    positionChildren(startMain = orientation.main(innerX, innerY), startCross = orientation.cross(innerX, innerY))
}
```

**Ràng buộc Zero-GC:** Local functions chỉ được capture `val` locals và class fields. Không capture `var` locals trong hot-path (tạo `Ref<T>` object).

**Khi KHÔNG dùng local function:**
- Helper có thể reuse ở nhiều hàm → `private fun`
- Helper cần unit test riêng → `internal fun`
- Hàm cha đã ngắn (<20 dòng) → inline trực tiếp
- Local function body >30 dòng → quá lớn, promote thành `private fun`

**Chấm điểm:**
- 🟡 WARNING: Hàm >80 dòng chứa các pha rõ ràng mà không dùng local functions hay private helpers để phân rã orchestration

---

### Quy Tắc 7 — Section Banners Cho File Dài

File >200 dòng bắt buộc có section banners để phân khu thị giác:

```kotlin
// ─────────────────────────────────────────────────────────────────────────
// 1. INTRINSIC CONSTRAINTS & GATEWAY SANITIZATION
// ─────────────────────────────────────────────────────────────────────────
```

**Quy tắc áp dụng:**
- File >200 dòng: bắt buộc có banners
- File 50–200 dòng: comment phân khu đơn giản là đủ (`// --- Section Name ---`)
- File <50 dòng: không cần banners

**Nội dung banner phải phản ánh đúng trách nhiệm của phần đó**, không phải tên kỹ thuật:
```kotlin
// ❌ Tên kỹ thuật
// === FIELDS ===

// ✅ Tên trách nhiệm
// ─── Visual Tokens (Background, Text, Alpha, Shape) ───────────────────
```

**Chấm điểm:**
- 🟡 WARNING: File >200 dòng không có bất kỳ section separator nào
- 🟢 GOOD: File có section banners rõ ràng với tên trách nhiệm

---

### Quy Tắc 8 — ASCII Architecture Diagrams Cho Thuật Toán Phức Tạp

Bất kỳ thuật toán nào cần >10 dòng để giải thích bằng văn xuôi đều cần ASCII diagram:

```kotlin
/**
 * Thuật toán Flex Clamping Redistribution:
 *
 *  freeSpace
 *  │
 *  ├── Chia đều theo stretchRatio cho unfrozen children
 *  │       │
 *  │       ├── tentative > maxSize? → Đóng băng (freeze) child đó
 *  │       │       │                   trả lại phần dư vào freeSpace
 *  │       │       └── Lặp lại với unfrozen còn lại
 *  │       │
 *  │       └── Không có child nào bị kẹp → Phân bổ cuối (Remainder Absorption)
 *  │               └── Child cuối nhận toàn bộ phần dư float precision
 *  │
 *  └── freeSpace == 0 → Skip redistribution
 */
```

**Vị trí đặt diagram:**
- Đặt trong KDoc của class/function chứa thuật toán
- Không đặt inline trong thân hàm (dùng comment pha thay thế)

**Khi nào cần diagram:**
- Thuật toán multi-pass (Flex, 3-pass event dispatch)
- Coordinate space conversion (Berlin Wall Y-axis)
- State machine (Frozen/Unfrozen cycle, Hover Enter/Exit)

**Chấm điểm:**
- 🟡 WARNING: Thuật toán multi-pass hoặc state machine không có diagram minh họa
- 🟢 GOOD: Diagram chính xác, phản ánh đúng implementation

---

## II. Quy Trình Rà Soát (Aesthetics Review Runbook)

### Bước 1: Đọc Toàn Bộ File, Không Chỉ Diff
Mở file đầy đủ. Diff có thể bỏ qua những đoạn code cũ vẫn còn xấu. Đánh giá toàn bộ file trong context của task hiện tại.

### Bước 2: Scan Function Bodies
Với mỗi function:
1. ≥1 biểu thức trả về giá trị → Kiểm tra có dùng expression body không (Quy Tắc 1)
2. `when`/`if-else` cuối hàm → Có đang dùng dạng statement thay vì expression không (Quy Tắc 2)
3. Dài >20 dòng → Có dòng trống phân đoạn đúng pha không (Quy Tắc 3)
4. Dài >80 dòng → Có thể dùng local functions để phân rã orchestration không (Quy Tắc 6)

### Bước 3: Scan Call Sites
- Constructor/hàm ≥3 tham số cùng kiểu → Named arguments (Quy Tắc 4)

### Bước 4: Scan Naming
- Tên biến có `temp`, `buf`, `tmp`, `scratch` kèm tên mô tả triển khai → Cần đổi tên (Quy Tắc 5)

### Bước 5: Kiểm Tra Cấu Trúc File
- Độ dài file → Section banners nếu >200 dòng (Quy Tắc 7)
- Có thuật toán multi-pass không → ASCII diagram (Quy Tắc 8)

### Bước 6: Xác Nhận Không Động Chạm Logic
**NGHIÊM CẤM** đề xuất bất kỳ thay đổi nào:
- Thay đổi giá trị tính toán, thứ tự phép toán
- Thêm/bỏ điều kiện logic
- Thay đổi kiểu trả về hay kiểu tham số
- Bất kỳ điều gì có thể ảnh hưởng đến kết quả test

---

## III. Format Báo Cáo Chuẩn

```markdown
## Code Aesthetics Review — [Tên File/Module]

### Score: X/10

### Findings:
1. 🔴 [CRITICAL] Block body bọc return đơn — LayoutNode.kt:349 — Vi phạm Quy Tắc 1
2. 🟡 [WARNING] resetModifierState() không có dòng trống phân đoạn — LayoutNode.kt:271-318 — Vi phạm Quy Tắc 3
3. 🟡 [WARNING] scratchTempMain tên mơ hồ — FlexLayoutPolicy.kt:67 — Vi phạm Quy Tắc 5
4. 🟢 [GOOD] FlexLayoutPolicy.computeMinSize() dùng expression body đúng chuẩn
5. 💡 [SUGGESTION] arrangeChildren() có thể dùng local functions để orchestration đọc rõ hơn

### Summary:
1-2 câu tóm tắt thực trạng thẩm mỹ của file.

### Recommendation: APPROVE | NEEDS_WORK | REJECT
```

**Bảng quy tắc chấm điểm:**
| Loại | Trừ điểm | Điều kiện |
|---|---|---|
| 🔴 CRITICAL | -2 | Block body bọc return đơn; Bức tường code >20 dòng 0 dòng trống |
| 🟡 WARNING | -1 | if/else thay vì when; Tên mờ; Thiếu named args; Thiếu section banner; Thiếu diagram |
| 🟢 GOOD | 0 (ghi nhận) | Pattern đẹp đáng làm gương |
| 💡 SUGGESTION | 0 | Cải thiện không bắt buộc |

Điểm khởi đầu: **10**. Tối thiểu: **0**.
