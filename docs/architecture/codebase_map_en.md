# Codebase Architecture Map

This document provides a comprehensive directory breakdown of all source files across NekoMod, including exact line counts (LOC), single-responsibility boundaries, and key exported symbols.

---

## 📊 Project Statistics Overview
* **Total Kotlin Source Files (`.kt`):** 49 files
* **Total Lines of Code:** ~4,450 lines
* **Architecture:** 100% Kotlin Multiplatform, Compose Virtual DOM, SDF GPU Shaders, 0% Arc Scene2D dependency.

---

## 1. ⚙️ Core Subsystems (`org.mdt.core.*`)

| File Path | LOC | Single Responsibility | Key Exported Symbols |
| :--- | :---: | :--- | :--- |
| [`core/async/AsyncDispatcher.kt`](../../src/main/kotlin/org/mdt/core/async/AsyncDispatcher.kt) | 58 | Coroutine I/O dispatching & GL Main-Thread synchronization | `AsyncDispatcher.launch`, `onMainThread` |
| [`core/net/HttpEngine.kt`](../../src/main/kotlin/org/mdt/core/net/HttpEngine.kt) | 52 | OkHttp 4.12 singleton with HTTP/2 connection pooling | `HttpEngine.client`, `call` |
| [`core/net/NetDsl.kt`](../../src/main/kotlin/org/mdt/core/net/NetDsl.kt) | 142 | Fluent asynchronous HTTP GET/POST/Streaming DSL | `Net.get()`, `Net.post()`, `awaitBytes()` |
| [`core/store/Storage.kt`](../../src/main/kotlin/org/mdt/core/store/Storage.kt) | 95 | Okio file operations with atomic crash-resilient write | `Storage.atomicWrite`, `readString` |
| [`core/store/KVStore.kt`](../../src/main/kotlin/org/mdt/core/store/KVStore.kt) | 102 | Debounced 300ms persistent Key-Value storage | `KVStore.default`, `putString`, `getInt` |
| [`core/cache/ResourceHandle.kt`](../../src/main/kotlin/org/mdt/core/cache/ResourceHandle.kt) | 56 | Reference-counted GPU texture lifecycle handle | `TextureHandle`, `acquire()`, `release()` |
| [`core/cache/LRUTextureCache.kt`](../../src/main/kotlin/org/mdt/core/cache/LRUTextureCache.kt) | 97 | 64MB VRAM-bounded LRU texture cache | `LRUTextureCache.shared`, `get()`, `put()` |
| [`core/image/ImageSource.kt`](../../src/main/kotlin/org/mdt/core/image/ImageSource.kt) | 36 | Image origin polymorphic representation (URL, Asset, Atlas) | `ImageSource.Url`, `Asset`, `Region`, `of()` |
| [`core/image/ImageLoader.kt`](../../src/main/kotlin/org/mdt/core/image/ImageLoader.kt) | 156 | Async non-blocking image loader with GPU upload throttle | `ImageLoader.load()`, `processUploadQueue()` |
| [`core/i18n/I18nEngine.kt`](../../src/main/kotlin/org/mdt/core/i18n/I18nEngine.kt) | 72 | Hierarchical nested key localization with `{param}` | `I18nEngine.t()`, `i18n()` |

---

## 2. 🌲 Virtual DOM Core (`org.mdt.ui.core.*`)

| File Path | LOC | Single Responsibility | Key Exported Symbols |
| :--- | :---: | :--- | :--- |
| [`ui/core/Math.kt`](../../src/main/kotlin/org/mdt/ui/core/Math.kt) | 41 | Immutable geometric data structures (`Float`) | `Vec2`, `Rect`, `Insets` |
| [`ui/core/Events.kt`](../../src/main/kotlin/org/mdt/ui/core/Events.kt) | 22 | Pointer and scroll event objects | `PointerEvent`, `ScrollEvent` |
| [`ui/core/UINode.kt`](../../src/main/kotlin/org/mdt/ui/core/UINode.kt) | 431 | Base Virtual DOM tree node | `UINode`, `hitTest()`, `draw()`, `layout()` |
| [`ui/core/CanvasNode.kt`](../../src/main/kotlin/org/mdt/ui/core/CanvasNode.kt) | 40 | Root Canvas representing full viewport bounds | `CanvasNode.resize()` |

---

## 3. 📐 Layout Engine (`org.mdt.ui.layout.*`)

| File Path | LOC | Single Responsibility | Key Exported Symbols |
| :--- | :---: | :--- | :--- |
| [`ui/layout/SizeFlags.kt`](../../src/main/kotlin/org/mdt/ui/layout/SizeFlags.kt) | 16 | Godot layout sizing behavior bitflags | `SizeFlags` |
| [`ui/layout/AnchorData.kt`](../../src/main/kotlin/org/mdt/ui/layout/AnchorData.kt) | 78 | 4-point anchors (`0.0..1.0`) and margin offsets | `AnchorData`, `LayoutPreset` |
| [`ui/layout/Alignment.kt`](../../src/main/kotlin/org/mdt/ui/layout/Alignment.kt) | 64 | Alignment and arrangement configuration | `Alignment`, `Arrangement` |
| [`ui/layout/GodotLayout.kt`](../../src/main/kotlin/org/mdt/ui/layout/GodotLayout.kt) | 319 | 2-Pass linear, grid, and anchor layout calculation | `GodotLayout.layoutBox`, `layoutGrid` |
| [`ui/layout/policy/MeasurePolicy.kt`](../../src/main/kotlin/org/mdt/ui/layout/policy/MeasurePolicy.kt) | 170 | Decoupled measurement & layout strategies | `ColumnMeasurePolicy`, `RowMeasurePolicy`, `BoxMeasurePolicy` |

---

## 4. 🎨 Rendering & GPU Shaders (`org.mdt.ui.render.*`)

| File Path | LOC | Single Responsibility | Key Exported Symbols |
| :--- | :---: | :--- | :--- |
| [`ui/render/Shaders.kt`](../../src/main/kotlin/org/mdt/ui/render/Shaders.kt) | 72 | GLSL 120 Uber-Shader compiler and uniforms | `Shaders.mainShader`, `ensure()` |
| [`ui/render/BoxRenderer.kt`](../../src/main/kotlin/org/mdt/ui/render/BoxRenderer.kt) | 147 | GPU SDF quad renderer for boxes, borders, and glows | `BoxRenderer.draw()` |
| [`ui/render/BoxBlur.kt`](../../src/main/kotlin/org/mdt/ui/render/BoxBlur.kt) | 110 | 2-Pass Ping-Pong Gaussian FBO blur coordinator | `BoxBlur.capture()`, `dispose()` |
| [`ui/render/TextRenderer.kt`](../../src/main/kotlin/org/mdt/ui/render/TextRenderer.kt) | 72 | BMFont glyph rendering & measurement cache | `TextRenderer.draw()` |
| [`ui/render/ScissorStack.kt`](../../src/main/kotlin/org/mdt/ui/render/ScissorStack.kt) | 92 | Hardware OpenGL scissor clipping stack | `ScissorStack.push()`, `pop()` |
| [`ui/render/EngineRenderer.kt`](../../src/main/kotlin/org/mdt/ui/render/EngineRenderer.kt) | 95 | Master render pass orchestrator & FBO manager | `EngineRenderer.render()` |

---

## 5. ⌨️ Input & State Machine (`org.mdt.ui.input.*`)

| File Path | LOC | Single Responsibility | Key Exported Symbols |
| :--- | :---: | :--- | :--- |
| [`ui/input/EngineInputProcessor.kt`](../../src/main/kotlin/org/mdt/ui/input/EngineInputProcessor.kt) | 173 | Reverse-DFS hit testing and Focus Routing | `EngineInputProcessor`, `requestFocus()` |
| [`ui/input/TextEditState.kt`](../../src/main/kotlin/org/mdt/ui/input/TextEditState.kt) | 279 | Headless string manipulation, caret, selection, Telex | `TextEditState`, `insert()`, `onKeyDown()` |

---

## 6. 📦 Virtual DOM Nodes (`org.mdt.ui.widgets.*`)

| File Path | LOC | Single Responsibility | Key Exported Symbols |
| :--- | :---: | :--- | :--- |
| [`ui/widgets/LayoutNode.kt`](../../src/main/kotlin/org/mdt/ui/widgets/LayoutNode.kt) | 89 | Unified Virtual DOM container with lazy visuals | `LayoutNode`, `ensureVisuals()` |
| [`ui/widgets/BoxNode.kt`](../../src/main/kotlin/org/mdt/ui/widgets/BoxNode.kt) | 15 | Backward-compatible box node | `BoxNode` |
| [`ui/widgets/BoxVisuals.kt`](../../src/main/kotlin/org/mdt/ui/widgets/BoxVisuals.kt) | 202 | SDF styling data (fill, border, shadow, blur) | `BoxVisuals`, `isVisible()` |
| [`ui/widgets/TextNode.kt`](../../src/main/kotlin/org/mdt/ui/widgets/TextNode.kt) | 77 | BMFont text node | `TextNode` |
| [`ui/widgets/TextVisuals.kt`](../../src/main/kotlin/org/mdt/ui/widgets/TextVisuals.kt) | 50 | Typography styling properties | `TextVisuals` |
| [`ui/widgets/ImageNode.kt`](../../src/main/kotlin/org/mdt/ui/widgets/ImageNode.kt) | 121 | Texture image node with scale modes | `ImageNode`, `ScaleMode` |
| [`ui/widgets/TextFieldNode.kt`](../../src/main/kotlin/org/mdt/ui/widgets/TextFieldNode.kt) | 206 | Text input node with Focus Glow and Caret line | `TextFieldNode` |
| [`ui/widgets/SliderNode.kt`](../../src/main/kotlin/org/mdt/ui/widgets/SliderNode.kt) | 133 | Draggable slider node | `SliderNode` |
| [`ui/widgets/ProgressBarNode.kt`](../../src/main/kotlin/org/mdt/ui/widgets/ProgressBarNode.kt) | 62 | Percentage progress bar node | `ProgressBarNode` |
| [`ui/widgets/ScrollContainerNode.kt`](../../src/main/kotlin/org/mdt/ui/widgets/ScrollContainerNode.kt) | 147 | Scrollable viewport container with scrollbars | `ScrollContainerNode` |
| [`ui/widgets/Containers.kt`](../../src/main/kotlin/org/mdt/ui/widgets/Containers.kt) | 100 | Backward-compatible container nodes | `RowNode`, `ColumnNode`, `GridContainerNode` |

---

## 7. 🧱 Compose DSL Layer (`org.mdt.ui.compose.*`)

| File Path | LOC | Single Responsibility | Key Exported Symbols |
| :--- | :---: | :--- | :--- |
| [`ui/compose/DslMarker.kt`](../../src/main/kotlin/org/mdt/ui/compose/DslMarker.kt) | 4 | DSL scope control annotation | `@UIDslMarker` |
| [`ui/compose/Dsl.kt`](../../src/main/kotlin/org/mdt/ui/compose/Dsl.kt) | 11 | `Modifier` factory shortcuts | `Modifier` |
| [`ui/compose/NodeApplier.kt`](../../src/main/kotlin/org/mdt/ui/compose/NodeApplier.kt) | 28 | Tree applier binding Compose runtime to DOM | `NodeApplier` |
| [`ui/compose/UIComposition.kt`](../../src/main/kotlin/org/mdt/ui/compose/UIComposition.kt) | 67 | Recomposition lifecycle manager | `UIComposition`, `CompositionManager` |
| [`ui/compose/Scopes.kt`](../../src/main/kotlin/org/mdt/ui/compose/Scopes.kt) | 69 | Scope extensions (`BoxScope`, `RowScope`, ...) | `BoxScope`, `RowScope`, `ColumnScope` |
| [`ui/compose/UIModifier.kt`](../../src/main/kotlin/org/mdt/ui/compose/UIModifier.kt) | 280 | Fluent modifier chain for layout, visuals, events | `Modifier.pad()`, `.background()`, `.size()` |
| [`ui/compose/Components.kt`](../../src/main/kotlin/org/mdt/ui/compose/Components.kt) | 255 | Core layout container composables | `Box`, `Row`, `Column`, `Grid`, `Spacer`, `Text` |
| [`ui/compose/Widgets.kt`](../../src/main/kotlin/org/mdt/ui/compose/Widgets.kt) | 233 | Interactive UI composables | `Button`, `Toggle`, `Card`, `Divider` |
| [`ui/compose/Image.kt`](../../src/main/kotlin/org/mdt/ui/compose/Image.kt) | 43 | Composable image component | `Image()` |
| [`ui/compose/TextField.kt`](../../src/main/kotlin/org/mdt/ui/compose/TextField.kt) | 90 | Composable text field | `TextField()` |
| [`ui/compose/Slider.kt`](../../src/main/kotlin/org/mdt/ui/compose/Slider.kt) | 78 | Composable slider | `Slider()` |
| [`ui/compose/ProgressBar.kt`](../../src/main/kotlin/org/mdt/ui/compose/ProgressBar.kt) | 68 | Composable progress bar | `ProgressBar()` |
| [`ui/compose/ScrollView.kt`](../../src/main/kotlin/org/mdt/ui/compose/ScrollView.kt) | 45 | Composable scrollable container | `ScrollView()` |
| [`ui/compose/Tooltip.kt`](../../src/main/kotlin/org/mdt/ui/compose/Tooltip.kt) | 114 | Hover tooltip modifier | `Modifier.tooltip()` |

---

## 8. 🚀 Game Entry & App (`org.mdt.*`)

| File Path | LOC | Single Responsibility | Key Exported Symbols |
| :--- | :---: | :--- | :--- |
| [`ui/EngineRuntime.kt`](../../src/main/kotlin/org/mdt/ui/EngineRuntime.kt) | 80 | Bridge connecting 60 FPS render loop & Compose | `EngineRuntime.setContent()`, `draw()` |
| [`mod/NekoMod.kt`](../../src/main/kotlin/org/mdt/mod/NekoMod.kt) | 130 | Mod lifecycle entry point & Interactive Demo | `NekoMod.init()` |
