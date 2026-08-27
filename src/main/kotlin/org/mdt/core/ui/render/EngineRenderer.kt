package org.mdt.core.ui.render

import arc.graphics.Gl
import arc.graphics.g2d.Draw
import arc.math.Mat
import org.mdt.core.ui.CanvasNode

/**
 * ## EngineRenderer
 *
 * Master UI GPU renderer managing orthographic projection, viewport transforms,
 * and scissor clipping boundaries for Virtual DOM elements.
 *
 * See: docs/rendering-shaders/rendering_shaders_en.md
 */
class EngineRenderer {
    private val prevProj = Mat()

    fun render(canvas: CanvasNode) {
        val sw = canvas.screenWidth.toInt()
        val sh = canvas.screenHeight.toInt()
        if (sw <= 0 || sh <= 0 || !canvas.visible) return

        // 1. Update ScissorStack viewport scaling
        ScissorStack.setViewport(canvas.screenWidth, canvas.screenHeight)

        Draw.flush()
        prevProj.set(Draw.proj())

        Draw.proj(0f, 0f, canvas.screenWidth, canvas.screenHeight)
        Draw.color()

        // 2. Render Virtual DOM UI tree
        canvas.draw(this)

        Draw.flush()
        ScissorStack.clear()
        Draw.proj(prevProj)
        Gl.activeTexture(Gl.texture0)
    }

    fun dispose() {
        BoxBlur.dispose()
    }
}
