# Compose DSL Guide

This document provides a comprehensive guide to utilizing NekoMod's declarative Kotlin DSL, scoped modifiers (`RowScope`, `ColumnScope`, `BoxScope`), fluent `UIModifier` chaining, and built-in component library.

---

## 1. Standard Screen Layout

```kotlin
EngineRuntime.setContent {
    var count by remember { mutableStateOf(0) }
    var textInput by remember { mutableStateOf("") }
    var showModal by remember { mutableStateOf(false) }

    Card(
        modifier = Modifier.anchor(LayoutPreset.CENTER),
        color = CardDefaults.color,
        borderColor = CardDefaults.borderColor,
        radius = CardDefaults.radius,
        isGlass = true
    ) {
        Column(gap = 12.0f) {
            Text(
                text = "NekoMod Dashboard",
                color = ThemeTokens.textPrimary
            )
            Text(
                text = "Declarative Pure KMP Compose Engine",
                color = ThemeTokens.textSecondary
            )

            TextField(
                value = textInput,
                onValueChange = { textInput = it },
                placeholder = "Type something...",
                modifier = Modifier.fillMaxWidth()
            )

            Row(gap = 8.0f) {
                Button(
                    text = "Count: $count",
                    onClick = { count++ },
                    colors = ButtonDefaults.filled(),
                    modifier = Modifier.weight(1.0f)
                )
                Button(
                    text = "Reset",
                    onClick = { count = 0 },
                    colors = ButtonDefaults.outlined(),
                    modifier = Modifier.weight(1.0f)
                )
            }
        }
    }

    ModalDialog(
        visible = showModal,
        onDismiss = { showModal = false }
    ) {
        Column(gap = 16.0f) {
            Text("Modal Dialog Content")
            Button("Close", onClick = { showModal = false })
        }
    }
}
```

---

## 2. Scoped Layout Modifiers

NekoMod enforces strict Jetpack/JetBrains Compose modifier scoping to prevent cross-axis distortion:

### A. `RowScope` (Horizontal Containers)
* `Modifier.weight(weight: Float)`: Allocates free horizontal space proportionally without distorting vertical sizing.
* `Modifier.align(alignment: VerticalAlign)`: Cross-axis vertical alignment within a row slot (`VerticalAlign.TOP`, `VerticalAlign.CENTER`, `VerticalAlign.BOTTOM`, `VerticalAlign.FILL`).

### B. `ColumnScope` (Vertical Containers)
* `Modifier.weight(weight: Float)`: Allocates free vertical space proportionally without distorting horizontal sizing.
* `Modifier.align(alignment: HorizontalAlign)`: Cross-axis horizontal alignment within a column slot (`HorizontalAlign.START`, `HorizontalAlign.CENTER`, `HorizontalAlign.END`, `HorizontalAlign.FILL`).

### C. `BoxScope` (2D Coordinate & Overlays)
* `Modifier.align(alignment: Alignment)`: Dual-axis alignment placement within Box bounds (`Alignment.Center`, `Alignment.TopStart`, `Alignment.BottomEnd`, etc.).

---

## 3. General `UIModifier` Elements

### A. Box Model & Sizing
* `Modifier.pad(all = 16.0f)` / `Modifier.pad(horizontal = 16.0f, vertical = 8.0f)`: Inward content padding insets.
* `Modifier.margin(all = 8.0f)`: Outward spacing margins.
* `Modifier.size(width = 100.0f, height = 50.0f)`: Fixed box dimensions.
* `Modifier.minWidth(100.0f)` / `Modifier.minHeight(40.0f)`: Protective minimum dimensional bounds.
* `Modifier.fillMaxWidth()` / `Modifier.fillMaxHeight()` / `Modifier.fillMaxSize()`: Container expansion.
* `Modifier.autoWidth()` / `Modifier.autoHeight()` / `Modifier.hugContent()`: Intrinsic auto-layout sizing.

### B. Visuals & Styling (1-Draw-Call UIBatch)
* `Modifier.background(color: Color)`: Solid or translucent fill color.
* `Modifier.radius(radius: Float)`: SDF rounded corners with analytical pixel radius.
* `Modifier.border(width: Float, color: Color)`: Outline border stroke.
* `Modifier.glass(enabled: Boolean = true)`: Frosted Dual-Kawase background sampling.
* `Modifier.shadow(radius: Float, color: Color, offsetX: Float, offsetY: Float)`: Soft SDF drop shadow.

### C. Interactivity & Events
* `Modifier.clickable(onClick = { ... })`: Pointer click event listener.
* `Modifier.hoverable { isHovered -> ... }`: Hover state listener.
* `Modifier.draggable(onDrag = { dx, dy -> ... })`: Continuous drag gesture tracker.
* `Modifier.consumePointer()`: Intercepts and consumes pointer events to prevent bubbling to backdrop scrims.
* `Modifier.cursor(cursor: CursorIcon)`: Hover mouse cursor override.

---

## 4. Built-in Component Library (Compose Material 3 Standards)

| Component | Role / Scope | Key Parameters |
| :--- | :--- | :--- |
| `Surface()` | Foundational visual container with SDF radius, glass blur, and `LocalContentColor` | `modifier`, `color`, `contentColor`, `radius`, `borderWidth`, `borderColor`, `isGlass`, `contentPadding`, `onClick`, `content: BoxScope` |
| `Card()` | Information grouping card built on `Surface` | `modifier`, `color`, `radius`, `borderWidth`, `borderColor`, `isGlass`, `contentPadding`, `content: BoxScope` |
| `Button()` | Interactive button built on `Surface` with `RowScope` slot | `onClick`, `modifier`, `colors: ButtonColors`, `enabled`, `contentPadding`, `content: RowScope` |
| `IconButton()` | Compact icon-centric action button | `region`, `onClick`, `modifier`, `colors`, `tint`, `enabled` |
| `TextField()` | Interactive text input field built on `Surface` with IME support | `value`, `onValueChange`, `placeholder`, `enabled`, `isMultiline`, `font` |
| `Text()` | Precise BMFont text label adapting to ambient `LocalContentColor` | `text`, `modifier`, `color`, `font`, `align`, `wrap`, `ellipsis` |
| `Image()` | Texture region visual with automatic tint inheritance | `region`, `modifier`, `tint`, `aspectRatio` |
| `ModalDialog()` | Full-screen modal overlay with frosted scrim and central `Surface` | `visible`, `onDismiss`, `modifier`, `scrimColor`, `content: BoxScope` |
| `TooltipBox()` | Floating hover tooltip surface with delay | `tooltip: BoxScope`, `modifier`, `delayMs`, `content: BoxScope` |
