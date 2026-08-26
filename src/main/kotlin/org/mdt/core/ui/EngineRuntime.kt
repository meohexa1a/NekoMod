package org.mdt.core.ui

import androidx.compose.runtime.Composable
import arc.util.Log
import org.mdt.core.engine.EngineContext
import org.mdt.core.ui.compose.CompositionManager
import org.mdt.core.ui.compose.UIComposition
import org.mdt.core.ui.input.EngineInputProcessor
import org.mdt.core.ui.render.EngineRenderer

/**
 * ## EngineRuntime
 *
 * Top-level UI orchestration runtime and display surface bridge.
 * Connects the host platform's window, viewport resizing, input event dispatch chain,
 * Compose snapshot frame scheduler, and OpenGL GPU batch renderer to the root [CanvasNode].
 *
 * Designed as the 1:1 host UI runtime bridge for the active display window.
 *
 * See: docs/ui-engine/ui_engine_en.md
 */
object EngineRuntime {

    /** Root Virtual DOM canvas container node. */
    val canvas = CanvasNode()

    /** Core 2D OpenGL batch renderer. */
    val renderer = EngineRenderer()

    /** High-priority input processor intercepting pointer and keyboard gestures for the UI canvas. */
    val inputProcessor = EngineInputProcessor(canvas)

    private var activeContext: EngineContext = EngineContext.default
    private var composition: UIComposition? = null
    private var initialized = false

    // =========================================================================
    // I. Lifecycle Initialization & Content Mounting
    // =========================================================================

    /**
     * Initializes the UI runtime bridge against the specified [EngineContext].
     * Hooks viewport resize listeners, input processor, and end-of-frame render loop onto [EngineContext.host].
     *
     * @param context Target engine context environment (defaults to [EngineContext.default]).
     */
    fun init(context: EngineContext = EngineContext.default) {
        if (initialized) return
        initialized = true
        activeContext = context

        val host = context.host

        // 1. Insert input processor at the front of the input dispatch chain
        host.addInputProcessor(inputProcessor)

        // 2. Set initial canvas surface dimensions
        canvas.resize(host.screenWidth, host.screenHeight)

        // 3. Listen for window resize events
        host.onResize { width, height -> canvas.resize(width, height) }

        // 4. Hook frame render dispatch to end of frame
        host.onFrameEnd { draw() }

        Log.info("[NekoMod] EngineRuntime initialized successfully via PlatformHost.")
    }

    /**
     * Mounts a declarative Compose UI tree onto the root [canvas].
     * Automatically initializes the runtime if not yet started.
     *
     * @param content Composable UI hierarchy definition.
     */
    fun setContent(content: @Composable () -> Unit) {
        init()
        composition?.dispose()
        composition = UIComposition(canvas, content)
    }

    // =========================================================================
    // II. Frame Rendering & Teardown
    // =========================================================================

    /**
     * Executes a single UI frame rendering pass:
     * synchronizes viewport dimensions, dispatches Compose recomposition frames,
     * and renders the virtual DOM tree via [EngineRenderer].
     */
    fun draw() {
        val host = activeContext.host
        val screenWidth = host.screenWidth
        val screenHeight = host.screenHeight
        if (screenWidth <= 0f || screenHeight <= 0f) return

        try {
            canvas.resize(screenWidth, screenHeight)
            CompositionManager.frame()
            renderer.render(canvas)
        } catch (renderError: Throwable) {
            Log.err("[NekoMod] Error in EngineRuntime.draw()", renderError)
        }
    }

    /**
     * Disposes the active Compose composition and unhooks the input processor from the host platform.
     */
    fun dispose() {
        composition?.dispose()
        composition = null
        activeContext.host.removeInputProcessor(inputProcessor)
        CompositionManager.stop()
        initialized = false
    }
}
