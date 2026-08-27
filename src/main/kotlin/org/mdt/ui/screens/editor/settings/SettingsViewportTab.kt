package org.mdt.ui.screens.editor.settings

import androidx.compose.runtime.*
import org.mdt.core.ui.compose.*
import org.mdt.core.ui.graphics.Color
import org.mdt.core.ui.layout.Alignment
import org.mdt.core.ui.layout.Arrangement
import org.mdt.ui.components.layout.Box
import org.mdt.ui.components.layout.Column
import org.mdt.ui.components.layout.Row
import org.mdt.ui.components.surface.Divider
import org.mdt.ui.components.text.MonoText
import org.mdt.ui.components.text.Text
import org.mdt.ui.theme.Theme

/**
 * ## SettingsViewportTab
 *
 * Canvas viewport preferences, grid sizing, default scene artboard geometry, and magnetic snapping.
 *
 * See: docs/design-system/design_system_en.md
 */
@Composable
fun SettingsViewportTab() {
    var selectedGridSize by remember { mutableStateOf(25f) }
    var selectedDefaultPreset by remember { mutableStateOf("1280x720") }
    var enableMagneticSnapping by remember { mutableStateOf(true) }
    var snapDistance by remember { mutableStateOf(8f) }

    val colors = Theme.colors
    val shapes = Theme.shapes
    val spacing = Theme.spacing
    val typography = Theme.typography

    Column(
        arrangement = Arrangement.spacedBy(spacing.lg),
        alignment = Alignment.TopStart,
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(arrangement = Arrangement.spacedBy(4f)) {
            Text(text = "Grid & Canvas Viewport", font = typography.title, color = colors.textPrimary)
            Text(text = "Configure 2D infinite canvas tiles, default scene artboards, and magnetic alignment snapping.", color = colors.textSecondary)
        }

        Divider(modifier = Modifier.fillMaxWidth().height(1f))

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .radius(shapes.md)
                .background(colors.surfacePrimary)
                .border(1f, colors.borderHairline)
                .pad(spacing.lg)
        ) {
            Column(arrangement = Arrangement.spacedBy(spacing.md), modifier = Modifier.fillMaxWidth()) {
                // Grid Cell Size
                Row(
                    arrangement = Arrangement.spacedBy(spacing.md),
                    alignment = Alignment.CenterStart,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.weight(1.0f), arrangement = Arrangement.spacedBy(2f)) {
                        MonoText(text = "Checkerboard Grid Cell Size", color = colors.textPrimary)
                        Text(text = "Base pixel dimension for procedural canvas tiles before zoom scaling.", color = colors.textSecondary)
                    }
                    Row(arrangement = Arrangement.spacedBy(4f)) {
                        listOf(20f, 25f, 30f, 40f).forEach { size ->
                            val isSel = selectedGridSize == size
                            Box(
                                modifier = Modifier
                                    .radius(shapes.xs)
                                    .background(if (isSel) colors.blue else colors.surfaceSecondary)
                                    .border(1f, if (isSel) colors.blue else colors.borderHairline)
                                    .clickable { selectedGridSize = size }
                                    .pad(horizontal = spacing.md, vertical = 4f)
                            ) {
                                MonoText(text = "${size.toInt()}px", color = if (isSel) Color.White else colors.textPrimary)
                            }
                        }
                    }
                }

                Divider(modifier = Modifier.fillMaxWidth().height(1f))

                // Default Artboard Size
                Row(
                    arrangement = Arrangement.spacedBy(spacing.md),
                    alignment = Alignment.CenterStart,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.weight(1.0f), arrangement = Arrangement.spacedBy(2f)) {
                        MonoText(text = "Default Scene Artboard Preset", color = colors.textPrimary)
                        Text(text = "Initial artboard dimensions for newly created scenes.", color = colors.textSecondary)
                    }
                    Row(arrangement = Arrangement.spacedBy(4f)) {
                        listOf(
                            "1920x1080" to "1080p FHD",
                            "1280x720" to "720p HD",
                            "390x844" to "Mobile"
                        ).forEach { (preset, label) ->
                            val isSel = selectedDefaultPreset == preset
                            Box(
                                modifier = Modifier
                                    .radius(shapes.xs)
                                    .background(if (isSel) colors.blue else colors.surfaceSecondary)
                                    .border(1f, if (isSel) colors.blue else colors.borderHairline)
                                    .clickable { selectedDefaultPreset = preset }
                                    .pad(horizontal = spacing.md, vertical = 4f)
                            ) {
                                MonoText(text = label, color = if (isSel) Color.White else colors.textPrimary)
                            }
                        }
                    }
                }

                Divider(modifier = Modifier.fillMaxWidth().height(1f))

                // Magnetic Snapping
                Row(
                    arrangement = Arrangement.spacedBy(spacing.md),
                    alignment = Alignment.CenterStart,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.weight(1.0f), arrangement = Arrangement.spacedBy(2f)) {
                        MonoText(text = "Magnetic Alignment Snapping", color = colors.textPrimary)
                        Text(text = "Automatically snap node edges and centers to sibling alignment rails while dragging.", color = colors.textSecondary)
                    }
                    Row(arrangement = Arrangement.spacedBy(6f)) {
                        Box(
                            modifier = Modifier
                                .radius(shapes.xs)
                                .background(if (enableMagneticSnapping) colors.blue else colors.surfaceSecondary)
                                .clickable { enableMagneticSnapping = !enableMagneticSnapping }
                                .pad(horizontal = spacing.md, vertical = 4f)
                        ) {
                            MonoText(text = if (enableMagneticSnapping) "Snapping: ON" else "Snapping: OFF", color = if (enableMagneticSnapping) Color.White else colors.textSecondary)
                        }

                        if (enableMagneticSnapping) {
                            listOf(4f, 8f, 12f).forEach { d ->
                                val isSel = snapDistance == d
                                Box(
                                    modifier = Modifier
                                        .radius(shapes.xs)
                                        .background(if (isSel) colors.blue else colors.surfaceSecondary)
                                        .border(1f, if (isSel) colors.blue else colors.borderHairline)
                                        .clickable { snapDistance = d }
                                        .pad(horizontal = spacing.sm, vertical = 4f)
                                ) {
                                    MonoText(text = "${d.toInt()}px", color = if (isSel) Color.White else colors.textSecondary)
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
