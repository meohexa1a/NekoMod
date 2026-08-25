# Future Development Roadmap

This document outlines the phased development plan for the UI Engine and extended ecosystem of NekoMod.

---

## 🎯 Phase 1: Foundation Standardization (COMPLETED ✅)
* [x] Virtual DOM Hierarchy (`UINode`, `CanvasNode`, `LayoutNode`).
* [x] Compose Multiplatform Runtime Integration (`CompositionManager`, `NodeApplier`).
* [x] Standardized `LayoutNode + MeasurePolicy` Architecture (Zero-Overhead DOM).
* [x] Godot 2-Pass Box Container Algorithm (`GodotLayout`).
* [x] Unified Box Model (Unified Margins & Padding on `UINode`).
* [x] Fluent `UIModifier` Chaining & `@UIDslMarker` Scopes.
* [x] Core Widget Suite (`Button`, `Toggle`, `Card`, `Divider`, `Text`, `Row`, `Column`, `Grid`, `Spacer`).
* [x] Hardware-Accelerated SDF Box & 2-Pass Gaussian Backdrop Blur (`BoxRenderer`, `BoxBlur`).
* [x] Full Core Subsystems (`AsyncDispatcher`, `HttpEngine`, `Storage`, `KVStore`, `LRUTextureCache`, `ImageLoader`, `I18nEngine`).

---

## 🚀 Phase 2: Advanced Widget Toolkit (IN PROGRESS 🔄)
* [x] **`ScrollView` & Scissor Clipping:**
  * Smooth vertical & horizontal scrolling with mouse wheel and drag inertia.
  * Multi-level nested hardware OpenGL scissor clipping (`ScissorStack`).
  * Modern slim rounded scrollbar track & thumb rendering.
* [x] **`TextField` / Text Input:**
  * Decoupled headless state machine (`TextEditState`).
  * Caret blink animation, selection highlighting, and OS clipboard (`Ctrl+C`, `Ctrl+V`, `Ctrl+X`, `Ctrl+A`).
  * 100% compatible with Vietnamese Telex IME (Unikey, EVKey).
* [x] **`Image` & `ImageNode`:**
  * Multi-source loading: Remote HTTP/HTTPS (OkHttp), Mindustry Sprite Atlas, and local files.
  * Burst VRAM upload throttling queue for steady 60-144 FPS.
* [ ] **`Slider` & `ProgressBar`:**
  * Slider for volume/values and progress/health bars with animated gradients.
* [ ] **`Dropdown` & `Tooltip`:**
  * Dropdown selector and mouse-anchored tooltip overlays.

---

## 🏗️ Phase 3: Game UI Replacement
* [ ] **`ModalDialog`:**
  * Centered modal popup with frosted backdrop blur and dismiss callbacks.
* [ ] **Floating World-to-Screen UI:**
  * In-world block/unit configuration overlays projected from World Tile $(x, y)$ to Screen $(x, y)$ coordinates.
* [ ] **In-Game Custom HUD:**
  * Custom resource monitor, minimap widgets, and command panels.

---

## 🛠️ Phase 4: Schema Architecture & Hot-Reload (Data-Driven Declarative UI)
* [ ] **Reactive Schema Parser (HJSON / JSON UI):**
  * Live loading and hot-reloading declarative layouts from `.hjson` / `.json` without rebuilding.
* [ ] **2-Way Data-Binding Bridge:**
  * Automatic state binding between JSON Schema definitions and `KVStore` properties.
