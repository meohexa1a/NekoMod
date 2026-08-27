package org.mdt.ui.screens.editor.canvas

import arc.graphics.g2d.Draw
import arc.graphics.g2d.Fill
import arc.graphics.g2d.Lines
import arc.util.Tmp
import mindustry.ui.Fonts
import org.mdt.core.ui.UINode
import org.mdt.core.ui.graphics.Color
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
        val boundX = bounds.x
        val boundY = bounds.y
        val boundWidth = bounds.width
        val boundHeight = bounds.height

        if (boundWidth <= 0f || boundHeight <= 0f) return

        val accentBlue = Color.valueOf("0a84ff")
        val handleSize = 8f

        // 1. Bounding Outline
        Draw.color(accentBlue.toArcColor(Tmp.c1))
        Lines.stroke(2f)
        Lines.rect(boundX, boundY, boundWidth, boundHeight)

        // 2. 8 Perimeter Handles
        val handles = listOf(
            boundX to boundY + boundHeight,                 // Top-Left
            boundX + boundWidth * 0.5f to boundY + boundHeight,     // Top-Center
            boundX + boundWidth to boundY + boundHeight,            // Top-Right
            boundX to boundY + boundHeight * 0.5f,          // Left-Center
            boundX + boundWidth to boundY + boundHeight * 0.5f,     // Right-Center
            boundX to boundY,                      // Bottom-Left
            boundX + boundWidth * 0.5f to boundY,          // Bottom-Center
            boundX + boundWidth to boundY                  // Bottom-Right
        )

        for ((handleX, handleY) in handles) {
            Draw.color(Color.White.toArcColor(Tmp.c1))
            Fill.rect(handleX, handleY, handleSize, handleSize)

            Draw.color(accentBlue.toArcColor(Tmp.c1))
            Lines.stroke(1.5f)
            Lines.rect(handleX - handleSize * 0.5f, handleY - handleSize * 0.5f, handleSize, handleSize)
        }

        // 3. Dimension & Position Tooltip Badge
        val currentX = selectedNode.anchorData.offsetLeft.toInt()
        val currentY = selectedNode.anchorData.offsetTop.toInt()
        val tagLabel = "${selectedNode.name.ifEmpty { selectedNode.javaClass.simpleName }}  ${(boundWidth / zoomScale).toInt()} × ${(boundHeight / zoomScale).toInt()} px  (x:$currentX, y:$currentY)"
        val font = Fonts.def
        val badgeWidth = 240f
        val badgeHeight = 22f
        val badgeX = boundX + boundWidth * 0.5f
        val badgeY = boundY - 16f

        Draw.color(Color(0.08f, 0.08f, 0.10f, 0.85f).toArcColor(Tmp.c1))
        Fill.rect(badgeX, badgeY, badgeWidth, badgeHeight)

        Draw.color(accentBlue.toArcColor(Tmp.c1))
        Lines.stroke(1f)
        Lines.rect(badgeX - badgeWidth * 0.5f, badgeY - badgeHeight * 0.5f, badgeWidth, badgeHeight)

        Draw.color()
        font.draw(tagLabel, badgeX - badgeWidth * 0.46f, badgeY + 5f)
        Draw.color()
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

        Draw.color(fillColor.toArcColor(Tmp.c1))
        Fill.rect(left + w * 0.5f, bottom + h * 0.5f, w, h)

        Draw.color(strokeColor.toArcColor(Tmp.c1))
        Lines.stroke(2f)
        Lines.rect(left, bottom, w, h)
        Draw.color()
    }
}
