# NekoMod Pure KMP UI Engine Documentation

Chào mừng bạn đến với hệ thống tài liệu chính thức của **NekoMod Declarative UI Engine & Core Subsystems** (Pure Virtual Node Tree chạy trên GPU OpenGL cho Mindustry).

---

## 📚 Mục Lục Tài Liệu / Documentation Index

| Chủ đề / Topic | Bản Tiếng Việt 🇻🇳 | English Version 🇬🇧 |
| :--- | :--- | :--- |
| **1. Tiêu chuẩn Lập trình & Kiến trúc**<br>*Coding Standards & Architecture Rules* | [coding_standards_vi.md](./coding-standards/coding_standards_vi.md) | [coding_standards_en.md](./coding-standards/coding_standards_en.md) |
| **2. Các Thách thức Phức tạp & Tương lai**<br>*Complex Challenges & Future Directions* | [complex_challenges_vi.md](./complex-challenges/complex_challenges_vi.md) | [complex_challenges_en.md](./complex-challenges/complex_challenges_en.md) |
| **3. Tổng quan Kiến trúc 5 Tầng**<br>*Architecture Overview* | [architecture_vi.md](./architecture/architecture_vi.md) | [architecture_en.md](./architecture/architecture_en.md) |
| **4. Hạ tầng Lõi (Network, Storage, Image, i18n)**<br>*Core Subsystems Architecture* | [core_subsystems_vi.md](./core-subsystems/core_subsystems_vi.md) | [core_subsystems_en.md](./core-subsystems/core_subsystems_en.md) |
| **5. Thuật toán Dàn trang & Tọa độ**<br>*Layout Engine & Coordinate System* | [layout_engine_vi.md](./layout-engine/layout_engine_vi.md) | [layout_engine_en.md](./layout-engine/layout_engine_en.md) |
| **6. Hướng dẫn viết Compose DSL**<br>*Compose Multiplatform DSL & Modifiers* | [compose_dsl_vi.md](./compose-dsl/compose_dsl_vi.md) | [compose_dsl_en.md](./compose-dsl/compose_dsl_en.md) |
| **7. GPU Rendering & Shaders**<br>*SDF Shaders & 2-Pass Gaussian Blur* | [rendering_shaders_vi.md](./rendering-shaders/rendering_shaders_vi.md) | [rendering_shaders_en.md](./rendering-shaders/rendering_shaders_en.md) |
| **8. Bản đồ Cấu trúc Mã nguồn**<br>*Codebase Architecture & LOC Map* | [codebase_map_vi.md](./architecture/codebase_map_vi.md) | [codebase_map_en.md](./architecture/codebase_map_en.md) |
| **9. Lộ trình phát triển tương lai**<br>*Future Development Roadmap* | [roadmap_vi.md](./roadmap/roadmap_vi.md) | [roadmap_en.md](./roadmap/roadmap_en.md) |

---

## 🚀 Cấu trúc dự án nguồn / Source Code Mapping

- `org.mdt.core.async`: Coroutines & Thread dispatching ([`AsyncDispatcher`](../src/main/kotlin/org/mdt/core/async/AsyncDispatcher.kt)).
- `org.mdt.core.net`: OkHttp Network engine & Fluent DSL ([`NetDsl`](../src/main/kotlin/org/mdt/core/net/NetDsl.kt)).
- `org.mdt.core.store`: Okio atomic storage & KV store ([`KVStore`](../src/main/kotlin/org/mdt/core/store/KVStore.kt)).
- `org.mdt.core.cache`: Safe reference-counted VRAM cache ([`LRUTextureCache`](../src/main/kotlin/org/mdt/core/cache/LRUTextureCache.kt)).
- `org.mdt.core.image`: Asynchronous image pipeline ([`ImageLoader`](../src/main/kotlin/org/mdt/core/image/ImageLoader.kt)).
- `org.mdt.core.i18n`: Modern nested key localization ([`I18nEngine`](../src/main/kotlin/org/mdt/core/i18n/I18nEngine.kt)).
- `org.mdt.ui.core`: Virtual Node DOM tree ([`UINode`](../src/main/kotlin/org/mdt/ui/core/UINode.kt), [`CanvasNode`](../src/main/kotlin/org/mdt/ui/core/CanvasNode.kt)).
- `org.mdt.ui.compose`: Compose Multiplatform bridge ([`UIComposition`](../src/main/kotlin/org/mdt/ui/compose/UIComposition.kt), [`Components`](../src/main/kotlin/org/mdt/ui/compose/Components.kt), [`Widgets`](../src/main/kotlin/org/mdt/ui/compose/Widgets.kt), [`Image`](../src/main/kotlin/org/mdt/ui/compose/Image.kt)).
- `org.mdt.ui.layout`: Godot-inspired 2-pass layout algorithm ([`GodotLayout`](../src/main/kotlin/org/mdt/ui/layout/GodotLayout.kt)).
- `org.mdt.ui.render`: GPU rendering pipeline ([`EngineRenderer`](../src/main/kotlin/org/mdt/ui/render/EngineRenderer.kt), BoxRenderer, BoxBlur).
