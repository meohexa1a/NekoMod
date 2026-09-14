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

/**
 * ## UIBatch
 *
 * Master GPU UI Batch Renderer cho NekoMod v3.
 *
 * TÍNH NĂNG CỐT LÕI:
 * 1. **2 Chế Độ Tinh Gọn (MODE_TEXT & MODE_BOX)**:
 * Master GPU Batcher chuyên biệt cho giao diện Declarative UI NekoMod v3.
 *
 * KIẾN TRÚC V3 (Sau tái cấu trúc):
 * 1. **Zero-GC Hot-Path**:
 *    - Toàn bộ dữ liệu đỉnh được ghi trực tiếp vào Bộ nhớ đệm NIO Off-Heap (`directByteBuffer`).
 *    - Triệt tiêu hoàn toàn việc cấp phát đối tượng `FloatArray` trung gian trong vòng lặp render.
 * 2. **Vertex Layout 26 Floats Chuẩn Mực**:
 *    - Tọa độ 2D: `a_position` (2 floats).
 *    - Màu đỉnh: `a_color` (1 packed float).
 *    - Tọa độ UV: `a_texCoord` (2 floats).
 *    - Thuộc tính SDF: `a_quadParams` (4 floats: width, height, mode, clipActive).
 *    - Bo 4 góc động: `a_cornerRadii` (4 floats: rTL, rTR, rBR, rBL).
 *    - Viền ngoài: `a_border` (2 floats: width, packed color).
 *    - Scissor Scissor Stack 2 tầng: `a_clipRect` (4 floats), `a_parentClipRect` (4 floats).
 *    - Bo góc cắt gọt: `a_clipCornerRadii` (3 floats: rTL, rTR, rBR).
 * 3. **Cắt Gọt Phân Cấp Phổ Quát (Analytical Scissor Stack)**:
 *    - Sử dụng giải thuật cắt gọt giải tích trực tiếp trên GPU Shader thay vì gọi `glScissor` liên tục.
 *    - Hỗ trợ lồng nhau 16 cấp độ (`MAX_CLIP_DEPTH`) mà không làm gãy (break) draw call batching.
 * 4. **An Toàn Tuyệt Đối Trong Headless Testing**:
 *    - Khi `Core.gl == null`, renderer tự động ghi nhận quads vào buffer để phục vụ kiểm thử đơn vị
 *    - mà không quăng lỗi hay gọi OpenGL native.
 * 5. **Singleton Vĩnh Viễn (Rule 0.5)**:
 *    - UIBatch là GPU Batcher toàn cục cấp tiến trình, không chứa hàm dispose() để tránh bị hủy từ View con.
 */
object UIBatch {

    /** Chế độ vẽ văn bản BMFont Glyph từ `u_atlas`. */
    const val MODE_TEXT = 0.0f

    /** Chế độ vẽ hình hộp SDF đa năng (nền trơn, bo góc, viền, ảnh/icon). */
    const val MODE_BOX = 1.0f

    /** Cờ báo hiệu Quad này không bị cắt gọt (GPU Shader sẽ bỏ qua hoàn toàn bước kiểm tra scissor). */
    const val CLIP_OFF = 0.0f

    /** Cờ báo hiệu Quad này nằm trong container cắt gọt (GPU Shader sẽ kích hoạt kiểm tra scissor). */
    const val CLIP_ON = 1.0f

    /** Giới hạn vô cực thực tế cho phép giao cắt hình chữ nhật AABB khi chưa bị giới hạn trục. */
    const val UNCLIPPED_BOUND = 100000.0f

    /** Số lượng quads tối đa trong một batch trước khi tự động flush tới GPU. */
    const val MAX_QUADS = 4096

    /** Số lượng floats cấu thành 1 đỉnh GPU (26 floats). */
    const val FLOATS_PER_VERTEX = 26

    /** Số lượng floats cho một hình chữ nhật quad (4 đỉnh * 26 = 104 floats). */
    const val FLOATS_PER_QUAD = FLOATS_PER_VERTEX * 4

    /** Số lượng đỉnh tối đa cho [MAX_QUADS]. */
    const val MAX_VERTICES = MAX_QUADS * 4

    /** Số lượng chỉ mục tam giác tối đa (6 indices per quad). */
    const val MAX_INDICES = MAX_QUADS * 6

    /** Độ sâu tối đa của ngăn xếp cắt gọt giải tích phân cấp. */
    const val MAX_CLIP_DEPTH = 64

    /** Bộ đệm đỉnh mảng phẳng tĩnh đạt chuẩn Zero-GC Hot-Path (104 floats per quad). */
    internal val vertexBuffer = FloatArray(MAX_QUADS * FLOATS_PER_QUAD)

    /** Chỉ mục float hiện tại trong [vertexBuffer]. */
    var vertexIndex: Int = 0
        internal set

    /** Số lượng quads đang xếp hàng chờ flush tới GPU. */
    var queuedQuadCount: Int = 0
        internal set

    private var mesh: Mesh? = null
    private var isMeshInitialized = false
    private var isDrawing = false

    // Analytical Scissor & SDF Clip Stack (8 primitives per depth level: minX, minY, maxX, maxY, rTS, rTE, rBE, rBS)
    private val clipStackBuffer = FloatArray(MAX_CLIP_DEPTH * 8)

    /** Độ sâu ngăn xếp cắt gọt hiện tại. */
    var clipDepth: Int = 0
        internal set

    /** Tọa độ X nhỏ nhất của vùng cắt giải tích hiện tại. */
    var clipMinX: Float = -UNCLIPPED_BOUND
        internal set

    /** Tọa độ Y nhỏ nhất của vùng cắt giải tích hiện tại. */
    var clipMinY: Float = -UNCLIPPED_BOUND
        internal set

    /** Tọa độ X lớn nhất của vùng cắt giải tích hiện tại. */
    var clipMaxX: Float = UNCLIPPED_BOUND
        internal set

    /** Tọa độ Y lớn nhất của vùng cắt giải tích hiện tại. */
    var clipMaxY: Float = UNCLIPPED_BOUND
        internal set

    /** Bán kính bo góc Top-Start của vùng cắt SDF hiện tại. */
    var clipRadiusTopStart: Float = 0f
        internal set

    /** Bán kính bo góc Top-End của vùng cắt SDF hiện tại. */
    var clipRadiusTopEnd: Float = 0f
        internal set

    /** Bán kính bo góc Bottom-End của vùng cắt SDF hiện tại. */
    var clipRadiusBottomEnd: Float = 0f
        internal set

    /** Bán kính bo góc Bottom-Start của vùng cắt SDF hiện tại. */
    var clipRadiusBottomStart: Float = 0f
        internal set

    private var activeAtlasTexture: Texture? = null
    private val textLayoutHelper by lazy(LazyThreadSafetyMode.NONE) { GlyphLayout() }

    /**
     * Kiểm tra xem môi trường hiện tại có hỗ trợ GPU OpenGL và UberShader đầy đủ không.
     * Trả về false trong môi trường Headless Unit Test mà không ném lỗi.
     */
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
                VertexAttribute(4, "a_style"),                              // x = radius, y = borderWidth, z = mode, w = clipActive
                VertexAttribute(4, "a_cornerRadii"),                        // x = topStart, y = topEnd, z = bottomEnd, w = bottomStart
                VertexAttribute(4, Gl.unsignedByte, true, "a_borderColor"), // rgba = packed ABGR border color
                VertexAttribute(4, "a_clipRect"),                           // xy = min(x,y), zw = max(x,y) analytical scissor clip
                VertexAttribute(4, "a_clipRadii")                           // x = topStart, y = topEnd, z = bottomEnd, w = bottomStart (clip corner radii)
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
        if (isDrawing) {
            end()
        }
        isDrawing = true

        setupGpuPipeline()

        vertexIndex = 0
        queuedQuadCount = 0
        clipDepth = 0
        clipMinX = -UNCLIPPED_BOUND
        clipMinY = -UNCLIPPED_BOUND
        clipMaxX = UNCLIPPED_BOUND
        clipMaxY = UNCLIPPED_BOUND
        clipRadiusTopStart = 0f
        clipRadiusTopEnd = 0f
        clipRadiusBottomEnd = 0f
        clipRadiusBottomStart = 0f
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
        try {
            flush()
            if (isSupported) {
                try {
                    Gl.activeTexture(Gl.texture0)
                } catch (_: Throwable) {
                }
            }
        } finally {
            isDrawing = false
            clipDepth = 0
            clipMinX = -UNCLIPPED_BOUND
            clipMinY = -UNCLIPPED_BOUND
            clipMaxX = UNCLIPPED_BOUND
            clipMaxY = UNCLIPPED_BOUND
            clipRadiusTopStart = 0f
            clipRadiusTopEnd = 0f
            clipRadiusBottomEnd = 0f
            clipRadiusBottomStart = 0f
        }
    }

    // --- ANALYTICAL SCISSOR & SDF CLIPPING ---

    /**
     * Đẩy một vùng cắt gọt mới lên ngăn xếp và giao cắt với vùng cắt hiện tại.
     * Hỗ trợ độc lập trục ngang [clipHorizontal], trục dọc [clipVertical] và bo góc SDF [radiusTopStart]..[radiusBottomStart].
     */
    fun pushClip(
        x: Float,
        y: Float,
        width: Float,
        height: Float,
        clipHorizontal: Boolean = true,
        clipVertical: Boolean = true,
        radiusTopStart: Float = 0f,
        radiusTopEnd: Float = 0f,
        radiusBottomEnd: Float = 0f,
        radiusBottomStart: Float = 0f
    ) {
        val offset = clipDepth * 8
        if (offset + 8 > clipStackBuffer.size) {
            arc.util.Log.err("[UIBatch] Clip stack overflow! Maximum depth $MAX_CLIP_DEPTH exceeded.")
            clipDepth++
            return
        }

        clipStackBuffer[offset] = clipMinX
        clipStackBuffer[offset + 1] = clipMinY
        clipStackBuffer[offset + 2] = clipMaxX
        clipStackBuffer[offset + 3] = clipMaxY
        clipStackBuffer[offset + 4] = clipRadiusTopStart
        clipStackBuffer[offset + 5] = clipRadiusTopEnd
        clipStackBuffer[offset + 6] = clipRadiusBottomEnd
        clipStackBuffer[offset + 7] = clipRadiusBottomStart

        if (clipHorizontal) {
            val minX = maxOf(clipMinX, x)
            val maxX = minOf(clipMaxX, x + width)
            clipMinX = minX
            clipMaxX = maxOf(minX, maxX)
        }

        if (clipVertical) {
            val minY = maxOf(clipMinY, y)
            val maxY = minOf(clipMaxY, y + height)
            clipMinY = minY
            clipMaxY = maxOf(minY, maxY)
        }

        if (radiusTopStart > 0.001f || radiusTopEnd > 0.001f || radiusBottomEnd > 0.001f || radiusBottomStart > 0.001f) {
            clipRadiusTopStart = radiusTopStart
            clipRadiusTopEnd = radiusTopEnd
            clipRadiusBottomEnd = radiusBottomEnd
            clipRadiusBottomStart = radiusBottomStart
        }

        clipDepth++
    }

    /**
     * Khôi phục vùng cắt gọt của container cha từ ngăn xếp.
     */
    fun popClip() {
        if (clipDepth <= 0) {
            clipMinX = -UNCLIPPED_BOUND
            clipMinY = -UNCLIPPED_BOUND
            clipMaxX = UNCLIPPED_BOUND
            clipMaxY = UNCLIPPED_BOUND
            clipRadiusTopStart = 0f
            clipRadiusTopEnd = 0f
            clipRadiusBottomEnd = 0f
            clipRadiusBottomStart = 0f
            return
        }

        clipDepth--
        if (clipDepth >= MAX_CLIP_DEPTH) return

        val offset = clipDepth * 8
        clipMinX = clipStackBuffer[offset]
        clipMinY = clipStackBuffer[offset + 1]
        clipMaxX = clipStackBuffer[offset + 2]
        clipMaxY = clipStackBuffer[offset + 3]
        clipRadiusTopStart = clipStackBuffer[offset + 4]
        clipRadiusTopEnd = clipStackBuffer[offset + 5]
        clipRadiusBottomEnd = clipStackBuffer[offset + 6]
        clipRadiusBottomStart = clipStackBuffer[offset + 7]
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
        val clipActive = if (clipDepth > 0) CLIP_ON else CLIP_OFF

        // Vertex 0: Bottom-Left (leftX, bottomY)
        vertexBuffer[offset++] = leftX; vertexBuffer[offset++] = bottomY; vertexBuffer[offset++] = uvMinU; vertexBuffer[offset++] = uvMinV
        vertexBuffer[offset++] = packedColor
        vertexBuffer[offset++] = 0.0f; vertexBuffer[offset++] = 0.0f; vertexBuffer[offset++] = width; vertexBuffer[offset++] = height
        vertexBuffer[offset++] = radiusTopStart; vertexBuffer[offset++] = borderWidth; vertexBuffer[offset++] = MODE_BOX; vertexBuffer[offset++] = clipActive
        vertexBuffer[offset++] = radiusTopStart; vertexBuffer[offset++] = radiusTopEnd; vertexBuffer[offset++] = radiusBottomEnd; vertexBuffer[offset++] = radiusBottomStart
        vertexBuffer[offset++] = packedBorderColor
        vertexBuffer[offset++] = clipMinX; vertexBuffer[offset++] = clipMinY; vertexBuffer[offset++] = clipMaxX; vertexBuffer[offset++] = clipMaxY
        vertexBuffer[offset++] = clipRadiusTopStart; vertexBuffer[offset++] = clipRadiusTopEnd; vertexBuffer[offset++] = clipRadiusBottomEnd; vertexBuffer[offset++] = clipRadiusBottomStart

        // Vertex 1: Top-Left (leftX, topY)
        vertexBuffer[offset++] = leftX; vertexBuffer[offset++] = topY; vertexBuffer[offset++] = uvMinU; vertexBuffer[offset++] = uvMaxV
        vertexBuffer[offset++] = packedColor
        vertexBuffer[offset++] = 0.0f; vertexBuffer[offset++] = height; vertexBuffer[offset++] = width; vertexBuffer[offset++] = height
        vertexBuffer[offset++] = radiusTopStart; vertexBuffer[offset++] = borderWidth; vertexBuffer[offset++] = MODE_BOX; vertexBuffer[offset++] = clipActive
        vertexBuffer[offset++] = radiusTopStart; vertexBuffer[offset++] = radiusTopEnd; vertexBuffer[offset++] = radiusBottomEnd; vertexBuffer[offset++] = radiusBottomStart
        vertexBuffer[offset++] = packedBorderColor
        vertexBuffer[offset++] = clipMinX; vertexBuffer[offset++] = clipMinY; vertexBuffer[offset++] = clipMaxX; vertexBuffer[offset++] = clipMaxY
        vertexBuffer[offset++] = clipRadiusTopStart; vertexBuffer[offset++] = clipRadiusTopEnd; vertexBuffer[offset++] = clipRadiusBottomEnd; vertexBuffer[offset++] = clipRadiusBottomStart

        // Vertex 2: Top-Right (rightX, topY)
        vertexBuffer[offset++] = rightX; vertexBuffer[offset++] = topY; vertexBuffer[offset++] = uvMaxU; vertexBuffer[offset++] = uvMaxV
        vertexBuffer[offset++] = packedColor
        vertexBuffer[offset++] = width; vertexBuffer[offset++] = height; vertexBuffer[offset++] = width; vertexBuffer[offset++] = height
        vertexBuffer[offset++] = radiusTopStart; vertexBuffer[offset++] = borderWidth; vertexBuffer[offset++] = MODE_BOX; vertexBuffer[offset++] = clipActive
        vertexBuffer[offset++] = radiusTopStart; vertexBuffer[offset++] = radiusTopEnd; vertexBuffer[offset++] = radiusBottomEnd; vertexBuffer[offset++] = radiusBottomStart
        vertexBuffer[offset++] = packedBorderColor
        vertexBuffer[offset++] = clipMinX; vertexBuffer[offset++] = clipMinY; vertexBuffer[offset++] = clipMaxX; vertexBuffer[offset++] = clipMaxY
        vertexBuffer[offset++] = clipRadiusTopStart; vertexBuffer[offset++] = clipRadiusTopEnd; vertexBuffer[offset++] = clipRadiusBottomEnd; vertexBuffer[offset++] = clipRadiusBottomStart

        // Vertex 3: Bottom-Right (rightX, bottomY)
        vertexBuffer[offset++] = rightX; vertexBuffer[offset++] = bottomY; vertexBuffer[offset++] = uvMaxU; vertexBuffer[offset++] = uvMinV
        vertexBuffer[offset++] = packedColor
        vertexBuffer[offset++] = width; vertexBuffer[offset++] = 0.0f; vertexBuffer[offset++] = width; vertexBuffer[offset++] = height
        vertexBuffer[offset++] = radiusTopStart; vertexBuffer[offset++] = borderWidth; vertexBuffer[offset++] = MODE_BOX; vertexBuffer[offset++] = clipActive
        vertexBuffer[offset++] = radiusTopStart; vertexBuffer[offset++] = radiusTopEnd; vertexBuffer[offset++] = radiusBottomEnd; vertexBuffer[offset++] = radiusBottomStart
        vertexBuffer[offset++] = packedBorderColor
        vertexBuffer[offset++] = clipMinX; vertexBuffer[offset++] = clipMinY; vertexBuffer[offset++] = clipMaxX; vertexBuffer[offset++] = clipMaxY
        vertexBuffer[offset++] = clipRadiusTopStart; vertexBuffer[offset++] = clipRadiusTopEnd; vertexBuffer[offset++] = clipRadiusBottomEnd; vertexBuffer[offset++] = clipRadiusBottomStart

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
        region: TextureRegion? = null,
        radius: Float = 0.0f,
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
        val clipActive = if (clipDepth > 0) CLIP_ON else CLIP_OFF

        // Vertex 0: Bottom-Left (leftX, bottomY)
        vertexBuffer[offset++] = leftX; vertexBuffer[offset++] = bottomY; vertexBuffer[offset++] = uvMinU; vertexBuffer[offset++] = uvMinV
        vertexBuffer[offset++] = packedColor
        vertexBuffer[offset++] = 0.0f; vertexBuffer[offset++] = 0.0f; vertexBuffer[offset++] = width; vertexBuffer[offset++] = height
        vertexBuffer[offset++] = 0.0f; vertexBuffer[offset++] = 0.0f; vertexBuffer[offset++] = MODE_TEXT; vertexBuffer[offset++] = clipActive
        vertexBuffer[offset++] = 0.0f; vertexBuffer[offset++] = 0.0f; vertexBuffer[offset++] = 0.0f; vertexBuffer[offset++] = 0.0f
        vertexBuffer[offset++] = packedBorderColor
        vertexBuffer[offset++] = clipMinX; vertexBuffer[offset++] = clipMinY; vertexBuffer[offset++] = clipMaxX; vertexBuffer[offset++] = clipMaxY
        vertexBuffer[offset++] = 0.0f; vertexBuffer[offset++] = 0.0f; vertexBuffer[offset++] = 0.0f; vertexBuffer[offset++] = 0.0f

        // Vertex 1: Top-Left (leftX, topY)
        vertexBuffer[offset++] = leftX; vertexBuffer[offset++] = topY; vertexBuffer[offset++] = uvMinU; vertexBuffer[offset++] = uvMaxV
        vertexBuffer[offset++] = packedColor
        vertexBuffer[offset++] = 0.0f; vertexBuffer[offset++] = height; vertexBuffer[offset++] = width; vertexBuffer[offset++] = height
        vertexBuffer[offset++] = 0.0f; vertexBuffer[offset++] = 0.0f; vertexBuffer[offset++] = MODE_TEXT; vertexBuffer[offset++] = clipActive
        vertexBuffer[offset++] = 0.0f; vertexBuffer[offset++] = 0.0f; vertexBuffer[offset++] = 0.0f; vertexBuffer[offset++] = 0.0f
        vertexBuffer[offset++] = packedBorderColor
        vertexBuffer[offset++] = clipMinX; vertexBuffer[offset++] = clipMinY; vertexBuffer[offset++] = clipMaxX; vertexBuffer[offset++] = clipMaxY
        vertexBuffer[offset++] = 0.0f; vertexBuffer[offset++] = 0.0f; vertexBuffer[offset++] = 0.0f; vertexBuffer[offset++] = 0.0f

        // Vertex 2: Top-Right (rightX, topY)
        vertexBuffer[offset++] = rightX; vertexBuffer[offset++] = topY; vertexBuffer[offset++] = uvMaxU; vertexBuffer[offset++] = uvMaxV
        vertexBuffer[offset++] = packedColor
        vertexBuffer[offset++] = width; vertexBuffer[offset++] = height; vertexBuffer[offset++] = width; vertexBuffer[offset++] = height
        vertexBuffer[offset++] = 0.0f; vertexBuffer[offset++] = 0.0f; vertexBuffer[offset++] = MODE_TEXT; vertexBuffer[offset++] = clipActive
        vertexBuffer[offset++] = 0.0f; vertexBuffer[offset++] = 0.0f; vertexBuffer[offset++] = 0.0f; vertexBuffer[offset++] = 0.0f
        vertexBuffer[offset++] = packedBorderColor
        vertexBuffer[offset++] = clipMinX; vertexBuffer[offset++] = clipMinY; vertexBuffer[offset++] = clipMaxX; vertexBuffer[offset++] = clipMaxY
        vertexBuffer[offset++] = 0.0f; vertexBuffer[offset++] = 0.0f; vertexBuffer[offset++] = 0.0f; vertexBuffer[offset++] = 0.0f

        // Vertex 3: Bottom-Right (rightX, bottomY)
        vertexBuffer[offset++] = rightX; vertexBuffer[offset++] = bottomY; vertexBuffer[offset++] = uvMaxU; vertexBuffer[offset++] = uvMinV
        vertexBuffer[offset++] = packedColor
        vertexBuffer[offset++] = width; vertexBuffer[offset++] = 0.0f; vertexBuffer[offset++] = width; vertexBuffer[offset++] = height
        vertexBuffer[offset++] = 0.0f; vertexBuffer[offset++] = 0.0f; vertexBuffer[offset++] = MODE_TEXT; vertexBuffer[offset++] = clipActive
        vertexBuffer[offset++] = 0.0f; vertexBuffer[offset++] = 0.0f; vertexBuffer[offset++] = 0.0f; vertexBuffer[offset++] = 0.0f
        vertexBuffer[offset++] = packedBorderColor
        vertexBuffer[offset++] = clipMinX; vertexBuffer[offset++] = clipMinY; vertexBuffer[offset++] = clipMaxX; vertexBuffer[offset++] = clipMaxY
        vertexBuffer[offset++] = 0.0f; vertexBuffer[offset++] = 0.0f; vertexBuffer[offset++] = 0.0f; vertexBuffer[offset++] = 0.0f

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
}
