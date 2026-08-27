# UI Component Architecture & Engineering Specification

This document establishes the mandatory standards, architectural design patterns, parameter conventions, sizing constraints, and interaction models for all declarative UI components in **NekoMod**.

---

## 🏛️ I. Component Classification & Taxonomy

UI components in NekoMod are organized into four distinct architectural tiers:

```
┌────────────────────────────────────────────────────────────────────────┐
│                        4. Feedback & Dialogs                           │
│              (ModalDialog, Tooltip, Badge, ProgressBar)                │
├────────────────────────────────────────────────────────────────────────┤
│                       3. Surfaces & Containers                         │
│             (Card, GroupedList, ListItem, Divider, ScrollView)         │
├────────────────────────────────────────────────────────────────────────┤
│                         2. Controls & Inputs                           │
│        (Button, TextButton, IconButton, Slider, Toggle, TextField)     │
├────────────────────────────────────────────────────────────────────────┤
│                         1. Layout Primitives                           │
│           (Box, Row, Column, Grid, Spacer, Canvas, Text, Image)        │
└────────────────────────────────────────────────────────────────────────┘
```

1. **Layout Primitives (`org.mdt.ui.components.layout`, `text`, `display`):**
   Fundamental building blocks mapping directly to virtual `UINode` trees with zero styling overhead.
2. **Controls & Inputs (`org.mdt.ui.components.input`, `surface`):**
   Interactive form controls, buttons, sliders, and text fields managing gestures, focus, and state feedback.
3. **Surfaces & Containers (`org.mdt.ui.components.surface`, `list`, `scroll`):**
   Structural cards, grouped list table views, and scroll containers implementing Apple HIG materials (Frosted Glass, Elevated, Outlined).
4. **Feedback & Dialogs (`org.mdt.ui.components.dialog`, `display`):**
   Floating modal sheets, tooltips, progress bars, and status indicator chips.

---

## 📐 II. Component Signature & Parameter Conventions

Every built-in composable function in NekoMod MUST adhere to the standardized parameter ordering:

```kotlin
@Composable
fun MyComponent(
    // 1. Mandatory Data / State
    value: Float,
    // 2. Event Callbacks
    onValueChange: (Float) -> Unit,
    // 3. Modifier (Chainable UIModifier with default UIModifier)
    modifier: UIModifier = UIModifier,
    // 4. Customization Options & Visual Variants
    enabled: Boolean = true,
    variant: MyComponentVariant = MyComponentVariant.DEFAULT,
    activeColor: Color = Theme.colors.blue,
    // 5. Optional Trailing Content Slot
    content: (@Composable () -> Unit)? = null
)
```

### Mandatory Signature Invariants:
1. **Default Modifier:** `modifier: UIModifier = UIModifier` must always have a default value.
2. **Modifier Chaining at the End:** The composable's root container MUST chain `.then(modifier)` at the very end of its modifier pipeline:
   ```kotlin
   Box(
       modifier = Modifier
           .radius(shapes.md)
           .background(currentBg)
           .pad(horizontal = 12f, vertical = 6f)
           .then(modifier) // Caller can override layout bounds, weights, or gestures
   )
   ```
3. **No Redundant Overloads:** Prefer optional parameters with sensible defaults over multiple ambiguous overloads.

---

## 📏 III. Sizing, Constraints & Layout Engine Invariants

### 1. Hug Content & Auto-Layout by Default (Rule 6)
* Leaf controls (`Button`, `Badge`, `Toggle`, `SegmentedControl`, `MonoText`) must calculate their dimensions dynamically based on their content (`getPrefWidth()` / `getPrefHeight()`) plus insets (`pad`).
* Containers (`Box`, `Row`, `Column`, `Card`, `GroupedList`) must never hardcode arbitrary widths or heights unless explicitly required by design.

### 2. Protective Bounds (`minWidth` / `minHeight`)
* Interactive input controls must enforce protective touch bounds to prevent layout collapse:
  * `Slider`: `minHeight(24f)` on touch target, `minHeight(8f)` on inner track.
  * `Button` / `IconButton`: `minHeight(28f)` or `minWidth(28f)`.
  * `TextField`: `minHeight(32f)`.
  * `Toggle`: fixed `size(44f, 24f)`.

### 3. Orthogonal 2-Pass Sizing Model
The layout engine evaluates the Horizontal (X) and Vertical (Y) axes independently:

$$\text{Dimension} = \begin{cases} 
\text{child.fixedDimension} & \text{if } \text{dimension} > 0\text{f} \\
\text{slotInnerDimension} & \text{if } (\text{flags} \ \& \ \text{SizeFlags.FILL}) \ne 0 \\
\min(\text{slotInnerDimension}, \text{childPureMinDimension}) & \text{otherwise (Hug Content)}
\end{cases}$$

* **In `Row` (Horizontal Flex):**
  * Main axis (X): Children take intrinsic width (`getChildMinWidth`), unless `Modifier.weight(ratio)` or `Modifier.fillMaxWidth()` is set.
  * Cross axis (Y): Children take intrinsic height and align vertically according to `Row.alignment` (defaults to `Alignment.CenterStart` / vertically centered).
* **In `Column` (Vertical Flex):**
  * Main axis (Y): Children stack top-down taking intrinsic height (`getChildMinHeight`), unless `Modifier.weight(ratio)` or `Modifier.fillMaxHeight()` is set.
  * Cross axis (X): Children take intrinsic width and align horizontally according to `Column.alignment` (defaults to `Alignment.TopStart` / left aligned).

---

## ⚡ IV. Memory & Zero-GC Invariants

To maintain 60 FPS performance without garbage collection pauses:

1. **Value Class Color:** Always use `org.mdt.core.ui.graphics.Color` (`@JvmInline value class Color(val value: ULong)`). Never instantiate `arc.graphics.Color` in render or layout paths.
2. **Arc Object Bridging:** When calling Arc font or graphics APIs, bridge via `arc.util.Tmp.c1` (e.g., `color.toArcColor(Tmp.c1)`).
3. **Typed Modifier Elements:** Modifiers must be implemented as immutable `data class Element : UIModifier.Element` with value-based `equals()` and `hashCode()` to enable Compose recomposition skipping.
4. **State Animations:** Animate interactive state changes (hover, press, progress) via `animateFloatAsState` with easing transitions (`tween(durationMillis = 140, easing = FastOutSlowInEasing)`).

---

## 🎮 V. Interaction, Gestures & Feedback Invariants

1. **Hover & Press States:**
   All clickable components must provide visual feedback:
   * **Hover:** Subtle glow, background brightening (`colors.glassThin.withAlpha(...)`), and border highlight.
   * **Press:** Tactile color dimming/deepening (`lerp(pressBg, pressAnim)`).
2. **Pointer Routing:**
   * Interactive elements must specify gestures via `clickable`, `hoverable`, `onPointerDown`, `onPointerDrag`, `onPointerUp`.
   * Drag gestures (Sliders, Resize Handles, Canvas Panning) must track continuous delta via `onPointerDrag` in `EngineInputProcessor`, never polling `Core.input` inside frame render loops.
3. **Hardware Scissor Clipping:**
   Scroll containers and bounded cards must enable `clip = true` (`Modifier.clip(true)`) to prevent visual overflow onto neighboring panels.
