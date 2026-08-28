package org.mdt.core.ui

import androidx.compose.runtime.Composable
import arc.util.Log
import org.mdt.core.engine.MindustryPlatformHost
import org.mdt.core.engine.PlatformHost
import org.mdt.core.ui.compose.CompositionManager
import org.mdt.core.ui.compose.UIComposition
import org.mdt.core.ui.input.EngineInputProcessor
import org.mdt.core.ui.render.UIBatch

/**
 * ## EngineRuntime
 *
 * Top-level UI orchestration runtime and display surface bridge.
 * Connects the host platform's window, viewport resizing, input event dispatch chain,
 * Compose snapshot frame scheduler, and OpenGL GPU batch renderer to the root [CanvasNode].
 *
 * See: docs/ui-engine/ui_engine_en.md
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
        canvas.clearOverlays()
        CompositionManager.stop()
        UIBatch.dispose()
        initialized = false
    }
}
