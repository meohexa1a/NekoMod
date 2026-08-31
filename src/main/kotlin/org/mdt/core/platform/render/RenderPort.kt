// [AGENT INVARIANT] Synchronously update @property, @param, and @see KDocs when modifying this file.

package org.mdt.core.platform.render

import org.mdt.core.platform.assets.AssetPort

/**
 * ## RenderPort
 *
 * Defines the abstract contract for 2D UI batch rendering, scene background blurring,
 * GLSL shader compilation, and bitmap font rasterization.
 *
 * @property batch Master 1-draw-call UI quad/SDF batch renderer.
 * @property blur Dual-Kawase progressive scene background blur pipeline.
 * @property shaders GLSL shader compiler and uniform registry.
 * @property fontRenderer BMFont glyph measurement and layout renderer.
 *
 * @see MindustryRenderPort
 * @see org.mdt.core.platform.PlatformHost
 */
interface RenderPort {

    val batch: UIBatch
    val blur: SceneBlur
    val shaders: ShaderRegistry
    val fontRenderer: FontRenderer

    /**
     * Begins the frame rendering pass for the given physical dimensions.
     */
    fun beginFrame(width: Float, height: Float)

    /**
     * Ends the frame rendering pass and flushes queued primitives to the GPU.
     */
    fun endFrame()

    /**
     * Releases all GPU buffers, shaders, framebuffers, and meshes.
     */
    fun dispose()

    /** Stub [RenderPort] implementation for headless execution or unit testing. */
    object NoOp : RenderPort {
        override val shaders: ShaderRegistry by lazy { ShaderRegistry() }
        override val blur: SceneBlur by lazy { SceneBlur() }
        override val batch: UIBatch by lazy { UIBatch() }
        override val fontRenderer: FontRenderer by lazy { FontRenderer() }

        override fun beginFrame(width: Float, height: Float) {}
        override fun endFrame() {}
        override fun dispose() {}
    }
}
