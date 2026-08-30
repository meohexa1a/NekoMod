// [AGENT INVARIANT] Synchronously update @property, @param, and @see KDocs when modifying this file.

package org.mdt.core.platform.port

import arc.input.InputProcessor

/**
 * ## InputPort
 *
 * Defines the abstract contract for pointer coordinates, keyboard modifier states, and input processor dispatch chains.
 * Includes a built-in [NoOp] stub for headless execution or unit testing.
 *
 * @property mouseX Current pointer X position in screen coordinates.
 * @property mouseY Current pointer Y position in screen coordinates.
 * @property isCtrlPressed Whether the Ctrl (or Cmd on macOS) key is currently held down.
 * @property isShiftPressed Whether the Shift key is currently held down.
 * @property isAltPressed Whether the Alt key is currently held down.
 *
 * @see org.mdt.core.platform.impl.MindustryInputPort
 * @see org.mdt.core.platform.PlatformHost
 */
interface InputPort {

    /** Current pointer X position in screen coordinates. */
    val mouseX: Float

    /** Current pointer Y position in screen coordinates. */
    val mouseY: Float

    /** Whether the Ctrl (or Cmd on macOS) key is currently pressed down. */
    val isCtrlPressed: Boolean

    /** Whether the Shift key is currently pressed down. */
    val isShiftPressed: Boolean

    /** Whether the Alt key is currently pressed down. */
    val isAltPressed: Boolean

    /** Adds an input processor to the top of the input dispatch chain. */
    fun addInputProcessor(processor: InputProcessor)

    /** Removes an input processor from the input dispatch chain. */
    fun removeInputProcessor(processor: InputProcessor)

    /** Stub [InputPort] implementation for headless or testing environments. */
    object NoOp : InputPort {
        override val mouseX: Float get() = 0.0f
        override val mouseY: Float get() = 0.0f
        override val isCtrlPressed: Boolean get() = false
        override val isShiftPressed: Boolean get() = false
        override val isAltPressed: Boolean get() = false
        override fun addInputProcessor(processor: InputProcessor) = Unit
        override fun removeInputProcessor(processor: InputProcessor) = Unit
    }
}
