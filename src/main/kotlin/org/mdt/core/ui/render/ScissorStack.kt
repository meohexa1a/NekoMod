package org.mdt.ui.render

import arc.Core
import arc.graphics.Gl
import arc.graphics.g2d.Draw
import org.mdt.core.ui.Rect
import java.util.ArrayDeque

/**
 * ## ScissorStack
 *
 * Hardware-accelerated OpenGL viewport scissor clipping stack with High-DPI physical
 * viewport coordinate transformation and automatic bounding box intersection.
 *
 * See: docs/complex-challenges/complex_challenges_en.md
 */
object ScissorStack {

    private val stack = ArrayDeque<Rect>()
    private var canvasW: Float = 1f
    private var canvasH: Float = 1f

    fun setViewport(width: Float, height: Float) {
        canvasW = if (width > 0f) width else 1f
        canvasH = if (height > 0f) height else 1f
    }

    private fun applyScissor(rect: Rect) {
        val physW = if (Core.graphics != null && Core.graphics.width > 0) Core.graphics.width.toFloat() else canvasW
        val physH = if (Core.graphics != null && Core.graphics.height > 0) Core.graphics.height.toFloat() else canvasH

        val scaleX = physW / canvasW
        val scaleY = physH / canvasH

        val sx = (rect.x * scaleX).toInt().coerceIn(0, physW.toInt())
        val sy = (rect.y * scaleY).toInt().coerceIn(0, physH.toInt())
        val sw = (rect.width * scaleX).toInt().coerceIn(0, physW.toInt() - sx)
        val sh = (rect.height * scaleY).toInt().coerceIn(0, physH.toInt() - sy)

        Gl.scissor(sx, sy, maxOf(0, sw), maxOf(0, sh))
    }

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

        if (effectiveRect.width <= 0.001f || effectiveRect.height <= 0.001f) return false
        if (stack.size == 1) Gl.enable(Gl.scissorTest)

        applyScissor(effectiveRect)
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
        if (current != null) applyScissor(current) else Gl.disable(Gl.scissorTest)
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
