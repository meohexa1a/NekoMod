package org.mdt.core.ui.render

import arc.graphics.gl.Shader
import arc.util.Disposable
import arc.util.Log
import org.mdt.core.ui.EngineRuntime

/**
 * ## Shaders [GPU Shader Lifecycle Coordinator]
 *
 * ### 1. 📖 Feature Specification & Core Architecture:
 * - Master GPU Shader coordinator managing compile, uniform binding, and disposal of [uberShader] and [blurShader].
 * - Reads vertex and fragment source definitions from modular mod assets via [EngineRuntime.host].
 * - Pre-binds static uniform sampler indices (`u_atlas = 0`, `u_gameBlur = 2`).
 *
 * ### 2. ⚡ Invariants & Non-Negotiable Rules:
 * - **Rule 1 (Arc Shader Headers & Precisions):** Shaders must never contain manual `#version` or `#ifdef GL_ES` headers.
 * - **Rule 2 (Lazy Lifecycle):** Shaders compile on-demand via `ensure()` and dispose safely via `dispose()`.
 *
 * ### 3. 🔗 Related Files & Subsystem Map:
 * - ⚡ **GPU Batcher:** `src/main/kotlin/org/mdt/core/ui/render/UIBatch.kt`
 * - 🌫️ **Blur Pipeline:** `src/main/kotlin/org/mdt/core/ui/render/GameBlurService.kt`
 * - 🔌 **Platform Host:** `src/main/kotlin/org/mdt/core/engine/PlatformHost.kt`
 * - 🎨 **Shader Sources:** `src/main/resources/shaders/uber_ui.vert`, `src/main/resources/shaders/uber_ui.frag`
 *
 * ### 4. ✅ Behavioral Verification Checklist:
 * - [x] `ensure()` compiles `uberShader` and `blurShader` idempotently.
 * - [x] `dispose()` releases GL programs and sets `isLoaded = false`.
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
