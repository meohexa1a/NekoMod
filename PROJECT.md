# NekoMod v3 — Project Specification (After-Spec)

UI framework khai báo cho Mindustry mod. Compose Runtime (`compose.runtime` only, **KHÔNG** `compose.ui`/`compose.foundation`/Skia) + Layout Engine tự viết + SDF shader rendering trên Arc Scene2D. Chạy trên Desktop + Android.

> Tài liệu này mô tả **code đang có**, không phải code mơ ước. Cập nhật cùng lúc khi code thay đổi.

---

## Quy Tắc Cho Agent

1. **🔒 Module Stable = Cấm sửa không kế hoạch.** Phải có kế hoạch approved trước khi chạm vào bất kỳ file nào đánh dấu 🟢 Stable. Kế hoạch phải nêu rõ: sửa gì, tại sao, ảnh hưởng gì.
2. **📝 After-Spec Driven.** Khi hoàn thành code mới hoặc sửa module, phải cập nhật lại mục tương ứng trong `PROJECT.md` (mục tiêu, ràng buộc, vấn đề đã biết).
3. **🧪 Test bắt buộc.** Mọi thay đổi phải chạy `.\gradlew.bat test --rerun-tasks` và pass toàn bộ 17 test files. Không được bỏ qua.
4. **📖 Đọc GEMINI.md trước khi viết code.** `GEMINI.md` chứa các bất biến lập trình bắt buộc (Zero-GC, Berlin Wall Y-axis, Gateway Sanitization, Entity Memory Hygiene...). Mọi dòng code phải tuân thủ.
5. **⚠️ Tốc độ sửa lỗi phải cẩn thận.** Sửa nhanh = phát sinh bug. Mỗi thay đổi phải verify kỹ.

---

## Kiến Trúc (As-Built)

```
Composable DSL (Box, Row, Column, Text, Button)
    │  @Composable functions + Modifier chain + Snapshot State
    ▼
LayoutNodeApplier (insertBottomUp O(N))
    │  Compose Runtime phát sinh mutations
    ▼
LayoutNode (Virtual DOM Entity, flat visual fields, pointer input filters)
    │  Policy-driven layout: BoxLayoutPolicy / FlexLayoutPolicy
    ▼
ComposeView (Arc Scene2D Element, Berlin Wall Y-flip)
    ├── NodeRenderer → UIBatch → UberShader → GPU (SDF rounded box)
    └── InputDispatcher → 3-pass pointer dispatch → Coroutine gestures
```

**Singleton:** `CompositionManager` quản lý 1 `Recomposer` + 1 `BroadcastFrameClock` chung cho tất cả `ComposeView`.

**Tọa độ:** 100% Top-Left Y-down bên trong engine. Chỉ lật Y tại 2 điểm biên trong `ComposeView` (input vào, draw ra).

---

## Module Registry

---

### Layout Engine — 🟢 Stable

**Files:**
- `core/layout/LayoutNode.kt` — Virtual DOM Entity, active self-validating, gateway sanitization
- `core/layout/LayoutPolicy.kt` — Strategy interface (2 implementations)
- `core/layout/policies/BoxLayoutPolicy.kt` — Stack/overlay layout solver
- `core/layout/policies/FlexLayoutPolicy.kt` — Row/Column solver, symmetric via `Orientation`
- `core/layout/SizeFlag.kt` — `SizeFlag`, `Orientation`, `Alignment` enums
- `core/layout/Anchor.kt` — Anchor system (port từ Godot Control)

**Mục tiêu:** Giải bài toán bố cục hình học thuần toán cho cây Virtual DOM. Không biết gì về rendering hay input.

**Ràng buộc:**
- Zero-GC trong `layout()`, `arrangeContent()`, `computeMinSize()`
- Gateway Sanitization tại mọi setter (0 ≤ min ≤ max, loại bỏ NaN)
- Tree integrity: chặn cycle, đồng bộ parent pointer, read-only `children`
- Top-Left Y-down 100%

**Sửa đổi:** Kế hoạch approved + test pass + không phá vỡ 10 test files layout.

**Vấn đề đã biết:**
- `SizeFlag.FILL` ngữ nghĩa mâu thuẫn: trên main axis của Flex, `FILL` hành xử giống `SHRINK` (vi phạm GEMINI.md Rule 0.2)
- `AnchorData` cấp phát cho mọi node dù 99% không dùng anchor
- `AnchorPreset` có 14 entries, chỉ dùng 4 (`TOP_RIGHT`, `BOTTOM_RIGHT`, `CENTER`, `FULL_RECT`)
- `LayoutNode` khai báo `open class` nhưng 0 subclass — nên là `class`

**Tests:** `AnchorResolutionTest`, `ArchitectureInvariantsTest`, `EdgeCaseLayoutTest`, `ExtremeBoundaryTest`, `FlexLayoutTest`, `FlexRedistributionTest`, `LayoutNodeMutationTest`, `NestedLayoutTest`, `TripleReversalAdversarialTest`, `ZeroGcStressTest`

---

### Graphics Pipeline — 🟢 Stable

**Files:**
- `core/graphics/UIBatch.kt` — GPU batcher, clip stack, Direct NIO buffer
- `core/graphics/UberShader.kt` — SDF shader compilation & uniform management
- `core/graphics/Shape.kt` — Shape definitions cho bo góc
- `src/main/resources/shaders/uber_ui.vert` — Vertex shader
- `src/main/resources/shaders/uber_ui.frag` — Fragment shader (SDF Inigo Quilez, fwidth AA, analytical border)
- `src/main/resources/shaders/blur.vert`, `blur.frag` — Gaussian blur

**Mục tiêu:** Render cây LayoutNode ra GPU qua SDF shader. Bo góc, viền, shadow, clip — tất cả giải tích trong fragment shader, 1 draw call.

**Ràng buộc:**
- Zero-GC trong draw loop
- Không gọi `ScissorStack` (dùng analytical clip trong shader)
- Batch state hygiene: `Draw.reset()` trong `finally` sau mỗi draw pass

**Sửa đổi:** Kế hoạch approved + `SdfMathVerificationTest` và `UIBatchTest` pass.

**Vấn đề đã biết:**
- `Shape` là `sealed interface` thừa — SDF shader chỉ hiểu 4 float bán kính góc, không bao giờ có SVG/custom path. Nên là `data class RoundedCornerShape` đơn giản + `val RectangleShape` + `val CircleShape`
- 3 khối `when(shape)` copy-paste trong `ModifierVisual`, `ModifierBorder`, `ModifierShape`

---

### Compose Core — 🟢 Stable

**Files:**
- `core/compose/CompositionManager.kt` — Singleton: 1 Recomposer + 1 BroadcastFrameClock + CoroutineScope (`Dispatchers.Unconfined`)
- `core/compose/LayoutNodeApplier.kt` — `AbstractApplier<LayoutNode>`, `insertBottomUp` O(N), onNodeRemoved cleanup
- `core/compose/Modifier.kt` — Modifier chain: `Modifier`, `Modifier.Element`, `CombinedModifier`, `then()`

**Mục tiêu:** Cầu nối Compose Runtime ↔ LayoutNode tree. Quản lý lifecycle Recomposer.

**Ràng buộc:**
- `CompositionManager` là singleton toàn cục, sống vĩnh viễn
- `insertBottomUp` bắt buộc (không dùng `insertTopDown` — gây O(N²))
- `LayoutNodeApplier.onNodeRemovedCallback` phải dọn pointer input filters

**Sửa đổi:** Kế hoạch approved + `ComposeLifecycleTest` và `ArchitectureEnhancementsTest` pass.

**Vấn đề đã biết:**
- `Modifier.foldIn`, `foldOut`, `any`, `all` — dead code, 0 callers. Nên xóa.

---

### ComposeView (Mount-Point) — 🟢 Stable

**Files:**
- `core/compose/view/ComposeView.kt` — Arc `Element`, Berlin Wall, `setScene`/`act`/`draw`/`hit`, `Table.compose` DSL
- `core/compose/view/InputDispatcher.kt` — 3-pass pointer event dispatch (Initial→Main→Final), hitTestChain, hover tracking
- `core/compose/view/NodeRenderer.kt` — Recursive tree renderer, alpha cascading

**Mục tiêu:** Cửa khẩu duy nhất giữa Arc Scene2D và Neko Engine. Xử lý lifecycle, input routing, rendering.

**Ràng buộc:**
- Berlin Wall: chỉ lật Y tại 2 điểm biên (input vào: `composeY = height - arcY`, draw ra: `arcY = height - (nodeY + nodeH)`)
- `dispose()` phải dọn sạch: pointer filters, composition, tracked pointers
- `draw()` phải gọi `Draw.reset()` trong `finally`
- Zero-GC trong `act()` và `draw()`

**Sửa đổi:** Kế hoạch approved + full test suite pass.

**Vấn đề đã biết:**
- `InputDispatcher`: `ArrayList<IntSize>` cho `boundsScratch` gây boxing mỗi frame (vi phạm Zero-GC). Nên thay bằng `FloatArray` scratchpad.
- `ComposeView` là `open class` — cần không?

---

### Pointer Input — 🟢 Stable

**Files:**
- `core/compose/input/PointerEvent.kt` — Data contracts: `Offset`, `IntSize`, `PointerId`, `PointerInputChange`, `PointerEvent`, enums
- `core/compose/input/PointerInputScope.kt` — `AwaitPointerEventScope`, `SuspendingPointerInputFilter`, `ViewConfiguration`
- `core/compose/input/InteractionSource.kt` — `PressInteraction`, `HoverInteraction`, `MutableInteractionSource`
- `core/compose/input/ModifierPointerInput.kt` — `Modifier.pointerInput()`, `Modifier.clickable()`, `Modifier.hoverable()`
- `core/compose/input/gestures/TapGestureDetector.kt` — `detectTapGestures` (tap, double-tap, long-press)
- `core/compose/input/gestures/DragGestureDetector.kt` — `detectVerticalDragGestures`, `detectHorizontalDragGestures`
- `core/compose/input/gestures/ForEachGesture.kt` — `forEachGesture`, `awaitAllPointersUp`

**Mục tiêu:** Port có chọn lọc bộ máy pointer input từ Jetpack Compose AOSP. Coroutine-based gesture recognition, không dùng ClickListener của Arc.

**Ràng buộc:**
- Không dùng `ClickListener` của Arc (rò rỉ bộ nhớ, click ma)
- Không kéo dependency `compose.ui` / `compose.foundation`
- Mọi tương tác qua `InteractionSource` flow, không gán trực tiếp `node.isPressed`
- Distance squared cho touch slop (không dùng `sqrt`)

**Sửa đổi:** Kế hoạch approved + `PointerInputTest` và `PointerInputAdversarialTest` pass.

**Vấn đề đã biết:**
- `ConsumedData` — wrapper vô nghĩa quanh 1 boolean, tạo heap object mỗi `PointerInputChange`
- `PointerId(value: Long)` — Arc pointer là `Int`, bọc thành `Long` rồi boxing khi nullable
- `ViewConfiguration` interface — chỉ 1 implementation (`DefaultViewConfiguration`), nên là object
- `InteractionSource` + `MutableInteractionSource` — 2 interfaces cho 1 implementation (nhưng có lý do tách read/write)
- `Offset` boxing khi truyền vào lambda callbacks (chấp nhận — cold-path)

---

### Modifiers — 🟢 Stable

**Files:**
- `core/compose/modifier/ModifierPadding.kt` — `Modifier.padding()`
- `core/compose/modifier/ModifierMargin.kt` — `Modifier.margin()`
- `core/compose/modifier/ModifierSize.kt` — `Modifier.size()`, `.width()`, `.height()`, `.fillMaxWidth()`, `.fillMaxHeight()`, `.fillMaxSize()`, `.widthIn()`, `.heightIn()`, `.weight()`, `.wrapContentWidth()`, `.wrapContentHeight()`
- `core/compose/modifier/ModifierVisual.kt` — `Modifier.background()`, `.alpha()`, `.visible()`
- `core/compose/modifier/ModifierBorder.kt` — `Modifier.border()`
- `core/compose/modifier/ModifierClip.kt` — `Modifier.clip()`, `.clipToBounds()`, `.cornerRadius()`
- `core/compose/modifier/ModifierText.kt` — `Modifier.textColor()`, `.font()`
- `core/compose/modifier/ModifierScroll.kt` — `Modifier.verticalScroll()`, `.horizontalScroll()` (hỗ trợ cuộn chuột chuẩn xác và quán tính Fling)
- `core/compose/modifier/ModifierLayout.kt` — `RowScope`, `ColumnScope`, `BoxScope` + scoped modifiers

**Mục tiêu:** Extension functions trên `Modifier` trả về `Modifier.Element` data classes. Mỗi element apply vào `LayoutNode` qua `applyTo()`.

**Ràng buộc:**
- Mọi `Modifier.Element` phải là `data class` (structural equality cho Compose diffing — GEMINI.md Rule 14)
- Cấm anonymous objects
- Scoped modifiers (`weight`, `align`) chỉ trong Scope tương ứng (GEMINI.md Rule 12)

**Sửa đổi:** Thêm modifier mới tự do. Sửa modifier hiện tại cần test pass.

**Vấn đề đã biết:**
- `PaddingModifier`, `MarginModifier` là `private` — cản trở NekoUI.kt re-export
- `RowScope`/`ColumnScope`/`BoxScope` là interface + object instance — có thể gộp thành object trực tiếp

---

### Foundation — 🟢 Stable

**Files:**
- `core/compose/foundation/ScrollState.kt` — Quản lý trạng thái cuộn qua Snapshot State, trang bị `snapTo()`, `dispatchRawDelta()`, `animateScrollTo()` (Cubic Ease-Out), `animateScrollBy()`
- `core/compose/input/gestures/VelocityTracker.kt` — Bộ theo dõi vận tốc cử chỉ Least Squares 1D mảng phẳng Zero-GC, chống giật quán tính fling

**Mục tiêu:** Quản lý trạng thái cuộn qua Compose Snapshot State. Tự động kẹp biên trần khi maxValue thay đổi (Rule 1.1 Active Entity).

---

### Primitives — 🟢 Stable

**Files:**
- `core/compose/primitive/Box.kt` — `@Composable fun Box(modifier, contentAlignment, content)`
- `core/compose/primitive/Row.kt` — `@Composable fun Row(modifier, gap, content: RowScope.() -> Unit)`
- `core/compose/primitive/Column.kt` — `@Composable fun Column(modifier, gap, content: ColumnScope.() -> Unit)`
- `core/compose/primitive/Text.kt` — `@Composable fun Text(text, modifier, color, font)`

**Mục tiêu:** Composable cơ bản tạo `LayoutNode` với policy tương ứng.

**Sửa đổi:** Thêm primitive mới tự do. Sửa API hiện tại cần kế hoạch (breaking change cho callers).

---

### Foundation — 🟡 WIP

**Files:**
- `core/compose/foundation/ScrollState.kt` — Scroll state management

**Mục tiêu:** Quản lý trạng thái cuộn qua Compose Snapshot State.

**Vấn đề đã biết:**
- `scrollTo()` là `suspend` nhưng body đồng bộ 100% — chữ ký nói dối
- Không có `animateScrollTo()`, fling/inertia, overscroll
- `interactionSource` khởi tạo `MutableSharedFlow` nhưng 0 callers đọc/ghi — phantom property
- `LayoutNode.scrollState` proxy — 0 callers, dead code

---

### UI Layer — 🟡 WIP

**Files:**
- `ui/NekoUI.kt` — Facade: typealiases + composable re-exports + `Table.compose`
- `ui/components/Button.kt` — `@Composable fun Button(onClick, modifier, interactionSource, content)`

**Mục tiêu:** Convenience layer cho end-users. Import `org.hubdustry.ui.*` để dùng nhanh.

**Vấn đề đã biết:**
- Modifier extension functions không thể re-export qua facade (Kotlin language limitation). Users phải thêm `import org.hubdustry.core.compose.modifier.*`
- Cần fix hybrid: giữ facade cho composables + types, chấp nhận import riêng cho modifiers

---

### Entry Point & Sample — ⬜ Utility

**Files:**
- `NekoMod.kt` — Mod entry point, gọi `SampleComposeDialog().show()` khi game load
- `sample/SampleComposeDialog.kt` — Demo dialog, showcase API

---

## Vấn Đề Thiết Kế Đã Biết (Tổng Hợp)

| # | Vấn đề | Mức độ | Module | Hướng sửa |
|:--|:---|:---|:---|:---|
| 1 | `SizeFlag.FILL` nói dối ngữ nghĩa trên main axis | 🔴 Semantic | Layout | Gộp `FILL`/`EXPAND` hoặc sửa FlexLayoutPolicy |
| 2 | `Shape` sealed interface thừa | 🟡 Over-abstraction | Graphics | Chuyển thành `data class` + `val` constants |
| 3 | `ConsumedData` wrapper 1 boolean | 🔴 Hot-path GC | Pointer Input | Inline thành field trên `PointerInputChange` |
| 4 | `IntSize` boxing trong `ArrayList` mỗi frame | 🔴 Hot-path GC | ComposeView | Thay bằng `FloatArray` scratchpad |
| 5 | `PointerId` boxing khi nullable | 🟡 GC | Pointer Input | Dùng `Int` primitive hoặc cấm nullable |
| 6 | `Modifier.foldOut/any/all` dead code | 🟡 Cruft | Compose Core | Xóa |
| 7 | `ScrollState.interactionSource` phantom | 🟡 Cruft | Foundation | Xóa |
| 8 | `scrollTo` suspend giả | 🟡 Semantic | Foundation | Bỏ `suspend` hoặc thêm animation thật |
| 9 | `when(shape)` copy-paste 3 chỗ | 🟡 DRY | Modifiers | Refactor sau khi Shape thành data class |
| 10 | `NekoUI.kt` modifier re-export vỡ | 🟡 API | UI Layer | Fix hybrid |
| 11 | `AnchorData` cấp phát mọi node | ⚪ Minor | Layout | Lazy init hoặc nullable |
| 12 | `LayoutNode` / `ComposeView` là `open` nhưng 0 subclass | ⚪ Minor | Layout/View | Bỏ `open` |

---

## Backlog (Tính Năng Tương Lai)

Ý tưởng đáng giữ. Triển khai khi cần, không phải bây giờ.

| Tính năng | Mô tả ngắn | Khi nào cần |
|:---|:---|:---|
| DirtyFlags bitmask 32-bit | Granular dirty tracking (MEASURE/LAYOUT/DRAW/HIERARCHY) thay cho boolean | Khi >500 nodes và layout chậm |
| Tier 2 Arc Native Fallback | `NinePatch` + `Lines` + `ScissorStack` cho GPU cổ | Khi có thiết bị không chạy được SDF shader |
| Analytical Rounded Scissor | SDF clip cho container bo góc lồng nhau | Khi UI có ảnh sát mép container tròn |
| ScrollState animation | `animateScrollTo()`, fling decay, overscroll spring | Khi cần cuộn mượt |
| IME tiếng Việt | `InputConnectionBridge`, NFC normalization cho EVKey/Unikey | Khi cần TextField |
| MindustryDispatcher | CoroutineDispatcher kiểm tra GL thread thay cho `Unconfined` | Khi có bug đa luồng |

---

## Test Suite

17 test files, chạy bằng `.\gradlew.bat test --rerun-tasks`:

```
core/layout/          → 10 files (layout, anchor, flex, mutation, stress, adversarial)
core/compose/         → 2 files (lifecycle, architecture enhancements)
core/compose/input/   → 2 files (pointer input, adversarial)
core/compose/foundation/ → 1 file (scroll)
core/graphics/        → 2 files (SDF math, UIBatch)
```

---

## Cấu Trúc Package

```
src/main/kotlin/org/hubdustry/
├── NekoMod.kt                          # Mod entry point
├── core/
│   ├── compose/
│   │   ├── CompositionManager.kt       # Singleton Recomposer
│   │   ├── LayoutNodeApplier.kt        # Compose → LayoutNode bridge
│   │   ├── Modifier.kt                 # Modifier chain
│   │   ├── foundation/
│   │   │   └── ScrollState.kt
│   │   ├── input/
│   │   │   ├── InteractionSource.kt
│   │   │   ├── ModifierPointerInput.kt
│   │   │   ├── PointerEvent.kt
│   │   │   ├── PointerInputScope.kt
│   │   │   └── gestures/
│   │   │       ├── DragGestureDetector.kt
│   │   │       ├── ForEachGesture.kt
│   │   │       └── TapGestureDetector.kt
│   │   ├── modifier/
│   │   │   ├── ModifierBorder.kt
│   │   │   ├── ModifierLayout.kt
│   │   │   ├── ModifierMargin.kt
│   │   │   ├── ModifierPadding.kt
│   │   │   ├── ModifierScroll.kt
│   │   │   ├── ModifierShape.kt
│   │   │   ├── ModifierSize.kt
│   │   │   ├── ModifierText.kt
│   │   │   └── ModifierVisual.kt
│   │   ├── primitive/
│   │   │   ├── Box.kt
│   │   │   ├── Column.kt
│   │   │   ├── Row.kt
│   │   │   └── Text.kt
│   │   └── view/
│   │       ├── ComposeView.kt
│   │       ├── InputDispatcher.kt
│   │       └── NodeRenderer.kt
│   ├── graphics/
│   │   ├── Shape.kt
│   │   ├── UIBatch.kt
│   │   └── UberShader.kt
│   └── layout/
│       ├── Anchor.kt
│       ├── LayoutNode.kt
│       ├── LayoutPolicy.kt
│       ├── SizeFlag.kt
│       └── policies/
│           ├── BoxLayoutPolicy.kt
│           └── FlexLayoutPolicy.kt
├── sample/
│   └── SampleComposeDialog.kt
└── ui/
    ├── NekoUI.kt
    └── components/
        └── Button.kt
```
