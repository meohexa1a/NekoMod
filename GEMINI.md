# NekoMod Project Invariants & Domain-Grouped Standards

---

## 🎨 I. Graphics, Shaders & Typography Standards
1. **Type System (Float Everywhere):** Use `Float` (`f` literal suffix) for all geometric dimensions, insets (padding/margin), coordinates, corner radii, opacities, blur weights, and shader uniforms. Never use `Double`.
2. **OpenGL Active Texture Enum:** Always call `Gl.activeTexture(Gl.texture0 + unit)`, NEVER `Gl.texture2d + unit` (which causes `[GL] Error: invalid enum`).
3. **Arc Shader Headers & Precisions:** Never declare `#ifdef GL_ES` or `#version` in raw shader files (`*.frag`, `*.vert`). Arc's `Shader` compiler automatically injects precision qualifiers internally; manual declarations cause duplicate definition crashes.
4. **Declarative Custom Graphics & Background Rendering (Canvas Composable):** Never hardcode game-specific renderers (e.g. `MenuRenderer`) inside `EngineRenderer`. Instead, use the declarative `Canvas(modifier) { ... }` composable at the screen level, managing lifecycle and GPU disposal via `remember` and `DisposableEffect`.
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
15. **Expressive Domain Naming (Ban Cryptic Abbreviations):** Ban overly cryptic 1-2 letter variables (`p`, `cr`, `hs`, `vis`, `d`, `v`, `w`, `h`, `f`, `rx`, `ry`) in business logic, rendering loops, and public APIs. Use clear, self-documenting domain identifiers (`point`, `cornerRadii`, `halfSize`, `visuals`, `fraction`, `rectWidth`, `slotInnerHeight`, `font`, `measuredWidth`).
16. **`if-else` Formatting & Guard Clause Breathing Room:** Avoid cramming complex `if-else` expressions into dense, long single lines. When using early returns (`if (condition) return ...`), always insert a blank line between guard clauses, `else` branches, and the subsequent execution block to ensure instant visual scanning.
17. **File Granularity (Cohesion & Single Responsibility):**
    - **Merge Micro-Files:** Consolidate tiny micro-files ($<30$ lines) that share the same domain (e.g. token data classes, companion enums) into a single cohesive domain file (`ThemeTokens.kt`).
    - **Split Bloated Files:** Decompose large files exceeding $\sim 300$ lines or handling multiple distinct responsibilities into modular, feature-sliced components.
18. **In-File Structural Organization:** Organize code within each file in a predictable, top-to-bottom layout:
    1. `Package & Imports` (clean, zero unused or fully qualified imports)
    2. `Public Component / Class Header with KDoc`
    3. `State & Properties`
    4. `Lifecycle & Primary Methods`
    5. `Private / Internal Helper Functions`
    6. `Companion Object & Extension Functions`
    Separate distinct logical sections with section header comments (`// --- LIFECYCLE ---`, `// --- GETTERS & SETTERS ---`).

---

## 💾 V. Storage, I/O & Concurrency
19. **Namespaced Storage & Serializable ConfigStore:** All storage and cache resolutions must be namespaced dynamically by `EngineContext.name` to guarantee total isolation between Game Mod (`nekomod`), Standalone Editor (`editor`), and GUI Launcher. Persistent configurations must use typed `@Serializable` data classes backed by `ConfigStore<T>`.
20. **Storage & Windows File Locking:** Debounce rapid in-memory mutations before writing to disk (300ms delay). `Storage.atomicWrite` must use unique nano-timestamped staging files with atomic moves to prevent Windows NTFS file lock conflicts (`process cannot access the file`).
21. **Local Library Source References (`.lib-source`):** Always inspect and read local library source codes in `.lib-source/` (`.lib-source/mindustry/`, `.lib-source/arc/`, `.lib-source/compose-runtime/`, `.lib-source/MindustryToolMod/`) when investigating engine behavior, internal APIs, or rendering pipelines. If a required dependency or library source is missing, clone it into `.lib-source/<lib-name>` before proceeding.

---

## 📖 VI. Documentation & In-Source Standards
22. **Bilingual Documentation:** Maintain parallel bilingual documentation pairs (`*_vi.md` and `*_en.md`) in `docs/`. All inter-document markdown links must strictly use relative paths (`./` or `../`), never hardcoded machine paths (`file:///C:/...`).
23. **In-Source KDoc Documentation:** All KDoc comments in Kotlin source files (`*.kt`) must be strictly 100% English. Every public class, interface, and composable must include a header `## SymbolName`, purpose description, and reference link `See: docs/path/file_en.md` (do not use `@see` with file paths).

---

## 🔬 VII. Systematic Diagnostic & Anti-Pattern Hunting Protocol (4-Lens Methodology)
24. **Coupling & Locality Audit (Isolation Test):** Low-level engines and core renderers (`EngineRenderer`) must remain 100% agnostic of specific domain screens, optional visual effects, or higher-level managers. If disabling or removing a feature leaves baggage in the core engine, encapsulate it immediately at the feature/screen level.
25. **Algorithmic Waste & Lazy Frame-Indexing:** Never run recursive tree traversals ($O(N)$ pre-scans) before render loops to check for optional node states. Defer heavy operations (FBO captures, blur passes) to the exact moment of execution on-demand, protected by monotonic frame indexing (`lastFrameId == currentFrameId`) for $O(1)$ single execution per frame.
26. **End-to-End Signal Chain Integrity:** When auditing UI primitives, verify the unbroken end-to-end signal flow: `Input Event` $\rightarrow$ `Actionable Node Matcher` (`findActionableNode`) $\rightarrow$ `Scissor Boundary Hit-Test` $\rightarrow$ `Event Bubbling` $\rightarrow$ `State Mutation` $\rightarrow$ `Layout/Draw`. Ensure no intermediate router silently drops or misroutes events for supported node types.
27. **Framework Idiomaticity & Zero-GC Invariants:** Enforce minimal primitive node counts (deferring layout/presentation to pure composables) and strictly use typed modifier elements (`data class Element : UIModifier.Element`) with value-based `equals()`/`hashCode()` to eliminate GC allocations and enable Compose recomposition skipping.

