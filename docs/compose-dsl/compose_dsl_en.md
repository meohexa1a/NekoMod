# Compose DSL Guide

This document provides a comprehensive guide to utilizing NekoMod's declarative Kotlin DSL, fluent `UIModifier` chaining, and built-in component library.

---

## 1. Standard Screen Layout

```kotlin
EngineRuntime.setContent {
    var count by remember { mutableStateOf(0) }
    var toggled by remember { mutableStateOf(true) }
    var sliderVal by remember { mutableStateOf(0.75f) }

    Card(
        modifier = Modifier.anchor(LayoutPreset.CENTER),
        backgroundColor = Color.valueOf("1e2030").a(0.85f),
        borderColor = Color.valueOf("363a4f").a(0.8f),
        radius = 12f,
        padding = 18f
    ) {
        Column(gap = 10f) {
            Text(text = "NekoMod Dashboard", scale = 1.0f, color = Color.white)
            Text(text = "Declarative Pure KMP Engine", scale = 1.0f, color = Color.valueOf("9399b2"))
            Divider(modifier = Modifier.margin(vertical = 2f))
            
            // Modern Capsule Pill Slider with Integrated Title & Value
            Slider(
                value = sliderVal,
                onValueChange = { sliderVal = it },
                label = "Power Level",
                modifier = Modifier.fillMaxWidth().height(30f)
            )

            Row(arrangement = Arrangement.spacedBy(10f)) {
                Button(
                    text = "Count: $count",
                    colors = ButtonColors.Primary,
                    onClick = { count++ }
                )
                Button(
                    text = "Reset",
                    colors = ButtonColors.Danger,
                    onClick = { count = 0 }
                )
                Toggle(
                    checked = toggled,
                    onToggle = { toggled = it }
                )
            }
        }
    }
}
```

---

## 2. The `UIModifier` System

The `Modifier` chain configures dimensions, insets, graphics styling, and interaction:

### A. Box Model & Sizing
* `Modifier.pad(all = 16f)`: Inset padding on all 4 sides.
* `Modifier.pad(horizontal = 16f, vertical = 8f)`: Axis-specific padding.
* `Modifier.margin(all = 8f)`: Outer margin on all 4 sides.
* `Modifier.size(w = 100f, h = 50f)`: Explicit width and height.
* `Modifier.width(120f)` / `Modifier.height(32f)`: Independent width or height.
* `Modifier.fillMaxWidth()` / `Modifier.fillMaxHeight()`: Expands to match parent bounds.
* `Modifier.weight(1.5f)`: Flexible flex-ratio growth within `Row` or `Column`.

### B. Visuals & Styling (SDF Shaders)
* `Modifier.background(Color.valueOf("1e2030"))`: Background fill color.
* `Modifier.cornerRadius(12f)`: Quad corner radii (`Float`).
* `Modifier.border(width = 1f, color = Color.white)`: Outline border stroke.
* `Modifier.shadow(color = Color.black.a(0.4f), blur = 16f, spread = 2f)`: Soft SDF outer drop shadow.
* `Modifier.glow(color = Color.valueOf("2563eb").a(0.4f), spread = 4f, blur = 8f)`: Outer radiant aura.

### C. Interactivity & Gestures
* `Modifier.clickable { ... }`: Left pointer click handler.
* `Modifier.hoverable { isHovered -> ... }`: Hover state listener.
* `Modifier.tooltip("Action description")`: Hover tooltip popup.

---

## 3. Built-in Component Library

| Component | Responsibility | Highlighted Parameters |
| :--- | :--- | :--- |
| `Card()` | Glassmorphic SDF container surface | `backgroundColor`, `borderColor`, `radius`, `padding` |
| `Slider()` | Modern Capsule Pill Slider with integrated labels | `value`, `label`, `valueText`, `valueRange`, `colors` |
| `Toggle()` | Modern pill switch with 3D inset thumb knob | `checked`, `onToggle`, `activeColor`, `inactiveColor` |
| `TextField()` | Interactive text input with Telex and focus glow | `value`, `onValueChange`, `placeholder` |
| `Button()` | Stateful interactive button with hover/press styles | `text`, `onClick`, `colors = ButtonColors.Primary` |
| `ProgressBar()` | Rounded progress bar with smooth fill | `progress`, `barHeight`, `colors` |
| `Text()` | Pixel-perfect BMFont label at $1.0\times$ natural scale | `text`, `color`, `font = Fonts.def`, `scale = 1.0f` |
| `Image()` | Async web/asset texture display with LRU cache | `source = "url / asset / region"`, `scaleType` |
