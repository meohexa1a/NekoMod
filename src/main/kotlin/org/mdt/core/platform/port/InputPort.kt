package org.mdt.core.platform.port

import arc.input.InputProcessor

/**
 * ## InputPort [Pointer & Hardware Input Contract]
 *
 * > 🤖 **AGENT DIRECTIVE:** When modifying this class or related logic, synchronously update this KDoc:
 * > 1) Keep Purpose accurate. 2) Update Key Rules & Checklist [x]/[ ]. 3) Maintain Related Files map.
 *
 * ### 1. Purpose
 * - Defines the abstract contract for pointer coordinates, keyboard modifier states, and input processor dispatch chains.
 * - Includes a built-in [NoOp] stub for headless execution or unit testing.
 *
 * ### 2. Key Rules & Checklist
 * - [x] Pointer coordinates must use OpenGL bottom-left coordinates (`0.0f` at bottom).
 * - [x] Processor chains must safely handle concurrent addition/removal.
 *
 * ### 3. Related Files
 * - Implementation: `src/main/kotlin/org/mdt/core/platform/impl/MindustryInputPort.kt`
 * - Platform Host: `src/main/kotlin/org/mdt/core/platform/PlatformHost.kt`
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
