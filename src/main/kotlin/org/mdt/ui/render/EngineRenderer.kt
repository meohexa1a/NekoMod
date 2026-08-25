package org.mdt.ui.render

import arc.graphics.Color
import arc.graphics.Gl
import arc.graphics.Texture
import arc.graphics.g2d.Draw
import arc.graphics.gl.FrameBuffer
import arc.math.Mat
import org.mdt.ui.core.CanvasNode
import org.mdt.ui.core.UINode
import org.mdt.ui.components.layout.BoxVisuals
import org.mdt.ui.components.layout.LayoutNode

/**
 * ## EngineRenderer
 *
 * Master UI GPU renderer managing orthographic projection, shared screen capture,
 * and backdrop blur coordinator.
 *
 * See: docs/rendering-shaders/rendering_shaders_en.md
 */
class EngineRenderer {
    private val prevProj = Mat()

    /** Shared backdrop blur processor instance. */
    val blurProcessor = BoxBlur()
    private var screenCaptureFbo: FrameBuffer? = null

    private fun hasBackdropBlur(node: UINode): Boolean {
        if (!node.visible) return false
        if (node is LayoutNode) {
            val vis = node.visuals
            if (vis != null && vis.blur && vis.backgroundMode == BoxVisuals.BackgroundMode.BACKDROP) {
                return true
            }
        }
        for (child in node.children) {
            if (hasBackdropBlur(child)) return true
        }
        return false
    }

    private fun captureScreen(sw: Int, sh: Int): Texture? {
        val existing = screenCaptureFbo
        if (existing != null && (existing.width != sw || existing.height != sh)) {
            existing.dispose()
            screenCaptureFbo = null
        }
        if (screenCaptureFbo == null) {
            screenCaptureFbo = FrameBuffer(sw, sh).apply {
                texture.setFilter(Texture.TextureFilter.linear)
            }
        }

        val fbo = screenCaptureFbo ?: return null
        Draw.flush()
        val blendWas = Gl.isEnabled(Gl.blend)
        Gl.disable(Gl.blend)
        Gl.depthMask(false)
        Gl.bindTexture(Gl.texture2d, fbo.texture.textureObjectHandle)
        Gl.copyTexSubImage2D(Gl.texture2d, 0, 0, 0, 0, 0, sw, sh)
        Gl.bindTexture(Gl.texture2d, 0)
        if (blendWas) Gl.enable(Gl.blend) else Gl.disable(Gl.blend)
        Gl.depthMask(true)
        Draw.flush()

        return fbo.texture
    }

    fun render(canvas: CanvasNode) {
        val sw = canvas.screenWidth.toInt()
        val sh = canvas.screenHeight.toInt()
        if (sw <= 0 || sh <= 0 || !canvas.visible) return

        // 1. Single Shared Screen Capture if any node requires backdrop blur
        val sharedCapture = if (hasBackdropBlur(canvas)) captureScreen(sw, sh) else null
        blurProcessor.setSharedCapture(sharedCapture, sw.toFloat(), sh.toFloat())

        Draw.flush()
        prevProj.set(Draw.proj())

        Draw.proj(0f, 0f, canvas.screenWidth, canvas.screenHeight)
        Draw.color(Color.white)

        canvas.draw(this)

        Draw.flush()
        Draw.proj(prevProj)
    }

    fun dispose() {
        screenCaptureFbo?.dispose()
        screenCaptureFbo = null
        blurProcessor.dispose()
    }
}
