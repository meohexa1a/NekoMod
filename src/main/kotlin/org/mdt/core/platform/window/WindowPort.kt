// [AGENT INVARIANT] Synchronously update @property, @param, and @see KDocs when modifying this file.

package org.mdt.core.platform.window

import arc.Graphics.Cursor

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
