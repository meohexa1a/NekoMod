# GPU Rendering & Shader Pipeline

This document details NekoMod's rendering architecture, the Signed Distance Field (SDF) box shader, and the **Shared Screen Capture + Parameterized Per-Box Blur** pipeline.

---

## 1. Orthographic Render Pass

UI rendering executes via Mindustry's draw hook:
```kotlin
Events.run(Trigger.uiDrawEnd) {
    renderer.render(rootCanvas)
}
```
* **Projection Matrix:** Configured using `Draw.proj(0, 0, screenW, screenH)` to align 1:1 with screen pixel coordinates.
* **Top-Layer Compositing:** The UI is rendered after Mindustry finishes drawing the game world and Scene2D stages, ensuring pristine, topmost display layers.

---

## 2. Signed Distance Field (SDF) Box Shader (`box.frag`)

Rather than relying on rasterized 9-patch bitmap textures (which suffer from scaling artifacts and pixelation), `BoxRenderer` computes geometry mathematically on the GPU using **Signed Distance Fields (SDF)**:

1. **Distance computation from pixel to rounded box:**
   $$d = \|\max(|p - \text{center}| - \text{halfSize} + r, 0)\| - r$$
2. **Sub-pixel Anti-Aliasing:**
   $$\text{alpha} = \text{smoothstep}(1.0, 0.0, d)$$
3. **Independent Corner Radii:**
   Supports 4 independent corner radii (`topLeftRadius`, `topRightRadius`, `bottomRightRadius`, `bottomLeftRadius`) and arbitrary `borderWidth` with zero CPU overhead.

---

## 3. Optimized Backdrop Blur Architecture (Shared Capture + Parameterized Blur)

```
┌───────────────────────────────────────────────────────────┐
│ 1. SCREEN CAPTURE (ONCE PER FRAME)                        │
│    EngineRenderer grabs screen once into Shared FBO       │
└─────────────────────────────┬─────────────────────────────┘
                              │
            ┌─────────────────┴─────────────────┐
            ▼                                   ▼
┌───────────────────────┐           ┌───────────────────────┐
│ Card A (Subtle Blur)  │           │ Modal Dialog (Opaque) │
│ • blurRadius = 4f     │           │ • blurRadius = 16f    │
│ • weight = 0.6        │           │ • weight = 0.95       │
│ • Tint = Pastel Blue  │           │ • Tint = Smoked Black │
└───────────────────────┘           └───────────────────────┘
```

1. **Single Screen Capture per Frame ($O(1)$):**
   * At the beginning of the frame, `EngineRenderer` checks if any visible node has `blur = true`. If so, it captures the screen **exactly once** into a shared downscaled FBO (`screenCaptureFbo`).
   * Eliminates GPU pipeline stalls caused by redundant screen grabs.
2. **Reusable Scratch Ping-Pong FBOs:**
   * Individual `BoxNode` instances do not allocate or hold private FBOs.
   * Nodes borrow the renderer's `scratchFboA` $\leftrightarrow$ `scratchFboB` to execute ping-pong convolution passes and immediately return them.
3. **Full Per-Box Customization:**
   * `blurRadius`: Kernel spread (e.g. `2f` for subtle tooltips, `16f` for opaque modals).
   * `blurIterations`: Number of ping-pong passes (1 to 4).
   * `backdropTint`: Color overlay (e.g. white, sky blue, dark tint).
   * `backdropWeight`: Opacity blending curve.
