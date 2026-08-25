# Complex Engineering Challenges & Resolved Patterns

This document details complex technical challenges encountered during the development of NekoMod's next-generation UI engine and the robust architectural solutions designed to address them.

---

## 🛑 Challenge 1: Nested Viewport Clipping with Rotations / 2D Transforms

### 🔍 Problem Breakdown:
* Hardware OpenGL scissor clipping (`glScissor(x, y, w, h)`) operates strictly on **axis-aligned bounding boxes (AABB)** parallel to screen coordinates.
* When a container undergoes a $30^\circ$ rotation or affine transformation, standard `glScissor` cannot clip rotated parallelograms.

### 💡 Architectural Solutions:
1. **Stencil Buffer Clipping:** Enable `GL_STENCIL_TEST` to render arbitrary mask geometry into the stencil buffer prior to child rendering.
2. **SDF Shader Clipping:** Pass the inverse 2D transformation matrix directly into the SDF fragment shader to evaluate clip boundaries per pixel.

---

## 🛑 Challenge 2: Vietnamese IME & Complex Text Shaping in `TextField`

### 🔍 Problem Breakdown:
* Vietnamese input engines (Telex/VNI via Unikey/EVKey) emit backspace characters (`\b`) paired with newly accented letters during live composition.
* Processing `\b` in `onKeyTyped` conflicts with `onKeyDown(KeyCode.backspace)`, resulting in duplicated backspace deletions.

### 💡 Resolved Architecture:
* `Backspace` is handled exclusively in `onKeyDown(KeyCode.backspace)`.
* `onKeyTyped` strictly filters for printable characters ($\ge 32$) and ignores `\b`.

---

## 🛑 Challenge 3: Global Event-Driven Drag Tracking & Layout Feedback Loops

### 🔍 Problem Breakdown:
1. **Gesture Loss on Pointer Exit:** Listening only to pointer events inside local node bounds causes gesture cancellation when dragging rapidly across the screen.
2. **Layout Feedback Loop (Oscillation):** When dynamic text width changes (e.g. `Power: 99%` $\rightarrow$ `100%`) expand the parent container, the slider shifts horizontally under the stationary cursor, causing the calculation to regress back to `99%` and oscillate infinitely.

### 💡 Resolved Architecture:
1. **Event-Driven Drag Routing (`onPointerDrag`):** `EngineInputProcessor.touchDragged()` routes pointer drag events directly to `pressedNode` globally.
2. **Fixed Typography Widths:** Enforce fixed `Modifier.width(...)` for dynamic readout labels to decouple text expansion from slider position.

---

## 🛑 Challenge 4: Windows NTFS File Locking & Async Disk Persistence

### 🔍 Problem Breakdown:
* Windows NTFS enforces mandatory file access locking (`process cannot access the file`) whenever an active file handle is open.
* Rapid slider drag mutations (60 writes/sec) saturate disk I/O and throw `IOException`.

### 💡 Resolved Architecture:
1. **300ms Debounce:** Disk writes are throttled to execute 300ms after the final user mutation on background `Dispatchers.IO`.
2. **Atomic Write with Nano-Staging Files:** Writes stream to a unique staging file (`target.nanoTime().tmp`) before performing an `atomicMove` under a `synchronized(lock)` block.

---

## 🛑 Challenge 5: Legacy OpenGL 2.0 / GLSL 120 Hardware Constraints

### 🔍 Problem Breakdown:
* Mindustry enforces **OpenGL 2.0 / GL ES 2.0 (GLSL 120 / GLSL ES 100)**:
  1. No Multiple Render Targets (MRT).
  2. Limited shader uniform register count.

### 💡 Resolved Architecture:
1. **Multi-Pass Sequential FBOs:** Complex multi-step effects (Gaussian Blur) execute via sequential ping-pong passes.
2. **Uniform Packing:** Corner radii and styling parameters are packed into vectors (`vec4 u_cornerRadii`) to minimize uniform overhead.
