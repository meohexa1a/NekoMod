# Mindustry Custom Declarative UI Engine — Architecture & Blueprint

> **Tài liệu đặc tả kiến trúc, nguyên lý thiết kế và quy chuẩn kỹ thuật cho hệ thống UI tùy biến thế hệ mới trong Mindustry.**

---

## 1. Tổng quan & Tầm nhìn Dự án (Project Overview)

Dự án hướng tới việc **thay thế hoàn toàn hệ thống UI mặc định của Mindustry (`Vars.ui` / Arc Scene2D)** bằng một **Custom Declarative UI Engine** viết bằng **Kotlin Multiplatform (KMP)**. 

### Các trụ cột cốt lõi:
1. **Lấy cảm hứng từ Godot Engine & Compose:** Mô hình Control Nodes / Container Layout (`MeasurePolicy`, `Box`, `Row`, `Column`, `Grid`, `Anchor 4-point`) với độ phức tạp $O(N)$, thuật toán 2-pass cực nhẹ, loại bỏ hoàn toàn Constraint Solver nặng nề.
2. **Pure Virtual Node Tree:** Cây UI hoàn toàn là các cấu trúc dữ liệu thuần túy (Pure Data Classes), không kế thừa bất kỳ class nào từ `arc.scene.Element` hay `WidgetGroup`.
3. **Hiệu năng Cao & Zero-GC trong Render Loop:** Sử dụng Shader SDF (Bo góc, viền, bóng trong, hào quang, backdrop blur) vẽ trực tiếp qua Batch, không sinh rác GC mỗi frame.
4. **Cấu trúc Domain Co-location:** Gom nhóm Widget vào domain sub-packages (`components.input.textfield`, `components.input.slider`, `components.display.image`, etc.) co-locating Virtual Node, Composable và State Machine cùng một nơi.

---

## 2. Vấn đề Kỹ thuật của Arc Scene2D (Problem Statement)

Hệ thống UI gốc của Mindustry dựa trên **Arc Scene2D** (fork từ LibGDX), tồn tại các hạn chế nghiêm trọng:
* **Relayout Thrashing:** Gọi `invalidateHierarchy()` đệ quy ngược lên root mỗi khi thay đổi thuộc tính nhỏ, gây nghẽn CPU.
* **GC Thrashing:** Khởi tạo hàng loạt object ngắn hạn (`Color`, `Vector2`, Lambdas, `Cell`) trong vòng lặp `draw()` / `act()`.
* **Draw Call Storm:** Tốn hàng chục đến hàng trăm draw call do switch texture/font/shader liên tục mà không có cơ chế Uber-Batching.
* **Xử lý Input phân tán:** Hệ thống listener phân tán, bắt sự kiện chạm/cuộn dễ bị xung đột, nuốt event ngầm.

---

## 3. Kiến trúc Đích (Target Architecture)

```
┌────────────────────────────────────────────────────────────────────────┐
│                        TẦNG KHAI BÁO & TƯƠNG TÁC                       │
│  ┌───────────────────────┐  ┌───────────────────┐  ┌────────────────┐  │
│  │ Kotlin Compose DSL    │  │ Text Schema (JSON)│  │ Live Settings  │  │
│  │ (Code UI khai báo)    │  │ (AI Agent/Okio)   │  │ (KVStore 300ms)│  │
│  └──────────┬────────────┘  └─────────┬─────────┘  └───────┬────────┘  │
└─────────────┼─────────────────────────┼────────────────────┼───────────┘
              ▼                         ▼                    ▼
┌────────────────────────────────────────────────────────────────────────┐
│               VIRTUAL NODE TREE (Pure KMP Data Structures)             │
│   • CanvasNode (Root Viewport)                                         │
│   • LayoutNode (Unified Container + MeasurePolicy)                     │
│   • Domain Nodes (TextFieldNode, SliderNode, ImageNode, TextNode)      │
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
│  • 1 Single Root Arc Canvas hook   │ │  • 1 Mindustry InputProcessor   │
│  • Batched SDF Quads / Uber-Shader │ │  • Reverse DFS Hit-Testing AABB │
│  • Single-Pass Global Backdrop FBO │ │  • Global onPointerDrag Event   │
└────────────────────────────────────┘ └─────────────────────────────────┘
```

---

## 4. Cấu trúc Package Mã nguồn

Toàn bộ mã nguồn được gom gọn gàng trong không gian tên `org.mdt.*`:

```
org.mdt
├── core                             # Hạ tầng lõi bất đồng bộ
│   ├── async                        # AsyncDispatcher (Coroutines Dispatchers.IO + Main)
│   ├── cache                        # LRUTextureCache 64MB & TextureHandle
│   ├── i18n                         # I18nEngine đa ngôn ngữ phân cấp
│   ├── image                        # ImageLoader pipeline & VRAM upload throttling
│   ├── net                          # HttpEngine OkHttp singleton & NetDsl
│   └── store                        # Storage Okio atomic write & KVStore debounce 300ms
│
├── mod                              # Điểm nhập cảnh mod Mindustry
│   └── NekoMod.kt
│
└── ui                               # Toàn bộ Custom UI Engine
    ├── components                   # Widget theo Feature Co-location (2-4 file/thư mục)
    │   ├── display                  # image, progress, tooltip
    │   ├── input                    # slider (Capsule Pill), textfield (Telex)
    │   ├── layout                   # Box, BoxVisuals, FlexLayouts, Grid, LayoutNode, Spacer
    │   ├── scroll                   # ScrollContainerNode, ScrollView
    │   ├── surface                  # Button, Card, Divider, Toggle
    │   └── text                     # Text, TextNode, TextVisuals (BMFont 1.0x)
    │
    ├── compose                      # Tầng cầu nối Compose Multiplatform
    │   ├── Dsl.kt, DslMarker.kt, NodeApplier.kt, Scopes.kt, UIComposition.kt, UIModifier.kt
    │
    ├── core                         # Khái niệm cốt lõi, toán học & Virtual Node
    │   ├── CanvasNode.kt, Events.kt, Math.kt, UINode.kt
    │
    ├── input                        # Xử lý tương tác & sự kiện
    │   └── EngineInputProcessor.kt (Reverse-DFS & onPointerDrag)
    │
    ├── layout                       # Layout Engine theo mô hình Godot
    │   ├── Alignment.kt, AnchorData.kt, GodotLayout.kt, SizeFlags.kt
    │   └── policy                   # MeasurePolicy, BoxMeasurePolicy, ColumnMeasurePolicy, ...
    │
    └── render                       # Pipeline đồ họa, Shaders & Zero-GC Rendering
        ├── BoxBlur.kt, BoxRenderer.kt, EngineRenderer.kt, ScissorStack.kt, Shaders.kt, TextRenderer.kt
```

---

## 5. Quy tắc Bất biến dành cho AI Agent & Modder (System Invariants)

1. **KHÔNG** gợi ý hoặc sử dụng các class layout gốc của Arc Scene2D (`Table`, `Cell`, `Group`).
2. **TẬP TRUNG** vào cấu trúc dữ liệu thuần Kotlin KMP, thuật toán bố cục phân cấp sạch sẽ và render trực tiếp lên `Batch`.
3. **ZERO GC & FLOAT TYPE:** Sử dụng `Float` 100% cho thông số đồ họa, nghiêm cấm khởi tạo object ngắn hạn trong render loop.
4. **THÂN THIỆN VỚI AI & LOCAL REFS:** Tuân thủ 15 quy tắc bất biến và luôn đọc mã nguồn gốc tại `.lib-source/`.
