# Project: NekoMod v3 Architecture Research & Specification

## Architecture
NekoMod v3 kết hợp thư viện giao diện khai báo Jetpack/JetBrains Compose Runtime với engine bố cục tự trị hiệu năng cao `org.hubdustry.libs.layout` và hệ thống dựng hình thích ứng (Dynamic Graphics Tiering) chạy trên nền tảng game engine Mindustry/Arc Scene2D.

### Luồng Dữ Liệu & Kiến Trúc Phân Tầng
```
+-----------------------------------------------------------------------------------+
| 1. COMPOSE RUNTIME LAYER                                                          |
|    - Composable DSL (@Composable fun Button, Row, Column, Box)                   |
|    - State Snapshot System (mutableStateOf, MVCC Snapshot)                        |
|    - Recomposer & CoroutineScope (MindustryDispatcher on Main GL Thread)         |
+------------------------------------------+----------------------------------------+
                                           | Phát sinh thay đổi cây qua Bottom-Up
                                           v
+-----------------------------------------------------------------------------------+
| 2. LAYOUT ENGINE LAYER (org.hubdustry.libs.layout)                                |
|    - Custom LayoutNodeApplier : AbstractApplier<LayoutNode>                       |
|    - Active Self-Validating LayoutNode (addChild, removeChildren, moveChildren)   |
|    - Dirty Bitflags (DIRTY_MEASURE, DIRTY_LAYOUT) with Branch Pruning (0-GC)     |
|    - Top-Left (Y-down) Pure Math Layout Policies (BoxLayoutPolicy, FlexPolicy)    |
+------------------------------------------+----------------------------------------+
                                           | Tính toán tọa độ và kích thước node
                                           v
+-----------------------------------------------------------------------------------+
| 3. GATEWAY MOUNT-POINT (ComposeView : arc.scene.Element)                          |
|    - Lifecycle Anchor: setScene(stage) -> mount/dispose                           |
|    - Frame Clock Sync: act(delta) -> frameClock.sendFrame(timeNanos)              |
|    - BỨC TƯỜNG BERLIN TRỤC Y:                                                    |
|        Input:  composeY = height - arcY                                           |
|        Draw:   arcY = height - (nodeY + nodeHeight)                               |
+------------------------------------------+----------------------------------------+
                                           | Phát lệnh hiển thị qua Paint Abstraction
                                           v
+-----------------------------------------------------------------------------------+
| 4. RENDER PIPELINE & DYNAMIC GRAPHICS TIERING                                    |
|    - DrawContext Abstraction (drawBox, drawText, pushScissor, popScissor)         |
|    - Tier 1 (High): Compact Quad (10 floats) + GLSL Inigo Quilez SDF Shader       |
|                     Direct NIO ByteBuffer Off-Heap Streaming (0-GC)              |
|                     Analytical Scissor Clip (0 draw calls, sub-pixel AA)          |
|    - Tier 2 (Low):  Arc Native Fast-Path Fallback (Draw.rect, NinePatch, Lines)  |
|                     Hardware ScissorStack, Hysteresis Auto-Degradation            |
+-----------------------------------------------------------------------------------+
```

---

## Feature Inventory
Bảng toàn bộ tính năng và yêu cầu kỹ thuật được phát hiện từ Survey phase (không tính năng nào bị bỏ sót):

| # | Feature | Description | Milestone | Source |
|---|---------|-------------|-----------|--------|
| 1 | F01_Lifecycle_Mount_Dispose | `ComposeView.setScene(stage)` quản lý vòng đời gắn kết, khởi tạo CoroutineScope/Recomposer và hủy dọn dẹp tài nguyên | M1 | Survey E1 |
| 2 | F02_FrameClock_Act_Sync | `ComposeView.act(delta)` đồng bộ `BroadcastFrameClock.sendFrame()` và Snapshot apply notifications | M1 | Survey E1/E2 |
| 3 | F03_Draw_Dispatch | `ComposeView.draw()` điều phối lệnh vẽ, bù trừ tọa độ Stage khi `transform == false` | M1 | Survey E1 |
| 4 | F04_Hit_Test_Delegation | `ComposeView.hit(x, y, touchable)` chuyển tiếp tương tác người dùng vào cây Neko | M1 | Survey E1 |
| 5 | F05_Berlin_Wall_Input | Chuẩn hóa lật trục Y tại cửa vào Input: `composeY = height - arcY` | M1 | Survey E1, GEMINI.md |
| 6 | F06_Berlin_Wall_Draw | Chuẩn hóa lật trục Y tại cửa ra Draw: `arcY = height - (nodeY + nodeHeight)` | M1 | Survey E1, GEMINI.md |
| 7 | F07_Scissor_Boundary_Integration | Cơ chế kết nối với ngăn xếp Arc `ScissorStack` ở cấp Element bao ngoài | M1 | Survey E1 |
| 8 | F08_Compact_Quad_Layout | Cấu trúc đỉnh nén 10 floats/vertex (40 bytes), căn chỉnh 8-byte/4-byte | M2 | Survey E3 |
| 9 | F09_Analytical_SDF_Shader | Giải tích SDF hình chữ nhật bo góc Inigo Quilez trong Fragment Shader | M2 | Survey E3 |
| 10 | F10_Micro_Antialiasing | Khử răng cưa vi mô thích ứng màn hình qua GLSL `fwidth(dist)` và clamp | M2 | Survey E3 |
| 11 | F11_Inner_Border_Drop_Shadow | Công thức viền trong không artifact và bóng đổ mềm Gaussian giải tích | M2 | Survey E3 |
| 12 | F12_Direct_NIO_Streaming | Bộ đệm Direct Off-Heap `ByteBuffer` / `FloatBuffer` 0-GC vertex streaming | M2 | Survey E3, GEMINI.md |
| 13 | F13_Tier2_Arc_Native_Fallback | Phương án dự phòng 100% Arc 2D (`NinePatch`, `Lines`, `Draw.rect`) | M2 | Survey E3 |
| 14 | F14_Paint_Abstraction_Dynamic_Tiering | Tầng trừu tượng hóa `DrawContext` / `PaintStyle` với chính sách Hysteresis FPS | M2 | Survey E3 |
| 15 | F15_Hardware_Cost_Model | Bảng so sánh định lượng phần cứng (100, 500, 2000 widgets) | M2 | Survey E3 |
| 16 | F16_Custom_LayoutNode_Applier | `LayoutNodeApplier : AbstractApplier<LayoutNode>` với chiến lược `insertBottomUp` | M3 | Survey E2 |
| 17 | F17_Autonomous_LayoutNode_API | Mở rộng `addChild(child, index)`, `removeChildren(index, count)` với Gateway Sanitization | M3 | Survey E2, GEMINI.md |
| 18 | F18_InPlace_Array_Rotation | Thuật toán Đảo 3 Khối tại chỗ (Triple In-Place Reversal) cho `moveChildren` đạt 0-GC | M3 | Survey E2, GEMINI.md |
| 19 | F19_BroadcastFrameClock_Binding | Đồng bộ nhịp Recomposer qua cờ lock-free `hasAwaiters` (<2ns/frame) | M3 | Survey E2 |
| 20 | F20_Targeted_Recomposition_DirtyFlags | Bitmask 32-bit nguyên thủy (`DIRTY_MEASURE`, `DIRTY_LAYOUT`) trên `LayoutNode` | M3 | Survey E2 |
| 21 | F21_Branch_Pruning_Optimization | Tỉa nhánh layout và dừng sớm upward bubbling (Early Pruning) 0-GC | M3 | Survey E2, GEMINI.md |
| 22 | F22_Mindustry_Thread_Dispatcher | `MindustryDispatcher : CoroutineDispatcher` điều phối Snapshot mutations về Main GL Thread | M3 | Survey E2 |
| 23 | F23_Contingency_Batch_Breaking | Khắc phục vỡ batch qua Uber-Shader 4 chế độ và Texture Atlas đồng nhất | M4 | Survey E3 |
| 24 | F24_Contingency_Rounded_Scissor | Khắc phục cắt góc phân cấp qua Analytical Multi-Rect SDF Scissor (0 draw calls) | M4 | Survey E3 |
| 25 | F25_Contingency_IME_Vietnamese | Giải pháp gõ tiếng Việt (EVKey/Unikey) trong Arc Scene2D với pre-edit composition | M4 | Survey E1, ORIGINAL_REQUEST |
| 26 | F26_Contingency_Concurrency | Giải pháp xung đột đa luồng Coroutines vs Mindustry GL Thread (MVCC Snapshot) | M4 | Survey E2, ORIGINAL_REQUEST |
| 27 | F27_Complete_PseudoCode_Dossier | Xây dựng bộ hồ sơ kiến trúc hoàn chỉnh tích hợp toàn bộ pseudo-code và bảng định lượng | M5 | Tất cả |
| 28 | F28_Verification_Audit | Thẩm định kiến trúc, review độc lập, stress test và forensic integrity audit | M6 | GEMINI.md, User Req |

---

## Milestones

| # | Name | Scope | Dependencies | Status |
|---|------|-------|-------------|--------|
| M1 | Spec: Mount-Point `ComposeView` & Arc Compatibility | F01 - F07 (Lifecycle, Berlin Wall Y-flip, Scissor Boundary) | None | IN_PROGRESS |
| M2 | Spec: Render Pipeline & Dynamic Graphics Tiering | F08 - F15 (SDF Compact Quad, Direct NIO, Tier 2 Fallback, Paint, Hardware Model) | None | IN_PROGRESS |
| M3 | Spec: Compose Runtime & Layout Engine Bridge | F16 - F22 (NodeApplier, LayoutNode API, FrameClock, Dirty Flags, Dispatcher) | None | IN_PROGRESS |
| M4 | Spec: Contingency Matrix (4 Technical Risks) | F23 - F26 (Batch breaking, Rounded scissor, IME Vietnamese, Concurrency) | M1, M2, M3 | PLANNED |
| M5 | Synthesis: Comprehensive Architectural Dossier | F27 (Ghép nối và hoàn thiện `ARCHITECTURE_V3_DOSSIER.md`) | M1, M2, M3, M4 | PLANNED |
| M6 | Verification: Independent Review & Forensic Integrity Audit | F28 (Reviewer, Challenger, Forensic Auditor validation) | M5 | PLANNED |

---

## Interface Contracts

### `ComposeView` ↔ `LayoutNode`
- `ComposeView.rootLayoutNode: LayoutNode`
- Khi `ComposeView` thay đổi kích thước `(width, height)`:
  - Gọi `rootLayoutNode.layout(width, height)`.
- Khi hit test `ComposeView.hit(arcX, arcY)`:
  - `composeY = height - arcY`
  - Gọi `rootLayoutNode.hitTest(arcX, composeY)`.

### `LayoutNodeApplier` ↔ `LayoutNode`
- `insertBottomUp(index: Int, instance: LayoutNode)`:
  - Gọi `current.addChild(instance, index)`
- `remove(index: Int, count: Int)`:
  - Gọi `current.removeChildren(index, count)`
- `move(from: Int, to: Int, count: Int)`:
  - Gọi `current.moveChildren(from, to, count)`
- `clear()`:
  - Gọi `root.clearChildren()`

### `LayoutNode` ↔ `DrawContext`
- `LayoutNode.draw(context: DrawContext, offsetX: Float, offsetY: Float)`:
  - Thuần túy truyền dữ liệu hình học: `context.drawBox(x + offsetX, y + offsetY, width, height, style)`
  - Hoàn toàn độc lập với việc `DrawContext` là Tier 1 hay Tier 2.

### `MindustryFrameClock` ↔ `Recomposer`
- `BroadcastFrameClock` phát nhịp nano `sendFrame(timeNanos)` trong `ComposeView.act(delta)`.
- `Recomposer` tiêu thụ nhịp, thực hiện recomposition và đẩy thay đổi xuống `LayoutNodeApplier` trên Main GL Thread.

---

## Code Layout
Cấu trúc module và package định hướng cho NekoMod v3:
```
src/main/kotlin/
└── org/
    └── hubdustry/
        ├── libs/
        │   └── layout/             # Engine bố cục thuần túy (Đã hoàn thiện & 100% test pass)
        │       ├── LayoutNode.kt
        │       ├── LayoutPolicy.kt
        │       ├── Anchor.kt
        │       ├── SizeFlag.kt
        │       └── policies/
        │           ├── BoxLayoutPolicy.kt
        │           └── FlexLayoutPolicy.kt
        ├── compose/                # Tầng cầu nối Compose Runtime & Cửa khẩu Arc
        │   ├── ComposeView.kt      # Mount-point kế thừa arc.scene.Element
        │   ├── LayoutNodeApplier.kt# Custom Applier gắn kết cây LayoutNode
        │   ├── MindustryClock.kt   # BroadcastFrameClock synchronization
        │   └── Dispatcher.kt       # Mindustry GL CoroutineDispatcher
        └── graphics/               # Tầng Render Pipeline & Graphics Tiering
            ├── DrawContext.kt      # Tầng trừu tượng hóa Paint (Dynamic Tiering)
            ├── PaintStyle.kt       # Cấu hình hiển thị (Màu, bo góc, viền, bóng)
            ├── tier1/
            │   ├── ModernBatch.kt  # Direct NIO ByteBuffer vertex streaming 0-GC
            │   ├── ModernShader.kt # Quản lý Shader SDF Inigo Quilez
            │   └── Tier1Context.kt # Thực thi vẽ Tier 1
            └── tier2/
                └── Tier2Context.kt # Thực thi vẽ dự phòng 100% Arc 2D Native
src/main/resources/
└── shaders/
    ├── modern_ui.vert              # Vertex shader nén 10 floats/vertex
    └── modern_ui.frag              # Fragment shader giải tích SDF, micro-AA, clip
```
