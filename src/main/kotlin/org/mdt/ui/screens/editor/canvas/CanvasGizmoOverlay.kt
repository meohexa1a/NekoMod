package org.mdt.ui.screens.editor.canvas

import arc.graphics.Color
import arc.graphics.g2d.Draw
import arc.graphics.g2d.Fill
import arc.graphics.g2d.Lines
import mindustry.ui.Fonts
import org.mdt.core.ui.UINode
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min

/**
 * ## CanvasGizmoOverlay
 *
 * High-precision transform gizmo renderer displaying bounding selection outlines,
 * 8 perimeter resize handles, interactive dimension badges, and creation overlays.
 *
 * See: docs/design-system/design_system_en.md
 */
object CanvasGizmoOverlay {

    /**
     * Draws the 8-point transform gizmo and dimension badge around the selected node.
     */
    fun drawSelectionGizmo(selectedNode: UINode, zoomScale: Float) {
        val bounds = selectedNode.bounds
        val bx = bounds.x
        val by = bounds.y
        val bw = bounds.width
        val bh = bounds.height

        if (bw <= 0f || bh <= 0f) return

        val blue = Color.valueOf("0a84ff")
        val handleSize = 8f

        // 1. Bounding Outline
        Draw.color(blue)
        Lines.stroke(2f)
        Lines.rect(bx, by, bw, bh)

        // 2. 8 Perimeter Handles
        val handles = listOf(
            bx to by + bh,                 // Top-Left
            bx + bw * 0.5f to by + bh,     // Top-Center
            bx + bw to by + bh,            // Top-Right
            bx to by + bh * 0.5f,          // Left-Center
            bx + bw to by + bh * 0.5f,     // Right-Center
            bx to by,                      // Bottom-Left
            bx + bw * 0.5f to by,          // Bottom-Center
            bx + bw to by                  // Bottom-Right
        )

        for ((hx, hy) in handles) {
            Draw.color(Color.white)
            Fill.rect(hx, hy, handleSize, handleSize)

            Draw.color(blue)
            Lines.stroke(1.5f)
            Lines.rect(hx - handleSize * 0.5f, hy - handleSize * 0.5f, handleSize, handleSize)
        }

        // 3. Dimension & Position Tooltip Badge
        val curX = selectedNode.anchorData.offsetLeft.toInt()
        val curY = selectedNode.anchorData.offsetTop.toInt()
        val tagLabel = "${selectedNode.name.ifEmpty { selectedNode.javaClass.simpleName }}  ${(bw / zoomScale).toInt()} × ${(bh / zoomScale).toInt()} px  (x:$curX, y:$curY)"
        val font = Fonts.def
        val badgeW = 240f
        val badgeH = 22f
        val badgeX = bx + bw * 0.5f
        val badgeY = by - 16f

        Draw.color(Color(0.08f, 0.08f, 0.10f, 0.85f))
        Fill.rect(badgeX, badgeY, badgeW, badgeH)

        Draw.color(blue)
        Lines.stroke(1f)
        Lines.rect(badgeX - badgeW * 0.5f, badgeY - badgeH * 0.5f, badgeW, badgeH)

        Draw.color(Color.white)
        font.draw(tagLabel, badgeX - badgeW * 0.46f, badgeY + 5f)
        Draw.color(Color.white)
    }

    /**
     * Draws the live bounding preview box while dragging to create a new Rect or Frame.
     */
    fun drawCreationPreview(
        startX: Float,
        startY: Float,
        currentX: Float,
        currentY: Float,
        isFrame: Boolean
    ) {
        val left = min(startX, currentX)
        val bottom = min(startY, currentY)
        val w = max(4f, abs(currentX - startX))
        val h = max(4f, abs(currentY - startY))

        val strokeColor = if (isFrame) Color.valueOf("bf5af2") else Color.valueOf("0a84ff")
        val fillColor = Color(strokeColor.r, strokeColor.g, strokeColor.b, 0.12f)

        Draw.color(fillColor)
        Fill.rect(left + w * 0.5f, bottom + h * 0.5f, w, h)

        Draw.color(strokeColor)
        Lines.stroke(2f)
        Lines.rect(left, bottom, w, h)
        Draw.color(Color.white)
    }
}
