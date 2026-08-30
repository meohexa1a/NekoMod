package org.mdt.core.platform.render

import arc.graphics.gl.Shader
import arc.util.Disposable
import arc.util.Log
import org.mdt.core.ui.EngineRuntime

/**
 * ## ShaderRegistry [GLSL Shader Manager & Registry]
 *
 * > 🤖 **AGENT DIRECTIVE:** When modifying this class or related logic, synchronously update this KDoc:
 * > 1) Keep Purpose accurate. 2) Update Key Rules & Checklist [x]/[ ]. 3) Maintain Related Files map.
 *
 * ### 1. Purpose
 * - Compiles, manages, and cleans up UI GLSL shaders ([uberShader] and [blurShader]).
 * - Configures shader uniforms for texture samplers (`u_atlas = 0`, `u_gameBlur = 2`).
 *
 * ### 2. Key Rules & Checklist
 * - [x] Raw shader files must never declare manual `#version` or `#ifdef GL_ES` headers.
 * - [x] Shaders load lazily on first access via `ensure()`.
 * - [x] `dispose()` safely destroys GPU shader programs.
 *
 * ### 3. Related Files
 * - GPU Batcher: `src/main/kotlin/org/mdt/core/platform/render/UIBatch.kt`
 * - Scene Blur: `src/main/kotlin/org/mdt/core/platform/render/blur/SceneBlur.kt`
 * - Platform Host: `src/main/kotlin/org/mdt/core/platform/PlatformHost.kt`
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
