# NekoMod Pure KMP UI Engine Documentation

Chào mừng bạn đến với hệ thống tài liệu kiến trúc chuẩn mực của **NekoMod Declarative UI Engine & Core Subsystems** (Pure Virtual Node Tree chạy trên GPU OpenGL cho Mindustry).

---

## 📚 Mục Lục Tài Liệu / Documentation Index

| STT | Chuyên mục Kiến trúc / Architecture Domain | Bản Tiếng Việt 🇻🇳 | English Version 🇬🇧 |
| :---: | :--- | :--- | :--- |
| **1** | **19 Tiêu chuẩn Lập trình & Bất biến Dự án**<br>*Coding Standards & Project Invariants* | [coding_standards_vi.md](./coding-standards/coding_standards_vi.md) | [coding_standards_en.md](./coding-standards/coding_standards_en.md) |
| **2** | **Kiến trúc Lõi Virtual DOM & 5 Tầng Hệ thống**<br>*Engine Architecture & Unified DOM Tree* | [architecture_vi.md](./architecture/architecture_vi.md) | [architecture_en.md](./architecture/architecture_en.md) |
| **3** | **Thuật toán Bố cục, Hug Content & Neo 4 Điểm**<br>*Layout Engine, Hug Content & Anchors* | [layout_engine_vi.md](./layout-engine/layout_engine_vi.md) | [layout_engine_en.md](./layout-engine/layout_engine_en.md) |
| **4** | **GPU Rendering, SDF Uber-Shader & BoxBlur**<br>*SDF Shaders & 2-Pass Gaussian Blur* | [rendering_shaders_vi.md](./rendering-shaders/rendering_shaders_vi.md) | [rendering_shaders_en.md](./rendering-shaders/rendering_shaders_en.md) |
| **5** | **Hướng dẫn Composable DSL & UIModifier**<br>*Compose Multiplatform DSL & Modifiers* | [compose_dsl_vi.md](./compose-dsl/compose_dsl_vi.md) | [compose_dsl_en.md](./compose-dsl/compose_dsl_en.md) |
| **6** | **Hạ tầng Lõi (Network, Storage, Image, i18n)**<br>*Core Subsystems Architecture* | [core_subsystems_vi.md](./core-subsystems/core_subsystems_vi.md) | [core_subsystems_en.md](./core-subsystems/core_subsystems_en.md) |
| **7** | **Lộ trình Phát triển Dự án**<br>*Development Roadmap* | [roadmap_vi.md](./roadmap/roadmap_vi.md) | [roadmap_en.md](./roadmap/roadmap_en.md) |

---

## 🚀 Cấu trúc Không gian Tên Mã nguồn / Source Code Mapping

- `org.mdt.core.async`: Coroutines & Thread dispatching ([`AsyncDispatcher`](../src/main/kotlin/org/mdt/core/async/AsyncDispatcher.kt)).
- `org.mdt.core.net`: OkHttp Network engine & Fluent DSL ([`NetDsl`](../src/main/kotlin/org/mdt/core/net/NetDsl.kt)).
- `org.mdt.core.store`: Okio atomic storage & KV store ([`KVStore`](../src/main/kotlin/org/mdt/core/store/KVStore.kt)).
- `org.mdt.core.cache`: Safe reference-counted VRAM cache ([`LRUTextureCache`](../src/main/kotlin/org/mdt/core/cache/LRUTextureCache.kt)).
- `org.mdt.core.image`: Asynchronous image pipeline ([`ImageLoader`](../src/main/kotlin/org/mdt/core/image/ImageLoader.kt)).
- `org.mdt.core.i18n`: Modern nested key localization ([`I18nEngine`](../src/main/kotlin/org/mdt/core/i18n/I18nEngine.kt)).
- `org.mdt.core.ui`: Virtual Node DOM tree primitives ([`UINode`](../src/main/kotlin/org/mdt/core/ui/UINode.kt), [`CanvasNode`](../src/main/kotlin/org/mdt/core/ui/CanvasNode.kt)).
- `org.mdt.ui.compose`: Compose Multiplatform bridge ([`UIComposition`](../src/main/kotlin/org/mdt/ui/compose/UIComposition.kt), [`UIModifier`](../src/main/kotlin/org/mdt/ui/compose/UIModifier.kt)).
- `org.mdt.ui.components.*`: Domain-Sliced UI Widgets (`layout`, `text`, `input`, `display`, `surface`, `scroll`).
- `org.mdt.ui.layout`: 2-Pass Godot Container layout & Measure Policies ([`GodotLayout`](../src/main/kotlin/org/mdt/ui/layout/GodotLayout.kt)).
- `org.mdt.ui.render`: GPU rendering pipeline ([`EngineRenderer`](../src/main/kotlin/org/mdt/ui/render/EngineRenderer.kt), [`BoxRenderer`](../src/main/kotlin/org/mdt/ui/render/BoxRenderer.kt)).
