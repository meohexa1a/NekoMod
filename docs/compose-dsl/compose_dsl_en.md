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
                    variant = ButtonVariant.FILLED,
                    modifier = Modifier.weight(1.0f)
                )
                Button(
                    text = "Reset",
                    onClick = { count = 0 },
                    variant = ButtonVariant.OUTLINED,
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
* `Modifier.align(alignment: VerticalAlign)`: Cross-axis vertical alignment (`VerticalAlign.Top`, `VerticalAlign.Center`, `VerticalAlign.Bottom`, `VerticalAlign.Fill`).

### B. `ColumnScope` (Vertical Containers)
* `Modifier.weight(weight: Float)`: Allocates free vertical space proportionally without distorting horizontal sizing.
* `Modifier.align(alignment: HorizontalAlign)`: Cross-axis horizontal alignment (`HorizontalAlign.Left`, `HorizontalAlign.Center`, `HorizontalAlign.Right`, `HorizontalAlign.Fill`).

### C. `BoxScope` (2D Coordinate & Overlays)
* `Modifier.align(alignment: Alignment)`: 2D spatial alignment (`Alignment.Center`, `Alignment.TopStart`, `Alignment.BottomEnd`, etc.).

---

## 3. General `UIModifier` Elements

### A. Box Model & Sizing
* `Modifier.pad(all = 16.0f)` / `Modifier.pad(horizontal = 16.0f, vertical = 8.0f)`: Inner padding.
* `Modifier.margin(all = 8.0f)`: Outer layout margin.
* `Modifier.size(width = 100.0f, height = 50.0f)`: Fixed explicit dimensions.
* `Modifier.minWidth(100.0f)` / `Modifier.minHeight(40.0f)`: Protective minimum sizing bounds.
* `Modifier.fillMaxWidth()` / `Modifier.fillMaxHeight()` / `Modifier.fillMaxSize()`: Container expansion.
* `Modifier.autoWidth()` / `Modifier.autoHeight()` / `Modifier.hugContent()`: Intrinsic auto-layout sizing.

### B. Visuals & Styling (1-Draw-Call UIBatch)
* `Modifier.background(color: Color)`: Solid or translucent background fill.
* `Modifier.radius(radius: Float)`: Rounded corner SDF radius in pixels.
* `Modifier.border(width: Float, color: Color)`: Outline border stroke.
* `Modifier.glass(enabled: Boolean = true)`: Dual-Kawase frosted glass background blur sampling.
* `Modifier.shadow(radius: Float, color: Color, offsetX: Float, offsetY: Float)`: Drop-shadow rendering.

### C. Interactivity & Gestures
* `Modifier.clickable(onClick = { ... })`: Left pointer click release handler.
* `Modifier.hoverable { isHovered -> ... }`: Hover enter/exit state listener.
* `Modifier.draggable(onDrag = { dx, dy -> ... })`: Continuous drag gesture tracking.
* `Modifier.consumePointer()`: Intercepts and consumes pointer events to prevent bubbling to background scrims.
* `Modifier.cursor(cursor: CursorIcon)`: Hover mouse cursor icon configuration.

---

## 4. Built-in Component Library

| Component | Scope / Description | Key Parameters |
| :--- | :--- | :--- |
| `Card()` | Glassmorphic SDF container surface | `modifier`, `color`, `radius`, `borderWidth`, `borderColor`, `isGlass`, `content: BoxScope` |
| `Button()` | Clickable interactive action button | `onClick`, `modifier`, `variant`, `enabled`, `contentPadding`, `content: BoxScope` |
| `IconButton()` | Compact action button for icon glyphs | `region`, `onClick`, `modifier`, `variant`, `tint`, `enabled` |
| `TextField()` | Controlled text input with IME support | `value`, `onValueChange`, `placeholder`, `enabled`, `isMultiline`, `font` |
| `Text()` | BMFont text label at natural scale | `text`, `modifier`, `color`, `font`, `align`, `wrap`, `ellipsis` |
| `Image()` | Texture region display | `region`, `modifier`, `tint`, `aspectRatio` |
| `ModalDialog()` | Full-screen modal overlay with backdrop scrim | `visible`, `onDismiss`, `modifier`, `scrimColor`, `content: BoxScope` |
| `TooltipBox()` | Delayed hover tooltip overlay container | `tooltip: BoxScope`, `modifier`, `delayMs`, `content: BoxScope` |
