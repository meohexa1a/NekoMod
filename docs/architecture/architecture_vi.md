# Tổng quan Kiến trúc UI Engine (Architecture Overview)

Tài liệu này giải thích chi tiết kiến trúc của **NekoMod UI Engine** — một framework giao diện người dùng khai báo (Declarative UI) thuần Kotlin Multiplatform (KMP), chạy trên cây Virtual DOM riêng biệt và vẽ trực tiếp lên GPU thông qua OpenGL mà không phụ thuộc vào hệ thống Scene2D cũ của Arc/Mindustry.

---

## 1. Mục tiêu thiết kế

1. **Hiệu năng cao:** Render trực tiếp trên GPU với các shader SDF (Signed Distance Field) và cơ chế làm mờ hậu cảnh 2-pass Gaussian blur ở tốc độ 60+ FPS.
2. **Khai báo hiện đại (Declarative):** Sử dụng Compose Multiplatform Runtime để quản lý trạng thái (`mutableStateOf`), tự động cập nhật lại UI khi dữ liệu thay đổi (Recomposition).
3. **Mô hình Dàn trang Vững chắc:** Kế thừa thuật toán Container 2-pass của Godot Engine, giải quyết triệt để các vấn đề chồng lấn, co giãn tỷ lệ và phân bố khoảng trống.
4. **Độc lập và Mở rộng:** Tách biệt hoàn toàn logic layout, logic input và logic render thành các tầng độc lập.

---

## 2. Mô hình 5 tầng phân lớp (5-Layer Architecture)

```
┌────────────────────────────────────────────────────────────────────────┐
│ 1. COMPOSE DECLARATIVE LAYER (Mặt tiền khai báo UI)                    │
│    Card, Row, Column, Text, Button, Toggle, Divider, UIModifier        │
└───────────────────────────────────┬────────────────────────────────────┘
                                    │ (NodeApplier & BroadcastFrameClock)
                                    ▼
┌────────────────────────────────────────────────────────────────────────┐
│ 2. VIRTUAL DOM TREE (Cây Node ảo trong bộ nhớ)                         │
│    CanvasNode (Root) ──► BoxNode ──► ColumnNode ──► TextNode           │
│    • Quản lý: parent/children, bounds (Rect), margin, padding, focus  │
└───────────────────────────────────┬────────────────────────────────────┘
                                    │ (isLayoutDirty cascade)
                                    ▼
┌────────────────────────────────────────────────────────────────────────┐
│ 3. LAYOUT ENGINE (Thuật toán dàn trang 2-Pass Godot)                   │
│    GodotLayout (layoutBox, layoutGrid, fitChildInRect, layoutAnchors)   │
│    • Pass 1: Tính kích thước mong muốn (Preferred/Min Sizes)           │
│    • Pass 2: Phân bổ kích thước (Slot Allocation) & Thụt lề Margin    │
└───────────────────────────────────┬────────────────────────────────────┘
                                    │ (Draw Loop Trigger)
                                    ▼
┌────────────────────────────────────────────────────────────────────────┐
│ 4. GPU RENDERING PIPELINE (Tầng vẽ đồ họa GPU)                         │
│    EngineRenderer (Orthographic Projection 0..Width, 0..Height)        │
│    • BoxRenderer (SDF Shader bo góc, viền, bóng đổ)                   │
│    • BoxBlur (2-Pass Gaussian Blur FrameBuffer)                        │
│    • TextRenderer (BMFont Baseline alignment & localization bundle)    │
└───────────────────────────────────┬────────────────────────────────────┘
                                    │ (User Interactions)
                                    ▼
┌────────────────────────────────────────────────────────────────────────┐
│ 5. INPUT & EVENT DISPATCHING (Bắt và truyền sự kiện)                   │
│    EngineInputProcessor (Hit testing, Actionable Ancestor, Hover)      │
└────────────────────────────────────────────────────────────────────────┘
```

---

## 3. Chi tiết các thành phần chính

### A. Virtual Node Hierarchy (`org.mdt.ui.core`)
* **`UINode`**: Lớp cơ sở chứa toàn bộ thuộc tính Box Model (4 hướng Margin và 4 hướng Padding), trạng thái kích thước (`minWidth`, `minHeight`, `sizeFlagsHorizontal`, `sizeFlagsVertical`, `stretchRatio`), danh sách con, sự kiện (`onClick`, `onHover`, `onPointerDown`, `onPointerUp`) và các hàm chuyển đổi hệ tọa độ `localToGlobal` / `globalToLocal`.
* **`CanvasNode`**: Node gốc của toàn màn hình. Tự động đồng bộ kích thước với màn hình game và khởi chạy chu trình tính toán `layout()` khi có bất kỳ node nào trong cây bị đánh dấu `isLayoutDirty = true`.

### B. Compose Multiplatform Integration (`org.mdt.ui.compose`)
* **`NodeApplier`**: Kế thừa `AbstractApplier<UINode>`, biến đổi các thao tác thêm/xóa/sửa node của Compose thành các thao tác trên cây `UINode` thực tế theo thứ tự deterministic top-down.
* **`CompositionManager`**: Quản lý vòng đời `BroadcastFrameClock` và đăng ký lắng nghe snapshot write (`Snapshot.registerGlobalWriteObserver`) để kích hoạt chu trình cập nhật giao diện bất đồng bộ mà không nghẽn luồng chính.

### C. Box Model (Margin $\rightarrow$ Border $\rightarrow$ Padding $\rightarrow$ Content)
Mọi node trong cây đều tuân theo mô hình hình hộp tiêu chuẩn:
1. **Margin (Lề ngoài):** Tạo khoảng trống cách ly node với các node lân cận.
2. **Bounds / Border (Khung & Viền):** Vùng vẽ thực tế của node (bao gồm nền màu, viền bo góc, bóng mờ).
3. **Padding (Lề trong):** Khoảng đệm thụt vào bên trong giữa viền và nội dung (con hoặc chữ).
4. **Content (Nội dung):** Vùng hiển thị chữ (`TextNode`) hoặc các node con (`RowNode`, `ColumnNode`).
