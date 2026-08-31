// [AGENT ARCHITECTURE & INVARIANTS]
// - Domain Role: GLSL Shader Manager & Uniform Registry.
// - Operating Mechanism: Lazy compilation on first access (`ensure`); binds samplers (`u_atlas=0`, `u_gameBlur=2`).
// - Invariants: Raw shader files must never declare manual `#version` or `#ifdef GL_ES`.
// - Dependencies: [UIBatch], [SceneBlur], [PlatformHost].
// - Directive: Synchronously update @property, @param, and @see KDocs when modifying this file.

package org.mdt.core.platform.render

import arc.graphics.gl.Shader
import arc.util.Log
import org.mdt.core.platform.PlatformHost
import org.mdt.core.platform.assets.AssetPort

/**
 * ## ShaderRegistry
 *
 * Compiles, caches, and disposes UI GLSL shaders ([uberShader] and [blurShader]).
 * Configures shader uniforms for texture samplers (`u_atlas = 0`, `u_gameBlur = 2`).
 *
 * @param hostProvider Provider lambda returning the ambient [PlatformHost] for asset resolution.
 *
 * @property uberShader Master 2D Uber UI shader instance for rendering quads, SDF boxes, borders, and text glyphs.
 * @property blurShader Dual-Kawase downsample/upsample shader instance for scene background blurring.
 * @property isLoaded Whether shaders have been compiled and bound to the GPU context.
 *
 * @see UIBatch
 * @see SceneBlur
 */
class ShaderRegistry(
    private val hostProvider: () -> PlatformHost = { PlatformHost.NoOp }
) {

    private val assets: AssetPort
        get() = hostProvider().assets

    // --- SHADER PATH CONSTANTS ---

    companion object {
        const val VERT_UBER = "shaders/uber_ui.vert"
        const val FRAG_UBER = "shaders/uber_ui.frag"
        const val VERT_BLUR = "shaders/blur.vert"
        const val FRAG_BLUR = "shaders/blur.frag"
    }

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
            if (vertUberSource.isNotEmpty() && fragUberSource.isNotEmpty()) {
                uberShader = Shader(vertUberSource, fragUberSource).apply {
                    bind()
                    setUniformi("u_atlas", 0)
                    setUniformi("u_gameBlur", 2)
                }
            } else {
                Log.err("[NekoMod] Uber shader source is empty or missing, skipping compilation.")
            }

            val vertBlurSource = readString(VERT_BLUR)
            val fragBlurSource = readString(FRAG_BLUR)
            if (vertBlurSource.isNotEmpty() && fragBlurSource.isNotEmpty()) {
                blurShader = Shader(vertBlurSource, fragBlurSource).apply {
                    bind()
                    setUniformi("u_texture", 0)
                }
            } else {
                Log.err("[NekoMod] Blur shader source is empty or missing, skipping compilation.")
            }

            isLoaded = uberShader != null
            if (isLoaded) {
                Log.info("[NekoMod] ShaderRegistry compiled and initialized successfully.")
            }
        } catch (compileError: Throwable) {
            Log.err("[NekoMod] Failed to compile ShaderRegistry shaders!", compileError)
        }
    }

    fun dispose() {
        uberShader?.dispose()
        uberShader = null
        blurShader?.dispose()
        blurShader = null
        isLoaded = false
    }

    // --- HELPERS ---

    internal fun readString(path: String): String = assets.readShaderSource(path)
}
