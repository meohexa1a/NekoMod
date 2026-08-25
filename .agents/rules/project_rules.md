---
trigger: always_on
description: Mandatory domain-grouped coding standards, OpenGL Arc engine invariants, input handling, and storage rules for NekoMod.
---

# NekoMod Project Invariants & Domain-Grouped Standards

---

## 🎨 I. Graphics, Shaders & Typography Standards
1. **Type System (Float Everywhere):** Use `Float` (`f` literal suffix) for all geometric dimensions, insets (padding/margin), coordinates, corner radii, opacities, blur weights, and shader uniforms. Never use `Double`.
2. **OpenGL Active Texture Enum:** Always call `Gl.activeTexture(Gl.texture0 + unit)`, NEVER `Gl.texture2d + unit` (which causes `[GL] Error: invalid enum`).
3. **Arc Shader Headers & Precisions:** Never declare `#ifdef GL_ES` or `#version` in raw shader files (`*.frag`, `*.vert`). Arc's `Shader` compiler automatically injects precision qualifiers internally; manual declarations cause duplicate definition crashes.
4. **Render Loop Hooks:** Process GPU texture upload queues, FrameBuffers, and UI frame ticks inside `Trigger.uiDrawEnd` or `EngineRuntime.draw()` so they execute across all game states (menus, dialogs, paused, in-game). Never rely solely on `Trigger.update`.
5. **Typography & BMFont Scaling:** Render BMFonts (`Fonts.def`, `Fonts.tech`, `Fonts.large`) at natural `scale = 1.0f` (or integer increments). Never apply fractional float scaling (e.g. `0.8f`, `0.85f`, `0.9f`, `1.1f`) to bitmap atlas fonts, which causes sub-pixel blurring and glyph distortion. Create visual hierarchy using color luminance (e.g. primary white vs muted gray `9399b2`) and font family variants (`Fonts.large`, `Fonts.tech`).

---

## 🎮 II. Input & Interaction Architecture
6. **Keyboard Input & IME:** Handle `Backspace` exclusively in `onKeyDown(KeyCode.backspace)`. `onKeyTyped` must only process printable characters ($\ge 32$) and ignore `\b` to prevent double-backspace deletion bugs with Vietnamese IME (Unikey/EVKey).
7. **Event-Driven Drag Tracking:** Route continuous drag gestures via `onPointerDrag` in `EngineInputProcessor.touchDragged`, never poll `Core.input` inside frame render loops (`drawSelf`/`draw`).

---

## 🧩 III. Code Architecture & Style
8. **Feature-Sliced Package Co-location:** Organize UI widgets into domain sub-packages (`components.input.textfield`, `components.input.slider`, `components.display.image`, etc.) co-locating the virtual node, composable, and state machine together. Avoid flat technical layers (`compose/`, `widgets/`).
9. **Declarative Post-Design:** Avoid embedding heavy runtime UI inspector overlays into the game client. Prioritize compile-time IDE Kotlin DSL design and reactive text schema (HJSON/JSON) with live hot-reload.
10. **UIModifier Chaining:** Every built-in composable applying default visual styles must conclude with `.then(modifier)` to allow callers to override layout and event properties.
11. **Concise Inline Lambdas:** Format trivial, single-statement callbacks/lambdas on a single line (e.g. `AsyncDispatcher.onMainThread { onResult(null, e) }`) to maximize readability and avoid visual clutter.

---

## 💾 IV. Storage, I/O & Concurrency
12. **Storage & Windows File Locking:** Debounce rapid in-memory mutations before writing to disk (300ms delay). `Storage.atomicWrite` must use unique nano-timestamped staging files with synchronization (`synchronized(lock)`) to prevent Windows NTFS file lock conflicts (`process cannot access the file`).
13. **Local Library Source References (`.lib-source`):** Always inspect and read local library source codes in `.lib-source/` (`.lib-source/mindustry/`, `.lib-source/arc/`, `.lib-source/compose-runtime/`, `.lib-source/MindustryToolMod/`) when investigating engine behavior, internal APIs, or rendering pipelines. If a required dependency or library source is missing, clone it into `.lib-source/<lib-name>` before proceeding.

---

## 📖 V. Documentation & In-Source Standards
14. **Bilingual Documentation:** Maintain parallel bilingual documentation pairs (`*_vi.md` and `*_en.md`) in `docs/`. All inter-document markdown links must strictly use relative paths (`./` or `../`), never hardcoded machine paths (`file:///C:/...`).
15. **In-Source KDoc Documentation:** All KDoc comments in Kotlin source files (`*.kt`) must be strictly 100% English. Reference documentation using plain text `See: docs/path/file_en.md` (do not use `@see` with file paths).
