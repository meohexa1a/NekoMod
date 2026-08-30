package org.mdt.core.platform.ime

/**
 * ## ImePort [Platform IME Contract]
 *
 * > 🤖 **AGENT DIRECTIVE:** When modifying this class or related logic, synchronously update this KDoc:
 * > 1) Keep Purpose accurate. 2) Update Key Rules & Checklist [x]/[ ]. 3) Maintain Related Files map.
 *
 * ### 1. Purpose
 * - Defines the platform-agnostic contract for native OS IME text composition sessions.
 * - Bridges text field focus and screen coordinates with operating system IME candidate windows.
 * - Includes a built-in [NoOp] implementation for headless servers, unit testing, or mock environments.
 *
 * ### 2. Key Rules & Checklist
 * - [x] All screen coordinates and bounds must use `Float` with OpenGL bottom-left origin.
 * - [x] `startSession` must handle ongoing sessions gracefully and set active composition listeners.
 * - [x] `stopSession` clears active callbacks and requests the OS to hide the virtual keyboard / IME box.
 *
 * ### 3. Related Files
 * - Sdl Reflection Port: `src/main/kotlin/org/mdt/core/platform/ime/SdlReflectionImePort.kt`
 * - Platform Host: `src/main/kotlin/org/mdt/core/platform/PlatformHost.kt`
 * - Input Processor: `src/main/kotlin/org/mdt/core/ui/input/EngineInputProcessor.kt`
 */
interface ImePort {

    /** Starts an interactive native OS IME text input session for a focused text field. */
    fun startSession(
        globalX: Float,
        globalY: Float,
        width: Float,
        height: Float,
        initialText: String,
        cursorPosition: Int,
        onCompositionChanged: (composition: String) -> Unit,
        onCompositionCleared: () -> Unit
    )

    /** Synchronizes screen position, size, text, and cursor bounds for the active native IME session. */
    fun syncSession(
        globalX: Float,
        globalY: Float,
        width: Float,
        height: Float,
        text: String,
        cursorPosition: Int
    )

    /** Stops the native OS text input session and clears active listeners. */
    fun stopSession()

    /**
     * Stub [ImePort] implementation for headless servers or unit testing environments.
     */
    object NoOp : ImePort {
        override fun startSession(
            globalX: Float,
            globalY: Float,
            width: Float,
            height: Float,
            initialText: String,
            cursorPosition: Int,
            onCompositionChanged: (composition: String) -> Unit,
            onCompositionCleared: () -> Unit
        ) = Unit

        override fun syncSession(
            globalX: Float,
            globalY: Float,
            width: Float,
            height: Float,
            text: String,
            cursorPosition: Int
        ) = Unit

        override fun stopSession() = Unit
    }
}
