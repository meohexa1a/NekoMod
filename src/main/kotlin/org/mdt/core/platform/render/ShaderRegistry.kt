// [AGENT INVARIANT] Synchronously update @property, @param, and @see KDocs when modifying this file.

package org.mdt.core.platform.render

import arc.graphics.gl.Shader
import arc.util.Log
import org.mdt.core.platform.PlatformHost
import org.mdt.core.platform.AssetPort

/**
 * ## ShaderRegistry
 *
 * Compiles and caches UI GLSL shaders ([uberShader] and [blurShader]).
 * Configures shader uniforms for texture samplers (u_atlas = 0, u_gameBlur = 2).
 *
 * @param hostProvider Provider lambda returning the ambient [PlatformHost] for asset resolution.
 *
 * @property uberShader Master 2D Uber UI shader instance for rendering quads, SDF boxes, borders, and text glyphs.
 * @property blurShader Dual-Kawase downsample/upsample shader instance for scene background blurring.
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

    // --- FALLBACK SHADER ---

    private val fallbackShader: Shader by lazy(LazyThreadSafetyMode.NONE) {
        Shader("", "")
    }

    // --- SHADER INSTANCES ---

    val uberShader: Shader by lazy(LazyThreadSafetyMode.NONE) {
        val vertUberSource = readString(VERT_UBER)
        val fragUberSource = readString(FRAG_UBER)
        if (vertUberSource.isEmpty() || fragUberSource.isEmpty()) {
            Log.err("[NekoMod] Uber shader source is empty or missing, using fallback.")
            return@lazy fallbackShader
        }

        try {
            Shader(vertUberSource, fragUberSource).apply {
                bind()
                setUniformi("u_atlas", 0)
                setUniformi("u_gameBlur", 2)
            }
        } catch (e: Throwable) {
            Log.err("[NekoMod] Failed to compile uber_ui shader, using fallback.", e)
            fallbackShader
        }
    }

    val blurShader: Shader by lazy(LazyThreadSafetyMode.NONE) {
        val vertBlurSource = readString(VERT_BLUR)
        val fragBlurSource = readString(FRAG_BLUR)
        if (vertBlurSource.isEmpty() || fragBlurSource.isEmpty()) {
            Log.err("[NekoMod] Blur shader source is empty or missing, using fallback.")
            return@lazy fallbackShader
        }

        try {
            Shader(vertBlurSource, fragBlurSource).apply {
                bind()
                setUniformi("u_texture", 0)
            }
        } catch (e: Throwable) {
            Log.err("[NekoMod] Failed to compile blur shader, using fallback.", e)
            fallbackShader
        }
    }

    // --- HELPERS ---

    internal fun readString(path: String): String = assets.readShaderSource(path)
}
