# Future Development Roadmap

This document outlines the phased development roadmap for the NekoMod UI Engine and extended ecosystem features.

---

## 🎯 Phase 1: Core Foundation (COMPLETED ✅)
* [x] Pure Virtual DOM tree (`UINode`, `CanvasNode`).
* [x] Compose Multiplatform Runtime integration (`CompositionManager`, `NodeApplier`).
* [x] Godot 2-Pass Container Layout Algorithm port (`GodotLayout`).
* [x] Unified Box Model (Margin & Padding centralized on `UINode`).
* [x] Fluent, chainable `UIModifier` system & `@UIDslMarker` scopes.
* [x] Core widget suite (`Button`, `Toggle`, `Card`, `Divider`, `Text`, `Row`, `Column`, `Grid`, `Spacer`).
* [x] GPU SDF rounded shader & 2-pass Gaussian glassmorphism blur (`BoxRenderer`, `BoxBlur`).

---

## 🚀 Phase 2: Widget Toolkit Expansion
* [ ] **`ScrollView` & Scissor Clipping:**
  * Smooth vertical & horizontal scrolling.
  * Viewport clipping using OpenGL Scissor (`ScissorStack`) to prevent content bleeding.
  * Customizable scrollbar thickness and thumb colors.
* [ ] **`TextField` / Input Field:**
  * Hardware keyboard text entry.
  * Blinking caret cursor, text selection highlight, clipboard copy/paste (`Ctrl+C`, `Ctrl+V`).
* [ ] **`Slider` & `ProgressBar`:**
  * Value slider bars, health bars, and building progress gauges.
* [ ] **`ImageNode`:**
  * Mindustry texture and atlas region rendering with scaling modes (`Fit`, `Crop`, `Stretch`).
* [ ] **`Dropdown` & `Tooltip`:**
  * Dropdown selector menus and contextual hover tooltips tracking pointer coordinates.

---

## 🏗️ Phase 3: Mindustry UI Replacement
* [ ] **`ModalDialog`:**
  * Modal popup windows with backdrop dimming and focus trapping.
* [ ] **Floating World-to-Screen UI:**
  * Floating configuration dialogs anchored to in-game tile coordinates (e.g. Message Blocks, Logic Processors, Core telemetry).
  * Real-time world-to-screen matrix projection tracking camera pan and zoom.
* [ ] **Custom In-Game HUD:**
  * Resource monitors, unit command dashboards, and custom minimaps.

---

## 🛠️ Phase 4: In-Game UI Inspector & Editor
* [ ] **UI Inspector (`F12`):**
  * Live runtime visual debugger highlighting bounding boxes, margins, padding, and node tree hierarchies.
* [ ] **Schema Parser (HJSON / JSON UI):**
  * Dynamic declarative UI loading from `.hjson` / `.json` files supporting instant live hot-reloading without re-compilation.
