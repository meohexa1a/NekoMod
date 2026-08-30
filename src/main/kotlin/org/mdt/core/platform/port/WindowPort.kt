package org.mdt.core.platform.port

import arc.Graphics.Cursor

/**
 * ## WindowPort [Display & Viewport Contract]
 *
 * > 🤖 **AGENT DIRECTIVE:** When modifying this class or related logic, synchronously update this KDoc:
 * > 1) Keep Purpose accurate. 2) Update Key Rules & Checklist [x]/[ ]. 3) Maintain Related Files map.
 *
 * ### 1. Purpose
 * - Defines the abstract contract for display surface dimensions, window resize events, and system cursor control.
 * - Includes a built-in [NoOp] stub for headless execution or unit testing.
 *
 * ### 2. Key Rules & Checklist
 * - [x] `width` and `height` must return `Float` in screen pixel coordinates.
 * - [x] Resize callbacks must be thread-safe.
 *
 * ### 3. Related Files
 * - Implementation: `src/main/kotlin/org/mdt/core/platform/impl/MindustryWindowPort.kt`
 * - Platform Host: `src/main/kotlin/org/mdt/core/platform/PlatformHost.kt`
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
