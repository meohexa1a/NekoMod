package org.mdt.ui.screens.editor.settings

import androidx.compose.runtime.*
import org.mdt.core.ui.compose.*
import org.mdt.core.ui.layout.Alignment
import org.mdt.core.ui.layout.Arrangement
import org.mdt.ui.components.layout.Box
import org.mdt.ui.components.layout.Column
import org.mdt.ui.components.layout.Row
import org.mdt.ui.components.surface.Divider
import org.mdt.ui.components.text.MonoText
import org.mdt.ui.components.text.Text
import org.mdt.ui.screens.editor.components.EditorOptionGroup
import org.mdt.ui.screens.editor.components.EditorPropertyRow
import org.mdt.ui.screens.editor.components.EditorToggleRow
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
                // 1. Grid Cell Size
                EditorPropertyRow(
                    label = "Checkerboard Grid Cell Size",
                    subtitle = "Procedural canvas tile base size"
                ) {
                    EditorOptionGroup(
                        options = listOf(20f, 25f, 30f, 40f),
                        selected = selectedGridSize,
                        onSelect = { selectedGridSize = it },
                        labelSelector = { "${it.toInt()}px" }
                    )
                }

                Divider(modifier = Modifier.fillMaxWidth().height(1f))

                // 2. Default Artboard Size
                EditorPropertyRow(
                    label = "Default Scene Artboard Preset",
                    subtitle = "Initial artboard dimensions for newly created scenes"
                ) {
                    EditorOptionGroup(
                        options = listOf("1920x1080", "1280x720", "390x844"),
                        selected = selectedDefaultPreset,
                        onSelect = { selectedDefaultPreset = it },
                        labelSelector = {
                            when (it) {
                                "1920x1080" -> "1080p FHD"
                                "1280x720" -> "720p HD"
                                else -> "Mobile (390x844)"
                            }
                        }
                    )
                }

                Divider(modifier = Modifier.fillMaxWidth().height(1f))

                // 3. Magnetic Snapping
                EditorToggleRow(
                    title = "Magnetic Alignment Snapping",
                    description = "Automatically snap node edges and centers to sibling alignment rails while dragging.",
                    checked = enableMagneticSnapping,
                    onToggle = { enableMagneticSnapping = it }
                )

                if (enableMagneticSnapping) {
                    EditorPropertyRow(
                        label = "Snap Distance Threshold",
                        subtitle = "${snapDistance.toInt()}px threshold"
                    ) {
                        EditorOptionGroup(
                            options = listOf(4f, 8f, 12f),
                            selected = snapDistance,
                            onSelect = { snapDistance = it },
                            labelSelector = { "${it.toInt()}px" }
                        )
                    }
                }
            }
        }
    }
}

