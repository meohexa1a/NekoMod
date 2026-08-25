# NekoMod Project Invariants & Domain-Grouped Standards

---

## 🎨 I. Graphics, Shaders & Typography Standards
1. **Type System (Float Everywhere):** Use `Float` (`f` literal suffix) for all geometric dimensions, insets (padding/margin), coordinates, corner radii, opacities, blur weights, and shader uniforms. Never use `Double`.
2. **OpenGL Active Texture Enum:** Always call `Gl.activeTexture(Gl.texture0 + unit)`, NEVER `Gl.texture2d + unit` (which causes `[GL] Error: invalid enum`).
3. **Arc Shader Headers & Precisions:** Never declare `#ifdef GL_ES` or `#version` in raw shader files (`*.frag`, `*.vert`). Arc's `Shader` compiler automatically injects precision qualifiers internally; manual declarations cause duplicate definition crashes.
4. **Direct Engine Background Rendering:** When building root screens (e.g. Main Menu), disable legacy Arc Scene2D menu groups completely (`menuGroup.visible = false`, `menuGroup.touchable = disabled`) and invoke native renderers (e.g. `MenuRenderer.render()`) directly in `EngineRenderer` before the Virtual DOM canvas pass.
5. **Typography, Scaling & Multi-Line Wrapping:** Render BMFonts (`Fonts.def`, `Fonts.tech`, `Fonts.large`) at natural `scale = 1.0f` (or integer increments). Never apply fractional float scaling (e.g. `0.8f`, `0.85f`) to bitmap atlas fonts. When `wrap = true`, pass container inner width `innerW` to `font.draw(text, x, y, innerW, align, true)` and compute height via `layoutHelper.setText(f, text, color, innerW, align, true)`.

---

## 📐 II. Layout Engine & Composition Standards
6. **Hug Content & Auto-Layout by Default:** UI containers (`Card`, `Column`, `Row`, `Button`, `Box`) must default to **HUG CONTENT** (intrinsic sizing via `getPrefWidth`/`getPrefHeight` factoring in padding and children). Avoid hardcoded fixed dimensions (`width(330f)`) when content can dynamically adapt. Use `minWidth`/`minHeight` only as protective bounds.
7. **Proportional Flex Weights:** In `Row` and `Column`, `Modifier.weight(ratio)` must distribute available free space proportionally ($(\text{availMain} - \text{unweightedMinSize} - \text{gaps}) \times \frac{\text{ratio}}{\text{totalRatio}}$) without distorting sibling nodes.
8. **Top-Layer Overlay Architecture:** Tooltips, context menus, and modal dialogs must NOT be drawn as inline tree children. They must register with global top-layer managers (`TooltipManager`) and render in the final top-layer pass of `CanvasNode.draw()` to guarantee top Z-index and immunity to parent clipping.
9. **OpenGL Bottom-Left Anchor Math:** In bottom-left coordinate systems ($y=0$ bottom, $y=\text{parentH}$ top):
   - Bottom-anchored (`anchorTop == 1.0f`): $y = \text{parentY} + \text{offsetBottom} + \text{marginB}$ (never subtract $h$ from $y$).
   - Vertical span (`anchorTop != anchorBottom`): $h = \text{maxOf}(0\text{f}, \text{topEdge} - \text{bottomEdge} - \text{marginT} - \text{marginB})$.

---

## 🎮 III. Input & Interaction Architecture
10. **Keyboard Input & IME:** Handle `Backspace` exclusively in `onKeyDown(KeyCode.backspace)`. `onKeyTyped` must only process printable characters ($\ge 32$) and ignore `\b` to prevent double-backspace deletion bugs with Vietnamese IME (Unikey/EVKey).
11. **Event-Driven Drag Tracking:** Route continuous drag gestures via `onPointerDrag` in `EngineInputProcessor.touchDragged`, never poll `Core.input` inside frame render loops (`drawSelf`/`draw`).

---

## 🧩 IV. Code Architecture & Style
12. **Feature-Sliced Package Co-location:** Organize UI widgets into domain sub-packages (`components.input.textfield`, `components.input.slider`, `components.display.image`, etc.) co-locating the virtual node, composable, and state machine together. Avoid flat technical layers (`compose/`, `widgets/`).
13. **Declarative Post-Design:** Avoid embedding heavy runtime UI inspector overlays into the game client. Prioritize compile-time IDE Kotlin DSL design and reactive text schema (HJSON/JSON) with live hot-reload.
14. **UIModifier Chaining:** Every built-in composable applying default visual styles must conclude with `.then(modifier)` to allow callers to override layout and event properties.
15. **Concise Inline Lambdas:** Format trivial, single-statement callbacks/lambdas on a single line (e.g. `AsyncDispatcher.onMainThread { onResult(null, e) }`) to maximize readability and avoid visual clutter.

---

## 💾 V. Storage, I/O & Concurrency
16. **Storage & Windows File Locking:** Debounce rapid in-memory mutations before writing to disk (300ms delay). `Storage.atomicWrite` must use unique nano-timestamped staging files with synchronization (`synchronized(lock)`) to prevent Windows NTFS file lock conflicts (`process cannot access the file`).
17. **Local Library Source References (`.lib-source`):** Always inspect and read local library source codes in `.lib-source/` (`.lib-source/mindustry/`, `.lib-source/arc/`, `.lib-source/compose-runtime/`, `.lib-source/MindustryToolMod/`) when investigating engine behavior, internal APIs, or rendering pipelines. If a required dependency or library source is missing, clone it into `.lib-source/<lib-name>` before proceeding.

---

## 📖 VI. Documentation & In-Source Standards
18. **Bilingual Documentation:** Maintain parallel bilingual documentation pairs (`*_vi.md` and `*_en.md`) in `docs/`. All inter-document markdown links must strictly use relative paths (`./` or `../`), never hardcoded machine paths (`file:///C:/...`).
19. **In-Source KDoc Documentation:** All KDoc comments in Kotlin source files (`*.kt`) must be strictly 100% English. Reference documentation using plain text `See: docs/path/file_en.md` (do not use `@see` with file paths).
