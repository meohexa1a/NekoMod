// [AGENT INVARIANT] Synchronously update @property, @param, and @see KDocs when modifying this file.

package org.mdt.core.platform.render

import org.mdt.core.platform.PlatformHost
import org.mdt.core.platform.assets.AssetPort

/**
 * ## MindustryRenderPort
 *
 * Arc and Mindustry OpenGL rendering port coordinating [ShaderRegistry], [SceneBlur], [UIBatch], and [FontRenderer].
 *
 * @param assets Assets port for resolving GLSL shaders and textures.
 * @param hostProvider Provider lambda for platform host metrics and resources.
 *
 * @property shaders Master GLSL shader compiler and uniform registry.
 * @property blur Dual-Kawase scene background blur pipeline.
 * @property batch Master 1-draw-call UI batch renderer.
 * @property fontRenderer BMFont layout measurement and glyph renderer.
 *
 * @see RenderPort
 * @see PlatformHost
 */
class MindustryRenderPort(
    private val hostProvider: () -> PlatformHost = { PlatformHost.NoOp }
) : RenderPort {

    override val shaders: ShaderRegistry by lazy { ShaderRegistry(hostProvider) }
    override val blur: SceneBlur by lazy { SceneBlur(hostProvider) }
    override val batch: UIBatch by lazy { UIBatch(hostProvider) }
    override val fontRenderer: FontRenderer by lazy { FontRenderer() }

    override fun beginFrame(width: Float, height: Float) {
        batch.begin(width, height)
    }

    override fun endFrame() {
        batch.end()
    }

    override fun dispose() {
        batch.dispose()
    }
}
