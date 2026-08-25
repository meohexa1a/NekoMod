# Coding Standards & Architecture Invariants

This document establishes the mandatory programming standards, naming conventions, memory safety principles, and architectural tiers for **NekoMod**.

---

## 🎨 I. Graphics, Shaders & Typography

### 1. Geometric Type System (`Float` 100%)
* **Mandatory Rule:** All geometric dimensions (`width`, `height`), insets (`margin`, `padding`), corner radii (`radius`), border strokes (`borderWidth`), opacities (`opacity`), blur weights (`blurRadius`, `backdropWeight`), and layout stretch weights (`weight`) **must use `Float`** (`f` literal suffix).
* **Rationale:**
  1. Perfect alignment with OpenGL and Arc graphics coordinates.
  2. Eliminates `.toFloat()` runtime cast overhead when binding uniforms to GPU shaders.

### 2. OpenGL Active Texture Enum
* Always call `Gl.activeTexture(Gl.texture0 + unit)`, NEVER `Gl.texture2d + unit` (which causes `[GL] Error: invalid enum`).

### 3. Arc Shader Headers & Precisions
* Never declare `#ifdef GL_ES` or `#version` in raw shader files (`*.frag`, `*.vert`). Arc's `Shader` compiler (`arc.graphics.gl.Shader`) automatically injects precision qualifiers internally; manual declarations cause duplicate definition crashes.

### 4. Render Loop Execution Hooks
* GPU texture upload queues, FrameBuffer allocations, and UI frame ticks must be executed within `Trigger.uiDrawEnd` or `EngineRuntime.draw()`.

### 5. Typography & BMFont Natural Scaling
* **Standard:** Render Bitmap Fonts (`Fonts.def`, `Fonts.tech`, `Fonts.large`) at natural `scale = 1.0f` (or integer increments $2.0\times$).
* **Rationale:** BMFonts are rasterized bitmap atlases. Fractional float scaling (`0.8f`, `0.85f`, `0.9f`, `1.1f`) causes GPU sub-pixel interpolation artifacts, resulting in blurry, distorted, or merged glyph strokes.
* **Hierarchy:** Create visual hierarchy via **Color Luminance** (`Color.white` vs `Color.valueOf("9399b2")`) and font family variants (`Fonts.large` for titles, `Fonts.tech` for statistics).

---

## 🎮 II. Input & Interaction Architecture

### 6. Keyboard Input & Vietnamese IME
* Handle `Backspace` exclusively in `onKeyDown(KeyCode.backspace)`.
* `onKeyTyped` must only process printable characters ($\ge 32$) and ignore `\b` to prevent double-backspace deletion bugs with Vietnamese IME (Unikey/EVKey).

### 7. Event-Driven Drag Tracking (`onPointerDrag`)
* Route continuous drag gestures via `onPointerDrag` in `EngineInputProcessor.touchDragged()`.
* Never poll `Core.input` inside frame render loops (`drawSelf()`) to ensure smooth gesture tracking even when pointer exits node bounds.

---

## 🧩 III. Code Architecture & Style

### 8. Feature-Sliced Package Co-location
* Organize UI widgets into domain sub-packages (`components.input.textfield`, `components.input.slider`, `components.display.image`, etc.) co-locating the virtual node, composable, and state machine together (max 2–4 files per folder).

### 9. Declarative Post-Design Philosophy
* Avoid embedding heavy runtime UI inspector overlays into the game client. Prioritize compile-time IDE Kotlin DSL design and reactive text schema (HJSON/JSON) with live hot-reload.

### 10. Composable Parameter Ordering & UIModifier Chaining
* Parameter order: (1) Data/State $\rightarrow$ (2) Callbacks $\rightarrow$ (3) `modifier: UIModifier = UIModifier` $\rightarrow$ (4) Visual Options $\rightarrow$ (5) Content Slot.
* Every built-in composable applying default visual styles must conclude with `.then(modifier)`.

### 11. Concise Inline Lambdas
* Format trivial, single-statement callbacks/lambdas on a single line (e.g. `AsyncDispatcher.onMainThread { onResult(null, e) }`) to maximize readability and reduce visual clutter.

---

## 💾 IV. Storage, I/O & Libraries

### 12. Atomic Disk Persistence & Windows File Locking
* Debounce rapid in-memory mutations before writing to disk (300ms delay). `Storage.atomicWrite` must use unique nano-timestamped staging files with synchronization (`synchronized(lock)`) to prevent Windows NTFS file lock conflicts.

### 13. Local Library Source References (`.lib-source`)
* Always inspect and read local library source codes in `.lib-source/` (`mindustry/`, `arc/`, `compose-runtime/`, `MindustryToolMod/`) when investigating engine behavior, internal APIs, or rendering pipelines. Auto-clone into `.lib-source/<lib-name>` if missing.

---

## 📖 V. Documentation & In-Source Standards

### 14. Parallel Bilingual Documentation
* Maintain parallel bilingual documentation pairs (`*_vi.md` and `*_en.md`) in `docs/`. All inter-document markdown links must strictly use relative paths (`./` or `../`).

### 15. In-Source KDoc Documentation
* All KDoc comments in Kotlin source files (`*.kt`) must be strictly 100% English. Reference documentation using plain text `See: docs/path/file_en.md`.
