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
class UberShader(
    vertexSource: String,
    fragmentSource: String
) : Shader(vertexSource, fragmentSource) {

    init {
        bind()
        setUniformi("u_atlas", 0)
    }

    /**
     * Đồng bộ ma trận phép chiếu orthographic cho shader.
     */
    fun applyProjection(proj: Mat) {
        setUniformMatrix4("u_projTrans", proj)
    }

    companion object {
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

            val vert = checkNotNull(loadResourceString("/shaders/uber_ui.vert")) {
                "🛑 [CRITICAL SHADER ERROR] Không tìm thấy file tài nguyên shader: /shaders/uber_ui.vert"
            }
            val frag = checkNotNull(loadResourceString("/shaders/uber_ui.frag")) {
                "🛑 [CRITICAL SHADER ERROR] Không tìm thấy file tài nguyên shader: /shaders/uber_ui.frag"
            }

            val shader = UberShader(vert, frag)
            check(shader.isCompiled) {
                "🛑 [CRITICAL SHADER ERROR] Biên dịch UberShader thất bại!\n${shader.log}"
            }

            instance = shader
            Log.info("[NekoMod] UberShader compiled successfully.")
            return shader
        }

        private fun loadResourceString(path: String): String? {
            val normalized = if (path.startsWith("/")) path else "/$path"
            val stream = UberShader::class.java.getResourceAsStream(normalized)
                ?: Thread.currentThread().contextClassLoader?.getResourceAsStream(normalized.removePrefix("/"))
            return stream?.use { it.readBytes().decodeToString().replace("\uFEFF", "").trim() }
        }
    }
}
