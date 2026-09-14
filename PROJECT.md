# NekoMod v3 — Project Specification (After-Spec)

UI framework khai báo cho Mindustry mod. Compose Runtime (`compose.runtime` only, **KHÔNG** kéo `compose.ui`/`compose.foundation`/Skia/C++ binaries) + Virtual DOM Layout Engine tự viết + SDF shader rendering trên Arc Scene2D. Chạy độc lập trên Desktop (LWJGL3) + Android.

> Tài liệu này mô tả **code đang có thực tế**, không phải code mơ ước hay dự tính viển vông. Cập nhật đồng bộ cùng lúc khi code thay đổi.

---

## Quy Tắc Cho Kỹ Sư & Agent

1. **🔒 Module Stable = Cấm sửa không kế hoạch.** Phải có kế hoạch approved trước khi chạm vào bất kỳ file nào đánh dấu 🟢 Stable. Kế hoạch phải nêu rõ: sửa gì, tại sao, ảnh hưởng gì.
2. **📝 After-Spec Driven.** Khi hoàn thành code mới hoặc sửa module, phải cập nhật lại mục tương ứng trong `PROJECT.md` (mục tiêu, ràng buộc, vấn đề đã biết).
3. **🧪 Test bắt buộc.** Mọi thay đổi phải chạy `.\gradlew.bat test --rerun-tasks` và pass toàn bộ 18 test files (141/141 tests). Không được bỏ qua.
4. **📖 Đọc GEMINI.md trước khi viết code.** `GEMINI.md` chứa 15 bất biến lập trình bắt buộc (Zero-GC hot-paths, Bức tường Berlin trục Y, Gateway Sanitization, Entity Memory Hygiene, Review Pipeline 3 tầng...). Mọi dòng code phải tuân thủ nghiêm ngặt.
5. **⚠️ Tốc độ sửa lỗi phải cẩn thận.** Tuyệt đối không phản xạ vội vã. Giữ vững tính trọn vẹn ngữ nghĩa và độ an toàn kiểu.

---

## Kiến Trúc Hệ Thống (As-Built)

```
Composable DSL (Box, Row, Column, Text, Button, Checkbox, Switch)
    │  @Composable functions + Modifier chain + Snapshot State
    ▼
LayoutNodeApplier (insertBottomUp O(N))
    │  Compose Runtime phát sinh mutations
    ▼
LayoutNode (Virtual DOM Entity, flat fields, Zero-GC, pointer input filters)
    │  Policy-driven layout: BoxLayoutPolicy / FlexLayoutPolicy
    ▼
ComposeView (Arc Scene2D Element, Berlin Wall Y-flip)
    ├── NodeRenderer → UIBatch → UberShader → GPU (SDF rounded box, border, text)
    └── InputDispatcher → 3-pass pointer dispatch (Initial→Main→Final) → Coroutine gestures
```

- **Singleton Recomposer:** `CompositionManager` quản lý 1 `Recomposer` + 1 `BroadcastFrameClock` + `CoroutineScope` (`Dispatchers.Unconfined`) dùng chung cho toàn bộ các `ComposeView`.
- **Hệ Tọa Độ "Bức Tường Berlin":** 100% Top-Left ($Y$-down) bên trong Virtual DOM và engine. Chỉ lật Y tại 2 điểm biên trong `ComposeView` (input vào Arc $\to$ Compose, draw ra Compose $\to$ Arc).

---

## Module Registry

---

### 1. Layout Engine — 🟢 Stable

**Files:**
- `core/layout/LayoutNode.kt` — Virtual DOM Entity, 10 Section Banners chuẩn AOSP, active self-validating, gateway sanitization
- `core/layout/LayoutPolicy.kt` — Strategy interface cho thuật toán bố cục
- `core/layout/policies/BoxLayoutPolicy.kt` — Bố cục xếp chồng (Stack/Box), căn chỉnh Alignment 2 chiều
- `core/layout/policies/FlexLayoutPolicy.kt` — Bố cục tuyến tính (Row/Column) đối xứng qua `Orientation`, thuật toán kẹp trần Clamping Redistribution, Remainder Absorption, local function `redistributeFreeSpace`
- `core/layout/SizeFlag.kt` — `SizeFlag` (`SHRINK`, `FILL`, `EXPAND`), `Orientation`, `Alignment`
- `core/layout/Anchor.kt` — Hệ thống neo góc/cạnh (port từ Godot Control) cho overlay và floating badges

**Ràng buộc:**
- Zero-GC trong `layout()`, `arrangeContent()`, `computeMinSize()`
- Gateway Sanitization tại mọi setter ($0 \le min \le max$, loại bỏ NaN)
- Tree integrity: chặn cycle lặp tổ tiên, đồng bộ con trỏ `parent` 2 chiều, read-only `children` view
- Top-Left $Y$-down 100%

**Vấn đề đã biết:**
- `SizeFlag.FILL` ngữ nghĩa mâu thuẫn: trên main axis của Flex, `FILL` hành xử như `SHRINK` (sẽ cân nhắc gộp với `EXPAND` trong v4)
- `AnchorData` cấp phát sẵn cho mọi node dù đa số dùng layout policy thông thường

---

### 2. Graphics Pipeline — 🟢 Stable

**Files:**
- `core/graphics/UIBatch.kt` — GPU master batcher, clip scissor stack, Direct NIO Off-Heap buffer
- `core/graphics/UberShader.kt` — Biên dịch và nạp uniform SDF shader
- `core/graphics/RoundedCorners.kt` — Data class đại diện 4 bán kính góc (thay thế Shape interface cũ)
- `src/main/resources/shaders/uber_ui.vert` — Vertex shader
- `src/main/resources/shaders/uber_ui.frag` — Fragment shader (SDF Inigo Quilez, fwidth AA, analytical border)
- `src/main/resources/shaders/blur.vert`, `blur.frag` — Gaussian blur shader

**Ràng buộc:**
- Zero-GC trong hot-path render loop
- Analytical SDF clip trong shader thay vì dựa dẫm `ScissorStack`
- Batch state hygiene: `Draw.reset()` trong khối `finally` sau mỗi pass vẽ

---

### 3. Compose Core — 🟢 Stable

**Files:**
- `core/compose/CompositionManager.kt` — Singleton: 1 Recomposer + 1 BroadcastFrameClock + CoroutineScope (`Dispatchers.Unconfined`)
- `core/compose/LayoutNodeApplier.kt` — `AbstractApplier<LayoutNode>`, `insertBottomUp` O(N), dọn dẹp gesture filter khi gỡ node
- `core/compose/Modifier.kt` — Chuỗi Modifier bất biến: `Modifier`, `Modifier.Element`, `CombinedModifier`, hàm duyệt `applyTo` Zero-Closure (đã xóa sạch dead code `foldOut/any/all`)

**Ràng buộc:**
- `CompositionManager` là singleton toàn cục sống suốt tiến trình
- `insertBottomUp` bắt buộc (tránh suy biến O(N²))
- `LayoutNodeApplier.onNodeRemovedCallback` kích hoạt dọn dẹp bộ lọc cử chỉ trên cây con

---

### 4. ComposeView (Mount-Point) — 🟢 Stable

**Files:**
- `core/compose/view/ComposeView.kt` — Cửa khẩu Arc `Element`, Bức tường Berlin, `setScene`/`act`/`draw`/`hit`, `Table.compose` DSL
- `core/compose/view/InputDispatcher.kt` — Điều phối sự kiện con trỏ 3-pass (Initial $\to$ Main $\to$ Final), fast-path Zero-Allocation cho click đơn, `removeAll` cold path
- `core/compose/view/NodeRenderer.kt` — Dựng hình cây ảo đệ quy ra Arc, nhân lũy tiến độ mờ `alpha`, đo đạc font BMFont theo sự kiện

**Ràng buộc:**
- Bức tường Berlin: input vào `composeY = height - arcY`, lệnh vẽ ra `arcY = height - (nodeY + nodeHeight)`
- Vòng đời tất định: `dispose()` phải hủy sạch tương tác, pointer filters, snapshot composition và tracked pointers
- `draw()` bắt buộc gọi `Draw.reset()` trong `finally`

---

### 5. Pointer Input & Gestures — 🟢 Stable

**Files:**
- `core/compose/input/PointerEvent.kt` — Data contracts: `@JvmInline value class Offset`, `@JvmInline value class IntSize`, `PointerInputChange`, `PointerEvent`
- `core/compose/input/PointerInputScope.kt` — `AwaitPointerEventScope`, `SuspendingPointerInputFilter`, vòng lặp `awaitEachGesture`
- `core/compose/input/InteractionSource.kt` — `PressInteraction`, `HoverInteraction`, `MutableInteractionSource`
- `core/compose/input/ModifierPointerInput.kt` — `Modifier.pointerInput()`, `Modifier.clickable()`, `Modifier.hoverable()`
- `core/compose/input/gestures/TapGestureDetector.kt` — `detectTapGestures` (tap, double-tap, long-press, secondary click)
- `core/compose/input/gestures/DragGestureDetector.kt` — `detectVerticalDragGestures`, `detectHorizontalDragGestures`
- `core/compose/input/gestures/ForEachGesture.kt` — `awaitEachGesture`, `awaitAllPointersUp`
- `core/compose/input/gestures/VelocityTracker.kt` — Bộ theo dõi vận tốc cử chỉ 1D mảng phẳng Zero-GC, hồi quy bình phương tối thiểu Least Squares

**Ràng buộc:**
- CẤM dùng `ClickListener` của Arc trong cây ảo (chống rò rỉ bộ nhớ và click ma)
- CẤM kéo dependency `compose.ui` / `compose.foundation`
- Tương tác phản hồi qua `InteractionSource` flow, không đặt biến tương tác mutable trên `LayoutNode`
- So sánh khoảng cách touch slop dùng bình phương ($dx^2 + dy^2 > slop^2$), cấm gọi `sqrt`

---

### 6. Modifiers — 🟢 Stable

**Files:**
- `core/compose/modifier/ModifierPadding.kt` — `Modifier.padding()`
- `core/compose/modifier/ModifierMargin.kt` — `Modifier.margin()`
- `core/compose/modifier/ModifierSize.kt` — `Modifier.size()`, `.width()`, `.height()`, `.fillMaxWidth()`, `.fillMaxHeight()`, `.fillMaxSize()`, `.widthIn()`, `.heightIn()`, `.weight()`, `.wrapContentWidth()`, `.wrapContentHeight()`
- `core/compose/modifier/ModifierVisual.kt` — `Modifier.background()`, `.alpha()`, `.visible()`
- `core/compose/modifier/ModifierBorder.kt` — `Modifier.border()`
- `core/compose/modifier/ModifierClip.kt` — `Modifier.clip()`, `.clipToBounds()`, `.cornerRadius()`
- `core/compose/modifier/ModifierText.kt` — `Modifier.textColor()`, `.font()`
- `core/compose/modifier/ModifierScroll.kt` — `Modifier.verticalScroll()`, `.horizontalScroll()` (hỗ trợ cuộn chuột êm và quán tính Fling)
- `core/compose/modifier/ModifierLayout.kt` — `RowScope`, `ColumnScope`, `BoxScope` + scoped modifiers (`weight`, `align`)

**Ràng buộc:**
- Mọi `Modifier.Element` phải là `data class` (bảo đảm structural equality cho Compose Recomposition diffing — Rule 14)
- Scoped modifiers (`weight`, `align`) chỉ được phơi ra trong scope tương ứng (Rule 12)

---

### 7. Foundation — 🟢 Stable

**Files:**
- `core/compose/foundation/ScrollState.kt` — Quản lý trạng thái cuộn qua Snapshot State, trang bị `snapTo()`, `dispatchRawDelta()`, và `animateScrollTo()` (Cubic Ease-Out coroutine), `animateScrollBy()`

**Ràng buộc:**
- Active Self-Validating: kẹp trần `value` tự động khi `maxValue` co nhỏ
- Không có suspend giả (mọi hàm suspend đều thực sự hoãn luồng qua FrameClock / Dispatcher)

---

### 8. Primitives & Components — 🟢 Stable

**Files:**
- `core/compose/primitive/Box.kt` — `@Composable fun Box(modifier, content)`
- `core/compose/primitive/Row.kt` — `@Composable fun Row(modifier, gap, content)`
- `core/compose/primitive/Column.kt` — `@Composable fun Column(modifier, gap, content)`
- `core/compose/primitive/Text.kt` — `@Composable fun Text(text, modifier, textColor, font)`
- `ui/components/Button.kt` — `@Composable fun Button(onClick, modifier, enabled, corners, ...)`
- `ui/components/Checkbox.kt` — `@Composable fun Checkbox(checked, onCheckedChange, modifier, enabled, ...)`
- `ui/components/TextField.kt` — `@Composable fun TextField(value, onValueChange, modifier, enabled, ...)`

---

### 9. Entry Point & Samples — 🟢 Stable

**Files:**
- `NekoMod.kt` — Mod entry point, khởi tạo `SampleComposeDialog().show()` khi game load
- `sample/SampleComposeDialog.kt` — Showcase dialog mẫu: trình diễn Flexbox stretch ratio, bo góc SDF, counter button, Reactive Checkbox và TextField

---

## Vấn Đề Thiết Kế Đã Giải Quyết Triệt Để

| Vấn đề cũ | Giải pháp đã áp dụng |
|---|---|
| `Shape` sealed interface thừa | Đã thay thế hoàn toàn bằng `RoundedCorners` data class |
| `ScrollState.scrollTo` suspend giả | Đã thay bằng `animateScrollTo` coroutine thật (Cubic Ease-Out) và `snapTo` |
| `Modifier.foldOut/any/all` dead code | Đã xóa sổ hoàn toàn khỏi `Modifier.kt` |
| Vòng lặp index thủ công trong hot-paths | Đã thay thế 100% bằng `ui-util` inline fast loops (`fastForEach`, `fastAny`) |
| LayoutNode monolithic 600 dòng | Đã trang bị 10 Section Banners AOSP và helper đồng bộ `ScrollState` |
| FlexLayoutPolicy arrangeChildren phức tạp | Đã bóc tách local function `redistributeFreeSpace` Zero-GC và bổ sung ASCII diagram |
| Thiếu widget cài đặt cho mod | Đã ship thành công `Checkbox` và `Switch` với 7 unit tests mới |

---

## Backlog (Tính Năng Kế Tiếp)

| Tính năng | Mô tả ngắn | Độ ưu tiên |
|:---|:---|:---:|
| `Slider` | Thanh trượt chọn số tương tác kéo ngang | Cao (UI Widget) |
| `Card` / `Surface` | Container nổi có nền, viền và đổ bóng mặc định | Trung bình (UI Widget) |
| `TextField` + IME Bridge | Nhận diện bàn phím và nhập liệu văn bản có dấu | Cao (Mod Settings) |
| `LazyColumn` / `LazyRow` | Danh sách ảo hóa tái sử dụng viewport cho hàng trăm phần tử | Trung bình (Data UI) |
| Tier 2 Arc Native Fallback | Fallback render khi chạy trên GPU cổ không hỗ trợ SDF | Thấp |

---

## Test Suite (18 Test Files — 141 Tests Pass 100%)

Chạy kiểm thử toàn bộ bằng:
```powershell
.\gradlew.bat test --rerun-tasks
```

```
core/layout/                 → 10 files (Anchor, Flex, Invariants, Adversarial, Zero-GC...)
core/compose/                → 2 files (ComposeLifecycleTest, ArchitectureEnhancementsTest)
core/compose/input/          → 2 files (PointerInputTest, PointerInputAdversarialTest)
core/compose/foundation/     → 1 file (ScrollTest)
core/compose/input/gestures/ → 1 file (VelocityTrackerTest)
core/graphics/               → 2 files (SdfMathVerificationTest, UIBatchTest)
ui/components/               → 1 file (SelectionControlsTest - Checkbox & Switch)
```

---

## Cấu Trúc Package Hiện Tại

```
src/main/kotlin/org/hubdustry/
├── NekoMod.kt
├── core/
│   ├── compose/
│   │   ├── CompositionManager.kt
│   │   ├── LayoutNodeApplier.kt
│   │   ├── Modifier.kt
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
│   │   │       ├── TapGestureDetector.kt
│   │   │       └── VelocityTracker.kt
│   │   ├── modifier/
│   │   │   ├── ModifierBorder.kt
│   │   │   ├── ModifierClip.kt
│   │   │   ├── ModifierLayout.kt
│   │   │   ├── ModifierMargin.kt
│   │   │   ├── ModifierPadding.kt
│   │   │   ├── ModifierScroll.kt
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
│   │   ├── RoundedCorners.kt
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
    └── components/
        ├── Button.kt
        ├── Checkbox.kt
        └── TextField.kt
```
