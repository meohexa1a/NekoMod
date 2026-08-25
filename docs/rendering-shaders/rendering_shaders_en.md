# GPU Rendering & Shader Pipeline

This document details NekoMod's graphics rendering pipeline, Signed Distance Field (SDF) box shader architecture, and OpenGL invariants across the Mindustry / Arc ecosystem.

---

## 1. Orthographic Render Pass

All UI rendering operations execute within the dedicated event hook:
```kotlin
Events.run(Trigger.uiDrawEnd) {
    renderer.render(rootCanvas)
}
```
* **Projection:** Configured via `Draw.proj(0f, 0f, screenW, screenH)` to draw in direct 1:1 pixel coordinate space.
* **Top-Layer Compositing:** UI renders after Mindustry finishes all game layers and Scene2D, ensuring sharp, unoccluded presentation.
* **FBO Projection Matrix Preservation:** When switching render targets to intermediate FrameBuffers, the active projection matrix is saved into `scratchMat` and restored immediately after pass completion to prevent canvas projection distortion.

---

## 2. SDF Box Shader Pipeline (`box.frag` & `box.vert`)

Rather than relying on 9-patch bitmap textures which degrade under dynamic scaling, `BoxRenderer` utilizes **Signed Distance Fields (SDF)** evaluated directly in the fragment shader:

```
┌──────────────────────────────────────────────────────────┐
│ BOX SHADER FRAGMENT COMPOSITING PIPELINE                 │
│ 1. Base Fill (Solid Color / Fill Texture)                │
│ 2. Backdrop Blur Sampling (Blended background texture)   │
│ 3. Outer Glow (Expanded gaussian aura)                   │
│ 4. Inner Shadow (Inner inset depth)                      │
│ 5. Border Stroke (Continuous / Dashed outline)           │
│ 6. Color Filter & Grain Noise                            │
│ 7. Opacity Multiplication & Single Alpha Discard         │
└──────────────────────────────────────────────────────────┘
```

1. **Signed Distance Computation:**
   $$d = \|\max(|p - \text{center}| - \text{halfSize} + r, 0)\| - r$$
2. **Smooth Anti-Aliasing:**
   $$\text{fillAlpha} = \text{clamp}(1.0 - \text{smoothstep}(-\text{edge}, \text{edge}, d), 0.0, 1.0)$$
3. **No Early Alpha Discard:**
   The shader never discards fragments early when `fillAlpha == 0`, because outer features like `Border` and `Outer Glow` exist outside the core box bounds ($d > 0$). The `if (color.a < 0.001) discard;` check executes strictly once at the end of the shader after all visual layers have been blended.

---

## 3. Arc Shader Compiler Invariants

* **Arc Shader Headers:** Arc's shader compiler (`arc.graphics.gl.Shader`) automatically injects `#ifdef GL_ES` and `precision mediump float;` qualifiers into fragment shaders before compiling.
* **Mandatory Rule:** Never declare `#ifdef GL_ES` or `#version` manually in raw shader files (`box.frag`, `blur.frag`, `box.vert`, `blur.vert`) to prevent duplicate definition crashes.

---

## 4. Typography & BMFont Rendering Standards

* **Bitmap Font Invariant:** Mindustry fonts (`Fonts.def`, `Fonts.tech`, `Fonts.large`) are pre-rasterized bitmap atlases.
* **Integer Scale Standard:** Always render at natural `scale = 1.0f` (or integer increments $2.0\times$). Avoid fractional float scales (`0.8f`, `0.85f`, `0.9f`, `1.1f`) which cause sub-pixel glyph distortion and blurring.
* **Visual Hierarchy:** Create contrast through color luminance (`Color.white` for primary titles, `Color.valueOf("9399b2")` for secondary subtitles) and specialized font families (`Fonts.large` for headings, `Fonts.tech` for telemetry/stats).
