package org.mdt.ui.screens.editor.inspector

import androidx.compose.runtime.Composable
import org.mdt.core.ui.UINode
import org.mdt.core.ui.compose.*
import org.mdt.core.ui.graphics.Color
import org.mdt.core.ui.layout.Alignment
import org.mdt.core.ui.layout.Arrangement
import org.mdt.ui.components.layout.BackgroundFill
import org.mdt.ui.components.layout.Box
import org.mdt.ui.components.layout.Column
import org.mdt.ui.components.layout.LayoutNode
import org.mdt.ui.components.layout.Row
import org.mdt.ui.components.text.MonoText
import org.mdt.ui.components.text.TextNode
import org.mdt.ui.screens.editor.components.EditorOptionGroup
import org.mdt.ui.screens.editor.components.EditorPropertyRow
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
        val visuals = node.ensureVisuals()

        // 1. Background Fill Palette
        EditorPropertyRow(label = "Background Fill") {
            Row(arrangement = Arrangement.spacedBy(4f), modifier = Modifier.fillMaxWidth()) {
                listOf("#0a84ff", "#30d158", "#bf5af2", "#ff9f0a", "#1c1d22", "#ffffff").forEach { hex ->
                    val fillColor = Color.valueOf(hex)
                    val isSelected = visuals.background.color == fillColor
                    Box(
                        modifier = Modifier
                            .size(24f, 24f)
                            .radius(shapes.xs)
                            .background(fillColor)
                            .border(if (isSelected) 2f else 1f, if (isSelected) colors.blue else colors.borderHairline)
                            .clickable {
                                visuals.background.mode = BackgroundFill.Mode.COLOR
                                visuals.background.color = fillColor
                                node.invalidateLayout()
                            }
                    )
                }
            }
        }

        // 2. Corner Radii
        EditorPropertyRow(
            label = "Corner Radius",
            subtitle = "${visuals.radii.topLeft.toInt()}px"
        ) {
            EditorOptionGroup(
                options = listOf(0f, 4f, 8f, 12f, 16f, 999f),
                selected = visuals.radii.topLeft,
                onSelect = { radiusValue ->
                    visuals.radii.set(radiusValue)
                    node.invalidateLayout()
                },
                labelSelector = { if (it >= 999f) "Pill" else "${it.toInt()}" }
            )
        }

        // 3. Apple Frosted Glass Blur
        EditorPropertyRow(
            label = "Frosted Glass Blur",
            subtitle = if (visuals.backdrop.enabled) "${visuals.backdrop.blurRadius.toInt()}px" else "Off"
        ) {
            EditorOptionGroup(
                options = listOf(0f, 12f, 20f, 24f, 32f),
                selected = if (visuals.backdrop.enabled) visuals.backdrop.blurRadius else 0f,
                onSelect = { blurRadius ->
                    visuals.backdrop.enabled = blurRadius > 0f
                    visuals.backdrop.blurRadius = blurRadius
                    if (blurRadius > 0f) {
                        visuals.background.mode = BackgroundFill.Mode.BACKDROP
                        visuals.background.color = Color(0.10f, 0.10f, 0.16f, 0.75f)
                        visuals.backdrop.tint = Color(0.10f, 0.10f, 0.16f, 0.75f)
                    }
                    node.invalidateLayout()
                },
                labelSelector = { if (it == 0f) "Off" else "${it.toInt()}px" }
            )
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
