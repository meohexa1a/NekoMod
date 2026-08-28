package org.mdt.core.ui

import androidx.compose.runtime.Composable
import arc.util.Log
import org.mdt.core.engine.MindustryPlatformHost
import org.mdt.core.engine.PlatformHost
import org.mdt.core.ui.compose.CompositionManager
import org.mdt.core.ui.compose.UIComposition
import org.mdt.core.ui.input.EngineInputProcessor
import org.mdt.core.ui.node.CanvasNode
import org.mdt.core.ui.render.UIBatch

/**
 * ## EngineRuntime [Master UI Orchestrator & Frame Coordinator]
 *
 * ### 1. 📖 Feature Specification & Core Architecture:
 * - Master singleton orchestrating the entire UI runtime lifecycle, input pipeline, and frame rendering loop.
 * - Bridges the host platform window/display surface ([PlatformHost]) to the root virtual DOM container ([CanvasNode]).
 * - Manages Compose slot-table recomposition frames via [CompositionManager] and dispatches top-layer overlay passes.
 * - Coordinates 1-Draw-Call GPU batch rendering via [UIBatch] at the end of each host render frame.
 *
 * ### 2. ⚡ Invariants & Non-Negotiable Rules:
 * - **Rule 1 (Zero Platform Bypass):** Viewport dimensions, input processor registration, resize listeners, and frame end hooks MUST go through [host].
 * - **Rule 2 (Exception Isolation):** Render errors inside [draw] are logged safely to prevent game crash loops.
 * - **Rule 3 (Safe Re-entrancy):** [setContent] automatically invokes [init] and disposes previous composition instances.
 *
 * ### 3. 🔗 Related Files & Subsystem Map:
 * - 🔌 **Platform Bridge:** `src/main/kotlin/org/mdt/core/engine/PlatformHost.kt`
 * - 🌲 **Root Virtual Node:** `src/main/kotlin/org/mdt/core/ui/node/CanvasNode.kt`
 * - 🎮 **Input Routing:** `src/main/kotlin/org/mdt/core/ui/input/EngineInputProcessor.kt`
 * - 🔄 **Compose Recomposer:** `src/main/kotlin/org/mdt/core/ui/compose/UIComposition.kt`
 * - ⚡ **GPU Batcher:** `src/main/kotlin/org/mdt/core/ui/render/UIBatch.kt`
 *
 * ### 4. ✅ Behavioral Verification Checklist:
 * - [x] `init()` registers `inputProcessor`, `resizeListener`, and `frameEndListener` on [PlatformHost].
 * - [x] `draw()` executes `inputProcessor.update()`, `canvas.resize()`, `CompositionManager.frame()`, and wraps canvas drawing inside `UIBatch.begin()` / `end()`.
 * - [x] `dispose()` clears overlays, cancels composition, stops recomposer, unregisters platform hooks, and frees GPU resources.
 */
object EngineRuntime {

    // --- STATE & CORE INSTANCES ---

    val canvas = CanvasNode()
    val inputProcessor = EngineInputProcessor(canvas)

    var host: PlatformHost = MindustryPlatformHost()
    private var composition: UIComposition? = null
    private var initialized = false

    private val frameEndListener: () -> Unit = { draw() }
    private val resizeListener: (Float, Float) -> Unit = { width, height -> canvas.resize(width, height) }

    // --- LIFECYCLE & INITIALIZATION ---

    fun init(platformHost: PlatformHost = MindustryPlatformHost()) {
        if (initialized) return

        initialized = true
        host = platformHost

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
        composition = UIComposition(canvas, content)
    }

    // --- FRAME RENDERING & DISPATCH ---

    fun draw() {
        val screenWidth = host.screenWidth
        val screenHeight = host.screenHeight
        if (screenWidth <= 0.0f || screenHeight <= 0.0f) return

        try {
            inputProcessor.update()
            canvas.resize(screenWidth, screenHeight)
            CompositionManager.frame()

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
        host.removeInputProcessor(inputProcessor)
        host.removeResize(resizeListener)
        host.removeFrameEnd(frameEndListener)
        CompositionManager.stop()
        UIBatch.dispose()
        initialized = false
    }
}
