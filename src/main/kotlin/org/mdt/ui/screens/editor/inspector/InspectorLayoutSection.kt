package org.mdt.ui.screens.editor.inspector

import androidx.compose.runtime.Composable
import org.mdt.core.ui.UINode
import org.mdt.core.ui.compose.*
import org.mdt.core.ui.graphics.Color
import org.mdt.core.ui.layout.Arrangement
import org.mdt.core.ui.layout.LayoutPreset
import org.mdt.ui.components.layout.Box
import org.mdt.ui.components.layout.Column
import org.mdt.ui.components.layout.LayoutNode
import org.mdt.ui.components.layout.Row
import org.mdt.ui.components.text.MonoText
import org.mdt.ui.screens.editor.components.EditorOptionGroup
import org.mdt.ui.screens.editor.components.EditorPropertyRow
import org.mdt.ui.theme.Theme

/**
 * ## InspectorLayoutSection
 *
 * Inspector controls for configuring 2D Box Model layout properties:
 * - 9-Point Anchor Preset Matrix
 * - Position (X & Y in px)
 * - Dimensions (Width & Height in px)
 * - Inward Padding
 *
 * See: docs/design-system/design_system_en.md
 */
@Composable
fun InspectorLayoutSection(node: UINode) {
    val colors = Theme.colors
    val shapes = Theme.shapes
    val spacing = Theme.spacing

    // 1. 9-Point Anchor Preset Matrix
    Column(arrangement = Arrangement.spacedBy(spacing.xs), modifier = Modifier.fillMaxWidth()) {
        MonoText(text = "Anchor Preset", color = colors.textSecondary)
        val anchorRows: List<List<Pair<LayoutPreset, String>>> = listOf(
            listOf(LayoutPreset.TOP_LEFT to "TL", LayoutPreset.CENTER_TOP to "TC", LayoutPreset.TOP_RIGHT to "TR"),
            listOf(LayoutPreset.CENTER_LEFT to "CL", LayoutPreset.CENTER to "C", LayoutPreset.CENTER_RIGHT to "CR"),
            listOf(LayoutPreset.BOTTOM_LEFT to "BL", LayoutPreset.CENTER_BOTTOM to "BC", LayoutPreset.BOTTOM_RIGHT to "BR")
        )

        Column(arrangement = Arrangement.spacedBy(2f), modifier = Modifier.fillMaxWidth()) {
            for (row in anchorRows) {
                Row(arrangement = Arrangement.spacedBy(2f), modifier = Modifier.fillMaxWidth()) {
                    for ((preset, label) in row) {
                        val isSel = node.anchorData.activePreset == preset
                        Box(
                            modifier = Modifier
                                .weight(1.0f)
                                .radius(shapes.xs)
                                .background(if (isSel) colors.blue else colors.surfaceSecondary)
                                .clickable {
                                    node.anchorData.setPreset(preset)
                                    node.invalidateLayout()
                                }
                                .pad(vertical = 4f)
                        ) {
                            MonoText(
                                text = label,
                                color = if (isSel) Color.White else colors.textSecondary
                            )
                        }
                    }
                }
            }
        }
    }

    // 2. Position X & Y
    // 2. Position Offset X & Y
    EditorPropertyRow(label = "Position (px)") {
        Row(arrangement = Arrangement.spacedBy(spacing.sm), modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.weight(1.0f), arrangement = Arrangement.spacedBy(2f)) {
                MonoText(text = "X: ${node.anchorData.offsetLeft.toInt()}", color = colors.textPrimary)
                EditorOptionGroup(
                    options = listOf(0f, 50f, 150f, 300f),
                    selected = node.anchorData.offsetLeft,
                    onSelect = {
                        node.anchorData.offsetLeft = it
                        node.invalidateLayout()
                    },
                    labelSelector = { "${it.toInt()}" }
                )
            }
            Column(modifier = Modifier.weight(1.0f), arrangement = Arrangement.spacedBy(2f)) {
                MonoText(text = "Y: ${node.anchorData.offsetTop.toInt()}", color = colors.textPrimary)
                EditorOptionGroup(
                    options = listOf(0f, 50f, 150f, 300f),
                    selected = node.anchorData.offsetTop,
                    onSelect = {
                        node.anchorData.offsetTop = it
                        node.invalidateLayout()
                    },
                    labelSelector = { "${it.toInt()}" }
                )
            }
        }
    }

    // 3. Dimensions Width & Height
    EditorPropertyRow(label = "Dimensions (px)") {
        Row(arrangement = Arrangement.spacedBy(spacing.sm), modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.weight(1.0f), arrangement = Arrangement.spacedBy(2f)) {
                MonoText(text = "W: ${if (node.width < 0f) "Auto" else "${node.width.toInt()}"}", color = colors.textPrimary)
                EditorOptionGroup(
                    options = listOf(100f, 200f, 400f, -1f),
                    selected = node.width,
                    onSelect = {
                        node.width = it
                        node.invalidateLayout()
                    },
                    labelSelector = { if (it < 0f) "Auto" else "${it.toInt()}" }
                )
            }

            Column(modifier = Modifier.weight(1.0f), arrangement = Arrangement.spacedBy(2f)) {
                MonoText(text = "H: ${if (node.height < 0f) "Auto" else "${node.height.toInt()}"}", color = colors.textPrimary)
                EditorOptionGroup(
                    options = listOf(40f, 80f, 140f, -1f),
                    selected = node.height,
                    onSelect = {
                        node.height = it
                        node.invalidateLayout()
                    },
                    labelSelector = { if (it < 0f) "Auto" else "${it.toInt()}" }
                )
            }
        }
    }

    // 4. Inward Padding (For LayoutNode)
    if (node is LayoutNode) {
        EditorPropertyRow(
            label = "Padding",
            subtitle = "${node.padL.toInt()}px"
        ) {
            EditorOptionGroup(
                options = listOf(0f, 8f, 16f, 22f, 32f),
                selected = node.padL,
                onSelect = { node.pad(it) },
                labelSelector = { "${it.toInt()}px" }
            )
        }
    }
}
