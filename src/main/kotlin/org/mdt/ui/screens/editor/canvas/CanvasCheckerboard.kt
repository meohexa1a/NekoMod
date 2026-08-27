package org.mdt.ui.screens.editor.canvas

import arc.Core
import arc.graphics.g2d.Draw
import arc.graphics.g2d.Fill
import arc.util.Tmp
import org.mdt.core.ui.graphics.Color
import org.mdt.ui.theme.ColorTokens

/**
 * ## CanvasCheckerboard
 *
 * High-performance procedural checkerboard background renderer scaling seamlessly with viewport zoom.
 *
 * See: docs/design-system/design_system_en.md
 */
object CanvasCheckerboard {

    /**
     * Draws the infinite tiled checkerboard pattern across the current viewport.
     */
    fun draw(panX: Float, panY: Float, zoomScale: Float, colors: ColorTokens) {
        val cellSize = 25f * zoomScale
        val screenW = Core.graphics?.width?.toFloat() ?: 1920f
        val screenH = Core.graphics?.height?.toFloat() ?: 1080f

        val startCol = (-panX / cellSize).toInt() - 2
        val endCol = ((screenW - panX) / cellSize).toInt() + 2
        val startRow = (-panY / cellSize).toInt() - 2
        val endRow = ((screenH - panY) / cellSize).toInt() + 2

        for (row in startRow..endRow) {
            val cellY = row * cellSize + panY
            for (col in startCol..endCol) {
                val cellX = col * cellSize + panX
                val isEven = ((row + col) % 2 + 2) % 2 == 0
                val color = if (isEven) colors.canvasGridDark else colors.canvasGridLight
                Draw.color(color.toArcColor(Tmp.c1))
                Fill.rect(cellX + cellSize * 0.5f, cellY + cellSize * 0.5f, cellSize, cellSize)
            }
        }
        Draw.color()
    }
}
