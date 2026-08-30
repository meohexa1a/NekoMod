// [AGENT INVARIANT] Synchronously update @property, @param, and @see KDocs when modifying this file.

package org.mdt.core.platform.render

import arc.graphics.Gl
import arc.graphics.Mesh
import arc.graphics.Texture
import arc.graphics.VertexAttribute
import arc.graphics.gl.FrameBuffer
import arc.util.Disposable
import arc.util.Log
import org.mdt.core.ui.EngineRuntime

/**
 * ## SceneBlur
 *
 * Executes real-time background blur captures using the 5-pass Dual-Kawase pyramid downsample/upsample algorithm.
 * Coordinates frame deduplication and interval throttling (~33ms / 30 FPS) to eliminate GPU fillrate bottlenecks.
 * Directly manages OpenGL FBOs and quad rasterization without unnecessary intermediate abstraction layers.
 *
 * @property isEnabled Master toggle for background blur rendering.
 * @property updateIntervalMs Refresh throttle in milliseconds (~33ms = 30 FPS update rate).
 * @property blurRadius Blur spread radius multiplier for upsampling passes.
 *
 * @see UIBatch
 * @see ShaderRegistry
 */
object SceneBlur : Disposable {

    // --- STATE & CONFIGURATION ---

    /** Master toggle for background blur rendering. */
    var isEnabled: Boolean = false

    /** Refresh throttle in milliseconds (~33ms = 30 FPS update rate). */
    var updateIntervalMs: Long = 33L

    /** Blur spread radius multiplier for upsampling (0.5f - 2.5f). Default: 1.2f. */
    var blurRadius: Float = 1.2f

    /** Execution time of the latest blur pass in milliseconds. */
    var lastBlurDurationMs: Float = 0.0f
        private set

    /** Execution time of the latest screen capture in milliseconds. */
    var lastCaptureDurationMs: Float = 0.0f
        private set

    // --- SCRATCH BUFFERS & GEOMETRY ---

    private var screenCaptureFbo: FrameBuffer? = null
    private var pingPongA: FrameBuffer? = null // Level 1 FBO: 1/2 scale (e.g. 960x540 for 1080p)
    private var pingPongB: FrameBuffer? = null // Level 2 FBO: 1/4 scale (e.g. 480x270 for 1080p)
    private var pingPongC: FrameBuffer? = null // Level 3 FBO: 1/8 scale (e.g. 240x135 for 1080p)
    private var blurredTexture: Texture? = null

    private var lastCapturedFrameId: Long = -1L
    private var lastCaptureTimestamp: Long = 0L

    /** Static 4-vertex full-screen quad mesh in Normalized Device Coordinates (NDC: -1..1). */
    private val quadMesh: Mesh by lazy {
        Mesh(
            true, 4, 6,
            VertexAttribute(2, "a_position"),
            VertexAttribute(2, "a_texCoords")
        ).apply {
            setVertices(floatArrayOf(
                // x,     y,     u,    v
                -1.0f, -1.0f,  0.0f, 0.0f, // Bottom-Left
                -1.0f,  1.0f,  0.0f, 1.0f, // Top-Left
                 1.0f,  1.0f,  1.0f, 1.0f, // Top-Right
                 1.0f, -1.0f,  1.0f, 0.0f  // Bottom-Right
            ))
            setIndices(shortArrayOf(0, 1, 2, 2, 3, 0))
        }
    }

    // --- BLUR PIPELINE EXECUTION ---

    /**
     * Executes progressive 5-pass Dual-Kawase background blur with monotonic frame and time throttling.
     *
     * @return The blurred background [Texture], or null if blur is disabled or capture failed.
     */
    fun captureAndBlur(): Texture? {
        if (!isEnabled) return null

        val currentFrameId = EngineRuntime.host.frameId
        val now = System.currentTimeMillis()

        // Reuse cached texture if already captured this frame OR if within throttle interval
        val isCached = blurredTexture != null && (lastCapturedFrameId == currentFrameId || (now - lastCaptureTimestamp < updateIntervalMs))
        if (isCached) return blurredTexture

        val screenWidth = EngineRuntime.host.screenWidth.toInt().coerceAtLeast(1)
        val screenHeight = EngineRuntime.host.screenHeight.toInt().coerceAtLeast(1)

        val captureTexture = captureScreen(screenWidth, screenHeight) ?: return null

        val blurStartNanos = System.nanoTime()

        // Level 1: 1/2 scale (960x540 for 1080p)
        val level1Width = maxOf(32, screenWidth / 2)
        val level1Height = maxOf(32, screenHeight / 2)

        // Level 2: 1/4 scale (480x270 for 1080p)
        val level2Width = maxOf(16, level1Width / 2)
        val level2Height = maxOf(16, level1Height / 2)

        // Level 3: 1/8 scale (240x135 for 1080p)
        val level3Width = maxOf(8, level2Width / 2)
        val level3Height = maxOf(8, level2Height / 2)

        val existingA = pingPongA
        val existingB = pingPongB
        val existingC = pingPongC
        if (existingA != null && (existingA.width != level1Width || existingA.height != level1Height) ||
            existingB != null && (existingB.width != level2Width || existingB.height != level2Height) ||
            existingC != null && (existingC.width != level3Width || existingC.height != level3Height)) {
            disposeScratch()
        }

        if (pingPongA == null) {
            try {
                pingPongA = FrameBuffer(level1Width, level1Height).apply {
                    texture.setFilter(Texture.TextureFilter.linear, Texture.TextureFilter.linear)
                    texture.setWrap(Texture.TextureWrap.clampToEdge, Texture.TextureWrap.clampToEdge)
                }
                pingPongB = FrameBuffer(level2Width, level2Height).apply {
                    texture.setFilter(Texture.TextureFilter.linear, Texture.TextureFilter.linear)
                    texture.setWrap(Texture.TextureWrap.clampToEdge, Texture.TextureWrap.clampToEdge)
                }
                pingPongC = FrameBuffer(level3Width, level3Height).apply {
                    texture.setFilter(Texture.TextureFilter.linear, Texture.TextureFilter.linear)
                    texture.setWrap(Texture.TextureWrap.clampToEdge, Texture.TextureWrap.clampToEdge)
                }
            } catch (fboError: Throwable) {
                Log.err("[NekoMod] Failed to allocate Dual-Kawase pyramid framebuffers", fboError)
                return null
            }
        }

        val fboA = pingPongA ?: return null
        val fboB = pingPongB ?: return null
        val fboC = pingPongC ?: return null

        ShaderRegistry.ensure()
        val blurShader = ShaderRegistry.blurShader
        if (blurShader == null) {
            Log.warn("[NekoMod] Dual-Kawase blur shader is unavailable.")
            return null
        }

        // --- PURE OPENGL PROGRESSIVE DUAL-KAWASE PYRAMID ---
        val wasBlend = Gl.isEnabled(Gl.blend)
        val wasDepth = Gl.isEnabled(Gl.depthTest)
        if (wasBlend) Gl.disable(Gl.blend)
        if (wasDepth) Gl.disable(Gl.depthTest)
        Gl.depthMask(false)

        blurShader.bind()
        blurShader.setUniformi("u_texture", 0)
        Gl.activeTexture(Gl.texture0)

        val effectiveRadius = blurRadius.coerceIn(0.5f, 2.5f)

        // --- PASS 1: Downsample Screen (1080p) -> Level 1 FBO (960x540) ---
        fboA.begin()
        Gl.viewport(0, 0, level1Width, level1Height)
        captureTexture.bind()
        blurShader.setUniformf("u_texelSize", 1.0f / screenWidth.toFloat(), 1.0f / screenHeight.toFloat())
        blurShader.setUniformf("u_radius", 1.0f)
        blurShader.setUniformf("u_mode", 0.0f) // Downsample
        quadMesh.render(blurShader, Gl.triangles)
        fboA.end()

        // --- PASS 2: Downsample Level 1 (960x540) -> Level 2 FBO (480x270) ---
        fboB.begin()
        Gl.viewport(0, 0, level2Width, level2Height)
        fboA.texture.bind()
        blurShader.setUniformf("u_texelSize", 1.0f / level1Width.toFloat(), 1.0f / level1Height.toFloat())
        blurShader.setUniformf("u_radius", 1.0f)
        blurShader.setUniformf("u_mode", 0.0f) // Downsample
        quadMesh.render(blurShader, Gl.triangles)
        fboB.end()

        // --- PASS 3: Downsample Level 2 (480x270) -> Level 3 FBO (240x135) ---
        fboC.begin()
        Gl.viewport(0, 0, level3Width, level3Height)
        fboB.texture.bind()
        blurShader.setUniformf("u_texelSize", 1.0f / level2Width.toFloat(), 1.0f / level2Height.toFloat())
        blurShader.setUniformf("u_radius", 1.0f)
        blurShader.setUniformf("u_mode", 0.0f) // Downsample
        quadMesh.render(blurShader, Gl.triangles)
        fboC.end()

        // --- PASS 4: Upsample Level 3 (240x135) -> Level 2 FBO (480x270) ---
        fboB.begin()
        Gl.viewport(0, 0, level2Width, level2Height)
        fboC.texture.bind()
        blurShader.setUniformf("u_texelSize", 1.0f / level3Width.toFloat(), 1.0f / level3Height.toFloat())
        blurShader.setUniformf("u_radius", effectiveRadius)
        blurShader.setUniformf("u_mode", 1.0f) // Upsample
        quadMesh.render(blurShader, Gl.triangles)
        fboB.end()

        // --- PASS 5: Upsample Level 2 (480x270) -> Level 1 FBO (960x540) ---
        fboA.begin()
        Gl.viewport(0, 0, level1Width, level1Height)
        fboB.texture.bind()
        blurShader.setUniformf("u_texelSize", 1.0f / level2Width.toFloat(), 1.0f / level2Height.toFloat())
        blurShader.setUniformf("u_radius", effectiveRadius)
        blurShader.setUniformf("u_mode", 1.0f) // Upsample
        quadMesh.render(blurShader, Gl.triangles)
        fboA.end()

        // Restore viewport & OpenGL pipeline states
        Gl.viewport(0, 0, screenWidth, screenHeight)
        if (wasBlend) Gl.enable(Gl.blend)
        if (wasDepth) Gl.enable(Gl.depthTest)
        Gl.depthMask(true)

        lastCapturedFrameId = currentFrameId
        lastCaptureTimestamp = now
        blurredTexture = fboA.texture
        lastBlurDurationMs = (System.nanoTime() - blurStartNanos) / 1_000_000.0f
        return blurredTexture
    }

    // --- SCREEN FRAMEBUFFER CAPTURE ---

    private fun captureScreen(physicalWidth: Int, physicalHeight: Int): Texture? {
        val existing = screenCaptureFbo
        if (existing != null && (existing.width != physicalWidth || existing.height != physicalHeight)) {
            existing.dispose()
            screenCaptureFbo = null
        }
        if (screenCaptureFbo == null) {
            try {
                screenCaptureFbo = FrameBuffer(physicalWidth, physicalHeight).apply {
                    texture.setFilter(Texture.TextureFilter.linear, Texture.TextureFilter.linear)
                    texture.setWrap(Texture.TextureWrap.clampToEdge, Texture.TextureWrap.clampToEdge)
                }
            } catch (captureError: Throwable) {
                Log.err("[NekoMod] Failed to allocate screen capture framebuffer", captureError)
                return null
            }
        }

        val captureStart = System.nanoTime()
        val fbo = screenCaptureFbo ?: return null

        val wasBlend = Gl.isEnabled(Gl.blend)
        Gl.disable(Gl.blend)
        Gl.depthMask(false)
        Gl.bindTexture(Gl.texture2d, fbo.texture.textureObjectHandle)
        Gl.copyTexSubImage2D(Gl.texture2d, 0, 0, 0, 0, 0, physicalWidth, physicalHeight)
        Gl.bindTexture(Gl.texture2d, 0)
        when {
            wasBlend -> Gl.enable(Gl.blend)
            else -> Gl.disable(Gl.blend)
        }
        Gl.depthMask(true)
        lastCaptureDurationMs = (System.nanoTime() - captureStart) / 1_000_000.0f

        return fbo.texture
    }

    // --- DISPOSAL ---

    override fun dispose() {
        screenCaptureFbo?.dispose()
        screenCaptureFbo = null
        disposeScratch()
        try {
            quadMesh.dispose()
        } catch (meshError: Throwable) {
            Log.err("[NekoMod] Error disposing quadMesh", meshError)
        }
    }

    private fun disposeScratch() {
        pingPongA?.dispose()
        pingPongA = null
        pingPongB?.dispose()
        pingPongB = null
        pingPongC?.dispose()
        pingPongC = null
        blurredTexture = null
        lastCapturedFrameId = -1L
        lastCaptureTimestamp = 0L
    }
}
