# Bản đồ Cấu trúc Mã nguồn (Codebase Architecture Map)

Tài liệu này cung cấp danh mục chi tiết toàn bộ các tệp nguồn trong dự án NekoMod sau khi đã được tái cấu trúc theo mô hình **Feature Slicing & Co-location (Gom nhóm theo Thành phần Tính năng)**.

---

## 📊 Tổng quan Thống kê Dự án
* **Tổng số tệp Kotlin (`.kt`):** 49 tệp
* **Mô hình tổ chức:** Feature Co-location — Virtual Node, Composable và State Machine của cùng một Widget nằm chung trong một thư mục chuyên biệt.
* **Số lượng file trên mỗi thư mục:** Tối đa 2–4 file (Dễ đọc, dễ nhớ, dễ sửa đổi).

---

## 1. ⚙️ Hạ tầng Cốt lõi (`org.mdt.core.*`)

| Thư mục / Tệp | Trách nhiệm Cốt lõi | API Chính |
| :--- | :--- | :--- |
| `core/async/AsyncDispatcher.kt` | Coroutine I/O & Main-Thread synchronization | `AsyncDispatcher.launch`, `onMainThread` |
| `core/net/HttpEngine.kt` | Singleton OkHttp 4.12 Connection Pool | `HttpEngine.client`, `call` |
| `core/net/NetDsl.kt` | DSL gửi HTTP GET/POST bất đồng bộ | `Net.get()`, `Net.post()`, `awaitBytes()` |
| `core/store/Storage.kt` | Ghi đĩa nguyên tử Okio (Atomic Write) | `Storage.atomicWrite`, `readString` |
| `core/store/KVStore.kt` | Bộ nhớ Key-Value Debounce 300ms | `KVStore.default`, `putString`, `getFloat` |
| `core/cache/ResourceHandle.kt` | Đếm tham chiếu vòng đời Texture | `TextureHandle`, `acquire()`, `release()` |
| `core/cache/LRUTextureCache.kt` | Bộ nhớ đệm 64MB VRAM LRU | `LRUTextureCache.shared`, `get()`, `put()` |
| `core/image/ImageSource.kt` | Định danh nguồn ảnh (URL, Asset, Atlas) | `ImageSource.Url`, `Asset`, `Region`, `of()` |
| `core/image/ImageLoader.kt` | Tải ảnh bất đồng bộ + Throttling VRAM | `ImageLoader.load()`, `processUploadQueue()` |
| `core/i18n/I18nEngine.kt` | Đa ngôn ngữ phân cấp lồng nhau `{param}` | `I18nEngine.t()`, `i18n()` |

---

## 2. 🌲 Virtual DOM & Render Pipeline (`org.mdt.ui.*`)

| Thư mục / Tệp | Trách nhiệm Cốt lõi | API Chính |
| :--- | :--- | :--- |
| `ui/core/UINode.kt` | Lớp cơ sở cây Virtual DOM | `UINode`, `hitTest()`, `draw()`, `layout()` |
| `ui/core/CanvasNode.kt` | Node gốc Canvas toàn màn hình | `CanvasNode.resize()` |
| `ui/core/Math.kt` | Cấu trúc hình học bất biến | `Vec2`, `Rect`, `Insets` |
| `ui/core/Events.kt` | Sự kiện con trỏ và cuộn chuột | `PointerEvent`, `ScrollEvent` |
| `ui/layout/GodotLayout.kt` | Thuật toán dàn trang 2-Pass Godot | `GodotLayout.layoutBox`, `layoutGrid` |
| `ui/layout/AnchorData.kt` | Neo tọa độ 4 điểm & Presets | `AnchorData`, `LayoutPreset` |
| `ui/layout/Alignment.kt` | Căn chỉnh và khoảng cách | `Alignment`, `Arrangement` |
| `ui/layout/policy/MeasurePolicy.kt` | Chiến lược đo đạc và bố cục | `BoxMeasurePolicy`, `ColumnMeasurePolicy`, `RowMeasurePolicy` |
| `ui/render/EngineRenderer.kt` | Master GPU Renderer & FBO | `EngineRenderer.render()` |
| `ui/render/BoxRenderer.kt` | Vẽ hộp SDF bo góc, viền, bóng đổ, glow | `BoxRenderer.draw()` |
| `ui/render/BoxBlur.kt` | Mờ kính 2-Pass Ping-Pong Gaussian FBO | `BoxBlur.capture()` |
| `ui/render/ScissorStack.kt` | Cắt xén phần cứng OpenGL Scissor | `ScissorStack.push()`, `pop()` |
| `ui/render/Shaders.kt` | GLSL 120 Uber-Shader Compiler | `Shaders.mainShader`, `ensure()` |
| `ui/render/TextRenderer.kt` | Vẽ BMFont & Cache định dạng chữ | `TextRenderer.draw()` |
| `ui/input/EngineInputProcessor.kt` | Reverse-DFS Hit-Testing & Focus Routing | `EngineInputProcessor`, `requestFocus()` |
| `ui/EngineRuntime.kt` | Cầu nối Render loop 60 FPS & Compose | `EngineRuntime.setContent()`, `draw()` |

---

## 3. 🧩 Tầng Compose Nền tảng (`org.mdt.ui.compose.*`)

| Tệp | Trách nhiệm Cốt lõi |
| :--- | :--- |
| `compose/UIModifier.kt` | Chuỗi Modifier cấu hình bố cục, màu sắc, hiệu ứng thị giác và tương tác |
| `compose/UIComposition.kt` | Quản lý vòng đời Recomposition và FrameClock |
| `compose/NodeApplier.kt` | Applier kết nối Compose Runtime với cây Virtual DOM |
| `compose/Scopes.kt` | Phạm vi Composable (`BoxScope`, `RowScope`, `ColumnScope`, `GridScope`) |
| `compose/Dsl.kt` | Phím tắt tạo `Modifier` |
| `compose/DslMarker.kt` | Annotation `@UIDslMarker` kiểm soát phạm vi gọi |

---

## 4. 📦 Bộ Thành phần Giao diện theo Domain (`org.mdt.ui.components.*`)

### 🔲 4.1 Bố cục Dàn trang (`components.layout.*`)
* [`LayoutNode.kt`](../../src/main/kotlin/org/mdt/ui/components/layout/LayoutNode.kt): Container thống nhất với Lazy Visuals (Zero-Overhead).
* [`BoxVisuals.kt`](../../src/main/kotlin/org/mdt/ui/components/layout/BoxVisuals.kt): Cấu hình màu nền, viền, bóng đổ, glow, backdrop.
* [`Box.kt`](../../src/main/kotlin/org/mdt/ui/components/layout/Box.kt): Composable `Box()`.
* [`FlexLayouts.kt`](../../src/main/kotlin/org/mdt/ui/components/layout/FlexLayouts.kt): Composable `Row()` và `Column()`.
* [`Grid.kt`](../../src/main/kotlin/org/mdt/ui/components/layout/Grid.kt): Composable `Grid()`.
* [`Spacer.kt`](../../src/main/kotlin/org/mdt/ui/components/layout/Spacer.kt): Composable `Spacer()`.
* [`Containers.kt`](../../src/main/kotlin/org/mdt/ui/components/layout/Containers.kt): Node container tương thích ngược.

### 📝 4.2 Văn bản (`components.text.*`)
* [`Text.kt`](../../src/main/kotlin/org/mdt/ui/components/text/Text.kt): Composable `Text()`.
* [`TextNode.kt`](../../src/main/kotlin/org/mdt/ui/components/text/TextNode.kt): Virtual DOM text node.
* [`TextVisuals.kt`](../../src/main/kotlin/org/mdt/ui/components/text/TextVisuals.kt): Typography configuration.

### ⌨️ 4.3 Nhập liệu & Kéo thả (`components.input.*`)
* **Ô nhập văn bản (`components.input.textfield.*`):**
  * [`TextEditState.kt`](../../src/main/kotlin/org/mdt/ui/components/input/textfield/TextEditState.kt): State machine (con trỏ, selection, clipboard, Telex).
  * [`TextFieldNode.kt`](../../src/main/kotlin/org/mdt/ui/components/input/textfield/TextFieldNode.kt): Virtual DOM text field node với Focus Glow.
  * [`TextField.kt`](../../src/main/kotlin/org/mdt/ui/components/input/textfield/TextField.kt): Composable `TextField()`.
* **Thanh trượt (`components.input.slider.*`):**
  * [`SliderNode.kt`](../../src/main/kotlin/org/mdt/ui/components/input/slider/SliderNode.kt): Virtual DOM draggable slider node.
  * [`Slider.kt`](../../src/main/kotlin/org/mdt/ui/components/input/slider/Slider.kt): Composable `Slider()`.

### 🖼️ 4.4 Hiển thị (`components.display.*`)
* **Ảnh (`components.display.image.*`):**
  * [`ImageNode.kt`](../../src/main/kotlin/org/mdt/ui/components/display/image/ImageNode.kt): Virtual DOM image node với đếm tham chiếu GPU.
  * [`Image.kt`](../../src/main/kotlin/org/mdt/ui/components/display/image/Image.kt): Composable `Image()`.
* **Thanh tiến độ (`components.display.progress.*`):**
  * [`ProgressBarNode.kt`](../../src/main/kotlin/org/mdt/ui/components/display/progress/ProgressBarNode.kt): Virtual DOM progress bar node.
  * [`ProgressBar.kt`](../../src/main/kotlin/org/mdt/ui/components/display/progress/ProgressBar.kt): Composable `ProgressBar()`.
* **Gợi ý (`components.display.tooltip.*`):**
  * [`Tooltip.kt`](../../src/main/kotlin/org/mdt/ui/components/display/tooltip/Tooltip.kt): `TooltipNode` và `Modifier.tooltip()`.

### 🎛️ 4.5 Bề mặt & Nút bấm (`components.surface.*`)
* [`Button.kt`](../../src/main/kotlin/org/mdt/ui/components/surface/Button.kt): Composable `Button()` và `ButtonColors`.
* [`Card.kt`](../../src/main/kotlin/org/mdt/ui/components/surface/Card.kt): Composable `Card()` kính mờ Glassmorphism.
* [`Toggle.kt`](../../src/main/kotlin/org/mdt/ui/components/surface/Toggle.kt): Composable `Toggle()` công tắc viên thuốc.
* [`Divider.kt`](../../src/main/kotlin/org/mdt/ui/components/surface/Divider.kt): Composable `Divider()` đường kẻ phân cách.

### 📜 4.6 Khung cuộn (`components.scroll.*`)
* [`ScrollContainerNode.kt`](../../src/main/kotlin/org/mdt/ui/components/scroll/ScrollContainerNode.kt): Viewport cuộn với thanh cuộn hiện đại.
* [`ScrollView.kt`](../../src/main/kotlin/org/mdt/ui/components/scroll/ScrollView.kt): Composable `ScrollView()`.
