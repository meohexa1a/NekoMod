# Codebase Architecture Map

This document provides a comprehensive directory breakdown of all source files across NekoMod reorganized according to **Feature Slicing & Co-location**.

---

## 📊 Project Statistics Overview
* **Total Kotlin Source Files (`.kt`):** 49 files
* **Organization Paradigm:** Feature Co-location — Virtual DOM Node, Composable, and State Machine for a single widget reside together in a dedicated directory.
* **Files per Directory:** Max 2–4 files (clean, intuitive, painless small edits).

---

## 1. ⚙️ Core Subsystems (`org.mdt.core.*`)

| File Path | Single Responsibility | Key Exported Symbols |
| :--- | :--- | :--- |
| `core/async/AsyncDispatcher.kt` | Coroutine I/O & Main-Thread synchronization | `AsyncDispatcher.launch`, `onMainThread` |
| `core/net/HttpEngine.kt` | OkHttp 4.12 singleton with Connection Pooling | `HttpEngine.client`, `call` |
| `core/net/NetDsl.kt` | Fluent asynchronous HTTP GET/POST DSL | `Net.get()`, `Net.post()`, `awaitBytes()` |
| `core/store/Storage.kt` | Okio atomic file persistence (Atomic Write) | `Storage.atomicWrite`, `readString` |
| `core/store/KVStore.kt` | Debounced 300ms persistent Key-Value storage | `KVStore.default`, `putString`, `getFloat` |
| `core/cache/ResourceHandle.kt` | Reference-counted GPU texture lifecycle | `TextureHandle`, `acquire()`, `release()` |
| `core/cache/LRUTextureCache.kt` | 64MB VRAM-bounded LRU texture cache | `LRUTextureCache.shared`, `get()`, `put()` |
| `core/image/ImageSource.kt` | Polymorphic image source origins | `ImageSource.Url`, `Asset`, `Region`, `of()` |
| `core/image/ImageLoader.kt` | Async image pipeline + VRAM Upload Throttle | `ImageLoader.load()`, `processUploadQueue()` |
| `core/i18n/I18nEngine.kt` | Hierarchical nested key localization with `{param}` | `I18nEngine.t()`, `i18n()` |

---

## 2. 🌲 Virtual DOM Core & Rendering Pipeline (`org.mdt.ui.*`)

| File Path | Single Responsibility | Key Exported Symbols |
| :--- | :--- | :--- |
| `ui/core/UINode.kt` | Base Virtual DOM tree node | `UINode`, `hitTest()`, `draw()`, `layout()` |
| `ui/core/CanvasNode.kt` | Root Canvas representing full viewport bounds | `CanvasNode.resize()` |
| `ui/core/Math.kt` | Immutable geometric data structures (`Float`) | `Vec2`, `Rect`, `Insets` |
| `ui/core/Events.kt` | Pointer and scroll event data classes | `PointerEvent`, `ScrollEvent` |
| `ui/layout/GodotLayout.kt` | 2-Pass Godot container layout algorithms | `GodotLayout.layoutBox`, `layoutGrid` |
| `ui/layout/AnchorData.kt` | 4-point anchors (`0.0..1.0`) & margin presets | `AnchorData`, `LayoutPreset` |
| `ui/layout/Alignment.kt` | Alignment and arrangement configurations | `Alignment`, `Arrangement` |
| `ui/layout/policy/MeasurePolicy.kt` | Decoupled measurement and layout strategies | `BoxMeasurePolicy`, `ColumnMeasurePolicy`, `RowMeasurePolicy` |
| `ui/render/EngineRenderer.kt` | Master GPU render coordinator & FBO manager | `EngineRenderer.render()` |
| `ui/render/BoxRenderer.kt` | GPU SDF quad renderer for boxes, borders, glows | `BoxRenderer.draw()` |
| `ui/render/BoxBlur.kt` | 2-Pass Ping-Pong Gaussian FBO blur coordinator | `BoxBlur.capture()` |
| `ui/render/ScissorStack.kt` | Hardware OpenGL scissor clipping stack | `ScissorStack.push()`, `pop()` |
| `ui/render/Shaders.kt` | GLSL 120 Uber-Shader compiler | `Shaders.mainShader`, `ensure()` |
| `ui/render/TextRenderer.kt` | BMFont glyph rendering & cache | `TextRenderer.draw()` |
| `ui/input/EngineInputProcessor.kt` | Reverse-DFS hit-testing and Focus Routing | `EngineInputProcessor`, `requestFocus()` |
| `ui/EngineRuntime.kt` | Bridge connecting 60 FPS render loop & Compose | `EngineRuntime.setContent()`, `draw()` |

---

## 3. 🧩 Compose Foundation (`org.mdt.ui.compose.*`)

| File Path | Single Responsibility |
| :--- | :--- |
| `compose/UIModifier.kt` | Fluent modifier chain for layout, visuals, and interactivity |
| `compose/UIComposition.kt` | Recomposition lifecycle and FrameClock coordinator |
| `compose/NodeApplier.kt` | Tree applier binding Compose runtime to DOM |
| `compose/Scopes.kt` | Composable scope extensions (`BoxScope`, `RowScope`, `ColumnScope`, `GridScope`) |
| `compose/Dsl.kt` | `Modifier` factory shortcuts |
| `compose/DslMarker.kt` | `@UIDslMarker` scope control annotation |

---

## 4. 📦 Domain-Specific UI Components (`org.mdt.ui.components.*`)

### 🔲 4.1 Layout Containers (`components.layout.*`)
* [`LayoutNode.kt`](../../src/main/kotlin/org/mdt/ui/components/layout/LayoutNode.kt): Unified Virtual DOM container with lazy visuals.
* [`BoxVisuals.kt`](../../src/main/kotlin/org/mdt/ui/components/layout/BoxVisuals.kt): SDF visual configuration parameters.
* [`Box.kt`](../../src/main/kotlin/org/mdt/ui/components/layout/Box.kt): Composable `Box()`.
* [`FlexLayouts.kt`](../../src/main/kotlin/org/mdt/ui/components/layout/FlexLayouts.kt): Composable `Row()` and `Column()`.
* [`Grid.kt`](../../src/main/kotlin/org/mdt/ui/components/layout/Grid.kt): Composable `Grid()`.
* [`Spacer.kt`](../../src/main/kotlin/org/mdt/ui/components/layout/Spacer.kt): Composable `Spacer()`.
* [`Containers.kt`](../../src/main/kotlin/org/mdt/ui/components/layout/Containers.kt): Backward-compatible container nodes.

### 📝 4.2 Typography & Text (`components.text.*`)
* [`Text.kt`](../../src/main/kotlin/org/mdt/ui/components/text/Text.kt): Composable `Text()`.
* [`TextNode.kt`](../../src/main/kotlin/org/mdt/ui/components/text/TextNode.kt): Virtual DOM text node.
* [`TextVisuals.kt`](../../src/main/kotlin/org/mdt/ui/components/text/TextVisuals.kt): Typography configuration parameters.

### ⌨️ 4.3 Input & Sliders (`components.input.*`)
* **Text Field (`components.input.textfield.*`):**
  * [`TextEditState.kt`](../../src/main/kotlin/org/mdt/ui/components/input/textfield/TextEditState.kt): Headless state machine (caret, selection, clipboard, Telex).
  * [`TextFieldNode.kt`](../../src/main/kotlin/org/mdt/ui/components/input/textfield/TextFieldNode.kt): Virtual DOM text field node with Focus Glow.
  * [`TextField.kt`](../../src/main/kotlin/org/mdt/ui/components/input/textfield/TextField.kt): Composable `TextField()`.
* **Slider (`components.input.slider.*`):**
  * [`SliderNode.kt`](../../src/main/kotlin/org/mdt/ui/components/input/slider/SliderNode.kt): Virtual DOM draggable slider node.
  * [`Slider.kt`](../../src/main/kotlin/org/mdt/ui/components/input/slider/Slider.kt): Composable `Slider()`.

### 🖼️ 4.4 Display Components (`components.display.*`)
* **Image (`components.display.image.*`):**
  * [`ImageNode.kt`](../../src/main/kotlin/org/mdt/ui/components/display/image/ImageNode.kt): Virtual DOM image node with GPU ref counting.
  * [`Image.kt`](../../src/main/kotlin/org/mdt/ui/components/display/image/Image.kt): Composable `Image()`.
* **Progress Bar (`components.display.progress.*`):**
  * [`ProgressBarNode.kt`](../../src/main/kotlin/org/mdt/ui/components/display/progress/ProgressBarNode.kt): Virtual DOM progress bar node.
  * [`ProgressBar.kt`](../../src/main/kotlin/org/mdt/ui/components/display/progress/ProgressBar.kt): Composable `ProgressBar()`.
* **Tooltip (`components.display.tooltip.*`):**
  * [`Tooltip.kt`](../../src/main/kotlin/org/mdt/ui/components/display/tooltip/Tooltip.kt): `TooltipNode` and `Modifier.tooltip()`.

### 🎛️ 4.5 Surface & Actions (`components.surface.*`)
* [`Button.kt`](../../src/main/kotlin/org/mdt/ui/components/surface/Button.kt): Composable `Button()` and `ButtonColors`.
* [`Card.kt`](../../src/main/kotlin/org/mdt/ui/components/surface/Card.kt): Composable `Card()` glassmorphism container.
* [`Toggle.kt`](../../src/main/kotlin/org/mdt/ui/components/surface/Toggle.kt): Composable `Toggle()` switch.
* [`Divider.kt`](../../src/main/kotlin/org/mdt/ui/components/surface/Divider.kt): Composable `Divider()` line separator.

### 📜 4.6 Scrolling (`components.scroll.*`)
* [`ScrollContainerNode.kt`](../../src/main/kotlin/org/mdt/ui/components/scroll/ScrollContainerNode.kt): Scrollable viewport node with slim scrollbars.
* [`ScrollView.kt`](../../src/main/kotlin/org/mdt/ui/components/scroll/ScrollView.kt): Composable `ScrollView()`.
