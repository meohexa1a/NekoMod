package org.mdt.ui.screens.editor

import androidx.compose.runtime.*
import arc.graphics.Color
import mindustry.gen.Icon
import org.mdt.core.ui.compose.*
import org.mdt.core.ui.layout.Alignment
import org.mdt.core.ui.layout.Arrangement
import org.mdt.ui.components.display.image.Image
import org.mdt.ui.components.layout.*
import org.mdt.ui.components.scroll.ScrollView
import org.mdt.ui.components.surface.Divider
import org.mdt.ui.components.text.MonoText
import org.mdt.ui.components.text.Text
import org.mdt.ui.theme.Theme

/**
 * ## NodeInspectorProperties
 *
 * Inspector data structure for presenting selected node properties dynamically.
 */
data class NodeInspectorProperties(
    val name: String,
    val type: String,
    val posX: Int,
    val posY: Int,
    val width: Int,
    val height: Int,
    val fillHex: String,
    val fillAlpha: String,
    val strokeHex: String,
    val strokeAlpha: String,
    val radius: Int
)

/**
 * ## EditorRightInspector
 *
 * Right panel displaying selected node properties, layout constraints, position coordinates,
 * typography, and appearance styling using Apple Design System tokens.
 *
 * See: docs/design-system/design_system_en.md
 */
@Composable
fun EditorRightInspector(
    selectedNode: String = "App Root Scene"
) {
    val colors = Theme.colors
    val shapes = Theme.shapes
    val spacing = Theme.spacing
    val typography = Theme.typography

    var selectedTab by remember { mutableStateOf("Design") }

    // Dynamic resolution based on the selected node
    val nodeProps = when (selectedNode) {
        "Hero Glass Card", "Card" -> NodeInspectorProperties(
            name = selectedNode,
            type = "Card",
            posX = 770,
            posY = 430,
            width = 380,
            height = 220,
            fillHex = "#1C1D22",
            fillAlpha = "100%",
            strokeHex = "#FFFFFF",
            strokeAlpha = "8%",
            radius = 16
        )
        "Button (Primary)", "Button" -> NodeInspectorProperties(
            name = selectedNode,
            type = "Button",
            posX = 790,
            posY = 450,
            width = 130,
            height = 36,
            fillHex = "#0A84FF",
            fillAlpha = "100%",
            strokeHex = "#FFFFFF",
            strokeAlpha = "0%",
            radius = 8
        )
        "Button (Options)" -> NodeInspectorProperties(
            name = selectedNode,
            type = "Button",
            posX = 930,
            posY = 450,
            width = 110,
            height = 36,
            fillHex = "#2C3E55",
            fillAlpha = "100%",
            strokeHex = "#FFFFFF",
            strokeAlpha = "0%",
            radius = 8
        )
        "Title Text", "Text" -> NodeInspectorProperties(
            name = selectedNode,
            type = "Text",
            posX = 840,
            posY = 600,
            width = 240,
            height = 18,
            fillHex = "#FFFFFF",
            fillAlpha = "100%",
            strokeHex = "#000000",
            strokeAlpha = "0%",
            radius = 0
        )
        "Header Container" -> NodeInspectorProperties(
            name = selectedNode,
            type = "Row",
            posX = 790,
            posY = 590,
            width = 340,
            height = 40,
            fillHex = "#000000",
            fillAlpha = "0%",
            strokeHex = "#000000",
            strokeAlpha = "0%",
            radius = 0
        )
        "Logo Brand Image", "Image" -> NodeInspectorProperties(
            name = selectedNode,
            type = "Image",
            posX = 790,
            posY = 590,
            width = 36,
            height = 36,
            fillHex = "#0A84FF",
            fillAlpha = "100%",
            strokeHex = "#FFFFFF",
            strokeAlpha = "0%",
            radius = 8
        )
        else -> NodeInspectorProperties(
            name = selectedNode,
            type = "Scene",
            posX = 0,
            posY = 0,
            width = 1920,
            height = 1080,
            fillHex = "#121214",
            fillAlpha = "100%",
            strokeHex = "#FFFFFF",
            strokeAlpha = "0%",
            radius = 0
        )
    }

    Box(
        modifier = Modifier
            .width(280f)
            .fillMaxHeight()
            .background(colors.surfacePrimary)
            .border(1f, colors.borderHairline)
    ) {
        ScrollView(
            modifier = Modifier.fillMaxSize(),
            enableVertical = true
        ) {
            Column(
                arrangement = Arrangement.spacedBy(spacing.md),
                alignment = Alignment.CenterStart,
                modifier = Modifier.fillMaxWidth().pad(spacing.md)
            ) {
                // -------------------------------------------------------------
                // 1. Inspector Tabs (Design | Prototype)
                // -------------------------------------------------------------
                Row(
                    arrangement = Arrangement.spacedBy(spacing.lg),
                    alignment = Alignment.CenterStart,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    for (tab in listOf("Design", "Prototype")) {
                        val isSelected = tab == selectedTab
                        Column(
                            arrangement = Arrangement.spacedBy(spacing.xs),
                            alignment = Alignment.CenterStart,
                            modifier = Modifier.clickable { selectedTab = tab }
                        ) {
                            Text(
                                text = tab,
                                color = if (isSelected) colors.textPrimary else colors.textTertiary,
                                font = typography.title
                            )
                            if (isSelected) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(2f)
                                        .radius(shapes.pill)
                                        .background(colors.blue)
                                )
                            }
                        }
                    }
                }

                Divider(modifier = Modifier.fillMaxWidth().height(1f))

                // -------------------------------------------------------------
                // 2. Active Selection Header
                // -------------------------------------------------------------
                Row(
                    arrangement = Arrangement.spacedBy(spacing.sm),
                    alignment = Alignment.CenterStart,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Image(
                        region = Icon.tree.region,
                        modifier = Modifier.size(16f),
                        tint = colors.blue
                    )
                    Text(text = nodeProps.name, color = colors.textPrimary, font = typography.body)
                    Spacer(modifier = Modifier.weight(1.0f))
                    MonoText(text = "<${nodeProps.type}>", color = colors.textTertiary)
                }

                Divider(modifier = Modifier.fillMaxWidth().height(1f))

                // -------------------------------------------------------------
                // 3. Transform Coordinates & Dimensions (2x2 Grid)
                // -------------------------------------------------------------
                Text(text = "Position & Size", color = colors.textSecondary, font = typography.title)

                Row(
                    arrangement = Arrangement.spacedBy(spacing.sm),
                    alignment = Alignment.CenterStart,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    // X
                    Row(
                        arrangement = Arrangement.spacedBy(spacing.xs),
                        alignment = Alignment.CenterStart,
                        modifier = Modifier
                            .weight(1.0f)
                            .radius(shapes.sm)
                            .background(colors.surfaceSecondary)
                            .border(1f, colors.borderHairline)
                            .pad(horizontal = spacing.sm, vertical = spacing.xs + 2f)
                    ) {
                        MonoText(text = "X", color = colors.textTertiary)
                        MonoText(text = "${nodeProps.posX}", color = colors.textPrimary)
                    }

                    // Y
                    Row(
                        arrangement = Arrangement.spacedBy(spacing.xs),
                        alignment = Alignment.CenterStart,
                        modifier = Modifier
                            .weight(1.0f)
                            .radius(shapes.sm)
                            .background(colors.surfaceSecondary)
                            .border(1f, colors.borderHairline)
                            .pad(horizontal = spacing.sm, vertical = spacing.xs + 2f)
                    ) {
                        MonoText(text = "Y", color = colors.textTertiary)
                        MonoText(text = "${nodeProps.posY}", color = colors.textPrimary)
                    }
                }

                Row(
                    arrangement = Arrangement.spacedBy(spacing.sm),
                    alignment = Alignment.CenterStart,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    // W
                    Row(
                        arrangement = Arrangement.spacedBy(spacing.xs),
                        alignment = Alignment.CenterStart,
                        modifier = Modifier
                            .weight(1.0f)
                            .radius(shapes.sm)
                            .background(colors.surfaceSecondary)
                            .border(1f, colors.borderHairline)
                            .pad(horizontal = spacing.sm, vertical = spacing.xs + 2f)
                    ) {
                        MonoText(text = "W", color = colors.textTertiary)
                        MonoText(text = "${nodeProps.width}", color = colors.textPrimary)
                    }

                    // H
                    Row(
                        arrangement = Arrangement.spacedBy(spacing.xs),
                        alignment = Alignment.CenterStart,
                        modifier = Modifier
                            .weight(1.0f)
                            .radius(shapes.sm)
                            .background(colors.surfaceSecondary)
                            .border(1f, colors.borderHairline)
                            .pad(horizontal = spacing.sm, vertical = spacing.xs + 2f)
                    ) {
                        MonoText(text = "H", color = colors.textTertiary)
                        MonoText(text = "${nodeProps.height}", color = colors.textPrimary)
                    }
                }

                Divider(modifier = Modifier.fillMaxWidth().height(1f))

                // -------------------------------------------------------------
                // 4. Appearance & Glass Properties
                // -------------------------------------------------------------
                Text(text = "Appearance", color = colors.textSecondary, font = typography.title)

                // Fill Color Chip
                Row(
                    arrangement = Arrangement.spacedBy(spacing.sm),
                    alignment = Alignment.CenterStart,
                    modifier = Modifier
                        .fillMaxWidth()
                        .radius(shapes.sm)
                        .background(colors.surfaceSecondary)
                        .border(1f, colors.borderHairline)
                        .pad(spacing.sm)
                ) {
                    Box(
                        modifier = Modifier
                            .size(18f, 18f)
                            .radius(shapes.xs)
                            .background(Color.valueOf(nodeProps.fillHex.removePrefix("#")))
                            .border(1f, colors.borderHairline)
                    )
                    MonoText(text = nodeProps.fillHex, color = colors.textPrimary)
                    Spacer(modifier = Modifier.weight(1.0f))
                    MonoText(text = nodeProps.fillAlpha, color = colors.textTertiary)
                }

                // Stroke Border Chip
                Row(
                    arrangement = Arrangement.spacedBy(spacing.sm),
                    alignment = Alignment.CenterStart,
                    modifier = Modifier
                        .fillMaxWidth()
                        .radius(shapes.sm)
                        .background(colors.surfaceSecondary)
                        .border(1f, colors.borderHairline)
                        .pad(spacing.sm)
                ) {
                    Box(
                        modifier = Modifier
                            .size(18f, 18f)
                            .radius(shapes.xs)
                            .background(Color.valueOf(nodeProps.strokeHex.removePrefix("#")))
                            .border(1f, colors.borderHairline)
                    )
                    MonoText(text = nodeProps.strokeHex, color = colors.textPrimary)
                    Spacer(modifier = Modifier.weight(1.0f))
                    MonoText(text = nodeProps.strokeAlpha, color = colors.textTertiary)
                }
            }
        }
    }
}
