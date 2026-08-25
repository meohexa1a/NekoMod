---
trigger: always_on
description: Mandatory coding standards, OpenGL Arc engine invariants, input handling, and storage rules for NekoMod.
---

# NekoMod Project Invariants & Coding Standards

1. **Type System:** Use `Float` (`f` literal suffix) for all geometric dimensions, insets (padding/margin), coordinates, corner radii, opacities, blur weights, and shader uniforms. Never use `Double`.
2. **In-Source Documentation:** All KDoc comments in Kotlin source files (`*.kt`) must be strictly 100% English. Reference documentation using plain text `See: docs/path/file_en.md` (do not use `@see` with file paths).
3. **OpenGL Active Texture Enum:** Always call `Gl.activeTexture(Gl.texture0 + unit)`, NEVER `Gl.texture2d + unit` (which causes `[GL] Error: invalid enum`).
4. **Render Loop Hooks:** Process GPU texture upload queues, FrameBuffers, and UI frame ticks inside `Trigger.uiDrawEnd` or `EngineRuntime.draw()` so they execute across all game states (menus, dialogs, paused, in-game). Never rely solely on `Trigger.update`.
5. **Keyboard Input & IME:** Handle `Backspace` exclusively in `onKeyDown(KeyCode.backspace)`. `onKeyTyped` must only process printable characters ($\ge 32$) and ignore `\b` to prevent double-backspace deletion bugs with Vietnamese IME (Unikey/EVKey).
6. **Storage & Windows File Locking:** Debounce rapid in-memory mutations before writing to disk (300ms delay). `Storage.atomicWrite` must use unique nano-timestamped staging files with synchronization (`synchronized(lock)`) to prevent Windows NTFS file lock conflicts (`process cannot access the file`).
7. **Documentation:** Maintain parallel bilingual documentation pairs (`*_vi.md` and `*_en.md`) in `docs/`. All inter-document markdown links must strictly use relative paths (`./` or `../`), never hardcoded machine paths (`file:///C:/...`).
8. **UIModifier Chaining:** Every built-in composable applying default visual styles must conclude with `.then(modifier)` to allow callers to override layout and event properties.
