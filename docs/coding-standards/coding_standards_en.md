# Engineering Standards & Architecture Conventions (Coding Standards)

This document establishes the mandatory programming standards, naming conventions, memory safety rules, and architectural boundaries for **NekoMod**.

---

## 1. Numerical & Graphics Type System (`Float` 100%)

* **Mandatory Rule:** All geometric dimensions (`width`, `height`), insets (`margin`, `padding`), corner radii (`radius`), stroke widths (`borderWidth`), opacities (`opacity`), blur weights (`blurRadius`, `backdropWeight`), and expansion ratios (`weight`) **must use `Float`** (`f` literal suffix).
* **Technical Rationale:**
  1. Native 1:1 compatibility with OpenGL coordinate buffers and Arc Graphics math.
  2. Zero runtime type-casting overhead (`.toFloat()`) when uploading shader uniforms.
  3. Clean, concise Compose syntax: `Modifier.pad(12f).radius(8f).border(1f, Color.white).opacity(0.9f)`.

---

## 2. Compose Declarative Layer & Modifier Conventions

### A. Parameter Ordering in `@Composable` Functions
All UI components must adhere to standard Jetpack Compose parameter sequencing:
1. **Primary Content / State:** `text: String`, `source: Any`, `checked: Boolean`.
2. **Event Callbacks:** `onClick: () -> Unit`, `onToggle: () -> Unit`.
3. **Modifier Parameter:** `modifier: UIModifier = UIModifier` (or alias `Modifier`).
4. **Optional Visual Configurations:** `colors: ButtonColors`, `radius: Float`, `scale: Float`.
5. **Child Content Lambda Slot:** `content: @Composable BoxScope.() -> Unit = {}` (always trailing).

### B. `UIModifier` Chaining
* All built-in components applying default visual styles **must end with `.then(modifier)`** to permit external layout or interactivity overrides.

---

## 3. Thread Dispatching & VRAM Memory Safety

```
┌────────────────────────────────────────────────────────┐
│ BACKGROUND THREAD (Dispatchers.IO)                     │
│ • OkHttp Network I/O & Streaming                       │
│ • Okio Disk Reading & Atomic Writing                   │
│ • Background Pixmap Byte Decoding                      │
└──────────────────────────┬─────────────────────────────┘
                           │ AsyncDispatcher.onMainThread / Core.app.post
                           ▼
┌────────────────────────────────────────────────────────┐
│ RENDER THREAD (Mindustry OpenGL Loop - 60 FPS)         │
│ • OpenGL Texture Creation & GPU Uploads                │
│ • Virtual DOM Mutations & Composition Clock            │
│ • 2-Pass Godot Layout & SDF Shader Drawing             │
└────────────────────────────────────────────────────────┘
```

* **VRAM Safety Rules:**
  * Never instantiate unmanaged `Texture(...)` instances directly within `@Composable` bodies.
  * All OpenGL textures must be managed via `TextureHandle` (Reference Counting) or `LRUTextureCache` for automatic disposal upon unmounting.

---

## 4. KDoc & Documentation Rules

1. **In-Source KDoc (`*.kt`):**
   * **Strictly 100% English.**
   * File references must use plain text: `See: docs/architecture/architecture_en.md` (avoid `@see` with file paths).
2. **Documentation in `docs/`:**
   * Always maintain **parallel bilingual pairs**: `*_vi.md` and `*_en.md`.
   * All inter-document markdown links must use **relative paths (`./` or `../`)**.

---

## 5. Architectural Layering & Packages

* `org.mdt.core.*`: Standalone subsystems (Async, Network, Storage, Cache, Image, I18n).
* `org.mdt.ui.core`: Virtual Node DOM tree (`UINode`, `CanvasNode`, Events, Math).
* `org.mdt.ui.layout`: 2-pass Godot layout engine (`GodotLayout`, SizeFlags, Anchors).
* `org.mdt.ui.render`: GPU Rendering Pipeline (`EngineRenderer`, `BoxRenderer`, `TextRenderer`, Shaders, BoxBlur).
* `org.mdt.ui.widgets`: Virtual UI elements (`BoxNode`, `TextNode`, `ImageNode`, `Containers`).
* `org.mdt.ui.compose`: Declarative Compose Multiplatform DSL (`Components`, `Widgets`, `UIModifier`, Scopes).
