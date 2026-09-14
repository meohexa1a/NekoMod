package org.hubdustry.core.graphics

import arc.Core
import arc.graphics.Color
import arc.graphics.Gl
import arc.graphics.Mesh
import arc.graphics.Texture
import arc.graphics.VertexAttribute
import arc.graphics.g2d.Draw
import arc.graphics.g2d.Font
import arc.graphics.g2d.GlyphLayout
import arc.graphics.g2d.TextureRegion
import arc.util.Align
import arc.util.Disposable

/**
 * ## UIBatch
 *
 * Master GPU UI Batch Renderer cho NekoMod v3.
 *
 * TÍNH NĂNG CỐT LÕI:
 * 1. **2 Chế Độ Tinh Gọn (MODE_TEXT & MODE_BOX)**:
 *    - `MODE_TEXT` (0.0f): Vẽ ký tự BMFont siêu nhẹ từ `u_atlas`.
 *    - `MODE_BOX` (1.0f): Vẽ toàn bộ các loại hộp (Nền trơn, Nút bấm, Thẻ bo góc, Ảnh/Icon bo góc, Viền).
 * 2. **Cắt Gọt Giải Tích Trên Từng Đỉnh (Analytical Scissor Clipping `a_clipRect`)**:
 *    Mỗi đỉnh mang theo hình chữ nhật cắt [minX, minY, maxX, maxY]. Fragment shader tự động discard
 *    pixel ngoài biên mà không cần gọi `Gl.scissor`, không gây vỡ batch khi lồng container/scrollpane.
 * 3. **Hot-Path Zero-GC**:
 *    Bộ đệm mảng phẳng, quản lý clip qua 4 trường primitive không cấp phát Heap, cú pháp Kotlin idiomatic.
 * 4. **An Toàn Tuyệt Đối Trong Headless Testing**:
 *    Khi `Core.gl == null`, renderer tự động ghi nhận quads vào buffer để phục vụ kiểm thử đơn vị
 *    mà không quăng lỗi hay gọi OpenGL native.
 */
object UIBatch : Disposable {

    const val MODE_TEXT = 0.0f
    const val MODE_BOX = 1.0f

    const val MAX_QUADS = 4096
    const val FLOATS_PER_VERTEX = 22
    const val FLOATS_PER_QUAD = FLOATS_PER_VERTEX * 4
    const val MAX_VERTICES = MAX_QUADS * 4
    const val MAX_INDICES = MAX_QUADS * 6
    const val MAX_CLIP_DEPTH = 64

    val vertexBuffer = FloatArray(MAX_QUADS * FLOATS_PER_QUAD)
    var vertexIndex: Int = 0
        internal set
    var queuedQuadCount: Int = 0
        internal set

    private var mesh: Mesh? = null
    private var isMeshInitialized = false
    private var isDrawing = false

    // Analytical Scissor Clip Stack (4 primitives per depth level)
    private val clipStackBuffer = FloatArray(MAX_CLIP_DEPTH * 4)
    var clipDepth: Int = 0
        internal set

    var clipMinX: Float = -100000.0f
        internal set
    var clipMinY: Float = -100000.0f
        internal set
    var clipMaxX: Float = 100000.0f
        internal set
    var clipMaxY: Float = 100000.0f
        internal set

    private var activeAtlasTexture: Texture? = null
    private val textLayoutHelper by lazy(LazyThreadSafetyMode.NONE) { GlyphLayout() }

    val isSupported: Boolean
        get() = try {
            Core.gl != null && Core.graphics != null && Core.atlas != null && UberShader.getOrCreate() != null
        } catch (_: Throwable) {
            false
        }

    private fun ensureMesh(): Boolean {
        if (isMeshInitialized && mesh != null) return true
        if (Core.gl == null || Core.graphics == null) return false

        return try {
            val attributes = arrayOf(
                VertexAttribute(4, "a_position"),                           // xy = screen pos, zw = uv coords
                VertexAttribute(4, Gl.unsignedByte, true, "a_color"),       // rgba = packed ABGR color
                VertexAttribute(4, "a_boxData"),                            // xy = local pos, zw = box dimensions
                VertexAttribute(4, "a_style"),                              // x = radius, y = borderWidth, z = mode, w = unused
                VertexAttribute(4, "a_cornerRadii"),                        // x = topStart, y = topEnd, z = bottomEnd, w = bottomStart
                VertexAttribute(4, Gl.unsignedByte, true, "a_borderColor"), // rgba = packed ABGR border color
                VertexAttribute(4, "a_clipRect")                            // xy = min(x,y), zw = max(x,y) analytical scissor clip
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

            mesh = Mesh(false, MAX_VERTICES, MAX_INDICES, *attributes).apply {
                setIndices(indices)
            }
            isMeshInitialized = true
            true
        } catch (_: Throwable) {
            false
        }
    }

    // --- FRAME LIFECYCLE ---

    /**
     * Bắt đầu phiên vẽ UI.
     * Cấu hình shader, ma trận biến đổi và khởi tạo trạng thái scissor clipping.
     */
    fun begin() {
        if (isDrawing) return
        isDrawing = true

        setupGpuPipeline()

        vertexIndex = 0
        queuedQuadCount = 0
        clipDepth = 0
        clipMinX = -100000.0f
        clipMinY = -100000.0f
        clipMaxX = 100000.0f
        clipMaxY = 100000.0f
    }

    private fun setupGpuPipeline() {
        if (!isSupported || !ensureMesh()) return
        try {
            Draw.flush()

            val shader = UberShader.getOrCreate() ?: return
            val atlasTexture = Core.atlas?.white()?.texture
            activeAtlasTexture = atlasTexture
            if (atlasTexture != null) {
                Gl.activeTexture(Gl.texture0)
                atlasTexture.bind()
            }

            shader.bind()
            shader.applyProjection(Draw.proj())

            Gl.depthMask(false)
            Gl.enable(Gl.blend)
            Gl.blendFunc(Gl.srcAlpha, Gl.oneMinusSrcAlpha)
        } catch (_: Throwable) {
        }
    }

    /**
     * Kết thúc phiên vẽ UI, submit toàn bộ quads còn lại lên GPU và dọn dẹp trạng thái.
     */
    fun end() {
        if (!isDrawing) return
        flush()
        if (isSupported) {
            try {
                Gl.activeTexture(Gl.texture0)
            } catch (_: Throwable) {
            }
        }
        isDrawing = false
    }

    // --- ANALYTICAL SCISSOR CLIPPING ---

    /**
     * Đẩy một vùng cắt gọt mới lên ngăn xếp và giao với vùng cắt hiện tại.
     * Hoàn toàn thuần toán học, không gọi lệnh OpenGL nào làm đứt batch.
     */
    fun pushClip(x: Float, y: Float, width: Float, height: Float) {
        val offset = clipDepth * 4
        if (offset + 4 > clipStackBuffer.size) {
            arc.util.Log.err("[UIBatch] Clip stack overflow! Maximum depth $MAX_CLIP_DEPTH exceeded.")
            clipDepth++
            return
        }

        clipStackBuffer[offset] = clipMinX
        clipStackBuffer[offset + 1] = clipMinY
        clipStackBuffer[offset + 2] = clipMaxX
        clipStackBuffer[offset + 3] = clipMaxY

        val minX = maxOf(clipMinX, x)
        val minY = maxOf(clipMinY, y)
        val maxX = minOf(clipMaxX, x + width)
        val maxY = minOf(clipMaxY, y + height)

        clipMinX = minX
        clipMinY = minY
        clipMaxX = maxOf(minX, maxX)
        clipMaxY = maxOf(minY, maxY)
        clipDepth++
    }

    /**
     * Khôi phục vùng cắt gọt của container cha từ ngăn xếp.
     */
    fun popClip() {
        if (clipDepth <= 0) {
            clipMinX = -100000.0f
            clipMinY = -100000.0f
            clipMaxX = 100000.0f
            clipMaxY = 100000.0f
            return
        }

        clipDepth--
        if (clipDepth >= MAX_CLIP_DEPTH) return

        val offset = clipDepth * 4
        clipMinX = clipStackBuffer[offset]
        clipMinY = clipStackBuffer[offset + 1]
        clipMaxX = clipStackBuffer[offset + 2]
        clipMaxY = clipStackBuffer[offset + 3]
    }

    // --- DRAWING PRIMITIVES ---

    /**
     * Dựng hình Box (Background, Thẻ bo góc SDF, Viền nổi, hoặc Ảnh/Icon/Avatar bo góc).
     * [x], [y] là tọa độ Bottom-Left của Arc Scene2D.
     */
    fun drawBox(
        x: Float,
        y: Float,
        width: Float,
        height: Float,
        region: TextureRegion? = null,
        radiusTopStart: Float = 0.0f,
        radiusTopEnd: Float = 0.0f,
        radiusBottomEnd: Float = 0.0f,
        radiusBottomStart: Float = 0.0f,
        color: Color = Color.white,
        borderWidth: Float = 0.0f,
        borderColor: Color = Color.clear
    ) {
        if (width <= 0.001f || height <= 0.001f) return

        val whiteRegion = Core.atlas?.white()
        val texture = region?.texture ?: whiteRegion?.texture
        if (texture != null && activeAtlasTexture != null && activeAtlasTexture != texture) {
            flush()
            activeAtlasTexture = texture
            if (isSupported) {
                try {
                    Gl.activeTexture(Gl.texture0)
                    texture.bind()
                } catch (_: Throwable) {
                }
            }
        }

        if (queuedQuadCount >= MAX_QUADS) {
            flush()
        }

        val targetRegion = region ?: whiteRegion
        val uvMinU = targetRegion?.u ?: 0f
        val uvMinV = targetRegion?.v2 ?: 1f
        val uvMaxU = targetRegion?.u2 ?: 1f
        val uvMaxV = targetRegion?.v ?: 0f

        val leftX = x
        val bottomY = y
        val rightX = x + width
        val topY = y + height

        val packedColor = color.toFloatBits()
        val packedBorderColor = borderColor.toFloatBits()

        var offset = vertexIndex

        // Vertex 0: Bottom-Left (leftX, bottomY)
        vertexBuffer[offset++] = leftX; vertexBuffer[offset++] = bottomY; vertexBuffer[offset++] = uvMinU; vertexBuffer[offset++] = uvMinV
        vertexBuffer[offset++] = packedColor
        vertexBuffer[offset++] = 0.0f; vertexBuffer[offset++] = 0.0f; vertexBuffer[offset++] = width; vertexBuffer[offset++] = height
        vertexBuffer[offset++] = radiusTopStart; vertexBuffer[offset++] = borderWidth; vertexBuffer[offset++] = MODE_BOX; vertexBuffer[offset++] = 0.0f
        vertexBuffer[offset++] = radiusTopStart; vertexBuffer[offset++] = radiusTopEnd; vertexBuffer[offset++] = radiusBottomEnd; vertexBuffer[offset++] = radiusBottomStart
        vertexBuffer[offset++] = packedBorderColor
        vertexBuffer[offset++] = clipMinX; vertexBuffer[offset++] = clipMinY; vertexBuffer[offset++] = clipMaxX; vertexBuffer[offset++] = clipMaxY

        // Vertex 1: Top-Left (leftX, topY)
        vertexBuffer[offset++] = leftX; vertexBuffer[offset++] = topY; vertexBuffer[offset++] = uvMinU; vertexBuffer[offset++] = uvMaxV
        vertexBuffer[offset++] = packedColor
        vertexBuffer[offset++] = 0.0f; vertexBuffer[offset++] = height; vertexBuffer[offset++] = width; vertexBuffer[offset++] = height
        vertexBuffer[offset++] = radiusTopStart; vertexBuffer[offset++] = borderWidth; vertexBuffer[offset++] = MODE_BOX; vertexBuffer[offset++] = 0.0f
        vertexBuffer[offset++] = radiusTopStart; vertexBuffer[offset++] = radiusTopEnd; vertexBuffer[offset++] = radiusBottomEnd; vertexBuffer[offset++] = radiusBottomStart
        vertexBuffer[offset++] = packedBorderColor
        vertexBuffer[offset++] = clipMinX; vertexBuffer[offset++] = clipMinY; vertexBuffer[offset++] = clipMaxX; vertexBuffer[offset++] = clipMaxY

        // Vertex 2: Top-Right (rightX, topY)
        vertexBuffer[offset++] = rightX; vertexBuffer[offset++] = topY; vertexBuffer[offset++] = uvMaxU; vertexBuffer[offset++] = uvMaxV
        vertexBuffer[offset++] = packedColor
        vertexBuffer[offset++] = width; vertexBuffer[offset++] = height; vertexBuffer[offset++] = width; vertexBuffer[offset++] = height
        vertexBuffer[offset++] = radiusTopStart; vertexBuffer[offset++] = borderWidth; vertexBuffer[offset++] = MODE_BOX; vertexBuffer[offset++] = 0.0f
        vertexBuffer[offset++] = radiusTopStart; vertexBuffer[offset++] = radiusTopEnd; vertexBuffer[offset++] = radiusBottomEnd; vertexBuffer[offset++] = radiusBottomStart
        vertexBuffer[offset++] = packedBorderColor
        vertexBuffer[offset++] = clipMinX; vertexBuffer[offset++] = clipMinY; vertexBuffer[offset++] = clipMaxX; vertexBuffer[offset++] = clipMaxY

        // Vertex 3: Bottom-Right (rightX, bottomY)
        vertexBuffer[offset++] = rightX; vertexBuffer[offset++] = bottomY; vertexBuffer[offset++] = uvMaxU; vertexBuffer[offset++] = uvMinV
        vertexBuffer[offset++] = packedColor
        vertexBuffer[offset++] = width; vertexBuffer[offset++] = 0.0f; vertexBuffer[offset++] = width; vertexBuffer[offset++] = height
        vertexBuffer[offset++] = radiusTopStart; vertexBuffer[offset++] = borderWidth; vertexBuffer[offset++] = MODE_BOX; vertexBuffer[offset++] = 0.0f
        vertexBuffer[offset++] = radiusTopStart; vertexBuffer[offset++] = radiusTopEnd; vertexBuffer[offset++] = radiusBottomEnd; vertexBuffer[offset++] = radiusBottomStart
        vertexBuffer[offset++] = packedBorderColor
        vertexBuffer[offset++] = clipMinX; vertexBuffer[offset++] = clipMinY; vertexBuffer[offset++] = clipMaxX; vertexBuffer[offset++] = clipMaxY

        vertexIndex = offset
        queuedQuadCount++
    }

    /**
     * Overload tiện lợi dựng hình hộp chữ nhật bo 4 góc đồng đều với cùng [radius].
     */
    fun drawBox(
        x: Float,
        y: Float,
        width: Float,
        height: Float,
        radius: Float = 0.0f,
        region: TextureRegion? = null,
        color: Color = Color.white,
        borderWidth: Float = 0.0f,
        borderColor: Color = Color.clear
    ) = drawBox(
        x = x,
        y = y,
        width = width,
        height = height,
        region = region,
        radiusTopStart = radius,
        radiusTopEnd = radius,
        radiusBottomEnd = radius,
        radiusBottomStart = radius,
        color = color,
        borderWidth = borderWidth,
        borderColor = borderColor
    )

    /**
     * Dựng hình một ký tự đơn BMFont (Mode = MODE_TEXT).
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
        fontTexture: Texture? = null
    ) {
        if (width <= 0.001f || height <= 0.001f) return

        val texture = fontTexture ?: Core.atlas?.white()?.texture
        if (texture != null && activeAtlasTexture != null && activeAtlasTexture != texture) {
            flush()
            activeAtlasTexture = texture
            if (isSupported) {
                try {
                    Gl.activeTexture(Gl.texture0)
                    texture.bind()
                } catch (_: Throwable) {
                }
            }
        }

        if (queuedQuadCount >= MAX_QUADS) {
            flush()
        }

        val leftX = x
        val bottomY = y
        val rightX = x + width
        val topY = y + height

        val packedColor = color.toFloatBits()
        val packedBorderColor = Color.clear.toFloatBits()

        var offset = vertexIndex

        // Vertex 0: Bottom-Left (leftX, bottomY)
        vertexBuffer[offset++] = leftX; vertexBuffer[offset++] = bottomY; vertexBuffer[offset++] = uvMinU; vertexBuffer[offset++] = uvMinV
        vertexBuffer[offset++] = packedColor
        vertexBuffer[offset++] = 0.0f; vertexBuffer[offset++] = 0.0f; vertexBuffer[offset++] = width; vertexBuffer[offset++] = height
        vertexBuffer[offset++] = 0.0f; vertexBuffer[offset++] = 0.0f; vertexBuffer[offset++] = MODE_TEXT; vertexBuffer[offset++] = 0.0f
        vertexBuffer[offset++] = 0.0f; vertexBuffer[offset++] = 0.0f; vertexBuffer[offset++] = 0.0f; vertexBuffer[offset++] = 0.0f
        vertexBuffer[offset++] = packedBorderColor
        vertexBuffer[offset++] = clipMinX; vertexBuffer[offset++] = clipMinY; vertexBuffer[offset++] = clipMaxX; vertexBuffer[offset++] = clipMaxY

        // Vertex 1: Top-Left (leftX, topY)
        vertexBuffer[offset++] = leftX; vertexBuffer[offset++] = topY; vertexBuffer[offset++] = uvMinU; vertexBuffer[offset++] = uvMaxV
        vertexBuffer[offset++] = packedColor
        vertexBuffer[offset++] = 0.0f; vertexBuffer[offset++] = height; vertexBuffer[offset++] = width; vertexBuffer[offset++] = height
        vertexBuffer[offset++] = 0.0f; vertexBuffer[offset++] = 0.0f; vertexBuffer[offset++] = MODE_TEXT; vertexBuffer[offset++] = 0.0f
        vertexBuffer[offset++] = 0.0f; vertexBuffer[offset++] = 0.0f; vertexBuffer[offset++] = 0.0f; vertexBuffer[offset++] = 0.0f
        vertexBuffer[offset++] = packedBorderColor
        vertexBuffer[offset++] = clipMinX; vertexBuffer[offset++] = clipMinY; vertexBuffer[offset++] = clipMaxX; vertexBuffer[offset++] = clipMaxY

        // Vertex 2: Top-Right (rightX, topY)
        vertexBuffer[offset++] = rightX; vertexBuffer[offset++] = topY; vertexBuffer[offset++] = uvMaxU; vertexBuffer[offset++] = uvMaxV
        vertexBuffer[offset++] = packedColor
        vertexBuffer[offset++] = width; vertexBuffer[offset++] = height; vertexBuffer[offset++] = width; vertexBuffer[offset++] = height
        vertexBuffer[offset++] = 0.0f; vertexBuffer[offset++] = 0.0f; vertexBuffer[offset++] = MODE_TEXT; vertexBuffer[offset++] = 0.0f
        vertexBuffer[offset++] = 0.0f; vertexBuffer[offset++] = 0.0f; vertexBuffer[offset++] = 0.0f; vertexBuffer[offset++] = 0.0f
        vertexBuffer[offset++] = packedBorderColor
        vertexBuffer[offset++] = clipMinX; vertexBuffer[offset++] = clipMinY; vertexBuffer[offset++] = clipMaxX; vertexBuffer[offset++] = clipMaxY

        // Vertex 3: Bottom-Right (rightX, bottomY)
        vertexBuffer[offset++] = rightX; vertexBuffer[offset++] = bottomY; vertexBuffer[offset++] = uvMaxU; vertexBuffer[offset++] = uvMinV
        vertexBuffer[offset++] = packedColor
        vertexBuffer[offset++] = width; vertexBuffer[offset++] = 0.0f; vertexBuffer[offset++] = width; vertexBuffer[offset++] = height
        vertexBuffer[offset++] = 0.0f; vertexBuffer[offset++] = 0.0f; vertexBuffer[offset++] = MODE_TEXT; vertexBuffer[offset++] = 0.0f
        vertexBuffer[offset++] = 0.0f; vertexBuffer[offset++] = 0.0f; vertexBuffer[offset++] = 0.0f; vertexBuffer[offset++] = 0.0f
        vertexBuffer[offset++] = packedBorderColor
        vertexBuffer[offset++] = clipMinX; vertexBuffer[offset++] = clipMinY; vertexBuffer[offset++] = clipMaxX; vertexBuffer[offset++] = clipMaxY

        vertexIndex = offset
        queuedQuadCount++
    }

    /**
     * Dựng hình văn bản BMFont trực tiếp vào stream quads đạt chuẩn Hot-Path Zero-GC.
     */
    fun drawText(
        font: Font,
        text: CharSequence,
        x: Float,
        y: Float,
        targetWidth: Float = 0.0f,
        align: Int = Align.left,
        wrap: Boolean = false,
        color: Color = Color.white
    ) {
        if (text.isEmpty()) return

        try {
            textLayoutHelper.setText(font, text, color, targetWidth, align, wrap)

            val scaleX = font.data.scaleX
            val scaleY = font.data.scaleY
            val runs = textLayoutHelper.runs
            val runCount = runs.size

            for (r in 0 until runCount) {
                val run = runs[r]
                val glyphs = run.glyphs
                val xAdvances = run.xAdvances
                var currentX = x + run.x
                val currentY = y + run.y
                val runColor = run.color
                val glyphCount = glyphs.size

                for (i in 0 until glyphCount) {
                    val glyph = glyphs[i]
                    currentX += xAdvances[i]

                    val drawX = currentX + glyph.xoffset * scaleX
                    val drawY = currentY + glyph.yoffset * scaleY
                    val glyphWidth = glyph.width * scaleX
                    val glyphHeight = glyph.height * scaleY
                    val fontRegion = if (glyph.page < font.regions.size) font.regions[glyph.page] else font.regions[0]
                    val fontTexture = fontRegion?.texture

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
                        fontTexture = fontTexture
                    )
                }
            }
        } catch (_: Throwable) {
            // An toàn trong môi trường Headless test khi font metrics chưa khởi tạo
        }
    }

    // --- GPU EXECUTION & FLUSH ---

    /**
     * Submit toàn bộ quads đã gom trong batch tới GPU.
     */
    fun flush() {
        if (queuedQuadCount == 0) return

        val currentMesh = mesh
        val currentShader = UberShader.getOrCreate()
        if (isSupported && currentMesh != null && currentShader != null) {
            try {
                currentShader.bind()
                currentShader.applyProjection(Draw.proj())

                currentMesh.setVertices(vertexBuffer, 0, vertexIndex)
                currentMesh.render(currentShader, Gl.triangles, 0, queuedQuadCount * 6)
            } catch (_: Throwable) {
            }
        }

        vertexIndex = 0
        queuedQuadCount = 0
    }

    override fun dispose() {
        flush()
        try {
            mesh?.dispose()
        } catch (_: Throwable) {
        }
        mesh = null
        isMeshInitialized = false
        UberShader.dispose()
        isDrawing = false
    }
}
