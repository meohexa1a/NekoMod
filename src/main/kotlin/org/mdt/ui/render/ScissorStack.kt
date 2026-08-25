package org.mdt.ui.render

import arc.graphics.Gl
import arc.graphics.g2d.Draw
import org.mdt.ui.core.Rect
import java.util.ArrayDeque

/**
 * ## ScissorStack
 *
 * Hardware-accelerated OpenGL viewport scissor clipping stack with automatic
 * bounding box intersection and state restoration.
 *
 * See: docs/complex-challenges/complex_challenges_en.md
 */
object ScissorStack {

    private val stack = ArrayDeque<Rect>()

    /**
     * Pushes a new clipping rectangle onto the stack, intersecting it with any active parent scissor.
     * Flushes the current batch before applying OpenGL scissor state.
     *
     * @return True if the resulting clipped region has positive area and rendering should proceed.
     */
    fun push(bounds: Rect): Boolean {
        Draw.flush()

        val current = stack.peekFirst()
        val effectiveRect = if (current == null) {
            Rect(bounds.x, bounds.y, bounds.width, bounds.height)
        } else {
            // Compute AABB intersection
            val minX = maxOf(current.x, bounds.x)
            val minY = maxOf(current.y, bounds.y)
            val maxX = minOf(current.x + current.width, bounds.x + bounds.width)
            val maxY = minOf(current.y + current.height, bounds.y + bounds.height)
            val w = maxOf(0f, maxX - minX)
            val h = maxOf(0f, maxY - minY)
            Rect(minX, minY, w, h)
        }

        stack.addFirst(effectiveRect)

        if (effectiveRect.width <= 0f || effectiveRect.height <= 0f) {
            // Empty intersection: nothing is visible
            return false
        }

        if (stack.size == 1) {
            Gl.enable(Gl.scissorTest)
        }

        Gl.scissor(
            effectiveRect.x.toInt(),
            effectiveRect.y.toInt(),
            effectiveRect.width.toInt(),
            effectiveRect.height.toInt()
        )
        return true
    }

    /**
     * Pops the topmost clipping rectangle and restores the parent scissor state.
     */
    fun pop() {
        if (stack.isEmpty()) return
        Draw.flush()
        stack.removeFirst()

        val current = stack.peekFirst()
        if (current != null) {
            Gl.scissor(
                current.x.toInt(),
                current.y.toInt(),
                current.width.toInt(),
                current.height.toInt()
            )
        } else {
            Gl.disable(Gl.scissorTest)
        }
    }

    /** Clears and disables all scissor tests. */
    fun clear() {
        if (stack.isNotEmpty()) {
            Draw.flush()
            stack.clear()
            Gl.disable(Gl.scissorTest)
        }
    }
}
