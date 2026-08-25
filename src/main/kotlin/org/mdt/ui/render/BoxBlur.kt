package org.mdt.ui.render

import arc.graphics.Color
import arc.graphics.Texture
import arc.graphics.g2d.Draw
import arc.graphics.g2d.Fill
import arc.graphics.gl.FrameBuffer
import arc.math.Mat
import org.mdt.ui.components.layout.BoxVisuals

/**
 * ## BoxBlur
 *
 * High-performance backdrop blur coordinator using screen-sized downscaled
 * ping-pong FrameBuffers.
 * Performs a single 2-pass Gaussian blur over the captured screen texture per frame,
 * allowing all backdrop glassmorphism boxes to sample at zero additional GPU allocation cost.
 *
 * See: docs/rendering-shaders/rendering_shaders_en.md
 */
class BoxBlur {
    private var sharedCapture: Texture? = null
    var screenWidth: Float = 1f
        private set
    var screenHeight: Float = 1f
        private set

    private var pingPongA: FrameBuffer? = null
    private var pingPongB: FrameBuffer? = null
    private var blurredTexture: Texture? = null
    private var hasBlurredThisFrame = false
    private val scratchMat = Mat()

    fun setSharedCapture(texture: Texture?, sw: Float, sh: Float) {
        this.sharedCapture = texture
        this.screenWidth = if (sw > 0f) sw else 1f
        this.screenHeight = if (sh > 0f) sh else 1f
        this.hasBlurredThisFrame = false
    }

    /**
     * Performs Gaussian blur on the shared screen capture and returns the blurred texture.
     */
    fun getBlurredTexture(visuals: BoxVisuals): Texture? {
        if (!visuals.blur || visuals.backgroundMode != BoxVisuals.BackgroundMode.BACKDROP) return null
        val capture = sharedCapture ?: return null

        if (hasBlurredThisFrame && blurredTexture != null) {
            return blurredTexture
        }

        val fbW = maxOf(64, (screenWidth * DOWNSCALE_FACTOR).toInt())
        val fbH = maxOf(64, (screenHeight * DOWNSCALE_FACTOR).toInt())

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

        // 1. Copy downscaled full screen capture into pingPongA with dedicated FBO projection
        dstA.begin()
        Draw.proj(0f, 0f, fbW.toFloat(), fbH.toFloat())
        Draw.color(visuals.backdropTint)
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
        var radius = visuals.blurRadius * DOWNSCALE_FACTOR
        val iterations = visuals.blurIterations.coerceIn(1, 4)

        repeat(iterations) {
            blurPass(dstA, dstB, fbW, fbH, radius, 1f, 0f)
            blurPass(dstB, dstA, fbW, fbH, radius, 0f, 1f)
            radius *= 1.25f
        }

        // Restore camera/canvas projection matrix
        Draw.proj(scratchMat)
        Draw.color(Color.white)
        hasBlurredThisFrame = true
        blurredTexture = dstA.texture
        return blurredTexture
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
        disposeScratch()
        sharedCapture = null
    }

    private fun disposeScratch() {
        pingPongA?.dispose()
        pingPongA = null
        pingPongB?.dispose()
        pingPongB = null
        blurredTexture = null
        hasBlurredThisFrame = false
    }

    companion object {
        private const val DOWNSCALE_FACTOR = 0.5f
    }
}
