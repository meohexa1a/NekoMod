// [AGENT INVARIANT] Synchronously update @property, @param, and @see KDocs when modifying this file.

package org.mdt.core.platform.window

import arc.Core
import arc.Events
import arc.Graphics.Cursor
import java.util.concurrent.CopyOnWriteArrayList
import mindustry.game.EventType.ResizeEvent

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
