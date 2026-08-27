package org.mdt.ui.screens.editor.settings

import androidx.compose.runtime.*
import org.mdt.core.ui.compose.*
import org.mdt.core.ui.graphics.Color
import org.mdt.core.ui.layout.Alignment
import org.mdt.core.ui.layout.Arrangement
import org.mdt.ui.components.display.*
import org.mdt.ui.components.input.SegmentedControl
import org.mdt.ui.components.input.Slider
import org.mdt.ui.components.input.textfield.TextField
import org.mdt.ui.components.layout.*
import org.mdt.ui.components.list.GroupedList
import org.mdt.ui.components.list.ListItem
import org.mdt.ui.components.scroll.ScrollView
import org.mdt.ui.components.surface.*
import org.mdt.ui.components.text.MonoText
import org.mdt.ui.components.text.Text
import org.mdt.ui.theme.StudioIcons
import org.mdt.ui.theme.Theme

/**
 * ## SettingsComponentsTab
 *
 * Interactive component verification gallery and showroom embedded directly into Studio Settings.
 * Allows developers to test and verify all built-in UI primitives, composite widgets, and advanced
 * layouts (including horizontal scrolling and multi-column grids) live.
 *
 * See: docs/design-system/design_system_en.md
 */
@Composable
fun SettingsComponentsTab() {
    var textFieldValue by remember { mutableStateOf("Editable text input") }
    var sliderValue by remember { mutableStateOf(45f) }
    var toggleState1 by remember { mutableStateOf(true) }
    var toggleState2 by remember { mutableStateOf(false) }
    var listToggle1 by remember { mutableStateOf(true) }
    var selectedSegment by remember { mutableStateOf(0) }

    val colors = Theme.colors
    val shapes = Theme.shapes
    val spacing = Theme.spacing
    val typography = Theme.typography

    Column(
        arrangement = Arrangement.spacedBy(spacing.xl),
        alignment = Alignment.TopStart,
        modifier = Modifier.fillMaxWidth()
    ) {
        // =============================================================
        // 1. Header Banner
        // =============================================================
        Column(arrangement = Arrangement.spacedBy(4f)) {
            Text(text = "Built-in UI Component Catalog", font = typography.title, color = colors.textPrimary)
            Text(text = "Live interactive verification showroom for all built-in UI primitives, composite widgets, and advanced layout behaviors.", color = colors.textSecondary)
        }

        Divider(modifier = Modifier.fillMaxWidth().height(1f))

        // =============================================================
        // 2. Section: Buttons, IconButtons & Toggles
        // =============================================================
        Column(arrangement = Arrangement.spacedBy(spacing.md), modifier = Modifier.fillMaxWidth()) {
            MonoText(text = "1. SURFACES, BUTTONS & TOGGLE CONTROLS", color = colors.blue)

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .radius(shapes.md)
                    .background(colors.surfacePrimary)
                    .border(1f, colors.borderHairline)
                    .pad(spacing.lg)
            ) {
                Column(arrangement = Arrangement.spacedBy(spacing.lg), modifier = Modifier.fillMaxWidth()) {
                    // Button Variants Row
                    Column(arrangement = Arrangement.spacedBy(spacing.xs)) {
                        MonoText(text = "Button Variants (Filled, Tinted, Destructive, Outlined, Glass, Plain)", color = colors.textSecondary)
                        Row(
                            arrangement = Arrangement.spacedBy(spacing.sm),
                            alignment = Alignment.CenterStart,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Button(text = "Filled Action", variant = ButtonVariant.FILLED, onClick = {})
                            Button(text = "Tinted Pill", variant = ButtonVariant.TINTED, onClick = {})
                            Button(text = "Destructive", variant = ButtonVariant.DESTRUCTIVE, onClick = {})
                            Button(text = "Outlined", variant = ButtonVariant.OUTLINED, onClick = {})
                            Button(text = "Glass Acrylic", variant = ButtonVariant.GLASS, onClick = {})
                            Button(text = "Plain Text", variant = ButtonVariant.PLAIN, onClick = {})
                        }
                    }

                    Divider(modifier = Modifier.fillMaxWidth().height(1f))

                    // IconButtons & Toggles Row
                    Row(
                        arrangement = Arrangement.spacedBy(spacing.xl),
                        alignment = Alignment.CenterStart,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        // IconButtons
                        Column(arrangement = Arrangement.spacedBy(spacing.xs)) {
                            MonoText(text = "Icon Buttons", color = colors.textSecondary)
                            Row(arrangement = Arrangement.spacedBy(spacing.xs)) {
                                IconButton(icon = StudioIcons.PLAY, onClick = {})
                                IconButton(icon = StudioIcons.REFRESH, onClick = {})
                                IconButton(icon = StudioIcons.SETTINGS, onClick = {})
                                IconButton(icon = StudioIcons.PLUS, onClick = {})
                            }
                        }

                        Spacer(modifier = Modifier.width(spacing.md))

                        // Toggle Switches
                        Column(arrangement = Arrangement.spacedBy(spacing.xs)) {
                            MonoText(text = "Interactive Toggles (Switch)", color = colors.textSecondary)
                            Row(
                                arrangement = Arrangement.spacedBy(spacing.md),
                                alignment = Alignment.CenterStart
                            ) {
                                Row(arrangement = Arrangement.spacedBy(spacing.xs), alignment = Alignment.CenterStart) {
                                    Toggle(checked = toggleState1, onToggle = { toggleState1 = it })
                                    MonoText(text = if (toggleState1) "Active" else "Disabled", color = colors.textPrimary)
                                }
                                Row(arrangement = Arrangement.spacedBy(spacing.xs), alignment = Alignment.CenterStart) {
                                    Toggle(checked = toggleState2, onToggle = { toggleState2 = it })
                                    MonoText(text = if (toggleState2) "Active" else "Disabled", color = colors.textPrimary)
                                }
                            }
                        }

                        Spacer(modifier = Modifier.width(spacing.md))

                        // Segmented Control
                        Column(arrangement = Arrangement.spacedBy(spacing.xs)) {
                            MonoText(text = "Segmented Control", color = colors.textSecondary)
                            SegmentedControl(
                                items = listOf("Design", "Prototype", "Inspect"),
                                selectedIndex = selectedSegment,
                                onSelect = { selectedSegment = it }
                            )
                        }
                    }
                }
            }
        }

        // =============================================================
        // 3. Section: Input Controls (TextField & Slider)
        // =============================================================
        Column(arrangement = Arrangement.spacedBy(spacing.md), modifier = Modifier.fillMaxWidth()) {
            MonoText(text = "2. INPUT CONTROLS (TEXTFIELD & SLIDER)", color = colors.blue)

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .radius(shapes.md)
                    .background(colors.surfacePrimary)
                    .border(1f, colors.borderHairline)
                    .pad(spacing.lg)
            ) {
                Column(arrangement = Arrangement.spacedBy(spacing.lg), modifier = Modifier.fillMaxWidth()) {
                    // TextFields Row
                    Row(
                        arrangement = Arrangement.spacedBy(spacing.lg),
                        alignment = Alignment.CenterStart,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.weight(1.0f), arrangement = Arrangement.spacedBy(spacing.xs)) {
                            MonoText(text = "Standard Text Field", color = colors.textSecondary)
                            TextField(
                                value = textFieldValue,
                                onValueChange = { textFieldValue = it },
                                placeholder = "Enter custom text...",
                                modifier = Modifier.fillMaxWidth()
                            )
                        }

                        Column(modifier = Modifier.weight(1.0f), arrangement = Arrangement.spacedBy(spacing.xs)) {
                            MonoText(text = "Disabled Text Field", color = colors.textSecondary)
                            TextField(
                                value = "Disabled text content",
                                onValueChange = {},
                                enabled = false,
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                    }

                    Divider(modifier = Modifier.fillMaxWidth().height(1f))

                    // Interactive Slider Row
                    Column(arrangement = Arrangement.spacedBy(spacing.xs), modifier = Modifier.fillMaxWidth()) {
                        Row(
                            arrangement = Arrangement.spacedBy(spacing.md),
                            alignment = Alignment.CenterStart,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            MonoText(text = "Continuous Range Slider", color = colors.textSecondary)
                            Spacer(modifier = Modifier.weight(1.0f))
                            Box(
                                modifier = Modifier
                                    .radius(shapes.xs)
                                    .background(colors.surfaceElevated)
                                    .border(1f, colors.borderHairline)
                                    .pad(horizontal = 8f, vertical = 2f)
                            ) {
                                MonoText(text = "Value: ${sliderValue.toInt()}%", color = colors.blue)
                            }
                        }

                        Slider(
                            value = sliderValue,
                            onValueChange = { sliderValue = it },
                            valueRange = 0f..100f,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }
            }
        }

        // =============================================================
        // 4. Section: Display Indicators (Badges & Progress Bars)
        // =============================================================
        Column(arrangement = Arrangement.spacedBy(spacing.md), modifier = Modifier.fillMaxWidth()) {
            MonoText(text = "3. STATUS INDICATORS (BADGES & PROGRESS BARS)", color = colors.blue)

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .radius(shapes.md)
                    .background(colors.surfacePrimary)
                    .border(1f, colors.borderHairline)
                    .pad(spacing.lg)
            ) {
                Column(arrangement = Arrangement.spacedBy(spacing.lg), modifier = Modifier.fillMaxWidth()) {
                    // Badges Row
                    Column(arrangement = Arrangement.spacedBy(spacing.xs)) {
                        MonoText(text = "Status Badges (Default, Primary, Success, Warning, Error)", color = colors.textSecondary)
                        Row(
                            arrangement = Arrangement.spacedBy(spacing.md),
                            alignment = Alignment.CenterStart
                        ) {
                            Badge(text = "Default", variant = BadgeVariant.DEFAULT)
                            Badge(text = "Primary Blue", variant = BadgeVariant.PRIMARY)
                            Badge(text = "Success 100%", variant = BadgeVariant.SUCCESS)
                            Badge(text = "Warning Alert", variant = BadgeVariant.WARNING)
                            Badge(text = "Error Critical", variant = BadgeVariant.ERROR)
                        }
                    }

                    Divider(modifier = Modifier.fillMaxWidth().height(1f))

                    // Progress Bars Row
                    Column(arrangement = Arrangement.spacedBy(spacing.xs)) {
                        MonoText(text = "Linear Progress Indicators", color = colors.textSecondary)
                        Column(arrangement = Arrangement.spacedBy(spacing.sm), modifier = Modifier.fillMaxWidth()) {
                            ProgressBar(progress = 0.25f, modifier = Modifier.fillMaxWidth())
                            ProgressBar(progress = 0.65f, modifier = Modifier.fillMaxWidth())
                            ProgressBar(progress = 0.90f, modifier = Modifier.fillMaxWidth())
                        }
                    }
                }
            }
        }

        // =============================================================
        // 5. Section: Cards, Glass Acrylic & Grouped Lists
        // =============================================================
        Column(arrangement = Arrangement.spacedBy(spacing.md), modifier = Modifier.fillMaxWidth()) {
            MonoText(text = "4. CARDS & GROUPED LISTS (APPLE HIG)", color = colors.blue)

            Row(
                arrangement = Arrangement.spacedBy(spacing.lg),
                alignment = Alignment.TopStart,
                modifier = Modifier.fillMaxWidth()
            ) {
                // Glass Card Example
                Card(
                    modifier = Modifier.weight(1.0f),
                    variant = CardVariant.GLASS
                ) {
                    Column(arrangement = Arrangement.spacedBy(spacing.sm)) {
                        Row(
                            arrangement = Arrangement.spacedBy(spacing.sm),
                            alignment = Alignment.CenterStart
                        ) {
                            Image(source = StudioIcons.STUDIO_LOGO, modifier = Modifier.size(20f), tint = colors.blue)
                            Text(text = "Frosted Glass Card", font = typography.title, color = colors.textPrimary)
                        }
                        Text(
                            text = "Apple frosted glass acrylic container utilizing real-time dual-pass Gaussian blur shaders.",
                            color = colors.textSecondary
                        )
                        Button(text = "Card Action", variant = ButtonVariant.FILLED, onClick = {})
                    }
                }

                // Grouped List Example
                Box(modifier = Modifier.weight(1.0f)) {
                    GroupedList(
                        header = "SETTINGS LIST ITEMS",
                        footer = "Standard grouped table view with reactive trailing toggle switch.",
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        ListItem(
                            title = "Hardware Acceleration",
                            subtitle = if (listToggle1) "OpenGL 2D SDF Pipelines (Enabled)" else "Software Fallback (Disabled)",
                            icon = StudioIcons.SETTINGS,
                            onClick = { listToggle1 = !listToggle1 },
                            trailing = {
                                Toggle(
                                    checked = listToggle1,
                                    onToggle = { listToggle1 = it }
                                )
                            }
                        )
                        ListItem(
                            title = "VRAM Texture Cache",
                            subtitle = "LRU 64 MB Retention",
                            icon = StudioIcons.IMAGE,
                            trailing = {
                                MonoText(text = "64 MB", color = colors.blue)
                            }
                        )
                        ListItem(
                            title = "Studio Shortcuts",
                            subtitle = "Global hardware keybindings",
                            icon = StudioIcons.CODE,
                            onClick = {}
                        )
                    }
                }
            }
        }

        // =============================================================
        // 6. Section: Advanced Layouts & Horizontal Scroll Test
        // =============================================================
        Column(arrangement = Arrangement.spacedBy(spacing.md), modifier = Modifier.fillMaxWidth()) {
            MonoText(text = "5. ADVANCED LAYOUTS & HORIZONTAL SCROLL STRIP", color = colors.blue)

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .radius(shapes.md)
                    .background(colors.surfacePrimary)
                    .border(1f, colors.borderHairline)
                    .pad(spacing.lg)
            ) {
                Column(arrangement = Arrangement.spacedBy(spacing.lg), modifier = Modifier.fillMaxWidth()) {

                    // Horizontal Scroll Carousel
                    Column(arrangement = Arrangement.spacedBy(spacing.xs), modifier = Modifier.fillMaxWidth()) {
                        Row(
                            arrangement = Arrangement.spacedBy(spacing.sm),
                            alignment = Alignment.CenterStart,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            MonoText(text = "Horizontal Carousel Strip (Scroll horizontally or Drag)", color = colors.textPrimary)
                            Spacer(modifier = Modifier.weight(1.0f))
                            Badge(text = "Scrollable X", variant = BadgeVariant.PRIMARY)
                        }

                        ScrollView(
                            modifier = Modifier
                                .fillMaxWidth()
                                .radius(shapes.sm)
                                .background(colors.surfaceSecondary)
                                .border(1f, colors.borderHairline)
                                .pad(spacing.sm),
                            enableHorizontal = true,
                            enableVertical = false
                        ) {
                            Row(
                                arrangement = Arrangement.spacedBy(10f),
                                alignment = Alignment.CenterStart
                            ) {
                                listOf(
                                    Triple("Duo Turret", "Tier 1", colors.blue),
                                    Triple("Laser Drill", "Tier 2", Color.valueOf("bf5af2")),
                                    Triple("Thorium Reactor", "Tier 3", colors.orange),
                                    Triple("Surge Crucible", "Tier 4", colors.green),
                                    Triple("Silicon Smelter", "Tier 2", colors.blue),
                                    Triple("Phase Weaver", "Tier 4", Color.valueOf("ff375f")),
                                    Triple("Overdrive Dome", "Tier 5", colors.indigo),
                                    Triple("Spectre Cannon", "Tier 5", colors.red)
                                ).forEach { (title, tier, tint) ->
                                    Box(
                                        modifier = Modifier
                                            .width(180f)
                                            .radius(shapes.sm)
                                            .background(colors.surfaceElevated)
                                            .border(1f, colors.borderHairline)
                                            .pad(spacing.sm)
                                    ) {
                                        Column(
                                            arrangement = Arrangement.spacedBy(6f),
                                            alignment = Alignment.TopStart,
                                            modifier = Modifier.fillMaxWidth()
                                        ) {
                                            Row(
                                                arrangement = Arrangement.spacedBy(6f),
                                                alignment = Alignment.CenterStart,
                                                modifier = Modifier.fillMaxWidth()
                                            ) {
                                                Box(
                                                    modifier = Modifier
                                                        .size(24f)
                                                        .radius(shapes.xs)
                                                        .background(tint.withAlpha(0.25f))
                                                        .pad(4f)
                                                ) {
                                                    Image(source = StudioIcons.CARD, modifier = Modifier.fillMaxSize(), tint = tint)
                                                }
                                                MonoText(text = tier, color = tint)
                                            }
                                            Text(text = title, font = typography.title, color = colors.textPrimary)
                                            Button(
                                                text = "Select",
                                                variant = ButtonVariant.PLAIN,
                                                modifier = Modifier.pad(horizontal = 6f, vertical = 2f),
                                                onClick = {}
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }

                    Divider(modifier = Modifier.fillMaxWidth().height(1f))

                    // Multi-Column Grid Test
                    Column(arrangement = Arrangement.spacedBy(spacing.xs), modifier = Modifier.fillMaxWidth()) {
                        MonoText(text = "Multi-Column Responsive Grid Layout", color = colors.textSecondary)

                        Grid(
                            columns = 3,
                            hGap = 10f,
                            vGap = 10f,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            listOf(
                                "Flex Weight Alpha" to "Proportional available space allocation",
                                "Flex Weight Beta" to "Balanced horizontal weight distribution",
                                "Flex Weight Gamma" to "Zero clipping adaptive child fitting"
                            ).forEach { (name, desc) ->
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .radius(shapes.sm)
                                        .background(colors.surfaceSecondary)
                                        .border(1f, colors.borderHairline)
                                        .pad(spacing.md)
                                ) {
                                    Column(arrangement = Arrangement.spacedBy(2f)) {
                                        MonoText(text = name, color = colors.blue)
                                        Text(text = desc, color = colors.textSecondary)
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
