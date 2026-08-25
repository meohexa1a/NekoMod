# NekoMod Pure KMP UI Engine Documentation

Chào mừng bạn đến với hệ thống tài liệu chính thức của **NekoMod Declarative UI Engine** (Pure Virtual Node Tree chạy trên GPU OpenGL cho Mindustry).

---

## 📚 Mục Lục Tài Liệu / Documentation Index

| Chủ đề / Topic | Bản Tiếng Việt 🇻🇳 | English Version 🇬🇧 |
| :--- | :--- | :--- |
| **1. Tổng quan Kiến trúc**<br>*Architecture Overview* | [architecture_vi.md](./architecture/architecture_vi.md) | [architecture_en.md](./architecture/architecture_en.md) |
| **2. Thuật toán Dàn trang & Tọa độ**<br>*Layout Engine & Coordinate System* | [layout_engine_vi.md](./layout-engine/layout_engine_vi.md) | [layout_engine_en.md](./layout-engine/layout_engine_en.md) |
| **3. Hướng dẫn viết Compose DSL**<br>*Compose Multiplatform DSL & Modifiers* | [compose_dsl_vi.md](./compose-dsl/compose_dsl_vi.md) | [compose_dsl_en.md](./compose-dsl/compose_dsl_en.md) |
| **4. GPU Rendering & Shaders**<br>*SDF Shaders & 2-Pass Gaussian Blur* | [rendering_shaders_vi.md](./rendering-shaders/rendering_shaders_vi.md) | [rendering_shaders_en.md](./rendering-shaders/rendering_shaders_en.md) |
| **5. Lộ trình phát triển tương lai**<br>*Future Development Roadmap* | [roadmap_vi.md](./roadmap/roadmap_vi.md) | [roadmap_en.md](./roadmap/roadmap_en.md) |

---

## 🚀 Cấu trúc dự án nguồn / Source Code Mapping

- `org.mdt.ui.core`: Virtual Node DOM tree ([`UINode`](../src/main/kotlin/org/mdt/ui/core/UINode.kt), [`CanvasNode`](../src/main/kotlin/org/mdt/ui/core/CanvasNode.kt), Events, Math).
- `org.mdt.ui.compose`: Compose Multiplatform bridge ([`UIComposition`](../src/main/kotlin/org/mdt/ui/compose/UIComposition.kt), [`Components`](../src/main/kotlin/org/mdt/ui/compose/Components.kt), [`Widgets`](../src/main/kotlin/org/mdt/ui/compose/Widgets.kt), [`UIModifier`](../src/main/kotlin/org/mdt/ui/compose/UIModifier.kt), Scopes).
- `org.mdt.ui.layout`: Godot-inspired 2-pass layout algorithm ([`GodotLayout`](../src/main/kotlin/org/mdt/ui/layout/GodotLayout.kt), Alignment, SizeFlags, Anchors).
- `org.mdt.ui.render`: GPU rendering pipeline ([`EngineRenderer`](../src/main/kotlin/org/mdt/ui/render/EngineRenderer.kt), BoxRenderer, TextRenderer, BoxBlur).
- `org.mdt.ui.input`: Event bubble & hit testing ([`EngineInputProcessor`](../src/main/kotlin/org/mdt/ui/input/EngineInputProcessor.kt)).
