# Complex Engineering Challenges & Future Directions (Complex Challenges)

This document records and deeply analyzes hard technical problems identified during the design of NekoMod's next-gen UI engine. These architectural challenges require dedicated research and should not be hastily implemented to avoid compromising engine stability.

---

## 🛑 Challenge 1: Nested Viewport Clipping (ScissorStack) under 2D Rotation / Affine Transforms

### 🔍 Technical Problem:
* Hardware rectangular clipping via OpenGL `glScissor(x, y, w, h)` operates strictly on **Axis-Aligned Bounding Boxes (AABB)** in window pixel coordinates.
* When a container (e.g. `ScrollContainer`) is rotated by $30^\circ$ or transformed with 2D scale/skew matrices, `glScissor` cannot clip rotated polygons or skewed boundaries.

### 💡 Future Architectural Approaches:
1. **Stencil Buffer Masking:** Enable `GL_STENCIL_TEST` to draw the clipping boundary polygon into the stencil buffer before drawing child nodes.
2. **SDF Shader Clipping:** Pass the inverse transform matrix and corner radii directly into the SDF Fragment Shader to discard pixels outside bounds programmatically.

---

## 🛑 Challenge 2: IME Keyboard Input & Complex Asian Text Shaping in `TextField`

### 🔍 Technical Problem:
* Vietnamese IME engines (Telex/VNI via Unikey/EVKey) and East Asian keyboards (CJK) rely on **Composition Strings (transient uncommitted text awaiting diacritic/character resolution)**.
* Mindustry and Arc Engine process keyboard events at a low level (`InputProcessor.keyDown` / `keyTyped`), bypassing OS-level IME composition windows, causing dropped accent marks or duplicate keystrokes.
* The game's default BMFont engine lacks advanced kerning pairs and does not integrate HarfBuzz or FreeType for color emoji or bidirectional text (Arabic/Hebrew).

### 💡 Future Architectural Approaches:
1. Intercept GLFW character and composition window callbacks (`glfwSetCharModsCallback`).
2. Separate composing (uncommitted) text and committed text states within `TextFieldNode`.

---

## 🛑 Challenge 3: Bi-directional Reactive Data-Binding between Dynamic HJSON Schema & Kotlin Live Memory

### 🔍 Technical Problem:
* In-game visual UI Editors generate dynamic JSON/HJSON schema trees at runtime.
* In contrast, Compose Multiplatform relies on statically compiled composable call graphs (Ahead-of-Time bytecode rewriting by the Compose Compiler Plugin) with typed `mutableStateOf`.
* The challenge is binding a visually dragged slider directly to arbitrary live Kotlin player variables without resorting to slow reflection on Android.

### 💡 Future Architectural Approaches:
1. **Dynamic Expression Compiler (Janino / Lightweight AST):** Compile expression strings like `binding: "player.health * 100"` into bytecode or cached AST evaluators.
2. **Observable Key-Path Dictionary:** Model dynamic variables using observable hashtables: `RuntimeState.observe("player.health")`.

---

## 🛑 Challenge 4: Mindustry's Legacy OpenGL 2.0 / GLSL 120 Hardware Baseline Constraints

### 🔍 Technical Problem:
* Because Arc Engine and Mindustry strictly enforce an **OpenGL 2.0 / GL ES 2.0 (GLSL 120 / GLSL ES 100)** baseline across all platforms (Desktop, Android, iOS) for maximum backward hardware compatibility:
  1. **No Multiple Render Targets (MRT):** Impossible to output multiple render passes (Albedo, Blur Mask, SDF Depth) into a single FrameBuffer in one pass.
  2. **Strict Uniform Register Caps:** Legacy GPUs often limit fragment uniform storage to 128 ~ 256 `vec4` registers, preventing large custom data arrays from being passed directly to shaders.
  3. **Non-Power-Of-Two (NPOT) Texture Limits:** In standard GL 2.0, NPOT textures do not support wrap modes like `GL_REPEAT` (only `GL_CLAMP_TO_EDGE` is supported).

### 💡 Future Architectural Approaches:
1. **Multi-Pass Ping-Pong Architecture:** Chain complex post-processing effects (e.g. Gaussian Blur + SDF compositing) into sequential FBO passes rather than expecting single-pass MRT.
2. **Tight Uniform Packing:** Pack booleans and scalar floats into packed vector registers (e.g. `vec4 u_cornerRadii`, `vec4 u_backdropCoords`) to conserve precious hardware uniform registers.

---

## 🛑 Challenge 5: VRAM Bandwidth Throttling during Rapid Multi-Image Streaming (Burst Upload Throttling)

### 🔍 Technical Problem:
* Fast scrolling through Mod Catalogs or Server Lists initiates dozens of simultaneous remote icon downloads.
* While network I/O and byte decoding to `Pixmap` execute safely on background coroutines (`Dispatchers.IO`), OpenGL texture creation (`glTexImage2D`) must occur on the **Mindustry Render Thread**.
* Uploading too many uncompressed textures in a single frame (e.g. 50 images in 1 frame) stalls the GPU pipeline and drops frame rates well below 60 FPS.

### 💡 Future Architectural Approaches:
1. **Frame Upload Budget Queue:** Cap texture uploads to a strict budget of $2 \sim 3$ textures per frame.
2. **Dynamic Texture Atlas Packing:** Pack multiple small icons ($32\times 32$) into a single shared Atlas page ($1024\times 1024$) to eliminate hundreds of discrete texture binds.
