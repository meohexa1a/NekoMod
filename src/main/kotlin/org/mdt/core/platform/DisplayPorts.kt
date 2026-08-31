package org.mdt.core.platform

import arc.Core
import arc.Events
import arc.Graphics.Cursor
import java.util.concurrent.CopyOnWriteArrayList
import mindustry.game.EventType.ResizeEvent
import org.mdt.core.platform.render.FontRenderer
import org.mdt.core.platform.render.SceneBlur
import org.mdt.core.platform.render.ShaderRegistry
import org.mdt.core.platform.render.UIBatch

/**
 * ## WindowPort
 *
 * Defines the abstract contract for display surface dimensions, window resize events, and system cursor control.
 * Includes a built-in [NoOp] stub for headless execution or unit testing.
 *
 * @property width Current display surface width in pixels.
 * @property height Current display surface height in pixels.
 *
 * @see MindustryWindowPort
 * @see org.mdt.core.platform.PlatformHost
 */
interface WindowPort {

    /** Current display surface width in pixels. */
    val width: Float

    /** Current display surface height in pixels. */
    val height: Float

    /** Registers a listener invoked when the display surface is resized. */
    fun onResize(block: (width: Float, height: Float) -> Unit)

    /** Removes a listener invoked when the display surface is resized. */
    fun removeResize(block: (width: Float, height: Float) -> Unit)

    /** Sets the system cursor to a hand/pointer cursor for clickable elements. */
    fun setCursorHand()

    /** Sets the active hardware/software cursor. */
    fun setCursor(cursor: Cursor?)

    /** Restores the hardware/software cursor to default. */
    fun restoreCursor()

    /** Stub [WindowPort] implementation for headless or testing environments. */
    object NoOp : WindowPort {
        override val width: Float get() = 1920.0f
        override val height: Float get() = 1080.0f
        override fun onResize(block: (width: Float, height: Float) -> Unit) = Unit
        override fun removeResize(block: (width: Float, height: Float) -> Unit) = Unit
        override fun setCursorHand() = Unit
        override fun setCursor(cursor: Cursor?) = Unit
        override fun restoreCursor() = Unit
    }
}



/**
 * ## MindustryWindowPort
 *
 * Implements [WindowPort] by bridging to Arc's `Core.graphics` and Mindustry's `ResizeEvent`.
 *
 * @see WindowPort
 * @see org.mdt.core.platform.PlatformHost
 */
class MindustryWindowPort : WindowPort {

    override val width: Float
        get() = Core.graphics?.width?.toFloat() ?: 0.0f

    override val height: Float
        get() = Core.graphics?.height?.toFloat() ?: 0.0f

    override fun onResize(block: (width: Float, height: Float) -> Unit) {
        if (!resizeListeners.contains(block)) {
            resizeListeners.add(block)
        }
    }

    override fun removeResize(block: (width: Float, height: Float) -> Unit) {
        resizeListeners.remove(block)
    }

    override fun setCursorHand() {
        Core.graphics?.cursor(Cursor.SystemCursor.hand)
    }

    override fun setCursor(cursor: Cursor?) {
        when {
            cursor != null -> Core.graphics?.cursor(cursor)
            else -> Core.graphics?.restoreCursor()
        }
    }

    override fun restoreCursor() {
        Core.graphics?.restoreCursor()
    }

    companion object {
        private val resizeListeners by lazy {
            val list = CopyOnWriteArrayList<(width: Float, height: Float) -> Unit>()

            Events.on(ResizeEvent::class.java) {
                val currentWidth = Core.graphics?.width?.toFloat() ?: 0.0f
                val currentHeight = Core.graphics?.height?.toFloat() ?: 0.0f

                for (listener in list) {
                    listener(currentWidth, currentHeight)
                }
            }

            list
        }
    }
}



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
        override val shaders: ShaderRegistry by lazy { ShaderRegistry { PlatformHost.NoOp } }
        override val blur: SceneBlur by lazy { SceneBlur { PlatformHost.NoOp } }
        override val batch: UIBatch by lazy { UIBatch { PlatformHost.NoOp } }
        override val fontRenderer: FontRenderer by lazy { FontRenderer() }

        override fun beginFrame(width: Float, height: Float) {}
        override fun endFrame() {}
        override fun dispose() {}
    }
}



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
    private val hostProvider: () -> PlatformHost
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
