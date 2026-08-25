# Bản đồ Cấu trúc Mã nguồn (Codebase Architecture Map)

Tài liệu này cung cấp danh mục chi tiết toàn bộ các tệp nguồn trong dự án NekoMod, số dòng mã (LOC), phân loại trách nhiệm đơn lẻ (Single Responsibility), và các API cốt lõi được xuất ra.

---

## 📊 Tổng quan Thống kê Dự án
* **Tổng số tệp Kotlin (`.kt`):** 49 tệp
* **Tổng số dòng mã:** ~4,450 dòng
* **Kiến trúc:** 100% Kotlin Multiplatform, Compose Virtual DOM, SDF GPU Shaders, 0% Arc Scene2D dependency.

---

## 1. ⚙️ Hạ tầng Cốt lõi (`org.mdt.core.*`)

| Đường dẫn tệp | LOC | Trách nhiệm Cốt lõi | API / Biểu tượng Chính |
| :--- | :---: | :--- | :--- |
| [`core/async/AsyncDispatcher.kt`](../../src/main/kotlin/org/mdt/core/async/AsyncDispatcher.kt) | 58 | Điều phối Coroutine I/O và chuyển luồng về Main Thread | `AsyncDispatcher.launch`, `onMainThread` |
| [`core/net/HttpEngine.kt`](../../src/main/kotlin/org/mdt/core/net/HttpEngine.kt) | 52 | Singleton OkHttp 4.12 với Connection Pooling | `HttpEngine.client`, `call` |
| [`core/net/NetDsl.kt`](../../src/main/kotlin/org/mdt/core/net/NetDsl.kt) | 142 | DSL gửi HTTP GET/POST/Streaming bất đồng bộ | `Net.get()`, `Net.post()`, `awaitBytes()` |
| [`core/store/Storage.kt`](../../src/main/kotlin/org/mdt/core/store/Storage.kt) | 95 | Quản lý file Okio, ghi đĩa nguyên tử (Atomic Write) | `Storage.atomicWrite`, `readString` |
| [`core/store/KVStore.kt`](../../src/main/kotlin/org/mdt/core/store/KVStore.kt) | 102 | Bộ nhớ Key-Value Debounce 300ms chống nghẽn đĩa | `KVStore.default`, `putString`, `getInt` |
| [`core/cache/ResourceHandle.kt`](../../src/main/kotlin/org/mdt/core/cache/ResourceHandle.kt) | 56 | Đếm tham chiếu vòng đời tài nguyên GPU | `TextureHandle`, `acquire()`, `release()` |
| [`core/cache/LRUTextureCache.kt`](../../src/main/kotlin/org/mdt/core/cache/LRUTextureCache.kt) | 97 | Bộ nhớ đệm 64MB VRAM giới hạn theo thuật toán LRU | `LRUTextureCache.shared`, `get()`, `put()` |
| [`core/image/ImageSource.kt`](../../src/main/kotlin/org/mdt/core/image/ImageSource.kt) | 36 | Nguồn gốc ảnh (URL, Asset, File, Sprite Atlas) | `ImageSource.Url`, `Asset`, `Region`, `of()` |
| [`core/image/ImageLoader.kt`](../../src/main/kotlin/org/mdt/core/image/ImageLoader.kt) | 156 | Pipeline tải ảnh bất đồng bộ + Hàng đợi nạp GPU | `ImageLoader.load()`, `processUploadQueue()` |
| [`core/i18n/I18nEngine.kt`](../../src/main/kotlin/org/mdt/core/i18n/I18nEngine.kt) | 72 | Đa ngôn ngữ phân cấp lồng nhau với nội suy `{param}` | `I18nEngine.t()`, `i18n()` |

---

## 2. 🌲 Cây Virtual DOM Cốt lõi (`org.mdt.ui.core.*`)

| Đường dẫn tệp | LOC | Trách nhiệm Cốt lõi | API / Biểu tượng Chính |
| :--- | :---: | :--- | :--- |
| [`ui/core/Math.kt`](../../src/main/kotlin/org/mdt/ui/core/Math.kt) | 41 | Cấu trúc dữ liệu hình học bất biến (`Float`) | `Vec2`, `Rect`, `Insets` |
| [`ui/core/Events.kt`](../../src/main/kotlin/org/mdt/ui/core/Events.kt) | 22 | Sự kiện tương tác con trỏ và cuộn chuột | `PointerEvent`, `ScrollEvent` |
| [`ui/core/UINode.kt`](../../src/main/kotlin/org/mdt/ui/core/UINode.kt) | 431 | Lớp cơ sở cho toàn bộ Node Virtual DOM | `UINode`, `hitTest()`, `draw()`, `layout()` |
| [`ui/core/CanvasNode.kt`](../../src/main/kotlin/org/mdt/ui/core/CanvasNode.kt) | 40 | Node gốc (Root) đại diện cho toàn bộ màn hình game | `CanvasNode.resize()` |

---

## 3. 📐 Thuật toán Bố cục Dàn trang (`org.mdt.ui.layout.*`)

| Đường dẫn tệp | LOC | Trách nhiệm Cốt lõi | API / Biểu tượng Chính |
| :--- | :---: | :--- | :--- |
| [`ui/layout/SizeFlags.kt`](../../src/main/kotlin/org/mdt/ui/layout/SizeFlags.kt) | 16 | Cờ kích thước Godot (`FILL`, `EXPAND`, `SHRINK`) | `SizeFlags` |
| [`ui/layout/AnchorData.kt`](../../src/main/kotlin/org/mdt/ui/layout/AnchorData.kt) | 78 | Neo tọa độ 4 điểm (`0.0..1.0`) và pixel offset | `AnchorData`, `LayoutPreset` |
| [`ui/layout/Alignment.kt`](../../src/main/kotlin/org/mdt/ui/layout/Alignment.kt) | 64 | Căn chỉnh và phân bổ phần tử con | `Alignment`, `Arrangement` |
| [`ui/layout/GodotLayout.kt`](../../src/main/kotlin/org/mdt/ui/layout/GodotLayout.kt) | 319 | Thuật toán dàn trang 2-Pass (Box, Grid, Anchor) | `GodotLayout.layoutBox`, `layoutGrid` |
| [`ui/layout/policy/MeasurePolicy.kt`](../../src/main/kotlin/org/mdt/ui/layout/policy/MeasurePolicy.kt) | 170 | Các chiến lược đo đạc và định vị độc lập | `ColumnMeasurePolicy`, `RowMeasurePolicy`, `BoxMeasurePolicy` |

---

## 4. 🎨 Đồ họa & GPU Shaders (`org.mdt.ui.render.*`)

| Đường dẫn tệp | LOC | Trách nhiệm Cốt lõi | API / Biểu tượng Chính |
| :--- | :---: | :--- | :--- |
| [`ui/render/Shaders.kt`](../../src/main/kotlin/org/mdt/ui/render/Shaders.kt) | 72 | Trình biên dịch GLSL 120 Uber-Shader | `Shaders.mainShader`, `ensure()` |
| [`ui/render/BoxRenderer.kt`](../../src/main/kotlin/org/mdt/ui/render/BoxRenderer.kt) | 147 | Vẽ hộp SDF bo góc, viền, bóng đổ, glow qua GPU | `BoxRenderer.draw()` |
| [`ui/render/BoxBlur.kt`](../../src/main/kotlin/org/mdt/ui/render/BoxBlur.kt) | 110 | Bộ xử lý mờ kính 2-Pass Ping-Pong Gaussian FBO | `BoxBlur.capture()`, `dispose()` |
| [`ui/render/TextRenderer.kt`](../../src/main/kotlin/org/mdt/ui/render/TextRenderer.kt) | 72 | Định dạng chữ BMFont và căn chỉnh | `TextRenderer.draw()` |
| [`ui/render/ScissorStack.kt`](../../src/main/kotlin/org/mdt/ui/render/ScissorStack.kt) | 92 | Ngăn xếp cắt xén phần cứng OpenGL Scissor | `ScissorStack.push()`, `pop()` |
| [`ui/render/EngineRenderer.kt`](../../src/main/kotlin/org/mdt/ui/render/EngineRenderer.kt) | 95 | Bộ vẽ tổng điều phối FBO và ma trận chiếu | `EngineRenderer.render()` |

---

## 5. ⌨️ Bộ máy Trạng thái & Bàn phím (`org.mdt.ui.input.*`)

| Đường dẫn tệp | LOC | Trách nhiệm Cốt lõi | API / Biểu tượng Chính |
| :--- | :---: | :--- | :--- |
| [`ui/input/EngineInputProcessor.kt`](../../src/main/kotlin/org/mdt/ui/input/EngineInputProcessor.kt) | 173 | Xử lý cảm ứng, hover, click kép và Focus Routing | `EngineInputProcessor`, `requestFocus()` |
| [`ui/input/TextEditState.kt`](../../src/main/kotlin/org/mdt/ui/input/TextEditState.kt) | 279 | Bộ máy trạng thái chuỗi ký tự, con trỏ, clipboard, Telex | `TextEditState`, `insert()`, `onKeyDown()` |

---

## 6. 📦 Các Phần tử Virtual DOM (`org.mdt.ui.widgets.*`)

| Đường dẫn tệp | LOC | Trách nhiệm Cốt lõi | API / Biểu tượng Chính |
| :--- | :---: | :--- | :--- |
| [`ui/widgets/LayoutNode.kt`](../../src/main/kotlin/org/mdt/ui/widgets/LayoutNode.kt) | 89 | Node container thống nhất với Visuals lười cấp phát | `LayoutNode`, `ensureVisuals()` |
| [`ui/widgets/BoxNode.kt`](../../src/main/kotlin/org/mdt/ui/widgets/BoxNode.kt) | 15 | Lớp hộp tương thích ngược | `BoxNode` |
| [`ui/widgets/BoxVisuals.kt`](../../src/main/kotlin/org/mdt/ui/widgets/BoxVisuals.kt) | 202 | Dữ liệu cấu hình màu nền, viền, bóng đổ, blur | `BoxVisuals`, `isVisible()` |
| [`ui/widgets/TextNode.kt`](../../src/main/kotlin/org/mdt/ui/widgets/TextNode.kt) | 77 | Node hiển thị văn bản BMFont | `TextNode` |
| [`ui/widgets/TextVisuals.kt`](../../src/main/kotlin/org/mdt/ui/widgets/TextVisuals.kt) | 50 | Thuộc tính typography (màu, scale, wrap) | `TextVisuals` |
| [`ui/widgets/ImageNode.kt`](../../src/main/kotlin/org/mdt/ui/widgets/ImageNode.kt) | 121 | Node hiển thị ảnh Texture với chế độ Scale | `ImageNode`, `ScaleMode` |
| [`ui/widgets/TextFieldNode.kt`](../../src/main/kotlin/org/mdt/ui/widgets/TextFieldNode.kt) | 206 | Node ô nhập liệu với viền phát sáng Focus Glow | `TextFieldNode` |
| [`ui/widgets/SliderNode.kt`](../../src/main/kotlin/org/mdt/ui/widgets/SliderNode.kt) | 133 | Node thanh trượt kéo thả giá trị | `SliderNode` |
| [`ui/widgets/ProgressBarNode.kt`](../../src/main/kotlin/org/mdt/ui/widgets/ProgressBarNode.kt) | 62 | Node thanh tiến độ phần trăm | `ProgressBarNode` |
| [`ui/widgets/ScrollContainerNode.kt`](../../src/main/kotlin/org/mdt/ui/widgets/ScrollContainerNode.kt) | 147 | Node khung cuộn với thanh cuộn hiện đại | `ScrollContainerNode` |
| [`ui/widgets/Containers.kt`](../../src/main/kotlin/org/mdt/ui/widgets/Containers.kt) | 100 | Các lớp container tương thích ngược | `RowNode`, `ColumnNode`, `GridContainerNode` |

---

## 7. 🧱 Tầng Compose DSL (`org.mdt.ui.compose.*`)

| Đường dẫn tệp | LOC | Trách nhiệm Cốt lõi | API / Biểu tượng Chính |
| :--- | :---: | :--- | :--- |
| [`ui/compose/DslMarker.kt`](../../src/main/kotlin/org/mdt/ui/compose/DslMarker.kt) | 4 | Annotation bảo vệ phạm vi DSL | `@UIDslMarker` |
| [`ui/compose/Dsl.kt`](../../src/main/kotlin/org/mdt/ui/compose/Dsl.kt) | 11 | Phím tắt tạo `Modifier` | `Modifier` |
| [`ui/compose/NodeApplier.kt`](../../src/main/kotlin/org/mdt/ui/compose/NodeApplier.kt) | 28 | Applier kết nối Compose Runtime với cây UINode | `NodeApplier` |
| [`ui/compose/UIComposition.kt`](../../src/main/kotlin/org/mdt/ui/compose/UIComposition.kt) | 67 | Quản lý vòng đời Recomposition | `UIComposition`, `CompositionManager` |
| [`ui/compose/Scopes.kt`](../../src/main/kotlin/org/mdt/ui/compose/Scopes.kt) | 69 | Phạm vi Composable (`BoxScope`, `RowScope`, ...) | `BoxScope`, `RowScope`, `ColumnScope` |
| [`ui/compose/UIModifier.kt`](../../src/main/kotlin/org/mdt/ui/compose/UIModifier.kt) | 280 | Chuỗi Modifier cấu hình layout, visual, event | `Modifier.pad()`, `.background()`, `.size()` |
| [`ui/compose/Components.kt`](../../src/main/kotlin/org/mdt/ui/compose/Components.kt) | 255 | Các layout container cơ bản | `Box`, `Row`, `Column`, `Grid`, `Spacer`, `Text` |
| [`ui/compose/Widgets.kt`](../../src/main/kotlin/org/mdt/ui/compose/Widgets.kt) | 233 | Các widget tương tác giao diện | `Button`, `Toggle`, `Card`, `Divider` |
| [`ui/compose/Image.kt`](../../src/main/kotlin/org/mdt/ui/compose/Image.kt) | 43 | Composable hiển thị ảnh | `Image()` |
| [`ui/compose/TextField.kt`](../../src/main/kotlin/org/mdt/ui/compose/TextField.kt) | 90 | Composable ô nhập văn bản | `TextField()` |
| [`ui/compose/Slider.kt`](../../src/main/kotlin/org/mdt/ui/compose/Slider.kt) | 78 | Composable thanh trượt | `Slider()` |
| [`ui/compose/ProgressBar.kt`](../../src/main/kotlin/org/mdt/ui/compose/ProgressBar.kt) | 68 | Composable thanh tiến độ | `ProgressBar()` |
| [`ui/compose/ScrollView.kt`](../../src/main/kotlin/org/mdt/ui/compose/ScrollView.kt) | 45 | Composable khung cuộn viewport | `ScrollView()` |
| [`ui/compose/Tooltip.kt`](../../src/main/kotlin/org/mdt/ui/compose/Tooltip.kt) | 114 | Gợi ý nổi khi rê chuột | `Modifier.tooltip()` |

---

## 8. 🚀 Điểm Vào Game (`org.mdt.*`)

| Đường dẫn tệp | LOC | Trách nhiệm Cốt lõi | API / Biểu tượng Chính |
| :--- | :---: | :--- | :--- |
| [`ui/EngineRuntime.kt`](../../src/main/kotlin/org/mdt/ui/EngineRuntime.kt) | 80 | Nhịp cầu kết nối Render loop 60 FPS & Compose | `EngineRuntime.setContent()`, `draw()` |
| [`mod/NekoMod.kt`](../../src/main/kotlin/org/mdt/mod/NekoMod.kt) | 130 | Điểm khởi đầu của Mod Mindustry & Demo Showcase | `NekoMod.init()` |
