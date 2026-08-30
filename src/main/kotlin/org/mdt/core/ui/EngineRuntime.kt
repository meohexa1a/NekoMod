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
 * ## EngineRuntime [Master UI Orchestrator & Frame Coordinator]
 *
 * > 🤖 **AGENT DIRECTIVE:** When modifying this class or related logic, synchronously update this KDoc:
 * > 1) Keep Purpose accurate. 2) Update Key Rules & Checklist [x]/[ ]. 3) Maintain Related Files map.
 *
 * ### 1. Purpose
 * - Master singleton controlling the UI engine lifecycle, input handling, and frame rendering loop.
 * - Connects [PlatformHost] to the root virtual DOM ([CanvasNode]) and manages Compose recomposition ([ComposePipeline]).
 * - Draws UI via [UIBatch] at the end of each game frame.
 *
 * ### 2. Key Rules & Checklist
 * - [x] All platform operations (screen size, input registration, frame hooks) must go through [host].
 * - [x] Render errors inside `draw()` must be caught and logged safely to avoid crashing the game.
 * - [x] `setContent()` must auto-initialize the engine and dispose previous compositions.
 * - [x] `init()` registers `inputProcessor`, `resizeListener`, and `frameEndListener`.
 * - [x] `dispose()` releases Compose pipeline, platform listeners, and GPU resources.
 *
 * ### 3. Related Files
 * - Platform Bridge: `src/main/kotlin/org/mdt/core/platform/PlatformHost.kt`
 * - Root Screen Node: `src/main/kotlin/org/mdt/core/ui/node/CanvasNode.kt`
 * - Input Routing: `src/main/kotlin/org/mdt/core/ui/input/EngineInputProcessor.kt`
 * - Compose Pipeline: `src/main/kotlin/org/mdt/core/ui/compose/ComposePipeline.kt`
 * - GPU Batcher: `src/main/kotlin/org/mdt/core/platform/render/UIBatch.kt`
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
