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
         * Lấy hoặc khởi tạo instance [UberShader] duy nhất (Singleton).
         * An toàn tuyệt đối trong môi trường Headless Unit Test (trả về null nếu không có OpenGL).
         */
        fun getOrCreate(): UberShader? {
            instance?.let { return it }
            if (Core.gl == null || Core.graphics == null) return null

            return try {
                val vert = loadResourceString("/shaders/uber_ui.vert") ?: return null
                val frag = loadResourceString("/shaders/uber_ui.frag") ?: return null

                val shader = UberShader(vert, frag)
                if (shader.isCompiled) {
                    instance = shader
                    Log.info("[NekoMod] UberShader compiled successfully.")
                    shader
                } else {
                    Log.err("[NekoMod] Failed to compile UberShader:\n" + shader.log)
                    null
                }
            } catch (t: Throwable) {
                Log.err("[NekoMod] Exception creating UberShader", t)
                null
            }
        }

        private fun loadResourceString(path: String): String? {
            val normalized = if (path.startsWith("/")) path else "/$path"
            val stream = UberShader::class.java.getResourceAsStream(normalized)
                ?: Thread.currentThread().contextClassLoader?.getResourceAsStream(normalized.removePrefix("/"))
            return stream?.use { it.readBytes().decodeToString().replace("\uFEFF", "").trim() }
        }

        /**
         * Giải phóng tài nguyên GPU của shader.
         */
        fun dispose() {
            try {
                instance?.dispose()
            } catch (_: Throwable) {
            }
            instance = null
        }
    }
}
