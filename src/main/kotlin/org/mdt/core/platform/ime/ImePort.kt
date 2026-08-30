// [AGENT INVARIANT] Synchronously update @property, @param, and @see KDocs when modifying this file.

package org.mdt.core.platform.ime

/**
 * ## ImePort
 *
 * Defines the platform-agnostic contract for native OS IME text composition sessions.
 * Bridges text field focus and screen coordinates with operating system IME candidate windows.
 * Includes a built-in [NoOp] implementation for headless servers, unit testing, or mock environments.
 *
 * @see SdlReflectionImePort
 * @see PlatformHost
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
