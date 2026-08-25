# Mindustry Custom Declarative UI Engine — Architecture & Blueprint

> **Tài liệu đặc tả kiến trúc, nguyên lý thiết kế và lộ trình triển khai cho hệ thống UI tùy biến thế hệ mới trong Mindustry.**

---

## 1. Tổng quan & Tầm nhìn Dự án (Project Overview)

Dự án hướng tới việc **thay thế hoàn toàn hệ thống UI mặc định của Mindustry (`Vars.ui` / Arc Scene2D)** bằng một **Custom Declarative UI Engine** viết bằng **Kotlin Multiplatform (KMP)**. 

### Các trụ cột cốt lõi:
1. **Lấy cảm hứng từ Godot Engine:** Mô hình Control Nodes / Container Layout (`VBox`, `HBox`, `Grid`, `Margin`, `Anchor 4-point`) với độ phức tạp $O(N)$, thuật toán 2-pass cực nhẹ, loại bỏ hoàn toàn Constraint Solver nặng nề.
2. **Pure Virtual Node Tree:** Cây UI hoàn toàn là các cấu trúc dữ liệu thuần túy (Pure Data Classes), không kế thừa bất kỳ class nào từ `arc.scene.Element` hay `WidgetGroup`.
3. **Hiệu năng Cao & Zero-GC trong Render Loop:** Sử dụng Shader SDF (Bo góc, viền đứt nét, bóng trong, hào quang, backdrop blur) vẽ trực tiếp qua Batch, không sinh rác GC mỗi frame.
4. **Cầu nối AI & In-Game Visual Editor:** Định dạng Text Schema (YAML/JSON) tối ưu cho AI Agent sinh/đọc layout, kết hợp với bộ công cụ kéo thả In-Game Editor (Gizmo bounds) và Hot-reload runtime (dùng `Okio` và `Janino`).

---

## 2. Vấn đề Kỹ thuật của Arc Scene2D (Problem Statement)

Hệ thống UI gốc của Mindustry dựa trên **Arc Scene2D** (fork từ LibGDX), tồn tại các hạn chế nghiêm trọng:
* **Relayout Thrashing:** Gọi `invalidateHierarchy()` đệ quy ngược lên root mỗi khi thay đổi thuộc tính nhỏ, gây nghẽn CPU.
* **GC Thrashing:** Khởi tạo hàng loạt object ngắn hạn (`Color`, `Vector2`, Lambdas, `Cell`) trong vòng lặp `draw()` / `act()`.
* **Draw Call Storm:** Tốn hàng chục đến hàng trăm draw call do switch texture/font/shader liên tục mà không có cơ chế Uber-Batching.
* **Xử lý Input dị hợm:** Hệ thống listener phân tán, bắt sự kiện chạm/cuộn dễ bị xung đột, nuốt event ngầm.
* **Thiếu Editor:** Không có công cụ kéo thả trực quan; chỉnh sửa giao diện phải code mò và khởi động lại game liên tục.

---

## 3. Kiến trúc Đích (Target Architecture)

```
┌────────────────────────────────────────────────────────────────────────┐
│                        TẦNG KHAI BÁO & TƯƠNG TÁC                       │
│  ┌───────────────────────┐  ┌───────────────────┐  ┌────────────────┐  │
│  │ Kotlin Compose DSL    │  │ Text Schema (YAML)│  │ In-Game Editor │  │
│  │ (Code UI linh hoạt)   │  │ (AI Agent/Okio)   │  │ (Visual Gizmo) │  │
│  └──────────┬────────────┘  └─────────┬─────────┘  └───────┬────────┘  │
└─────────────┼─────────────────────────┼────────────────────┼───────────┘
              ▼                         ▼                    ▼
┌────────────────────────────────────────────────────────────────────────┐
│               VIRTUAL NODE TREE (Pure KMP Data Structures)             │
│   • CanvasNode (Root)                                                  │
│   • ContainerNode (BoxContainer, GridContainer, MarginContainer, ...)  │
│   • VisualNodes (ShaderBoxNode, TextNode, ImageNode, CustomNode)       │
└───────────────────────────────────┬────────────────────────────────────┘
                                    ▼
┌────────────────────────────────────────────────────────────────────────┐
│                    2-PASS GODOT LAYOUT ENGINE (Zero-GC)                │
│   1. Measure Pass (Bottom-Up): Tính minimum / preferred size           │
│   2. Layout Pass  (Top-Down) : Phân bổ final Rect (x, y, w, h) & fit   │
│   * Hỗ trợ SizeFlags (FILL, EXPAND, SHRINK) & Anchor 4 điểm (0.0..1.0) │
└──────────────────┬─────────────────────────────────┬───────────────────┘
                   ▼                                 ▼
┌────────────────────────────────────┐ ┌─────────────────────────────────┐
│     RENDER PIPELINE (Direct Batch) │ │   INPUT SYSTEM (Independent)    │
│  • 1 Single Root Arc Canvas hook   │ │  • 1 LibGDX InputProcessor hook │
│  • Batched SDF Quads / Uber-Shader │ │  • Reverse DFS Hit-Testing AABB │
│  • Single-Pass Global Backdrop FBO │ │  • Event Bubbling & Focus state │
└────────────────────────────────────┘ └─────────────────────────────────┘
```

---

## 4. Cấu trúc Package Mục tiêu (Semantic Target Hierarchy)

Toàn bộ mã nguồn được gom gọn gàng trong không gian tên `org.mdt.ui.*`:

```
org.mdt
├── mod
│   └── NekoMod.kt                   # Mod entrypoint & Game Lifecycle
│
└── ui                               # Toàn bộ Custom UI Engine
    ├── core                         # Khái niệm cốt lõi, toán học & Virtual Node
    │   ├── Math.kt                  # Insets, Rect, Vec2, Color helpers
    │   ├── UINode.kt                # Pure Virtual Tree Node
    │   ├── CanvasNode.kt            # Root Canvas quản lý toàn bộ cây
    │   └── Events.kt                # Event definitions (Click, Drag, Scroll, Key)
    │
    ├── layout                       # Layout Engine theo mô hình Godot
    │   ├── SizeFlags.kt             # SHRINK_BEGIN, FILL, EXPAND, SHRINK_CENTER, SHRINK_END
    │   ├── AnchorData.kt            # 4-point anchors (0.0..1.0) & pixel offsets & presets
    │   ├── GodotLayout.kt           # Thuật toán fitChildInRect, Box, Grid, Margin, Anchor
    │   ├── BoxLayout.kt            # Thuộc tính layout của Box
    │   └── TextLayout.kt           # Thuộc tính layout của Text
    │
    ├── render                       # Pipeline đồ họa, Shaders & Zero-GC Rendering
    │   ├── Shaders.kt               # Quản lý GLSL Shaders
    │   ├── BoxRenderer.kt           # SDF Box Quad drawing
    │   ├── BoxBlur.kt               # Global Screen Backdrop FBO blur
    │   └── TextRenderer.kt          # GlyphLayout & FontCache drawing
    │
    ├── widgets                      # Các phần tử UI (UI Elements & Visual States)
    │   ├── Box.kt                   # Box container element
    │   ├── BoxVisuals.kt            # Thuộc tính visual (border, radius, shadow, filter)
    │   ├── Text.kt                  # Text element
    │   └── TextVisuals.kt           # Thuộc tính typography (wrap, align, fontScale)
    │
    ├── compose                      # Tầng cầu nối Jetpack Compose Runtime
    │   ├── ComposeBridge.kt         # CompositionManager, GameFrameClock, Applier
    │   ├── Components.kt            # ArcBox, ArcText, ArcRow, ArcColumn, ArcGrid, ArcMargin
    │   ├── Widgets.kt               # ArcButton, ArcToggle
    │   └── Dsl.kt                   # Extension functions: expandFill(), pad(), fixed()
    │
    ├── schema                       # [Module Sắp tới] Scene Schema (Okio + Janino)
    │   ├── SchemaNode.kt            # Schema AST definition (YAML/JSON data tree)
    │   ├── SchemaLoader.kt          # Streaming Parser đọc file qua Okio
    │   └── ScriptEngine.kt          # Dynamic script / expression evaluator dùng Janino
    │
    └── editor                       # [Module Sắp tới] In-Game Visual Editor
        ├── Gizmo.kt                 # Khung chọn & 8 điểm handles resize/drag
        └── Inspector.kt             # Realtime property editor & YAML exporter
```

---

## 5. Chiến lược Cách ly Arc Scene (Runway & Isolation Strategy)

Để triệt tiêu các lỗi ngầm và xung đột với UI mặc định của Mindustry:

1. **Cách ly Render:**
   * Tạo **đúng 01 Arc `Element` duy nhất** (`EngineCanvas`) gắn vào `Core.scene.root`, bật `setFillParent(true)`.
   * Gốc Canvas này sẽ override hàm `draw()`. Khi Arc gọi `draw()`, Canvas sẽ chuyển giao toàn bộ quyền render cho cây `UINode` duyệt xuống GPU Batch trực tiếp.
   * Arc Scene2D hoàn toàn không biết bên trong có bao nhiêu node con, tránh được toàn bộ cơ chế `invalidateHierarchy()` của Arc.

2. **Cách ly Input:**
   * Đăng ký **01 `InputProcessor` độc lập** với LibGDX.
   * Xử lý click, hover, drag, scroll thông qua duyệt cây `UINode` từ trên xuống dưới (Reverse DFS / Z-Index), kiểm tra va chạm `AABB.contains(x, y)`.
   * Không sử dụng bất kỳ `ClickListener` hay `InputListener` nào của Arc Scene2D.

---

## 6. Lộ trình Triển khai (Roadmap)

* [x] **Phase 0:** Dọn dẹp dependencies (loại bỏ ConstraintLayout & Ktor, tích hợp Janino & Okio).
* [x] **Phase 1:** Triển khai nền tảng Godot Container Model (`SizeFlags`, `AnchorData`, `GodotLayout`).
* [ ] **Phase 2:** Cách ly Arc Scene (Tạo `EngineCanvas` & Input Interceptor) + Tái cấu trúc package theo đúng Semantic.
* [ ] **Phase 3:** Chuyển đổi toàn diện sang `Pure UINode Tree` (Decouple hoàn toàn khỏi `WidgetGroup`/`Element`).
* [ ] **Phase 4:** Xây dựng Tầng Schema & Scripting Engine (`Okio` YAML/JSON parser + `Janino` runtime evaluation).
* [ ] **Phase 5:** Xây dựng In-Game Visual Editor (Gizmo, Drag & Drop, Live Inspector & 2-way Schema export).

---

## 7. Quy tắc Bất biến dành cho AI Agent & Modder (System Rules)

1. **KHÔNG** gợi ý hoặc sử dụng các class layout gốc của Arc Scene2D (`Table`, `Cell`, `Group`).
2. **TẬP TRUNG** vào cấu trúc dữ liệu thuần Kotlin KMP, thuật toán bố cục phân cấp sạch sẽ và render trực tiếp lên `Batch`.
3. **ZERO GC:** Nghiêm cấm khởi tạo object ngắn hạn (`new Color()`, `new Vector2()`, `lambdas per-frame`) trong render loop / draw call.
4. **THÂN THIỆN VỚI AI:** Mọi thuộc tính layout và widget phải có khả năng serialize thành Text Schema ngắn gọn, phân cấp rõ ràng.
