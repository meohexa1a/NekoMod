package org.mdt.core.ui.render

import arc.graphics.gl.Shader
import arc.util.Disposable
import arc.util.Log
import org.mdt.core.ui.EngineRuntime

/**
 * ## Shaders
 *
 * Master GPU Shader lifecycle coordinator for NekoMod.
 * Manages compile, cache, and disposal of the unified [uberShader] and [blurShader].
 *
 * See: docs/rendering-shaders/rendering_shaders_en.md
 */
object Shaders : Disposable {

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
            Log.info("[NekoMod] Uber UI Shaders compiled and initialized successfully.")
        } catch (compileError: Throwable) {
            Log.err("[NekoMod] Failed to compile Uber UI Shaders!", compileError)
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
