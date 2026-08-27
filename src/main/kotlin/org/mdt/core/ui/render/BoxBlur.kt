package org.mdt.core.ui.render

import arc.Core
import arc.graphics.Gl
import arc.graphics.Texture
import arc.graphics.g2d.Draw
import arc.graphics.g2d.Fill
import arc.graphics.gl.FrameBuffer
import arc.math.Mat
import org.mdt.ui.components.layout.BoxVisuals

/**
 * ## BoxBlur
 *
 * High-performance 2-pass Gaussian backdrop blur coordinator.
 * Lazily captures the framebuffer and performs ping-pong blur passes on-demand.
 *
 * See: docs/rendering-shaders/rendering_shaders_en.md
 */
object BoxBlur {

    /**
     * Blur resolution downscale factor (e.g. 1.0f for Full-Res, 0.5f for Half-Res, <= 0f to disable).
     * Synchronized with persistent [AppSettings].
     */
    var downscaleFactor: Float = 1.0f
        set(value) {
            val clamped = value.coerceIn(0.0f, 1.0f)
            if (field != clamped) {
                field = clamped
                disposeScratch()
            }
        }

    private var screenCaptureFbo: FrameBuffer? = null
    private var pingPongA: FrameBuffer? = null
    private var pingPongB: FrameBuffer? = null
    private var blurredTexture: Texture? = null

    private var lastFrameId: Long = -1L
    private val scratchMat = Mat()

    /**
     * Lazily captures the current screen buffer and performs Gaussian blur on-demand.
     * Guaranteed to execute at most ONCE per frame regardless of how many backdrop boxes are rendered.
     */
    fun getBlurredTexture(visuals: BoxVisuals): Texture? {
        if (!visuals.backdrop.enabled || visuals.background.mode != org.mdt.ui.components.layout.BackgroundFill.Mode.BACKDROP || downscaleFactor <= 0.001f) {
            return null
        }

        val currentFrame = if (Core.graphics != null) Core.graphics.frameId else System.nanoTime()
        if (lastFrameId == currentFrame && blurredTexture != null) {
            return blurredTexture
        }

        val screenW = if (Core.graphics != null && Core.graphics.width > 0) Core.graphics.width else 1920
        val screenH = if (Core.graphics != null && Core.graphics.height > 0) Core.graphics.height else 1080

        val capture = captureScreen(screenW, screenH) ?: return null

        val scale = downscaleFactor.coerceIn(0.1f, 1.0f)
        val fbW = maxOf(64, (screenW * scale).toInt())
        val fbH = maxOf(64, (screenH * scale).toInt())

        val existing = pingPongA
        if (existing != null && (existing.width != fbW || existing.height != fbH)) {
            disposeScratch()
        }

        if (pingPongA == null) {
            pingPongA = FrameBuffer(fbW, fbH).apply { texture.setFilter(Texture.TextureFilter.linear) }
            pingPongB = FrameBuffer(fbW, fbH).apply { texture.setFilter(Texture.TextureFilter.linear) }
        }

        val dstA = pingPongA ?: return null
        val dstB = pingPongB ?: return null

        Draw.flush()
        scratchMat.set(Draw.proj())

        // 1. Copy captured screen into pingPongA with dedicated FBO projection (pure white color)
        dstA.begin()
        Draw.proj(0f, 0f, fbW.toFloat(), fbH.toFloat())
        Draw.color()
        Fill.quad(
            capture,
            0f, 0f, Draw.getColor().toFloatBits(), 0f, 0f,
            0f, fbH.toFloat(), Draw.getColor().toFloatBits(), 0f, 1f,
            fbW.toFloat(), fbH.toFloat(), Draw.getColor().toFloatBits(), 1f, 1f,
            fbW.toFloat(), 0f, Draw.getColor().toFloatBits(), 1f, 0f
        )
        Draw.flush()
        dstA.end()

        // 2. Perform 2-pass Gaussian blur ping-pong passes
        Shaders.ensure()
        var radius = visuals.backdrop.blurRadius * scale
        val iterations = visuals.backdrop.iterations.coerceIn(1, 4)

        repeat(iterations) {
            blurPass(dstA, dstB, fbW, fbH, radius, 1f, 0f)
            blurPass(dstB, dstA, fbW, fbH, radius, 0f, 1f)
            radius *= 1.15f
        }

        // Restore camera/canvas projection matrix
        Draw.proj(scratchMat)
        Draw.color()
        lastFrameId = currentFrame
        blurredTexture = dstA.texture
        return blurredTexture
    }

    private fun captureScreen(physW: Int, physH: Int): Texture? {
        val existing = screenCaptureFbo
        if (existing != null && (existing.width != physW || existing.height != physH)) {
            existing.dispose()
            screenCaptureFbo = null
        }
        if (screenCaptureFbo == null) {
            screenCaptureFbo = FrameBuffer(physW, physH).apply {
                texture.setFilter(Texture.TextureFilter.linear)
            }
        }

        val fbo = screenCaptureFbo ?: return null
        Draw.flush()
        val blendWas = Gl.isEnabled(Gl.blend)
        Gl.disable(Gl.blend)
        Gl.depthMask(false)
        Gl.bindTexture(Gl.texture2d, fbo.texture.textureObjectHandle)
        Gl.copyTexSubImage2D(Gl.texture2d, 0, 0, 0, 0, 0, physW, physH)
        Gl.bindTexture(Gl.texture2d, 0)
        if (blendWas) Gl.enable(Gl.blend) else Gl.disable(Gl.blend)
        Gl.depthMask(true)
        Draw.flush()

        return fbo.texture
    }

    private fun blurPass(src: FrameBuffer, dst: FrameBuffer, fbW: Int, fbH: Int, radius: Float, dx: Float, dy: Float) {
        dst.begin()
        Draw.proj(0f, 0f, fbW.toFloat(), fbH.toFloat())
        val s = Shaders.blurShader
        if (s != null) {
            val prevShader = Draw.getShader()
            Draw.shader(s)
            s.bind()
            s.setUniformf("u_texelSize", 1f / fbW.toFloat(), 1f / fbH.toFloat())
            s.setUniformf("u_radius", radius)
            s.setUniformf("u_dir", dx, dy)
            Draw.color()

            Fill.quad(
                src.texture,
                0f, 0f, Draw.getColor().toFloatBits(), 0f, 0f,
                0f, fbH.toFloat(), Draw.getColor().toFloatBits(), 0f, 1f,
                fbW.toFloat(), fbH.toFloat(), Draw.getColor().toFloatBits(), 1f, 1f,
                fbW.toFloat(), 0f, Draw.getColor().toFloatBits(), 1f, 0f
            )
            Draw.flush()
            Draw.shader(prevShader)
        }
        dst.end()
    }

    fun dispose() {
        screenCaptureFbo?.dispose()
        screenCaptureFbo = null
        disposeScratch()
    }

    private fun disposeScratch() {
        pingPongA?.dispose()
        pingPongA = null
        pingPongB?.dispose()
        pingPongB = null
        blurredTexture = null
        lastFrameId = -1L
    }
}
