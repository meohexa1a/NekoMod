package org.mdt.ui.screens.editor.inspector

import androidx.compose.runtime.Composable
import arc.graphics.Color
import org.mdt.core.ui.UINode
import org.mdt.core.ui.compose.*
import org.mdt.core.ui.layout.Arrangement
import org.mdt.core.ui.layout.LayoutPreset
import org.mdt.ui.components.layout.Box
import org.mdt.ui.components.layout.Column
import org.mdt.ui.components.layout.LayoutNode
import org.mdt.ui.components.layout.Row
import org.mdt.ui.components.text.MonoText
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
                                color = if (isSel) Color.white else colors.textSecondary
                            )
                        }
                    }
                }
            }
        }
    }

    // 2. Position X & Y
    Column(arrangement = Arrangement.spacedBy(spacing.xs), modifier = Modifier.fillMaxWidth()) {
        MonoText(text = "Position (px)", color = colors.textSecondary)
        Row(arrangement = Arrangement.spacedBy(spacing.sm), modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.weight(1.0f), arrangement = Arrangement.spacedBy(2f)) {
                MonoText(text = "X: ${node.anchorData.offsetLeft.toInt()}", color = colors.textPrimary)
                Row(arrangement = Arrangement.spacedBy(2f)) {
                    listOf(0f, 50f, 150f, 300f).forEach { x ->
                        Box(
                            modifier = Modifier
                                .radius(shapes.xs)
                                .background(if (node.anchorData.offsetLeft == x) colors.surfaceHighlight else colors.surfaceSecondary)
                                .clickable {
                                    node.anchorData.offsetLeft = x
                                    node.invalidateLayout()
                                }
                                .pad(horizontal = 4f, vertical = 2f)
                        ) {
                            MonoText(text = "${x.toInt()}", color = colors.textSecondary)
                        }
                    }
                }
            }
            Column(modifier = Modifier.weight(1.0f), arrangement = Arrangement.spacedBy(2f)) {
                MonoText(text = "Y: ${node.anchorData.offsetTop.toInt()}", color = colors.textPrimary)
                Row(arrangement = Arrangement.spacedBy(2f)) {
                    listOf(0f, 50f, 150f, 300f).forEach { y ->
                        Box(
                            modifier = Modifier
                                .radius(shapes.xs)
                                .background(if (node.anchorData.offsetTop == y) colors.surfaceHighlight else colors.surfaceSecondary)
                                .clickable {
                                    node.anchorData.offsetTop = y
                                    node.invalidateLayout()
                                }
                                .pad(horizontal = 4f, vertical = 2f)
                        ) {
                            MonoText(text = "${y.toInt()}", color = colors.textSecondary)
                        }
                    }
                }
            }
        }
    }

    // 3. Dimensions Width & Height
    Column(arrangement = Arrangement.spacedBy(spacing.xs), modifier = Modifier.fillMaxWidth()) {
        MonoText(text = "Dimensions (px)", color = colors.textSecondary)
        Row(arrangement = Arrangement.spacedBy(spacing.sm), modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.weight(1.0f), arrangement = Arrangement.spacedBy(2f)) {
                MonoText(text = "W: ${if (node.width < 0f) "Auto" else "${node.width.toInt()}"}", color = colors.textPrimary)
                Row(arrangement = Arrangement.spacedBy(2f)) {
                    listOf(100f, 200f, 400f, -1f).forEach { w ->
                        Box(
                            modifier = Modifier
                                .radius(shapes.xs)
                                .background(if (node.width == w) colors.surfaceHighlight else colors.surfaceSecondary)
                                .clickable {
                                    node.width = w
                                    node.invalidateLayout()
                                }
                                .pad(horizontal = 4f, vertical = 2f)
                        ) {
                            MonoText(text = if (w < 0f) "Auto" else "${w.toInt()}", color = colors.textSecondary)
                        }
                    }
                }
            }

            Column(modifier = Modifier.weight(1.0f), arrangement = Arrangement.spacedBy(2f)) {
                MonoText(text = "H: ${if (node.height < 0f) "Auto" else "${node.height.toInt()}"}", color = colors.textPrimary)
                Row(arrangement = Arrangement.spacedBy(2f)) {
                    listOf(40f, 80f, 140f, -1f).forEach { h ->
                        Box(
                            modifier = Modifier
                                .radius(shapes.xs)
                                .background(if (node.height == h) colors.surfaceHighlight else colors.surfaceSecondary)
                                .clickable {
                                    node.height = h
                                    node.invalidateLayout()
                                }
                                .pad(horizontal = 4f, vertical = 2f)
                        ) {
                            MonoText(text = if (h < 0f) "Auto" else "${h.toInt()}", color = colors.textSecondary)
                        }
                    }
                }
            }
        }
    }

    // 4. Inward Padding (For LayoutNode)
    if (node is LayoutNode) {
        Column(arrangement = Arrangement.spacedBy(spacing.xs), modifier = Modifier.fillMaxWidth()) {
            MonoText(text = "Padding: ${node.padL.toInt()}px", color = colors.textSecondary)
            Row(arrangement = Arrangement.spacedBy(4f)) {
                listOf(0f, 8f, 16f, 22f, 32f).forEach { p ->
                    Box(
                        modifier = Modifier
                            .radius(shapes.xs)
                            .background(if (node.padL == p) colors.surfaceHighlight else colors.surfaceSecondary)
                            .border(1f, colors.borderHairline)
                            .clickable {
                                node.pad(p)
                            }
                            .pad(horizontal = 6f, vertical = 2f)
                    ) {
                        MonoText(text = "${p.toInt()}px", color = colors.textPrimary)
                    }
                }
            }
        }
    }
}
