package org.mdt.core.platform.impl

import arc.Core
import arc.Events
import arc.Graphics.Cursor
import java.util.concurrent.CopyOnWriteArrayList
import mindustry.game.EventType.ResizeEvent
import org.mdt.core.platform.port.WindowPort

/**
 * ## MindustryWindowPort [Mindustry / Arc Window Implementation]
 *
 * > 🤖 **AGENT DIRECTIVE:** When modifying this class or related logic, synchronously update this KDoc:
 * > 1) Keep Purpose accurate. 2) Update Key Rules & Checklist [x]/[ ]. 3) Maintain Related Files map.
 *
 * ### 1. Purpose
 * - Implements [WindowPort] by bridging to Arc's `Core.graphics` and Mindustry's `ResizeEvent`.
 *
 * ### 2. Key Rules & Checklist
 * - [x] Resize listeners must be thread-safe ([CopyOnWriteArrayList]).
 * - [x] Fall back safely to `0.0f` if graphics context is uninitialized.
 * - [x] Keep initialization flat and explicit without excessive inline scope functions.
 *
 * ### 3. Related Files
 * - Window Port: `src/main/kotlin/org/mdt/core/platform/port/WindowPort.kt`
 * - Platform Host: `src/main/kotlin/org/mdt/core/platform/PlatformHost.kt`
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
