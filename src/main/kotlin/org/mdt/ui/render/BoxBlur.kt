package org.mdt.ui.render

import arc.graphics.Color
import arc.graphics.Texture
import arc.graphics.g2d.Draw
import arc.graphics.g2d.Fill
import arc.graphics.gl.FrameBuffer
import org.mdt.ui.widgets.BoxVisuals

/**
 * ## BoxBlur
 *
 * High-performance backdrop blur processor using a shared screen capture texture
 * and reusable scratch ping-pong FrameBuffers.
 * Supports per-box parameterized radius, iterations, and backdrop tints.
 *
 * See: docs/rendering-shaders/rendering_shaders_en.md
 */
class BoxBlur {
    private var sharedCapture: Texture? = null
    private var screenWidth: Float = 1f
    private var screenHeight: Float = 1f

    private var scratchFboA: FrameBuffer? = null
    private var scratchFboB: FrameBuffer? = null

    fun setSharedCapture(texture: Texture?, sw: Float, sh: Float) {
        this.sharedCapture = texture
        this.screenWidth = if (sw > 0f) sw else 1f
        this.screenHeight = if (sh > 0f) sh else 1f
    }

    fun capture(visuals: BoxVisuals, screenX: Float, screenY: Float, w: Float, h: Float): Texture? {
        if (!visuals.blur || visuals.backgroundMode != BoxVisuals.BackgroundMode.BACKDROP) return null
        val capture = sharedCapture ?: return null

        val fbW = maxOf((w * BACKDROP_SCALE).toInt(), MIN_FBO_SIZE)
        val fbH = maxOf((h * BACKDROP_SCALE).toInt(), MIN_FBO_SIZE)

        val existing = scratchFboA
        if (existing != null && (existing.width != fbW || existing.height != fbH)) disposeScratch()
        if (scratchFboA == null) {
            scratchFboA = FrameBuffer(fbW, fbH).apply { texture.setFilter(Texture.TextureFilter.linear) }
            scratchFboB = FrameBuffer(fbW, fbH).apply { texture.setFilter(Texture.TextureFilter.linear) }
        }

        val dstA = scratchFboA ?: return null
        val dstB = scratchFboB ?: return null

        // 1. Sample sub-region UV from shared screen capture into scratchFboA
        val u0 = (screenX / screenWidth).coerceIn(0f, 1f)
        val v0 = (screenY / screenHeight).coerceIn(0f, 1f)
        val u1 = ((screenX + w) / screenWidth).coerceIn(0f, 1f)
        val v1 = ((screenY + h) / screenHeight).coerceIn(0f, 1f)

        dstA.begin()
        Draw.flush()
        Draw.color(visuals.backdropTint)
        Fill.quad(
            capture,
            0f, 0f, Draw.getColor().toFloatBits(), u0, v0,
            0f, fbH.toFloat(), Draw.getColor().toFloatBits(), u0, v1,
            fbW.toFloat(), fbH.toFloat(), Draw.getColor().toFloatBits(), u1, v1,
            fbW.toFloat(), 0f, Draw.getColor().toFloatBits(), u1, v0
        )
        Draw.flush()
        dstA.end()

        // 2. Perform parameterized ping-pong Gaussian blur passes
        Shaders.ensure()
        var radius = visuals.blurRadius
        val iterations = visuals.blurIterations.coerceIn(1, 8)
        repeat(iterations) {
            blurPass(dstA, dstB, fbW, fbH, radius, 1f, 0f)
            blurPass(dstB, dstA, fbW, fbH, radius, 0f, 1f)
            radius *= 1.4f
        }

        Draw.color(Color.white)
        return dstA.texture
    }

    private fun blurPass(src: FrameBuffer, dst: FrameBuffer, fbW: Int, fbH: Int, radius: Float, dx: Float, dy: Float) {
        dst.begin()
        Shaders.blurShader?.let { s ->
            s.bind()
            s.setUniformf("u_texelSize", 1f / fbW, 1f / fbH)
            s.setUniformf("u_radius", radius)
            s.setUniformf("u_dir", dx, dy)
        }
        Draw.blit(src.texture, Shaders.blurShader)
        dst.end()
        Draw.flush()
    }

    fun dispose() {
        disposeScratch()
        sharedCapture = null
    }

    private fun disposeScratch() {
        scratchFboA?.dispose(); scratchFboA = null
        scratchFboB?.dispose(); scratchFboB = null
    }

    companion object {
        private const val BACKDROP_SCALE = 0.35f
        private const val MIN_FBO_SIZE = 4
    }
}
