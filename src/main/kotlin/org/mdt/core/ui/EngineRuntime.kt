// [AGENT ARCHITECTURE & INVARIANTS]
// - Domain Role: UI Engine Master Orchestrator & Frame Loop Driver.
// - Operating Mechanism: Manages global lifecycle, drives layout passes, renders CanvasNode, and routes input.
// - Invariants: All frame rendering executed on main thread; single host facade access via [PlatformHost].
// - Dependencies: [CanvasNode], [ComposePipeline], [EngineInputProcessor], [PlatformHost].
// - Directive: Synchronously update @property, @param, and @see KDocs when modifying this file.

package org.mdt.core.ui

import androidx.compose.runtime.Composable
import arc.util.Log
import org.mdt.core.platform.MindustryPlatformHost
import org.mdt.core.platform.PlatformHost
import org.mdt.core.ui.compose.ComposePipeline
import org.mdt.core.ui.compose.UIComposition
import org.mdt.core.ui.input.EngineInputProcessor
import org.mdt.core.ui.node.CanvasNode

/**
 * ## EngineRuntime
 *
 * Master singleton controlling the UI engine lifecycle, input handling, and frame rendering loop.
 * Connects [PlatformHost] to the root virtual DOM ([CanvasNode]) and coordinates Compose recomposition ([ComposePipeline]).
 * Dispatches frame rendering through [PlatformHost.render] at the end of each game frame.
 *
 * @property canvas Root virtual screen node ([CanvasNode]) containing the entire UI hierarchy.
 * @property inputProcessor Master input processor routing pointer, touch, scroll, and key events.
 * @property host Active platform host implementation providing window, asset, input, system, and render ports.
 *
 * @see PlatformHost
 * @see CanvasNode
 * @see EngineInputProcessor
 * @see ComposePipeline
 */
object EngineRuntime {

    // --- STATE & CORE INSTANCES ---

    var host: PlatformHost = MindustryPlatformHost()
    val canvas = CanvasNode { host }
    val inputProcessor = EngineInputProcessor(canvas) { host }

    init {
        canvas.inputProcessor = inputProcessor
    }

    private var pipelineInstance: ComposePipeline? = null
    private var compositionInstance: UIComposition? = null

    val pipeline: ComposePipeline
        get() {
            var currentPipeline = pipelineInstance
            if (currentPipeline == null) {
                currentPipeline = ComposePipeline()
                pipelineInstance = currentPipeline
            }
            return currentPipeline
        }

    val composition: UIComposition
        get() {
            var currentComposition = compositionInstance
            if (currentComposition == null) {
                currentComposition = UIComposition(canvas, pipeline.recomposer)
                compositionInstance = currentComposition
            }
            return currentComposition
        }

    private var initialized = false

    private val frameEndListener: () -> Unit = { draw() }
    private val resizeListener: (Float, Float) -> Unit = { width, height -> canvas.resize(width, height) }

    // --- LIFECYCLE & INITIALIZATION ---

    fun init(platformHost: PlatformHost = MindustryPlatformHost()) {
        if (initialized) return

        initialized = true
        host = platformHost

        host.input.addInputProcessor(inputProcessor)
        canvas.resize(host.window.width, host.window.height)

        host.window.onResize(resizeListener)
        host.system.onFrameEnd(frameEndListener)

        Log.info("[NekoMod] EngineRuntime initialized successfully.")
    }

    // --- CONTENT MOUNTING ---

    fun setContent(content: @Composable () -> Unit) {
        init()
        composition.setContent(content)
    }

    // --- FRAME RENDERING & DISPATCH ---

    fun draw() {
        val screenWidth = host.window.width
        val screenHeight = host.window.height
        if (screenWidth <= 0.0f || screenHeight <= 0.0f) return

        try {
            inputProcessor.update()
            canvas.resize(screenWidth, screenHeight)
            pipeline.frame()

            var frameBegun = false
            try {
                host.render.beginFrame(canvas.screenWidth, canvas.screenHeight)
                frameBegun = true
                canvas.draw(host.render.batch)
            } finally {
                if (frameBegun) {
                    host.render.endFrame()
                }
            }
        } catch (renderError: Throwable) {
            Log.err("[NekoMod] Error in EngineRuntime.draw()", renderError)
        }
    }

    // --- DISPOSAL ---

    fun dispose() {
        compositionInstance?.dispose()
        compositionInstance = null

        pipelineInstance?.dispose()
        pipelineInstance = null

        host.input.removeInputProcessor(inputProcessor)
        host.window.removeResize(resizeListener)
        host.system.removeFrameEnd(frameEndListener)
        host.render.dispose()
        initialized = false
    }
}
