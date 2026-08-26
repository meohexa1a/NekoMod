package org.mdt.core.ui.render

import arc.Core
import arc.graphics.gl.Shader
import arc.util.Log
import mindustry.Vars

object Shaders {
    const val VERT_BOX = "shaders/box.vert"
    const val FRAG_BOX = "shaders/box.frag"
    const val VERT_BLUR = "shaders/blur.vert"
    const val FRAG_BLUR = "shaders/blur.frag"

    const val TEX_UNIT_FILL = 2
    const val TEX_UNIT_BACKDROP = 3

    var mainShader: Shader? = null
    var blurShader: Shader? = null
    var loaded = false

    fun ensure() {
        if (loaded) return

        try {
            mainShader = Shader(readString(VERT_BOX), readString(FRAG_BOX)).apply {
                bind()
                setUniformi("u_fillTexture", TEX_UNIT_FILL)
                setUniformi("u_backdropTex", TEX_UNIT_BACKDROP)
            }

            blurShader = Shader(readString(VERT_BLUR), readString(FRAG_BLUR)).apply {
                bind()
                setUniformi("u_texture", 0)
            }

            loaded = true
            Log.info("[NekoMod] Shaders loaded successfully.")
        } catch (e: Throwable) {
            Log.err("[NekoMod] Failed to compile shaders!", e)
        }
    }

    fun dispose() {
        mainShader?.dispose()
        mainShader = null
        blurShader?.dispose()
        blurShader = null
        loaded = false
    }

    internal fun readString(path: String): String {
        // 1. Try classloader / classpath resource
        val stream = Shaders::class.java.classLoader.getResourceAsStream(path)
        if (stream != null) {
            return stream.bufferedReader().use { it.readText() }
        }

        // 2. Try Mindustry tree
        val treeFi = Vars.tree?.get(path)
        if (treeFi != null && treeFi.exists()) {
            return treeFi.readString()
        }

        // 3. Try Arc internal files
        val internalFi = Core.files?.internal(path)
        if (internalFi != null && internalFi.exists()) {
            return internalFi.readString()
        }

        throw IllegalStateException("Shader file not found in resources or asset tree: $path")
    }
}
