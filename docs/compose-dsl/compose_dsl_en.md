# Compose DSL & Modifiers Guide

This document provides a comprehensive guide on using the declarative Compose DSL, `UIModifier` chaining, and built-in widget toolkits in NekoMod.

---

## 1. Typical Screen Structure

```kotlin
EngineRuntime.setContent {
    var count by remember { mutableStateOf(0) }

    Card(
        modifier = Modifier.anchor(LayoutPreset.CENTER),
        backgroundColor = Color.valueOf("1e1e2e"),
        borderColor = Color.valueOf("89b4fa"),
        borderWidth = 1.8,
        radius = 12.0,
        padding = 20f
    ) {
        Column(gap = 12f) {
            Text("Dashboard Title", scale = 1.3, color = Color.white)
            Divider(modifier = Modifier.margin(vertical = 4f))
            
            Row(arrangement = Arrangement.spacedBy(8f)) {
                Button("Increment: $count", colors = ButtonColors.Primary, onClick = { count++ })
                Button("Reset", colors = ButtonColors.Danger, onClick = { count = 0 })
            }
        }
    }
}
```

---

## 2. `UIModifier` System

Modifiers allow chaining layout constraints, box model insets, graphical visuals, and interaction handlers concisely:

### A. Box Model & Sizing
* `Modifier.pad(all = 16f)`: Inward 4-sided padding.
* `Modifier.pad(horizontal = 16f, vertical = 8f)`: Inward padding along axes.
* `Modifier.margin(all = 8f)`: Outward 4-sided margin separation.
* `Modifier.margin(horizontal = 12f, vertical = 6f)`: Outward margin along axes.
* `Modifier.size(w = 100f, h = 50f)`: Explicit desired width & height.
* `Modifier.fixed(w = 100f, h = 100f)`: Rigid unyielding fixed dimensions.
* `Modifier.fillMaxWidth()` / `Modifier.fillMaxHeight()`: Expands to fill available slot dimensions.
* `Modifier.weight(1.5f)`: Consumes available container free space proportionally (inside `Row`/`Column`).

### B. Visuals & Styling
* `Modifier.background(Color.valueOf("1e1e2e"))`: Solid box background tint.
* `Modifier.radius(12.0)`: Rounded corner radius in pixels.
* `Modifier.border(width = 2.0, color = Color.white)`: Border stroke outline.
* `Modifier.shadow(color = Pal.shadow, spread = 4.0, blur = 8.0)`: Inner drop shadow.
* `Modifier.glow(color = Color.royal, spread = 6.0, blur = 12.0)`: Outer glow aura.
* `Modifier.backdrop(blur = true, weight = 0.8)`: 2-pass Gaussian blur behind the node.

### C. Interactivity
* `Modifier.onClick { ... }`: Single left click handler.
* `Modifier.onDoubleClick { ... }`: Double click handler.
* `Modifier.onHover { isHovered -> ... }`: Hover transition listener.

---

## 3. Built-in Components

| Component | Description |
| :--- | :--- |
| **`Box`** | Fundamental container block supporting background, border, and nesting. |
| **`Row`** | Horizontal linear container stacking items Left to Right. |
| **`Column`** | Vertical linear container stacking items Top to Bottom. |
| **`Grid`** | Multi-column and multi-row layout grid table. |
| **`Spacer`** | Fixed or flexible space consumer (`weight`). |
| **`Text`** | BMFont typography with bundle resolution (`$key`), font scaling, and auto-wrapping. |
| **`Button`** | Pre-styled button with automated hover/pressed color states and palettes (`Primary`, `Danger`, `Success`). |
| **`Toggle`** | Binary boolean switch. |
| **`Card`** | Pre-styled container card with border, background, and default padding. |
| **`Divider`** | Slim horizontal separator line. |
