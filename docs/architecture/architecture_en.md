# UI Engine Architecture Overview

This document details the architecture of the **NekoMod UI Engine** — a pure Kotlin Multiplatform (KMP) declarative UI framework operating on a standalone Virtual DOM tree and rendering directly to the GPU via OpenGL 2.0 / GLSL 120.

---

## 1. Core Architectural Objectives

1. **High Performance ($144\text{ FPS}$):** GPU-native SDF shader rendering, strict `Float` type invariant eliminating cast overhead, and zero synchronous VRAM stalls.
2. **Modern Declarativity:** Powered by Compose Multiplatform Runtime for reactive state management (`mutableStateOf`) with automatic fine-grained recomposition.
3. **Robust Layout Paradigm:** 2-Pass Godot container algorithm paired with modular `MeasurePolicy` strategies, resolving child overlap and flexible flex expansion.
4. **Modularity & Scalability:** Strict layer decoupling across layout, input, and render tiers using Feature-Sliced Package Co-location.

---

## 2. 5-Layer Architectural Model

```
┌────────────────────────────────────────────────────────────────────────┐
│ 1. COMPOSE DECLARATIVE LAYER (Declarative Frontend)                    │
│    Card, Row, Column, Text, Slider, Toggle, Button, UIModifier         │
└───────────────────────────────────┬────────────────────────────────────┘
                                    │ (NodeApplier & BroadcastFrameClock)
                                    ▼
┌────────────────────────────────────────────────────────────────────────┐
│ 2. VIRTUAL DOM TREE (In-Memory Node Hierarchy)                         │
│    CanvasNode (Root) ──► LayoutNode (Box/Row/Col) ──► TextNode         │
│    • Node State: parent/children, bounds (Rect), margin, padding, focus│
│    • MeasurePolicy: BoxMeasurePolicy, ColumnMeasurePolicy, ...        │
└───────────────────────────────────┬────────────────────────────────────┘
                                    │ (isLayoutDirty cascade)
                                    ▼
┌────────────────────────────────────────────────────────────────────────┐
│ 3. LAYOUT ENGINE (2-Pass Godot Sizing Algorithm)                       │
│    GodotLayout (layoutBox, layoutGrid, fitChildInRect, layoutAnchors)   │
│    • Pass 1: Measure Preferred & Minimum Dimensions                    │
│    • Pass 2: Slot Allocation & Margin Inset Placement                  │
└───────────────────────────────────┬────────────────────────────────────┘
                                    │ (Draw Loop Trigger: Trigger.uiDrawEnd)
                                    ▼
┌────────────────────────────────────────────────────────────────────────┐
│ 4. GPU RENDERING PIPELINE (Hardware GPU Drawing)                       │
│    EngineRenderer (Orthographic Projection 0..Width, 0..Height)        │
│    • BoxRenderer (SDF Shader corners, borders, shadows, outer glow)   │
│    • TextRenderer (BMFont Baseline alignment @ scale = 1.0f)           │
└───────────────────────────────────┬────────────────────────────────────┘
                                    │ (User Interactions)
                                    ▼
┌────────────────────────────────────────────────────────────────────────┐
│ 5. INPUT & EVENT DISPATCHING (Interaction & Routing)                   │
│    EngineInputProcessor (Reverse-DFS Hit-Testing, onPointerDrag, Focus)│
└────────────────────────────────────────────────────────────────────────┘
```

---

## 3. Subsystem Breakdown

### A. Virtual Node Hierarchy (`org.mdt.core.ui` & `components.layout`)
* **`UINode`**: Core foundation containing 4-sided Margins, Paddings, sizing flags (`minWidth`, `minHeight`, `sizeFlagsHorizontal`, `sizeFlagsVertical`, `stretchRatio`), child lists, and pointer callbacks (`onClick`, `onHover`, `onPointerDown`, `onPointerDrag`, `onPointerUp`).
* **`LayoutNode`**: Unified container node backed by a swappable `measurePolicy: MeasurePolicy` (`BoxMeasurePolicy`, `ColumnMeasurePolicy`, `RowMeasurePolicy`, `GridMeasurePolicy`) and zero-overhead lazy visual allocation via `ensureVisuals()`.
* **`CanvasNode`**: Root viewport container managing screen resize events and driving the top-level 2-pass layout cascade.

### B. Recomposition & Frame Clock (`org.mdt.ui.compose`)
* **`UIComposition`**: Manages the `Recomposer` lifecycle and `NodeApplier` bound to `rootCanvas`.
* **`BroadcastFrameClock`**: Synchronizes recomposition ticks with Mindustry's frame loop via `Trigger.uiDrawEnd` or `EngineRuntime.draw()`.

### C. Input Pipeline & Gestures (`org.mdt.ui.input`)
* **`EngineInputProcessor`**: Executes Reverse-DFS hit-testing to identify the deepest actionable leaf node under the pointer.
* **Event-Driven Drag Routing:** Continuous pointer drag gestures are dispatched directly from `touchDragged` to `pressedNode.onPointerDrag`, ensuring seamless tracking across the entire screen.
