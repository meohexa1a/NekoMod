# NekoMod Project Invariants & Domain-Grouped Standards

---

## 🎨 I. Graphics, Uber Shader & Typography Standards
1. **Type System (Float Everywhere):** Use `Float` (`f` literal suffix) for all geometric dimensions, insets (padding/margin), coordinates, corner radii, opacities, blur weights, and shader uniforms. Never use `Double`.
2. **OpenGL Active Texture Enum:** Always call `Gl.activeTexture(Gl.texture0 + unit)`, NEVER `Gl.texture2d + unit` (which causes `[GL] Error: invalid enum`).
3. **Arc Shader Headers & Precisions:** Never declare `#ifdef GL_ES` or `#version` in raw shader files (`*.frag`, `*.vert`). Arc's `Shader` compiler automatically injects precision qualifiers internally; manual declarations cause duplicate definition crashes.
4. **1-Draw-Call GPU Batching (UIBatch & SceneBlur):** All 2D visual elements (quads, rounded SDF boxes, borders, textures, text glyphs, frosted glass) render in a single unified draw call via `UIBatch`. Scene background blur captures execute progressively via 5-pass Dual-Kawase in `SceneBlur`, guarded by monotonic `frameId` caching.
5. **Typography & Text Rendering:** Render BMFonts (`Fonts.def`, `Fonts.tech`, `Fonts.large`) at natural `scale = 1.0f` (or integer increments). Never apply fractional float scaling (e.g. `0.8f`, `0.85f`) to bitmap atlas fonts. Text measurement and line wrapping stream directly through `FontRenderer` and analytical scissor clipping.

---

## 📐 II. Layout Engine & Virtual DOM Standards
6. **Hug Content & Auto-Layout by Default:** UI containers (`Card`, `Column`, `Row`, `Button`, `Box`) must default to **HUG CONTENT** (intrinsic sizing via `getPrefWidth`/`getPrefHeight` factoring in padding and children). Avoid hardcoded fixed dimensions (`width(330f)`) when content can dynamically adapt. Use `minWidth`/`minHeight` only as protective bounds.
7. **Proportional Flex Weights:** In `Row` and `Column`, `Modifier.weight(ratio)` must distribute available free space proportionally ($(\text{availMain} - \text{unweightedMinSize} - \text{gaps}) \times \frac{\text{ratio}}{\text{totalRatio}}$) without distorting sibling nodes.
8. **Declarative Compose Overlays:** Tooltips, modals, and dropdown overlays must be expressed declaratively via composables (`TooltipBox`, `ModalDialog`) and positioned with `Alignment`/`LayoutPreset` within the virtual node tree.
9. **OpenGL Bottom-Left Anchor Math:** In bottom-left coordinate systems ($y=0$ bottom, $y=\text{parentH}$ top):
   - Bottom-anchored (`anchorTop == 1.0f`): $y = \text{parentY} + \text{offsetBottom} + \text{marginB}$ (never subtract $h$ from $y$).
   - Vertical span (`anchorTop != anchorBottom`): $h = \text{maxOf}(0\text{f}, \text{topEdge} - \text{bottomEdge} - \text{marginT} - \text{marginB})$.
10. **Decoupled Pure-Kotlin Core Layout & Math:** The core layout and math domain (`org.mdt.core.ui.layout`, `org.mdt.core.ui.unit`) must remain **100% Pure Kotlin** without direct dependencies on game engine internal classes (e.g., Arc `IntSeq`/`FloatSeq`). Use internal reusable primitive buffers (`IntList`, `FloatList`) to achieve Zero-GC while guaranteeing headless unit test execution without engine JARs.

---

## 🎮 III. Input, Gestures & IME Architecture
11. **Keyboard Input & IME:** Handle `Backspace` exclusively in `onKeyDown(KeyCode.backspace)`. `onKeyTyped` must only process printable characters ($\ge 32$) and ignore `\b` to prevent double-backspace deletion bugs with Vietnamese IME (Unikey/EVKey).
12. **Event-Driven Drag Tracking:** Route continuous drag gestures via `onPointerDrag` in `EngineInputProcessor.touchDragged`, never poll `Core.input` inside frame render loops (`drawSelf`/`draw`).
13. **3-Pass Pointer Pipeline:** Pointer events propagate through a 3-pass lifecycle: `INITIAL` (tunneling/preview), `MAIN` (bubbling/action), and `FINAL` (hover tracking and cleanup). Actionable controls consume events with `event.consume()`.
14. **Idempotent State Mutators:** State mutating functions (`setText()`, `resize()`, `setBounds()`, `setSize()`, `setPreset()`) must self-check for dirty changes internally (`if (field == newValue) return`). Call-sites must invoke these mutators directly without redundant outer wrapper checks (`if (old != new)`).

---

## 🧩 IV. Code Architecture, Syntax & Micro-Style
15. **Failure-First Early Return (Flat Happy Path):** Banish the "Arrow Anti-pattern" (deep nested `if` pyramids). Always test for preconditions, invalid states, or nulls **first** and exit immediately (`return`, `continue`, `break`). The main execution path must stay flat at the base indentation level.
16. **Prefer `when` Expressions Over `if-else` Chains:** In Kotlin, chaining `if - else if - else` ($\ge 2$ conditions) is banned. Use `when` expressions (with a subject or argumentless `when { ... }`) to cleanly express multi-branch logic, type checking (`is`), enum/sealed class matching, and state assignments (`val x = when (...) { ... }`).
17. **Scope Function Safety (Ban `as? ... ?.let {} ?: run {}`):** Never chain `as?` with `?.let { } ?: run { }` to mutate or inspect subtypes. Use `when (val target = ...)` with natural Kotlin smart-casting (`is SubType -> ...; else -> ...`) to eliminate lambda closure allocations and ensure instant readability.
18. **Strict Bracket Usage Rules (`{}` vs Bracketless):**
    - **Bracketless ALLOWED ONLY FOR:** Trivial single-line guard returns (e.g., `if (width <= 0f) return`).
    - **Brackets `{}` MANDATORY FOR:** Any block with $\ge 2$ statements, multiline expressions, `for`/`while` loops, and any `if-else` where either branch is complex. Never write multiline bracketless code.
19. **`when` Branch Formatting Standards:**
    - **Single simple expression:** Single line `Condition -> expression` (no brackets).
    - **$\ge 2$ statements or nested checks:** Must break to a new indented block enclosed in curly braces `{ ... }`.
20. **Expressive Domain Naming (Ban Cryptic Abbreviations):** Ban overly cryptic 1-2 letter variables (`p`, `cr`, `hs`, `vis`, `d`, `v`, `w`, `h`, `f`, `rx`, `ry`, `padL`, `padT`, `hFlags`, `vFlags`) in business logic, rendering loops, public APIs, and test DSLs. Use clear, self-documenting domain identifiers (`point`, `cornerRadii`, `halfSize`, `isVisible`, `fraction`, `rectWidth`, `slotInnerHeight`, `font`, `measuredWidth`, `paddingLeft`, `horizontalFlags`).
21. **Guard Clause & Logical Block Breathing Room:** Always insert a blank line:
    - After guard clause early returns.
    - Between distinct logical steps within a function body.
    - Before and after major branching blocks (`when`, `if-else`, `try-catch`).
22. **File Granularity (Cohesion & Single Responsibility):**
    - **Merge Micro-Files:** Consolidate tiny micro-files ($<30$ lines) that share the same domain (e.g. token data classes, wrapper composables) into a single cohesive domain file (`Box.kt`, `ThemeTokens.kt`).
    - **Split Bloated Files:** Decompose large files exceeding $\sim 300$ lines or handling multiple distinct responsibilities into modular, feature-sliced components.
23. **In-File Structural Organization:** Organize code within each file in a predictable, top-to-bottom layout:
    1. `Line 1 Invariant Header` (`// [AGENT INVARIANT] Synchronously update @property, @param, and @see KDocs when modifying this file.`)
    2. `Package & Imports` (clean, grouped, zero unused, zero wildcard, zero fully qualified inline imports)
    3. `Public Component / Class Header with KDoc`
    4. `State & Properties`
    5. `Lifecycle & Primary Entrypoint Methods`
    6. `Step-Down Helper Functions` (Private/Internal placed immediately below the calling function)
    7. `Companion Object & Extension Functions`
    Separate distinct logical sections with section header comments (`// --- LIFECYCLE ---`, `// --- DRAWING PRIMITIVES ---`).

---

## 🔌 V. Platform Port Abstraction & Resource Pipelines
24. **Port & Facade Architecture (`PlatformHost`):** Keep UI and rendering layers 100% agnostic of specific game engines or native backends. Wire all platform operations through focused sub-ports (`WindowPort`, `InputPort`, `AssetPort`, `SystemPort`, `ImePort`) coordinated by `PlatformHost`.
25. **Single Source of Truth for Resource Resolution (DRY Asset Pipeline):** All asset loading (shaders, textures, fonts, configs) must delegate through `AssetPort.resolveAssetString` or `resolveAssetBytes`. Never duplicate classloader or filesystem cascading fallbacks in individual subsystems.
26. **Clean Reflection & Native Interop Boundaries:** Isolate reflection hooks (e.g., `SdlReflectionImePort`) and JNI bridges inside dedicated port implementations. Cache reflection fields once, avoid chained Elvis operators with inline side effects, and log warnings gracefully if reflection targets differ across platforms.
27. **Strict Explicit Named Imports (Zero Wildcards):** Never use wildcard imports (`import pkg.*`). Every imported type, function, and extension must be explicitly imported and enforced via `.editorconfig` (`ij_kotlin_name_count_to_use_star_import = 2147483647`).
28. **Local Library Source References (`.lib-source`):** Always inspect and read local library source codes in `.lib-source/` (`.lib-source/mindustry/`, `.lib-source/arc/`, `.lib-source/compose-runtime/`, `.lib-source/MindustryToolMod/`) when investigating engine behavior, internal APIs, or rendering pipelines. If a required dependency or library source is missing, clone it into `.lib-source/<lib-name>` before proceeding.

---

## 💾 VI. Zero-GC Hot-Loop Invariants & In-Source Documentation
29. **Zero-GC Hot-Loop Invariant:** In hot-paths executed every frame (`draw()`, `drawSelf()`, `layout()`, `measureWidth()`, `measureHeight()`):
    - Never call `withIndex()` on collections (allocates `IndexedValue` iterators). Use indexed `for (i in 0 until size)`.
    - Never instantiate temporary `Pair`/`Triple` instances to destructure return values. Use primitive local variables.
    - Never allocate temporary collections (`ArrayList`, `HashMap`) per frame. Use reusable scratch buffers with `.clear()`.
30. **Bilingual Documentation:** Maintain parallel bilingual documentation pairs (`*_vi.md` and `*_en.md`) in `docs/`. All inter-document markdown links must strictly use relative paths (`./` or `../`), never hardcoded machine paths (`file:///C:/...`).
31. **Modern Type-Checked KDoc & Line-1 Invariant Header Standard:**
    All KDoc comments in Kotlin source files (`*.kt`) must be written in simple, plain English and act as living, type-checked documentation optimized for IntelliJ IDEA / Dokka hover extraction:
    - **Line 1 Agent Invariant Header:** Put `// [AGENT INVARIANT] Synchronously update @property, @param, and @see KDocs when modifying this file.` at Line 1 (outside KDoc so it never pollutes IDE hover popups, while serving as an immediate memory trigger for agents).
    - **Header & Purpose:** Clean markdown title (`## ClassName`) followed by a concise 1-2 sentence description of domain purpose and responsibilities.
    - **Property Documentation (`@property`):** Declare `@property <name> <description>` on the class/interface header for all key properties (primary constructor and body properties). IntelliJ automatically extracts and renders the exact `@property` description when developers hover over `instance.propertyName`.
    - **Parameter Documentation (`@param`):** Declare `@param <name> <description>` on functions and composables.
    - **Type-Checked Cross-Linking (`@see [Symbol]`):** Use `@see [TargetClass]` or `[ClassName.propertyName]` in text instead of hardcoded file paths. This guarantees type-safe refactoring—IntelliJ automatically updates references upon symbol renaming/moving.
    - **Noisy Comments Ban:** Never add redundant trivial comments (e.g. `/** Loading */ LOADING`). Inline KDoc is reserved exclusively for non-obvious side-effects, unit/hardware constraints, or deprecations.
    ```kotlin
    // [AGENT INVARIANT] Synchronously update @property, @param, and @see KDocs when modifying this file.

    package org.mdt.core.platform.render

    import arc.graphics.g2d.TextureRegion

    /**
     * ## ImageCacheManager
     *
     * In-memory texture cache managing texture lifetime and eviction strategies.
     *
     * @property cacheLimit Maximum number of images kept in RAM before LRU cleanup.
     * @property isInitialized Whether the texture cache is ready for operations.
     *
     * @see TextureRegion
     * @see UIBatch
     */
    class ImageCacheManager { ... }
    ```
32. **Systematic Diagnostic & Signal Chain Integrity:** Verify the unbroken end-to-end signal flow: `Input Event` $\rightarrow$ `Actionable Node Matcher` (`isInteractiveOrOpaque`) $\rightarrow$ `Scissor Boundary Hit-Test` $\rightarrow$ `Event Bubbling` $\rightarrow$ `State Mutation` $\rightarrow$ `Layout/Draw`. Ensure no intermediate router silently drops or misroutes events for supported node types.


