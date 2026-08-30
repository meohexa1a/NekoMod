// [AGENT INVARIANT] Synchronously update @property, @param, and @see KDocs when modifying this file.

package org.mdt.core.platform.render

import arc.graphics.gl.Shader
import arc.util.Disposable
import arc.util.Log
import org.mdt.core.ui.EngineRuntime

/**
 * ## ShaderRegistry
 *
 * Compiles, caches, and disposes UI GLSL shaders ([uberShader] and [blurShader]).
 * Configures shader uniforms for texture samplers (`u_atlas = 0`, `u_gameBlur = 2`).
 *
 * @property uberShader Master 2D Uber UI shader instance for rendering quads, SDF boxes, borders, and text glyphs.
 * @property blurShader Dual-Kawase downsample/upsample shader instance for scene background blurring.
 * @property isLoaded Whether shaders have been compiled and bound to the GPU context.
 *
 * @see UIBatch
 * @see SceneBlur
 */
object ShaderRegistry : Disposable {

    // --- SHADER PATH CONSTANTS ---

    const val VERT_UBER = "shaders/uber_ui.vert"
    const val FRAG_UBER = "shaders/uber_ui.frag"
    const val VERT_BLUR = "shaders/blur.vert"
    const val FRAG_BLUR = "shaders/blur.frag"

    // --- SHADER INSTANCES ---

    var uberShader: Shader? = null
        private set

    var blurShader: Shader? = null
        private set

    var isLoaded: Boolean = false
        private set

    // --- LIFECYCLE & INITIALIZATION ---

    /**
     * Lazily compiles and initializes the GPU shaders if not already loaded.
     */
    fun ensure() {
        if (isLoaded) return

        try {
            val vertUberSource = readString(VERT_UBER)
            val fragUberSource = readString(FRAG_UBER)
            uberShader = Shader(vertUberSource, fragUberSource).apply {
                bind()
                setUniformi("u_atlas", 0)
                setUniformi("u_gameBlur", 2)
            }

            val vertBlurSource = readString(VERT_BLUR)
            val fragBlurSource = readString(FRAG_BLUR)
            blurShader = Shader(vertBlurSource, fragBlurSource).apply {
                bind()
                setUniformi("u_texture", 0)
            }

            isLoaded = true
            Log.info("[NekoMod] ShaderRegistry compiled and initialized successfully.")
        } catch (compileError: Throwable) {
            Log.err("[NekoMod] Failed to compile ShaderRegistry shaders!", compileError)
        }
    }

    override fun dispose() {
        uberShader?.dispose()
        uberShader = null
        blurShader?.dispose()
        blurShader = null
        isLoaded = false
    }

    // --- HELPERS ---

    internal fun readString(path: String): String = EngineRuntime.host.readShaderSource(path)
}
