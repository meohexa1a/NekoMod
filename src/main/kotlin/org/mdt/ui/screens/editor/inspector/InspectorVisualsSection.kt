package org.mdt.ui.screens.editor.inspector

import androidx.compose.runtime.Composable
import arc.graphics.Color
import org.mdt.core.ui.UINode
import org.mdt.core.ui.compose.*
import org.mdt.core.ui.layout.Alignment
import org.mdt.core.ui.layout.Arrangement
import org.mdt.ui.components.layout.BackgroundFill
import org.mdt.ui.components.layout.Box
import org.mdt.ui.components.layout.Column
import org.mdt.ui.components.layout.LayoutNode
import org.mdt.ui.components.layout.Row
import org.mdt.ui.components.text.MonoText
import org.mdt.ui.components.text.TextNode
import org.mdt.ui.theme.Theme

/**
 * ## InspectorVisualsSection
 *
 * Inspector controls for configuring visual styling and typography:
 * - Background Fill Palette
 * - Corner Radii
 * - Apple Frosted Glass Backdrop Blur
 * - Text Content string
 *
 * See: docs/design-system/design_system_en.md
 */
@Composable
fun InspectorVisualsSection(node: UINode) {
    val colors = Theme.colors
    val shapes = Theme.shapes
    val spacing = Theme.spacing

    if (node is LayoutNode) {
        val vis = node.ensureVisuals()

        // 1. Background Fill Palette
        Column(arrangement = Arrangement.spacedBy(spacing.xs), modifier = Modifier.fillMaxWidth()) {
            MonoText(text = "Background Fill", color = colors.textSecondary)
            Row(arrangement = Arrangement.spacedBy(4f), modifier = Modifier.fillMaxWidth()) {
                listOf("#0a84ff", "#30d158", "#bf5af2", "#ff9f0a", "#1c1d22", "#ffffff").forEach { hex ->
                    val col = Color.valueOf(hex)
                    val isSel = vis.background.color == col
                    Box(
                        modifier = Modifier
                            .size(24f, 24f)
                            .radius(shapes.xs)
                            .background(col)
                            .border(if (isSel) 2f else 1f, if (isSel) colors.blue else colors.borderHairline)
                            .clickable {
                                vis.background.mode = BackgroundFill.Mode.COLOR
                                vis.background.color.set(col)
                                node.invalidateLayout()
                            }
                    )
                }
            }
        }

        // 2. Corner Radii
        Column(arrangement = Arrangement.spacedBy(spacing.xs), modifier = Modifier.fillMaxWidth()) {
            MonoText(text = "Corner Radius: ${vis.radii.topLeft.toInt()}px", color = colors.textSecondary)
            Row(arrangement = Arrangement.spacedBy(4f)) {
                listOf(0f, 4f, 8f, 12f, 16f, 999f).forEach { r ->
                    Box(
                        modifier = Modifier
                            .radius(shapes.xs)
                            .background(if (vis.radii.topLeft == r) colors.surfaceHighlight else colors.surfaceSecondary)
                            .border(1f, colors.borderHairline)
                            .clickable {
                                vis.radii.set(r)
                                node.invalidateLayout()
                            }
                            .pad(horizontal = 6f, vertical = 2f)
                    ) {
                        MonoText(text = "${r.toInt()}", color = colors.textPrimary)
                    }
                }
            }
        }

        // 3. Apple Frosted Glass Blur
        Column(arrangement = Arrangement.spacedBy(spacing.xs), modifier = Modifier.fillMaxWidth()) {
            MonoText(text = "Frosted Glass Blur: ${if (vis.backdrop.enabled) "${vis.backdrop.blurRadius.toInt()}px" else "Off"}", color = colors.textSecondary)
            Row(arrangement = Arrangement.spacedBy(4f)) {
                listOf(0f, 12f, 20f, 24f, 32f).forEach { b ->
                    Box(
                        modifier = Modifier
                            .radius(shapes.xs)
                            .background(if (vis.backdrop.enabled && vis.backdrop.blurRadius == b) colors.surfaceHighlight else colors.surfaceSecondary)
                            .border(1f, colors.borderHairline)
                            .clickable {
                                vis.backdrop.enabled = b > 0f
                                vis.backdrop.blurRadius = b
                                if (b > 0f) {
                                    vis.background.mode = BackgroundFill.Mode.BACKDROP
                                    vis.background.color.set(Color(0.10f, 0.10f, 0.16f, 0.75f))
                                    vis.backdrop.tint.set(Color(0.10f, 0.10f, 0.16f, 0.75f))
                                }
                                node.invalidateLayout()
                            }
                            .pad(horizontal = 6f, vertical = 2f)
                    ) {
                        MonoText(text = if (b == 0f) "Off" else "${b.toInt()}px", color = colors.textPrimary)
                    }
                }
            }
        }
    }

    // 4. Text Content (For TextNode)
    if (node is TextNode) {
        Column(arrangement = Arrangement.spacedBy(spacing.xs), modifier = Modifier.fillMaxWidth()) {
            MonoText(text = "Text Content", color = colors.textSecondary)
            Row(
                arrangement = Arrangement.spacedBy(spacing.xs),
                alignment = Alignment.CenterStart,
                modifier = Modifier
                    .fillMaxWidth()
                    .radius(shapes.sm)
                    .background(colors.surfaceSecondary)
                    .border(1f, colors.borderHairline)
                    .pad(spacing.sm)
            ) {
                MonoText(text = "\"${node.text}\"", color = colors.green)
            }
        }
    }
}
