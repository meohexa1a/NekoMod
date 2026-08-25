# Tổng quan Kiến trúc UI Engine (Architecture Overview)

Tài liệu này giải thích chi tiết kiến trúc của **NekoMod UI Engine** — một framework giao diện người dùng khai báo (Declarative UI) thuần Kotlin Multiplatform (KMP), chạy trên cây Virtual DOM độc lập và vẽ trực tiếp lên GPU thông qua OpenGL 2.0 / GLSL 120.

---

## 1. Mục tiêu Thiết kế

1. **Hiệu năng cao ($144\text{ FPS}$):** Render trực tiếp trên GPU với các shader SDF (Signed Distance Field), triệt tiêu hoàn toàn chi phí ép kiểu `Float` và loại bỏ các thao tác copy VRAM đồng bộ nghẽn pipeline.
2. **Khai báo Hiện đại (Declarative):** Tận dụng Compose Multiplatform Runtime để quản lý trạng thái (`mutableStateOf`), tự động tái cấu trúc giao diện khi dữ liệu thay đổi (Recomposition).
3. **Mô hình Dàn trang Vững chắc:** Kế thừa thuật toán Container 2-Pass của Godot Engine kết hợp chiến lược `MeasurePolicy` linh hoạt, giải quyết triệt để vấn đề chồng lấn và co giãn tỷ lệ.
4. **Độc lập và Mở rộng:** Tách biệt hoàn toàn logic layout, logic input và logic render thành các tầng độc lập theo mô hình Feature Slicing & Co-location.

---

## 2. Mô hình 5 Tầng Phân lớp (5-Layer Architecture)

```
┌────────────────────────────────────────────────────────────────────────┐
│ 1. COMPOSE DECLARATIVE LAYER (Mặt tiền khai báo UI)                    │
│    Card, Row, Column, Text, Slider, Toggle, Button, UIModifier         │
└───────────────────────────────────┬────────────────────────────────────┘
                                    │ (NodeApplier & BroadcastFrameClock)
                                    ▼
┌────────────────────────────────────────────────────────────────────────┐
│ 2. VIRTUAL DOM TREE (Cây Node ảo trong bộ nhớ)                         │
│    CanvasNode (Root) ──► LayoutNode (Box/Row/Col) ──► TextNode         │
│    • Quản lý: parent/children, bounds (Rect), margin, padding, focus  │
│    • MeasurePolicy: BoxMeasurePolicy, ColumnMeasurePolicy, ...        │
└───────────────────────────────────┬────────────────────────────────────┘
                                    │ (isLayoutDirty cascade)
                                    ▼
┌────────────────────────────────────────────────────────────────────────┐
│ 3. LAYOUT ENGINE (Thuật toán dàn trang 2-Pass Godot)                   │
│    GodotLayout (layoutBox, layoutGrid, fitChildInRect, layoutAnchors)   │
│    • Pass 1: Tính kích thước mong muốn (Preferred/Min Sizes)           │
│    • Pass 2: Phân bổ kích thước (Slot Allocation) & Thụt lề Margin    │
└───────────────────────────────────┬────────────────────────────────────┘
                                    │ (Draw Loop Trigger: Trigger.uiDrawEnd)
                                    ▼
┌────────────────────────────────────────────────────────────────────────┐
│ 4. GPU RENDERING PIPELINE (Tầng vẽ đồ họa GPU)                         │
│    EngineRenderer (Orthographic Projection 0..Width, 0..Height)        │
│    • BoxRenderer (SDF Shader bo góc, viền, bóng đổ, outer glow)       │
│    • TextRenderer (BMFont Baseline alignment @ scale = 1.0f)           │
└───────────────────────────────────┬────────────────────────────────────┘
                                    │ (User Interactions)
                                    ▼
┌────────────────────────────────────────────────────────────────────────┐
│ 5. INPUT & EVENT DISPATCHING (Bắt và truyền sự kiện)                   │
│    EngineInputProcessor (Reverse-DFS Hit-Testing, onPointerDrag, Focus)│
└────────────────────────────────────────────────────────────────────────┘
```

---

## 3. Chi tiết các Thành phần Chính

### A. Virtual Node Hierarchy (`org.mdt.core.ui` & `components.layout`)
* **`UINode`**: Lớp cơ sở chứa toàn bộ thuộc tính Box Model (4 hướng Margin và 4 hướng Padding), trạng thái kích thước (`minWidth`, `minHeight`, `sizeFlagsHorizontal`, `sizeFlagsVertical`, `stretchRatio`), danh sách con, và các sự kiện (`onClick`, `onHover`, `onPointerDown`, `onPointerDrag`, `onPointerUp`).
* **`LayoutNode`**: Container hợp nhất đa năng sở hữu `measurePolicy: MeasurePolicy` (hỗ trợ `BoxMeasurePolicy`, `ColumnMeasurePolicy`, `RowMeasurePolicy`, `GridMeasurePolicy`) và cơ chế tạo hiệu ứng hình ảnh lười `ensureVisuals()` (Zero-Overhead).
* **`CanvasNode`**: Node gốc đại diện cho toàn bộ khung nhìn (Viewport) màn hình, chịu trách nhiệm đón nhận sự kiện thay đổi kích thước cửa sổ và kích hoạt dàn trang 2-pass.

### B. Cơ chế Recomposition & Frame Clock (`org.mdt.ui.compose`)
* **`UIComposition`**: Khởi tạo `Recomposer` và cấp phát `NodeApplier` gắn trực tiếp vào `rootCanvas`.
* **`BroadcastFrameClock`**: Phát xung nhịp `sendFrame()` đồng bộ theo chu kỳ khung hình $60\text{ FPS}$ của game thông qua hook `Trigger.uiDrawEnd` hoặc `EngineRuntime.draw()`.

### C. Input Pipeline & Gestures (`org.mdt.ui.input`)
* **`EngineInputProcessor`**: Thực thi thuật toán duyệt cây ngược (Reverse-DFS) để tìm node lá sâu nhất nằm dưới con trỏ chuột (`hitTest`).
* **Event-Driven Drag Routing:** Khi nhận `touchDragged`, tự động chuyển tiếp tới `pressedNode.onPointerDrag` để đảm bảo thao tác kéo thả mượt mà trên toàn màn hình.
