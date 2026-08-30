// [AGENT INVARIANT] Synchronously update @property, @param, and @see KDocs when modifying this file.

package org.mdt.core.ui

import androidx.compose.runtime.Composable
import arc.util.Log
import org.mdt.core.platform.MindustryPlatformHost
import org.mdt.core.platform.PlatformHost
import org.mdt.core.ui.compose.ComposePipeline
import org.mdt.core.ui.compose.UIComposition
import org.mdt.core.ui.input.EngineInputProcessor
import org.mdt.core.ui.node.CanvasNode
import org.mdt.core.platform.render.UIBatch

/**
 * ## EngineRuntime
 *
 * Master singleton controlling the UI engine lifecycle, input handling, and frame rendering loop.
 * Connects [PlatformHost] to the root virtual DOM ([CanvasNode]) and coordinates Compose recomposition ([ComposePipeline]).
 * Draws UI via [UIBatch] at the end of each game frame.
 *
 * @property canvas Root virtual screen node ([CanvasNode]) containing the entire UI hierarchy.
 * @property inputProcessor Master input processor routing pointer, touch, scroll, and key events.
 * @property host Active platform host implementation providing window, asset, input, and system ports.
 * @property isInitialized Whether the engine runtime has been initialized and platform hooks registered.
 *
 * @see PlatformHost
 * @see CanvasNode
 * @see EngineInputProcessor
 * @see ComposePipeline
 * @see UIBatch
 */
object EngineRuntime {

    // --- STATE & CORE INSTANCES ---

    val canvas = CanvasNode()
    val inputProcessor = EngineInputProcessor(canvas)

    var host: PlatformHost = MindustryPlatformHost()
    private var pipeline: ComposePipeline? = null
    private var composition: UIComposition? = null
    private var initialized = false

    private val frameEndListener: () -> Unit = { draw() }
    private val resizeListener: (Float, Float) -> Unit = { width, height -> canvas.resize(width, height) }

    // --- LIFECYCLE & INITIALIZATION ---

    fun init(platformHost: PlatformHost = MindustryPlatformHost()) {
        if (initialized) return

        initialized = true
        host = platformHost

        if (pipeline == null) {
            pipeline = ComposePipeline()
        }

        host.addInputProcessor(inputProcessor)
        canvas.resize(host.screenWidth, host.screenHeight)

        host.onResize(resizeListener)
        host.onFrameEnd(frameEndListener)

        Log.info("[NekoMod] EngineRuntime initialized successfully.")
    }

    // --- CONTENT MOUNTING ---

    fun setContent(content: @Composable () -> Unit) {
        init()
        composition?.dispose()
        val currentPipeline = pipeline ?: ComposePipeline().also { pipeline = it }
        composition = UIComposition(canvas, currentPipeline.recomposer, content)
    }

    // --- FRAME RENDERING & DISPATCH ---

    fun draw() {
        val screenWidth = host.screenWidth
        val screenHeight = host.screenHeight
        if (screenWidth <= 0.0f || screenHeight <= 0.0f) return

        try {
            inputProcessor.update()
            canvas.resize(screenWidth, screenHeight)
            pipeline?.frame()

            UIBatch.begin(canvas.screenWidth, canvas.screenHeight)
            canvas.draw()
            UIBatch.end()
        } catch (renderError: Throwable) {
            Log.err("[NekoMod] Error in EngineRuntime.draw()", renderError)
        }
    }

    // --- DISPOSAL ---

    fun dispose() {
        composition?.dispose()
        composition = null

        pipeline?.dispose()
        pipeline = null

        host.removeInputProcessor(inputProcessor)
        host.removeResize(resizeListener)
        host.removeFrameEnd(frameEndListener)
        UIBatch.dispose()
        initialized = false
    }
}
