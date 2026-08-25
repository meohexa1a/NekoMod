# Future Development Roadmap

This document outlines the phased roadmap for NekoMod's UI Engine architecture and future feature expansions.

---

## 🎯 Phase 1: Engine Foundation & Core DOM (COMPLETED ✅)
* [x] Standalone Virtual DOM tree (`UINode`, `CanvasNode`, `LayoutNode`).
* [x] Compose Multiplatform Runtime integration (`CompositionManager`, `NodeApplier`).
* [x] Unified `LayoutNode + MeasurePolicy` architecture (Zero-Overhead DOM).
* [x] 2-Pass Godot Container Layout algorithm port (`GodotLayout`).
* [x] Comprehensive Box Model (Unified Margin & Padding on `UINode`).
* [x] Fluent `UIModifier` chain with `@UIDslMarker` scope protection.
* [x] Fundamental Widget suite (`Button`, `Toggle`, `Card`, `Divider`, `Text`, `Row`, `Column`, `Grid`, `Spacer`).
* [x] GPU SDF corner shaders and 2-pass Gaussian blur (`BoxRenderer`, `BoxBlur`).
* [x] Core Subsystems (`AsyncDispatcher`, `HttpEngine`, `Storage`, `KVStore`, `LRUTextureCache`, `ImageLoader`, `I18nEngine`).

---

## 🚀 Phase 2: Advanced Interactive Component Suite (COMPLETED ✅)
* [x] **`ScrollView` & Scissor Clipping:**
  * Smooth vertical/horizontal scrolling with mouse wheel and drag gestures.
  * Multi-level OpenGL hardware scissor clipping (`ScissorStack`).
  * Sleek modern scrollbars.
* [x] **`TextField` / Text Input:**
  * Standalone editing state machine (`TextEditState`).
  * Blinking cursor, selection range, clipboard operations (`Ctrl+C`, `Ctrl+V`, `Ctrl+X`, `Ctrl+A`).
  * Full Vietnamese IME compatibility (Unikey, EVKey).
* [x] **`Image` & `ImageNode`:**
  * Multi-source resolution: Web URL (OkHttp), Mindustry Atlas, and local files.
  * VRAM upload throttling with frame upload budgets.
* [x] **`Slider` (Capsule Pill Style) & `ProgressBar`:**
  * Modern Capsule Pill Slider with integrated title/percentage and global `onPointerDrag`.
  * Smooth rounded progress bar with multiple semantic color palettes.
* [x] **`Tooltip` & `Toggle`:**
  * Hover tooltip attachments.
  * Sleek 3D inset thumb switch toggle with glowing active track.

---

## 🏗️ Phase 3: Game UI Replacement & World HUD (IN PROGRESS 🔄)
* [ ] **`ModalDialog`:**
  * Backdrop-blurring modal dialogs blocking underlying pointer interactions.
* [ ] **Floating World-to-Screen UI:**
  * World-attached floating inspector menus bound to in-game structures (Message Blocks, Processors, Core telemetry).
  * Automatic World $(x, y)$ to Screen $(x, y)$ coordinate projection during camera zoom/pan.
* [ ] **In-Game Custom HUD:**
  * Dynamic resource telemetry, custom minimap overlays, and unit command panels.

---

## 🛠️ Phase 4: Data-Driven Schema & Live Hot-Reload
* [ ] **Schema Parser (HJSON / JSON UI):**
  * Dynamic UI loading and hot-reloading from `.hjson` / `.json` without recompilation.
* [ ] **2-Way Data-Binding Bridge:**
  * Direct reactive bindings between JSON schemas and `KVStore` states.
