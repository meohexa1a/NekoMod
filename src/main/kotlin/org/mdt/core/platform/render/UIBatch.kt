// [AGENT INVARIANT] Synchronously update @property, @param, and @see KDocs when modifying this file.

package org.mdt.core.platform.render

import arc.graphics.Gl
import arc.graphics.Mesh
import arc.graphics.Texture
import arc.graphics.VertexAttribute
import arc.graphics.g2d.Draw
import arc.graphics.g2d.Font
import arc.graphics.g2d.GlyphLayout
import arc.graphics.g2d.TextureRegion
import arc.math.Mat
import arc.util.Align
import java.nio.FloatBuffer
import org.mdt.core.platform.PlatformHost
import org.mdt.core.ui.unit.Color

/**
 * ## UIBatch
 *
 * Master GPU UI Batch Renderer.
 * Batches text glyphs, rounded SDF boxes, borders, textures, and frosted glass.
 * Streams 18-float vertex data into a pre-allocated mesh buffer and executes per-pixel analytical scissor clipping in shaders.
 * Flushes transparently when switching texture atlas pages or when buffer capacity is reached.
 *
 * @param hostProvider Non-null provider lambda returning [PlatformHost] for asset texture and shader resolution.
 *
 * @property lastBatchDurationMs Execution time of the latest UIBatch draw call in milliseconds.
 * @property totalQuadsLastFrame Total number of quads rendered in the latest frame.
 *
 * @see ShaderRegistry
 * @see SceneBlur
 * @see FontMeasurer
 * @see org.mdt.core.ui.node.LayoutNode
 * @see org.mdt.core.ui.node.TextNode
 */
class UIBatch(
    private val hostProvider: () -> PlatformHost = { PlatformHost.NoOp },
) {

    val host: PlatformHost
        get() = hostProvider()

    private val shaders by lazy(LazyThreadSafetyMode.NONE) { ShaderRegistry(hostProvider) }

    val blur: SceneBlur
        get() = host.render.blur

    private val textLayoutHelper = GlyphLayout()
    private val arcColorHelper = arc.graphics.Color()

    companion object {
        const val MODE_FONT = 0.0f
        const val MODE_SDF_BOX = 1.0f
        const val MODE_TEXTURE = 2.0f
        const val MODE_GLASS = 3.0f

        private const val MAX_QUADS = 16384
        private const val FLOATS_PER_VERTEX = 18
        private const val FLOATS_PER_QUAD = FLOATS_PER_VERTEX * 4
        private const val MAX_VERTICES = MAX_QUADS * 4
        private const val MAX_INDICES = MAX_QUADS * 6
        private const val MAX_CLIP_DEPTH = 64
    }

    private val quadBuffer = FloatArray(FLOATS_PER_QUAD)
    private val verticesBuffer: FloatBuffer by lazy { mesh.verticesBuffer }
    private var vertexIndex = 0
    private var queuedQuadCount = 0

    private val mesh: Mesh by lazy {
        val attributes = arrayOf(
            VertexAttribute(4, "a_position"),                          // xy = screen pos, zw = uv coords
            VertexAttribute(
                4,
                Gl.unsignedByte,
                true,
                "a_color",
            ),      // rgba = packed ABGR color (unpacked by GL hardware)
            VertexAttribute(4, "a_boxData"),                           // xy = local pos, zw = box dimensions
            VertexAttribute(
                4,
                "a_style",
            ),                             // x = radius, y = borderWidth, z = mode, w = texUnit
            VertexAttribute(4, Gl.unsignedByte, true, "a_borderColor"),// rgba = packed ABGR border color
            VertexAttribute(
                4,
                "a_clipRect",
            ),                           // xy = min(x,y), zw = max(x,y) analytical scissor clip
        )

        val indices = ShortArray(MAX_INDICES)
        var vertexOffset = 0
        for (indexOffset in 0 until MAX_INDICES step 6) {
            indices[indexOffset] = vertexOffset.toShort()
            indices[indexOffset + 1] = (vertexOffset + 1).toShort()
            indices[indexOffset + 2] = (vertexOffset + 2).toShort()
            indices[indexOffset + 3] = (vertexOffset + 2).toShort()
            indices[indexOffset + 4] = (vertexOffset + 3).toShort()
            indices[indexOffset + 5] = vertexOffset.toShort()
            vertexOffset += 4
        }

        Mesh(false, MAX_VERTICES, MAX_INDICES, *attributes).apply {
            setIndices(indices)
        }
    }
    private var isDrawing = false

    private val previousProjection = Mat()
    private var screenWidth = 1920.0f
    private var screenHeight = 1080.0f

    // Analytical Scissor Clip Stack (minX, minY, maxX, maxY)
    private val clipStackBuffer = FloatArray(MAX_CLIP_DEPTH * 4)
    private var clipDepth = 0
    private val currentClip = FloatArray(4)

    private var activeAtlasTexture: Texture? = null
    private var activeBlurTexture: Texture? = null

    /** Execution time of the latest UIBatch draw call in milliseconds. */
    var lastBatchDurationMs: Float = 0.0f
        private set

    /** Total number of quads rendered in the latest frame. */
    var totalQuadsLastFrame: Int = 0
        private set

    private var batchStartNanos: Long = 0L

    // --- FRAME LIFECYCLE ---

    /**
     * Begins the UI rendering pass.
     * Binds the Uber Shader, multi-texture slots, and configures orthographic projection.
     */
    fun begin(width: Float, height: Float) {
        if (isDrawing) return

        isDrawing = true
        screenWidth = if (width > 0.0f) width else 1920.0f
        screenHeight = if (height > 0.0f) height else 1080.0f

        // 1. Flush Arc SpriteBatch FIRST to commit pending game world & prior UI primitives
        Draw.flush()

        // 2. Capture and Blur game background AFTER Arc batch has flushed
        val blurTexture = blur.captureAndBlur()

        val shader = shaders.uberShader

        previousProjection.set(Draw.proj())
        Draw.proj(0.0f, 0.0f, screenWidth, screenHeight)

        // 3. Texture Unit 0: Master Atlas (Contains fonts, icons, sprites, white pixel)
        val atlasTexture = host.assets.resolveWhiteRegion().texture
        activeAtlasTexture = atlasTexture
        if (atlasTexture != null) {
            Gl.activeTexture(Gl.texture0)
            atlasTexture.bind()
        }

        // 4. Texture Unit 2: Pre-pass Blurred Game Background
        val targetBlurTexture = blurTexture ?: atlasTexture
        activeBlurTexture = targetBlurTexture
        if (targetBlurTexture != null) {
            Gl.activeTexture(Gl.texture0 + 2)
            targetBlurTexture.bind()
        }

        Gl.activeTexture(Gl.texture0)

        // 5. Bind Uber Shader & Set Uniforms
        shader.bind()
        shader.setUniformMatrix4("u_projTrans", Draw.proj())
        shader.setUniformf("u_screenSize", screenWidth, screenHeight)
        shader.setUniformf("u_hasBlur", if (blurTexture != null) 1.0f else 0.0f)
        shader.setUniformi("u_atlas", 0)
        shader.setUniformi("u_gameBlur", 2)

        Gl.depthMask(false)
        Gl.enable(Gl.blend)
        Gl.blendFunc(Gl.srcAlpha, Gl.oneMinusSrcAlpha)

        vertexIndex = 0
        queuedQuadCount = 0
        verticesBuffer.position(0)
        verticesBuffer.limit(verticesBuffer.capacity())
        clipDepth = 0
        currentClip[0] = 0.0f
        currentClip[1] = 0.0f
        currentClip[2] = 100000.0f
        currentClip[3] = 100000.0f
        batchStartNanos = System.nanoTime()
    }

    /**
     * Ends the UI rendering pass, flushes all remaining quads, and restores GPU states.
     */
    fun end() {
        if (!isDrawing) return

        totalQuadsLastFrame = queuedQuadCount
        flush()

        Draw.proj(previousProjection)
        Gl.activeTexture(Gl.texture0)
        isDrawing = false
        lastBatchDurationMs = (System.nanoTime() - batchStartNanos) / 1_000_000.0f
    }

    // --- DRAWING PRIMITIVES ---

    /**
     * Renders a universal UI box primitive (Background, Rounded SDF Box, Border, Image, or Glass).
     */
    fun drawBox(
        x: Float,
        y: Float,
        width: Float,
        height: Float,
        region: TextureRegion? = null,
        radius: Float = 0.0f,
        color: Color = Color.White,
        borderWidth: Float = 0.0f,
        borderColor: Color = Color.Clear,
        isGlass: Boolean = false,
    ) {
        if (width <= 0.001f || height <= 0.001f) return

        val whiteRegion = host.assets.resolveWhiteRegion()
        val texture = region?.texture ?: whiteRegion.texture
        if (texture != null && (activeAtlasTexture == null || activeAtlasTexture != texture)) {
            flush()
            activeAtlasTexture = texture
            Gl.activeTexture(Gl.texture0)
            texture.bind()
        }

        if (queuedQuadCount >= MAX_QUADS) {
            flush()
        }

        val targetRegion = region ?: whiteRegion

        val uvMinU = targetRegion.u
        val uvMinV = targetRegion.v2
        val uvMaxU = targetRegion.u2
        val uvMaxV = targetRegion.v

        val mode = when {
            isGlass -> MODE_GLASS
            radius > 0.001f || borderWidth > 0.001f -> MODE_SDF_BOX
            else -> MODE_TEXTURE
        }
        val textureUnit = if (isGlass) 2.0f else 0.0f

        val leftX = x
        val bottomY = y
        val rightX = x + width
        val topY = y + height

        val packedColor = color.toGLPackedFloat()
        val packedBorderColor = borderColor.toGLPackedFloat()
        val clipMinX = currentClip[0]
        val clipMinY = currentClip[1]
        val clipMaxX = currentClip[2]
        val clipMaxY = currentClip[3]

        var offset = 0

        // Vertex 0: Bottom-Left (leftX, bottomY)
        quadBuffer[offset++] = leftX; quadBuffer[offset++] = bottomY; quadBuffer[offset++] =
            uvMinU; quadBuffer[offset++] = uvMinV
        quadBuffer[offset++] = packedColor
        quadBuffer[offset++] = 0.0f; quadBuffer[offset++] = 0.0f; quadBuffer[offset++] = width; quadBuffer[offset++] =
            height
        quadBuffer[offset++] = radius; quadBuffer[offset++] = borderWidth; quadBuffer[offset++] =
            mode; quadBuffer[offset++] = textureUnit
        quadBuffer[offset++] = packedBorderColor
        quadBuffer[offset++] = clipMinX; quadBuffer[offset++] = clipMinY; quadBuffer[offset++] =
            clipMaxX; quadBuffer[offset++] = clipMaxY

        // Vertex 1: Top-Left (leftX, topY)
        quadBuffer[offset++] = leftX; quadBuffer[offset++] = topY; quadBuffer[offset++] = uvMinU; quadBuffer[offset++] =
            uvMaxV
        quadBuffer[offset++] = packedColor
        quadBuffer[offset++] = 0.0f; quadBuffer[offset++] = height; quadBuffer[offset++] = width; quadBuffer[offset++] =
            height
        quadBuffer[offset++] = radius; quadBuffer[offset++] = borderWidth; quadBuffer[offset++] =
            mode; quadBuffer[offset++] = textureUnit
        quadBuffer[offset++] = packedBorderColor
        quadBuffer[offset++] = clipMinX; quadBuffer[offset++] = clipMinY; quadBuffer[offset++] =
            clipMaxX; quadBuffer[offset++] = clipMaxY

        // Vertex 2: Top-Right (rightX, topY)
        quadBuffer[offset++] = rightX; quadBuffer[offset++] = topY; quadBuffer[offset++] =
            uvMaxU; quadBuffer[offset++] = uvMaxV
        quadBuffer[offset++] = packedColor
        quadBuffer[offset++] = width; quadBuffer[offset++] = height; quadBuffer[offset++] =
            width; quadBuffer[offset++] = height
        quadBuffer[offset++] = radius; quadBuffer[offset++] = borderWidth; quadBuffer[offset++] =
            mode; quadBuffer[offset++] = textureUnit
        quadBuffer[offset++] = packedBorderColor
        quadBuffer[offset++] = clipMinX; quadBuffer[offset++] = clipMinY; quadBuffer[offset++] =
            clipMaxX; quadBuffer[offset++] = clipMaxY

        // Vertex 3: Bottom-Right (rightX, bottomY)
        quadBuffer[offset++] = rightX; quadBuffer[offset++] = bottomY; quadBuffer[offset++] =
            uvMaxU; quadBuffer[offset++] = uvMinV
        quadBuffer[offset++] = packedColor
        quadBuffer[offset++] = width; quadBuffer[offset++] = 0.0f; quadBuffer[offset++] = width; quadBuffer[offset++] =
            height
        quadBuffer[offset++] = radius; quadBuffer[offset++] = borderWidth; quadBuffer[offset++] =
            mode; quadBuffer[offset++] = textureUnit
        quadBuffer[offset++] = packedBorderColor
        quadBuffer[offset++] = clipMinX; quadBuffer[offset++] = clipMinY; quadBuffer[offset++] =
            clipMaxX; quadBuffer[offset++] = clipMaxY

        verticesBuffer.position(vertexIndex)
        verticesBuffer.put(quadBuffer, 0, FLOATS_PER_QUAD)
        vertexIndex += FLOATS_PER_QUAD
        queuedQuadCount++
    }

    /**
     * Renders a single BMFont character glyph (Mode = 0, Texture Unit = 0).
     */
    fun drawGlyph(
        x: Float,
        y: Float,
        width: Float,
        height: Float,
        uvMinU: Float,
        uvMinV: Float,
        uvMaxU: Float,
        uvMaxV: Float,
        color: Color,
        fontTexture: Texture? = null,
    ) {
        if (width <= 0.001f || height <= 0.001f) return

        val texture = fontTexture ?: host.assets.resolveWhiteRegion().texture
        if (texture != null && (activeAtlasTexture == null || activeAtlasTexture != texture)) {
            flush()
            activeAtlasTexture = texture
            Gl.activeTexture(Gl.texture0)
            texture.bind()
        }

        if (queuedQuadCount >= MAX_QUADS) {
            flush()
        }

        val leftX = x
        val bottomY = y
        val rightX = x + width
        val topY = y + height

        val packedColor = color.toGLPackedFloat()
        val packedBorderColor = Color.Clear.toGLPackedFloat()
        val clipMinX = currentClip[0]
        val clipMinY = currentClip[1]
        val clipMaxX = currentClip[2]
        val clipMaxY = currentClip[3]

        var offset = 0

        // Vertex 0: Bottom-Left (leftX, bottomY) -> (uvMinU, uvMinV)
        quadBuffer[offset++] = leftX; quadBuffer[offset++] = bottomY; quadBuffer[offset++] =
            uvMinU; quadBuffer[offset++] = uvMinV
        quadBuffer[offset++] = packedColor
        quadBuffer[offset++] = 0.0f; quadBuffer[offset++] = 0.0f; quadBuffer[offset++] = width; quadBuffer[offset++] =
            height
        quadBuffer[offset++] = 0.0f; quadBuffer[offset++] = 0.0f; quadBuffer[offset++] =
            MODE_FONT; quadBuffer[offset++] = 0.0f
        quadBuffer[offset++] = packedBorderColor
        quadBuffer[offset++] = clipMinX; quadBuffer[offset++] = clipMinY; quadBuffer[offset++] =
            clipMaxX; quadBuffer[offset++] = clipMaxY

        // Vertex 1: Top-Left (leftX, topY) -> (uvMinU, uvMaxV)
        quadBuffer[offset++] = leftX; quadBuffer[offset++] = topY; quadBuffer[offset++] = uvMinU; quadBuffer[offset++] =
            uvMaxV
        quadBuffer[offset++] = packedColor
        quadBuffer[offset++] = 0.0f; quadBuffer[offset++] = height; quadBuffer[offset++] = width; quadBuffer[offset++] =
            height
        quadBuffer[offset++] = 0.0f; quadBuffer[offset++] = 0.0f; quadBuffer[offset++] =
            MODE_FONT; quadBuffer[offset++] = 0.0f
        quadBuffer[offset++] = packedBorderColor
        quadBuffer[offset++] = clipMinX; quadBuffer[offset++] = clipMinY; quadBuffer[offset++] =
            clipMaxX; quadBuffer[offset++] = clipMaxY

        // Vertex 2: Top-Right (rightX, topY) -> (uvMaxU, uvMaxV)
        quadBuffer[offset++] = rightX; quadBuffer[offset++] = topY; quadBuffer[offset++] =
            uvMaxU; quadBuffer[offset++] = uvMaxV
        quadBuffer[offset++] = packedColor
        quadBuffer[offset++] = width; quadBuffer[offset++] = height; quadBuffer[offset++] =
            width; quadBuffer[offset++] = height
        quadBuffer[offset++] = 0.0f; quadBuffer[offset++] = 0.0f; quadBuffer[offset++] =
            MODE_FONT; quadBuffer[offset++] = 0.0f
        quadBuffer[offset++] = packedBorderColor
        quadBuffer[offset++] = clipMinX; quadBuffer[offset++] = clipMinY; quadBuffer[offset++] =
            clipMaxX; quadBuffer[offset++] = clipMaxY

        // Vertex 3: Bottom-Right (rightX, bottomY) -> (uvMaxU, uvMinV)
        quadBuffer[offset++] = rightX; quadBuffer[offset++] = bottomY; quadBuffer[offset++] =
            uvMaxU; quadBuffer[offset++] = uvMinV
        quadBuffer[offset++] = packedColor
        quadBuffer[offset++] = width; quadBuffer[offset++] = 0.0f; quadBuffer[offset++] = width; quadBuffer[offset++] =
            height
        quadBuffer[offset++] = 0.0f; quadBuffer[offset++] = 0.0f; quadBuffer[offset++] =
            MODE_FONT; quadBuffer[offset++] = 0.0f
        quadBuffer[offset++] = packedBorderColor
        quadBuffer[offset++] = clipMinX; quadBuffer[offset++] = clipMinY; quadBuffer[offset++] =
            clipMaxX; quadBuffer[offset++] = clipMaxY

        verticesBuffer.position(vertexIndex)
        verticesBuffer.put(quadBuffer, 0, FLOATS_PER_QUAD)
        vertexIndex += FLOATS_PER_QUAD
        queuedQuadCount++
    }

    /**
     * Draws [text] directly into the batch with optional [ellipsis] truncation or [wrap].
     */
    fun drawText(
        font: Font,
        text: CharSequence,
        x: Float,
        y: Float,
        targetWidth: Float = 0.0f,
        align: Int = Align.left,
        wrap: Boolean = false,
        ellipsis: Boolean = false,
        color: Color = Color.White,
    ) {
        if (text.isEmpty()) return

        val textToRender = when {
            ellipsis && !wrap && targetWidth > 0.0f -> host.render.fontMeasurer.truncateWithEllipsis(
                font,
                text.toString(),
                targetWidth,
            )

            else -> text
        }

        textLayoutHelper.setText(font, textToRender, color.toArcColor(arcColorHelper), targetWidth, align, wrap)

        val scaleX = font.data.scaleX
        val scaleY = font.data.scaleY
        val runs = textLayoutHelper.runs
        val runCount = runs.size

        for (r in 0 until runCount) {
            val run = runs.get(r)
            val glyphs = run.glyphs
            val xAdvances = run.xAdvances
            var currentX = x + run.x
            val currentY = y + run.y
            val runColor = Color.fromArc(run.color)
            val glyphCount = glyphs.size

            for (i in 0 until glyphCount) {
                val glyph = glyphs.get(i)
                currentX += xAdvances.get(i)

                val drawX = currentX + glyph.xoffset * scaleX
                val drawY = currentY + glyph.yoffset * scaleY
                val glyphWidth = glyph.width * scaleX
                val glyphHeight = glyph.height * scaleY
                val fontRegion = when {
                    glyph.page < font.regions.size -> font.regions.get(glyph.page)
                    else -> font.regions.first()
                }
                val fontTexture = fontRegion.texture

                drawGlyph(
                    x = drawX,
                    y = drawY,
                    width = glyphWidth,
                    height = glyphHeight,
                    uvMinU = glyph.u,
                    uvMinV = glyph.v,
                    uvMaxU = glyph.u2,
                    uvMaxV = glyph.v2,
                    color = runColor,
                    fontTexture = fontTexture,
                )
            }
        }
    }

    // --- ANALYTICAL SCISSOR CLIPPING ---

    /**
     * Pushes a new clipping rectangle onto the analytical scissor stack without breaking the GPU batch.
     */
    fun pushClip(x: Float, y: Float, width: Float, height: Float) {
        val offset = clipDepth * 4
        if (offset + 4 <= clipStackBuffer.size) {
            clipStackBuffer[offset] = currentClip[0]
            clipStackBuffer[offset + 1] = currentClip[1]
            clipStackBuffer[offset + 2] = currentClip[2]
            clipStackBuffer[offset + 3] = currentClip[3]
            clipDepth++
        }

        val minX = maxOf(currentClip[0], x)
        val minY = maxOf(currentClip[1], y)
        val maxX = minOf(currentClip[2], x + width)
        val maxY = minOf(currentClip[3], y + height)

        currentClip[0] = minX
        currentClip[1] = minY
        currentClip[2] = maxOf(minX, maxX)
        currentClip[3] = maxOf(minY, maxY)
    }

    /**
     * Pops the topmost clipping rectangle and restores the parent clip boundary without breaking the GPU batch.
     */
    fun popClip() {
        if (clipDepth > 0) {
            clipDepth--
            val offset = clipDepth * 4
            currentClip[0] = clipStackBuffer[offset]
            currentClip[1] = clipStackBuffer[offset + 1]
            currentClip[2] = clipStackBuffer[offset + 2]
            currentClip[3] = clipStackBuffer[offset + 3]
        } else {
            currentClip[0] = 0.0f
            currentClip[1] = 0.0f
            currentClip[2] = 100000.0f
            currentClip[3] = 100000.0f
        }
    }

    // --- GPU EXECUTION & FLUSH ---

    /**
     * Flushes all queued UI quad vertices to the GPU in a single draw call.
     */
    fun flush() {
        if (queuedQuadCount == 0) return

        val shader = shaders.uberShader ?: return

        mesh.verticesBuffer
        verticesBuffer.position(0)
        verticesBuffer.limit(vertexIndex)

        mesh.render(shader, Gl.triangles, 0, queuedQuadCount * 6)

        verticesBuffer.limit(verticesBuffer.capacity())
        verticesBuffer.position(0)
        vertexIndex = 0
        queuedQuadCount = 0
    }

    // --- DISPOSAL ---

    fun dispose() {
        mesh.dispose()
        blur.dispose()
        shaders.dispose()
        isDrawing = false
    }
}
