package org.mdt.ui.screens.editor

import androidx.compose.runtime.*
import arc.Core
import arc.graphics.Color
import arc.graphics.g2d.Draw
import arc.graphics.g2d.Fill
import org.mdt.core.ui.compose.*
import org.mdt.core.ui.layout.LayoutPreset
import org.mdt.ui.components.display.canvas.Canvas
import org.mdt.ui.components.layout.Box
import org.mdt.ui.components.text.MonoText
import org.mdt.ui.theme.Theme

/**
 * ## EditorCenterCanvas
 *
 * True infinite 2D viewport (Figma & Godot 2D Engine paradigm) with interactive drag panning
 * and a 25px alternating checkerboard background texture using Apple Design tokens.
 *
 * @param modifier Chainable layout modifier.
 * @param content Declarative preview content slotted into the infinite world space.
 *
 * See: docs/design-system/design_system_en.md
 */
@Composable
fun EditorCenterCanvas(
    modifier: UIModifier = UIModifier,
    content: @Composable BoxScope.() -> Unit = {}
) {
    var panX by remember { mutableStateOf(0f) }
    var panY by remember { mutableStateOf(0f) }
    var isDraggingCanvas by remember { mutableStateOf(false) }
    var lastTouchX by remember { mutableStateOf(0f) }
    var lastTouchY by remember { mutableStateOf(0f) }

    val colors = Theme.colors
    val shapes = Theme.shapes
    val spacing = Theme.spacing

    Box(
        modifier = modifier
            .fillMaxSize()
            .clip(true)
            .cursor(if (isDraggingCanvas) arc.Graphics.Cursor.SystemCursor.hand else arc.Graphics.Cursor.SystemCursor.arrow)
            .onPointerDown { event ->
                isDraggingCanvas = true
                lastTouchX = event.x
                lastTouchY = event.y
            }
            .onPointerUp {
                isDraggingCanvas = false
            }
            .onPointerDrag { event ->
                val deltaX = event.x - lastTouchX
                val deltaY = event.y - lastTouchY
                lastTouchX = event.x
                lastTouchY = event.y
                panX += deltaX
                panY += deltaY
            }
    ) {
        // =====================================================================
        // 1. Procedural 25px Alternating Checkerboard Background
        // =====================================================================
        Canvas(modifier = Modifier.fillMaxSize()) { _ ->
            val cellSize = 25f
            val screenW = if (Core.graphics != null) Core.graphics.width.toFloat() else 1920f
            val screenH = if (Core.graphics != null) Core.graphics.height.toFloat() else 1080f

            val startCol = (-panX / cellSize).toInt() - 2
            val endCol = ((screenW - panX) / cellSize).toInt() + 2
            val startRow = (-panY / cellSize).toInt() - 2
            val endRow = ((screenH - panY) / cellSize).toInt() + 2

            for (row in startRow..endRow) {
                val cellY = row * cellSize + panY
                for (col in startCol..endCol) {
                    val cellX = col * cellSize + panX
                    val isEven = ((row + col) % 2 + 2) % 2 == 0
                    Draw.color(if (isEven) colors.canvasGridDark else colors.canvasGridLight)
                    Fill.rect(cellX + cellSize * 0.5f, cellY + cellSize * 0.5f, cellSize, cellSize)
                }
            }
            Draw.color(Color.white)
        }

        // =====================================================================
        // 2. Infinite World Canvas Slot (Positioned directly by panX, panY)
        // =====================================================================
        Box(
            modifier = Modifier
                .anchor(LayoutPreset.CENTER)
                .margin(left = panX * 2f, bottom = panY * 2f)
        ) {
            content()
        }

        // =====================================================================
        // 3. Floating Coordinates & Status Readout (Top-Right)
        // =====================================================================
        Box(
            modifier = Modifier
                .anchor(LayoutPreset.TOP_RIGHT)
                .margin(top = spacing.md, right = spacing.md)
                .radius(shapes.sm)
                .background(colors.surfaceElevated)
                .border(1f, colors.borderHairline)
                .pad(horizontal = spacing.md, vertical = spacing.xs + 1f)
        ) {
            MonoText(
                text = "X: ${panX.toInt()}  Y: ${panY.toInt()}  Zoom: 100%",
                color = colors.textSecondary,
                scale = 0.9f
            )
        }
    }
}
