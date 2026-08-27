package org.mdt.ui.screens.editor

import androidx.compose.runtime.*
import arc.graphics.Color
import arc.graphics.g2d.Draw
import arc.graphics.g2d.Fill
import org.mdt.core.ui.compose.*
import org.mdt.core.ui.layout.Alignment
import org.mdt.core.ui.layout.Arrangement
import org.mdt.core.ui.layout.ColumnMeasurePolicy
import org.mdt.core.ui.layout.LayoutPreset
import org.mdt.core.ui.layout.RowMeasurePolicy
import org.mdt.ui.components.display.canvas.Canvas
import org.mdt.ui.components.display.image.Image
import org.mdt.ui.components.layout.BackgroundFill
import org.mdt.ui.components.layout.Box
import org.mdt.ui.components.layout.LayoutNode
import org.mdt.ui.components.layout.Row
import org.mdt.ui.components.text.MonoText
import org.mdt.ui.components.text.TextNode
import org.mdt.ui.theme.StudioIcons
import org.mdt.ui.theme.Theme

/**
 * ## ComponentState
 *
 * Interactive state modes for live previewing isolated components.
 */
enum class ComponentState(val label: String, val iconUrl: String) {
    NORMAL("Normal", StudioIcons.SELECT),
    HOVERED("Hovered", StudioIcons.SELECT),
    PRESSED("Pressed", StudioIcons.BUTTON),
    DISABLED("Disabled", StudioIcons.SETTINGS),
    LOADING("Loading", StudioIcons.REFRESH)
}

/**
 * ## ComponentCanvas
 *
 * Dedicated isolated workspace for previewing and testing parametric Virtual Node components.
 * Operates 100% directly on live in-memory [LayoutNode] trees with zero XML parsing.
 *
 * See: docs/design-system/design_system_en.md
 */
@Composable
fun ComponentCanvas(
    componentName: String = "PrimaryButton",
    modifier: UIModifier = UIModifier
) {
    var activeState by remember { mutableStateOf(ComponentState.NORMAL) }
    var activeSize by remember { mutableStateOf("MD") }

    val colors = Theme.colors
    val shapes = Theme.shapes
    val spacing = Theme.spacing

    // Dynamic In-Memory Virtual Node Component
    val componentNode = remember(componentName, activeState, activeSize) {
        val root = LayoutNode().apply {
            anchorData.setPreset(LayoutPreset.CENTER)
        }

        when (componentName) {
            "GlassCard" -> {
                val blurVal = if (activeState == ComponentState.HOVERED) 32f else 24f
                val card = LayoutNode().apply {
                    width = 360f
                    pad(20f)
                    val vis = ensureVisuals()
                    vis.background.mode = BackgroundFill.Mode.BACKDROP
                    vis.backdrop.enabled = true
                    vis.backdrop.blurRadius = blurVal
                    vis.backdrop.tint.set(Color(0.10f, 0.10f, 0.16f, 0.75f))
                    vis.background.color.set(Color(0.10f, 0.10f, 0.16f, 0.75f))
                    vis.radii.set(16f)
                    vis.border.width = 1f
                    vis.border.color.set(Color(1f, 1f, 1f, 0.14f))
                    measurePolicy = ColumnMeasurePolicy(gap = 12f)
                }

                val row = LayoutNode().apply {
                    measurePolicy = RowMeasurePolicy(gap = 10f, alignment = Alignment.CenterStart)
                    addChild(LayoutNode().apply {
                        width = 32f
                        height = 32f
                        val v = ensureVisuals()
                        v.background.mode = BackgroundFill.Mode.COLOR
                        v.background.color.set(Color.valueOf("0a84ff"))
                        v.radii.set(8f)
                    })
                    addChild(TextNode(text = "Glass Card Component • ${activeState.label}").apply {
                        textVisuals.color = Color.white
                    })
                }

                val desc = TextNode(text = "Parametric isolated component container with dynamic Apple frosted glass shader.").apply {
                    textVisuals.color = Color(0.9f, 0.9f, 0.95f, 0.7f)
                    textVisuals.wrap = true
                }

                card.addChild(row)
                card.addChild(desc)
                root.addChild(card)
            }
            "StatusBadge" -> {
                val bg = when (activeState) {
                    ComponentState.DISABLED -> Color.valueOf("333338")
                    ComponentState.HOVERED -> Color.valueOf("30d158")
                    else -> Color.valueOf("34c759")
                }
                val badge = LayoutNode().apply {
                    pad(horizontal = 14f, vertical = 6f)
                    val vis = ensureVisuals()
                    vis.background.mode = BackgroundFill.Mode.COLOR
                    vis.background.color.set(bg)
                    vis.radii.set(999f)
                    measurePolicy = RowMeasurePolicy(gap = 6f, alignment = Alignment.CenterStart)
                    addChild(TextNode(text = "Active • ${activeState.label}").apply {
                        textVisuals.color = Color.white
                    })
                }
                root.addChild(badge)
            }
            else -> {
                // Default PrimaryButton
                val btnBg = when (activeState) {
                    ComponentState.HOVERED -> Color.valueOf("0071e3")
                    ComponentState.PRESSED -> Color.valueOf("005bb5")
                    ComponentState.DISABLED -> Color.valueOf("2c2c2e")
                    else -> Color.valueOf("0a84ff")
                }
                val padV = if (activeSize == "SM") 6f else if (activeSize == "LG") 14f else 10f
                val padH = if (activeSize == "SM") 12f else if (activeSize == "LG") 24f else 16f

                val btn = LayoutNode().apply {
                    pad(horizontal = padH, vertical = padV)
                    val vis = ensureVisuals()
                    vis.background.mode = BackgroundFill.Mode.COLOR
                    vis.background.color.set(btnBg)
                    vis.radii.set(8f)
                    addChild(TextNode(text = "$componentName • ${activeState.label}").apply {
                        textVisuals.color = if (activeState == ComponentState.DISABLED) Color.valueOf("8e8e93") else Color.white
                    })
                }
                root.addChild(btn)
            }
        }
        root
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(colors.canvasVoid)
    ) {
        // =====================================================================
        // 1. Procedural 25px Checkerboard Grid Background
        // =====================================================================
        Canvas(modifier = Modifier.fillMaxSize()) {
            val tileSize = 25f
            val screenW = arc.Core.graphics?.width?.toFloat() ?: 1920f
            val screenH = arc.Core.graphics?.height?.toFloat() ?: 1080f

            val cols = (screenW / tileSize).toInt() + 2
            val rows = (screenH / tileSize).toInt() + 2

            for (row in 0..rows) {
                val cellY = row * tileSize
                for (col in 0..cols) {
                    val cellX = col * tileSize
                    val isEven = ((col + row) % 2 + 2) % 2 == 0
                    Draw.color(if (isEven) colors.canvasGridDark else colors.canvasGridLight)
                    Fill.rect(cellX + tileSize * 0.5f, cellY + tileSize * 0.5f, tileSize, tileSize)
                }
            }
            Draw.color(Color.white)
        }

        // =====================================================================
        // 2. Direct Virtual Node Component Render Pass
        // =====================================================================
        Canvas(modifier = Modifier.fillMaxSize()) { renderer ->
            val screenW = arc.Core.graphics?.width?.toFloat() ?: 1920f
            val screenH = arc.Core.graphics?.height?.toFloat() ?: 1080f

            componentNode.setBounds(0f, 0f, screenW, screenH)
            componentNode.layout()
            componentNode.draw(renderer)
        }

        // =====================================================================
        // 3. Top Floating State Switcher Bar
        // =====================================================================
        Box(
            modifier = Modifier
                .anchor(LayoutPreset.CENTER_TOP)
                .margin(top = spacing.lg)
                .radius(shapes.pill)
                .background(colors.surfaceElevated)
                .border(1f, colors.borderHairline)
                .pad(horizontal = spacing.sm, vertical = spacing.xs)
        ) {
            Row(
                arrangement = Arrangement.spacedBy(spacing.xs),
                alignment = Alignment.CenterStart
            ) {
                for (state in ComponentState.values()) {
                    val isSelected = state == activeState
                    Box(
                        modifier = Modifier
                            .radius(shapes.pill)
                            .background(if (isSelected) colors.blue else Color.clear)
                            .clickable { activeState = state }
                            .pad(horizontal = spacing.md, vertical = spacing.xs + 1f)
                    ) {
                        Row(
                            arrangement = Arrangement.spacedBy(spacing.xs + 2f),
                            alignment = Alignment.CenterStart
                        ) {
                            Image(
                                source = state.iconUrl,
                                modifier = Modifier.size(12f),
                                tint = if (isSelected) Color.white else colors.textSecondary
                            )
                            MonoText(
                                text = state.label,
                                color = if (isSelected) Color.white else colors.textSecondary
                            )
                        }
                    }
                }
            }
        }

        // =====================================================================
        // 4. Bottom HUD: Component Tag & Size Variant Toggles
        // =====================================================================
        Box(
            modifier = Modifier
                .anchor(LayoutPreset.CENTER_BOTTOM)
                .margin(bottom = spacing.xl)
                .radius(shapes.md)
                .background(colors.surfaceElevated)
                .border(1f, colors.borderHairline)
                .pad(horizontal = spacing.md, vertical = spacing.sm)
        ) {
            Row(
                arrangement = Arrangement.spacedBy(spacing.lg),
                alignment = Alignment.CenterStart
            ) {
                Row(
                    arrangement = Arrangement.spacedBy(spacing.xs),
                    alignment = Alignment.CenterStart
                ) {
                    MonoText(text = "Component:", color = colors.textTertiary)
                    MonoText(text = "<$componentName>", color = colors.blue)
                }

                Row(
                    arrangement = Arrangement.spacedBy(spacing.xs),
                    alignment = Alignment.CenterStart
                ) {
                    MonoText(text = "Size:", color = colors.textTertiary)
                    for (size in listOf("SM", "MD", "LG")) {
                        val isSel = size == activeSize
                        Box(
                            modifier = Modifier
                                .radius(shapes.xs)
                                .background(if (isSel) colors.surfaceHighlight else Color.clear)
                                .border(if (isSel) 1f else 0f, if (isSel) colors.blue else Color.clear)
                            .clickable { activeSize = size }
                            .pad(horizontal = spacing.xs + 2f, vertical = 2f)
                        ) {
                            MonoText(text = size, color = if (isSel) colors.textPrimary else colors.textTertiary)
                        }
                    }
                }
            }
        }
    }
}
