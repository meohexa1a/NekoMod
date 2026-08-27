package org.mdt.ui.screens.editor.model

import org.mdt.core.ui.Rect

/**
 * ## GizmoHandle
 *
 * 8 perimeter interactive resize handles and body drag handle for 2D transform manipulation.
 *
 * See: docs/design-system/design_system_en.md
 */
enum class GizmoHandle {
    NONE, BODY,
    TOP_LEFT, TOP_CENTER, TOP_RIGHT,
    LEFT_CENTER, RIGHT_CENTER,
    BOTTOM_LEFT, BOTTOM_CENTER, BOTTOM_RIGHT;

    companion object {
        /**
         * Hit-tests pointer coordinates against the 8 perimeter handles of a node's bounding rectangle.
         */
        fun testHit(px: Float, py: Float, bounds: Rect, hitTolerance: Float = 10f): GizmoHandle {
            val bx = bounds.x
            val by = bounds.y
            val bw = bounds.width
            val bh = bounds.height

            fun hit(hx: Float, hy: Float): Boolean =
                px in (hx - hitTolerance)..(hx + hitTolerance) && py in (hy - hitTolerance)..(hy + hitTolerance)

            return when {
                hit(bx, by + bh) -> TOP_LEFT
                hit(bx + bw * 0.5f, by + bh) -> TOP_CENTER
                hit(bx + bw, by + bh) -> TOP_RIGHT
                hit(bx, by + bh * 0.5f) -> LEFT_CENTER
                hit(bx + bw, by + bh * 0.5f) -> RIGHT_CENTER
                hit(bx, by) -> BOTTOM_LEFT
                hit(bx + bw * 0.5f, by) -> BOTTOM_CENTER
                hit(bx + bw, by) -> BOTTOM_RIGHT
                else -> NONE
            }
        }
    }
}
