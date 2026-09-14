package org.hubdustry.core.graphics

import arc.Core
import arc.graphics.gl.Shader
import arc.math.Mat
import arc.util.Log

/**
 * ## UberShader
 *
 * Shader GPU chuyên biệt cho giao diện NekoMod v3.
 * Kế thừa trực tiếp từ [arc.graphics.gl.Shader] chuẩn Arc, tự đóng gói toàn bộ
 * việc thiết lập uniforms và samplers mà không để rò rỉ Feature Envy ra ngoài.
 *
 * Chế độ hỗ trợ:
 * - `MODE_TEXT` (0.0f): BMFont Glyph rendering từ Texture Unit 0 (`u_atlas`).
 * - `MODE_BOX` (1.0f): SDF Rounded Rectangle, solid card, button, rounded avatar/icon, và viền nổi.
 */
class UberShader internal constructor(
    vertexSource: String,
    fragmentSource: String
) : Shader(vertexSource, fragmentSource) {

    // ─────────────────────────────────────────────────────────────────────────
    // 1. CONSTANTS & COMPANION FACTORY (NEWSPAPER TOP)
    // ─────────────────────────────────────────────────────────────────────────

    companion object {
        const val VERTEX_SHADER_PATH = "/shaders/uber_ui.vert"
        const val FRAGMENT_SHADER_PATH = "/shaders/uber_ui.frag"

        const val UNIFORM_ATLAS = "u_atlas"
        const val UNIFORM_PROJECTION_TRANS = "u_projTrans"
        const val TEXTURE_UNIT_ATLAS = 0

        private const val UTF8_BOM = "\uFEFF"

        @Volatile
        private var instance: UberShader? = null

        /**
         * Lấy hoặc khởi tạo instance [UberShader] duy nhất (Singleton toàn cục).
         * - Trong môi trường Headless Unit Test (không có OpenGL / Core.gl == null): trả về null.
         * - Trong runtime đồ họa thực tế: BẮT BUỘC shader phải nạp và biên dịch thành công.
         *   Nếu thiếu file shader hoặc biên dịch thất bại -> SẬP NGAY (Fail-Fast) thay vì nuốt lỗi.
         */
        fun getOrCreate(): UberShader? {
            instance?.let { return it }
            if (Core.gl == null || Core.graphics == null) return null

            val vertexShaderSource = checkNotNull(loadResourceString(VERTEX_SHADER_PATH)) {
                "🛑 [CRITICAL SHADER ERROR] Không tìm thấy file tài nguyên shader: $VERTEX_SHADER_PATH"
            }
            val fragmentShaderSource = checkNotNull(loadResourceString(FRAGMENT_SHADER_PATH)) {
                "🛑 [CRITICAL SHADER ERROR] Không tìm thấy file tài nguyên shader: $FRAGMENT_SHADER_PATH"
            }

            val shader = UberShader(vertexShaderSource, fragmentShaderSource)
            check(shader.isCompiled) {
                "🛑 [CRITICAL SHADER ERROR] Biên dịch UberShader thất bại!\n${shader.log}"
            }

            instance = shader
            Log.info("[NekoMod] UberShader compiled successfully.")
            return shader
        }

        private fun loadResourceString(path: String): String? {
            val normalizedPath = if (path.startsWith("/")) path else "/$path"
            val stream = UberShader::class.java.getResourceAsStream(normalizedPath)
                ?: Thread.currentThread().contextClassLoader?.getResourceAsStream(normalizedPath.removePrefix("/"))
            return stream?.use { it.readBytes().decodeToString().replace(UTF8_BOM, "").trim() }
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // 2. UNIFORM LOCATIONS & INITIALIZATION
    // ─────────────────────────────────────────────────────────────────────────

    init {
        bind()
        setUniformi(UNIFORM_ATLAS, TEXTURE_UNIT_ATLAS)
    }

    // ─────────────────────────────────────────────────────────────────────────
    // 3. PUBLIC RENDERING API
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * Đồng bộ ma trận phép chiếu orthographic cho shader.
     */
    fun applyProjection(projectionMatrix: Mat) =
        setUniformMatrix4(UNIFORM_PROJECTION_TRANS, projectionMatrix)
}
