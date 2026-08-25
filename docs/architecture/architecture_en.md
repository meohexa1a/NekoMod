# UI Engine Architecture Overview

This document details the architecture of the **NekoMod UI Engine** — a pure Kotlin Multiplatform (KMP) declarative UI framework running on an independent Virtual DOM tree rendered directly to the GPU via OpenGL without depending on Arc/Mindustry's legacy Scene2D.

---

## 1. Design Goals

1. **High Performance:** Direct GPU rendering using Signed Distance Field (SDF) shaders and a 2-pass Gaussian blur pipeline maintaining 60+ FPS.
2. **Modern Declarative Paradigm:** Powered by Compose Multiplatform Runtime (`mutableStateOf`), supporting automated recomposition on state changes.
3. **Robust Layout Model:** Implements Godot Engine's 2-pass container algorithm to eliminate overlap bugs, handle flexible stretch ratios, and distribute spacing cleanly.
4. **Decoupled Architecture:** Clean separation of concerns between layout calculation, event handling, and GPU rendering.

---

## 2. 5-Layer Architecture

```
┌────────────────────────────────────────────────────────────────────────┐
│ 1. COMPOSE DECLARATIVE LAYER                                           │
│    Card, Row, Column, Text, Button, Toggle, Divider, UIModifier        │
└───────────────────────────────────┬────────────────────────────────────┘
                                    │ (NodeApplier & BroadcastFrameClock)
                                    ▼
┌────────────────────────────────────────────────────────────────────────┐
│ 2. VIRTUAL DOM TREE                                                    │
│    CanvasNode (Root) ──► BoxNode ──► ColumnNode ──► TextNode           │
│    • Manages: parent/children, bounds (Rect), margin, padding, focus  │
└───────────────────────────────────┬────────────────────────────────────┘
                                    │ (isLayoutDirty cascade)
                                    ▼
┌────────────────────────────────────────────────────────────────────────┐
│ 3. LAYOUT ENGINE (Godot 2-Pass Layout)                                 │
│    GodotLayout (layoutBox, layoutGrid, fitChildInRect, layoutAnchors)   │
│    • Pass 1: Compute Preferred/Minimum Sizes                           │
│    • Pass 2: Slot Allocation & Margin Insetting                        │
└───────────────────────────────────┬────────────────────────────────────┘
                                    │ (Draw Loop Trigger)
                                    ▼
┌────────────────────────────────────────────────────────────────────────┐
│ 4. GPU RENDERING PIPELINE                                              │
│    EngineRenderer (Orthographic Projection 0..Width, 0..Height)        │
│    • BoxRenderer (SDF Shader for rounded rects, borders, shadows)     │
│    • BoxBlur (2-Pass Gaussian Blur FrameBuffer)                        │
│    • TextRenderer (BMFont Baseline alignment & localization bundle)    │
└───────────────────────────────────┬────────────────────────────────────┘
                                    │ (User Interactions)
                                    ▼
┌────────────────────────────────────────────────────────────────────────┐
│ 5. INPUT & EVENT DISPATCHING                                           │
│    EngineInputProcessor (Hit testing, Actionable Ancestor, Hover)      │
└────────────────────────────────────────────────────────────────────────┘
```

---

## 3. Core Modules

### A. Virtual Node Hierarchy (`org.mdt.ui.core`)
* **`UINode`**: The base class for all UI elements. Holds the full Box Model (4-sided Margin and Padding), size constraints (`minWidth`, `minHeight`, `sizeFlagsHorizontal`, `sizeFlagsVertical`, `stretchRatio`), children list, event listeners (`onClick`, `onHover`, `onPointerDown`, `onPointerUp`), and coordinate transform helpers (`localToGlobal`, `globalToLocal`, `getGlobalBounds`).
* **`CanvasNode`**: Root container representing the full screen viewport. Synchronizes with window resizing and triggers `layout()` recalculation whenever an `isLayoutDirty` flag is set.

### B. Compose Multiplatform Integration (`org.mdt.ui.compose`)
* **`NodeApplier`**: Extends `AbstractApplier<UINode>`, translating Compose tree operations into deterministic top-down modifications on the virtual `UINode` tree.
* **`CompositionManager`**: Drives the frame clock (`BroadcastFrameClock`) and observes snapshot writes (`Snapshot.registerGlobalWriteObserver`) to trigger asynchronous recomposition without blocking the main game loop.

### C. Box Model (Margin $\rightarrow$ Border $\rightarrow$ Padding $\rightarrow$ Content)
Every node strictly follows the standard Box Model:
1. **Margin (Outward Spacing):** Creates separation space between the node and neighboring siblings.
2. **Bounds / Border (Visual Outline):** The actual graphical boundary drawn on screen (fill color, rounded corners, border strokes, shadows).
3. **Padding (Inward Spacing):** Inner insets separating the border boundary from inner content or child nodes.
4. **Content:** The actual rendered payload (`TextNode` glyphs or child container nodes).
