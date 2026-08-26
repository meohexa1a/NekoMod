package org.mdt.core.ui

import arc.Core
import arc.Events
import arc.util.Log
import mindustry.game.EventType.ResizeEvent
import mindustry.game.EventType.Trigger
import org.mdt.core.image.ImageLoader
import org.mdt.core.ui.compose.CompositionManager
import org.mdt.core.ui.compose.UIComposition
import org.mdt.core.ui.CanvasNode
import org.mdt.core.ui.input.EngineInputProcessor
import org.mdt.ui.render.EngineRenderer

object EngineRuntime {
    val canvas = CanvasNode()
    val renderer = EngineRenderer()
    val inputProcessor = EngineInputProcessor(canvas)

    private var composition: UIComposition? = null
    private var initialized = false

    fun init() {
        if (initialized) return
        initialized = true

        // 1. Insert our input processor at the very front (Priority 1)
        val processors = Core.input.inputProcessors
        if (!processors.contains(inputProcessor)) processors.insert(0, inputProcessor)

        // 2. Set initial canvas dimensions
        canvas.resize(Core.graphics.width.toFloat(), Core.graphics.height.toFloat())

        // 3. Listen for window resize events
        Events.on(ResizeEvent::class.java) {
            canvas.resize(Core.graphics.width.toFloat(), Core.graphics.height.toFloat())
        }

        // 4. Hook render loop to Trigger.uiDrawEnd (runs ON TOP of native UI)
        Events.run(Trigger.uiDrawEnd) { draw() }

        Log.info("[NekoMod] EngineRuntime initialized successfully.")
    }

    fun setContent(content: @androidx.compose.runtime.Composable () -> Unit) {
        init()
        composition?.dispose()
        composition = UIComposition(canvas, content)
    }

    fun draw() {
        if (Core.graphics == null) return
        val w = Core.graphics.width.toFloat()
        val h = Core.graphics.height.toFloat()
        if (w <= 0f || h <= 0f) return

        try {
            // Process queued GPU uploads for ImageLoader
            ImageLoader.processUploadQueue()

            canvas.resize(w, h)
            CompositionManager.frame()
            renderer.render(canvas)
        } catch (t: Throwable) {
            Log.err("[NekoMod] Error in EngineRuntime.draw()", t)
        }
    }

    fun dispose() {
        composition?.dispose()
        composition = null
        Core.input.removeProcessor(inputProcessor)
        CompositionManager.stop()
        initialized = false
    }
}
