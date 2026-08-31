// [AGENT INVARIANT] Synchronously update @property, @param, and @see KDocs when modifying this file.

package org.mdt.core.platform

import arc.Core
import arc.input.InputMultiplexer
import arc.input.InputProcessor
import arc.input.KeyBind
import arc.input.KeyCode
import arc.struct.Seq
import arc.util.Log
import java.lang.reflect.Field
import java.lang.reflect.Method

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
 * @see MindustryInputPort
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

/**
 * ## MindustryInputPort
 *
 * Implements [InputPort] by bridging to Arc's `Core.input` and injecting a local [InputMultiplexer].
 *
 * @see InputPort
 * @see org.mdt.core.platform.PlatformHost
 */
class MindustryInputPort : InputPort {

    override val mouseX: Float
        get() = Core.input?.mouseX()?.toFloat() ?: 0.0f

    override val mouseY: Float
        get() = Core.input?.mouseY()?.toFloat() ?: 0.0f

    override val isCtrlPressed: Boolean
        get() = Core.input?.ctrl() ?: false

    override val isShiftPressed: Boolean
        get() = Core.input?.shift() ?: false

    override val isAltPressed: Boolean
        get() = Core.input?.alt() ?: false

    override fun addInputProcessor(processor: InputProcessor) {
        val processors = Core.input?.inputProcessors
        if (processors != null && !processors.contains(localMultiplexer, true)) {
            processors.insert(0, localMultiplexer)
        }

        if (!localMultiplexer.processors.contains(processor, true)) {
            localMultiplexer.addProcessor(0, processor)
        }
    }

    override fun removeInputProcessor(processor: InputProcessor) {
        localMultiplexer.removeProcessor(processor)
    }

    companion object {
        private val localMultiplexer by lazy {
            val inputMultiplexer = InputMultiplexer()
            val inputProcessors = Core.input?.inputProcessors

            when {
                inputProcessors != null && !inputProcessors.contains(
                    inputMultiplexer,
                    true,
                ) -> inputProcessors.insert(0, inputMultiplexer)

                inputProcessors == null -> Log.warn("[NekoMod] Core.input is uninitialized when creating InputMultiplexer")
            }

            inputMultiplexer
        }
    }
}

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
        onCompositionCleared: () -> Unit,
    )

    /** Synchronizes screen position, size, text, and cursor bounds for the active native IME session. */
    fun syncSession(
        globalX: Float,
        globalY: Float,
        width: Float,
        height: Float,
        text: String,
        cursorPosition: Int,
    )

    fun stopSession()

    companion object {
        val NoOp: ImePort = object : ImePort {
            override fun startSession(
                globalX: Float,
                globalY: Float,
                width: Float,
                height: Float,
                initialText: String,
                cursorPosition: Int,
                onCompositionChanged: (String) -> Unit,
                onCompositionCleared: () -> Unit,
            ) {
            }

            override fun syncSession(
                globalX: Float,
                globalY: Float,
                width: Float,
                height: Float,
                text: String,
                cursorPosition: Int,
            ) {
            }

            override fun stopSession() {}
        }
    }
}

// --- SAFE REFLECTION HELPER FOR SDL PLATFORM ---

private object SdlNativeHelper {
    val isAvailable: Boolean by lazy {
        try {
            Class.forName("arc.backend.sdl.jni.SDL")
            true
        } catch (_: Throwable) {
            false
        }
    }

    private val startTextInputMethod: Method? by lazy {
        try {
            Class.forName("arc.backend.sdl.jni.SDL").getMethod("SDL_StartTextInput")
        } catch (_: Throwable) {
            null
        }
    }
    private val stopTextInputMethod: Method? by lazy {
        try {
            Class.forName("arc.backend.sdl.jni.SDL").getMethod("SDL_StopTextInput")
        } catch (_: Throwable) {
            null
        }
    }
    private val setTextInputRectMethod: Method? by lazy {
        try {
            Class.forName("arc.backend.sdl.jni.SDL").getMethod(
                "SDL_SetTextInputRect",
                Integer.TYPE,
                Integer.TYPE,
                Integer.TYPE,
                Integer.TYPE,
            )
        } catch (_: Throwable) {
            null
        }
    }

    fun startTextInput() {
        try {
            startTextInputMethod?.invoke(null)
        } catch (_: Throwable) {
        }
    }

    fun stopTextInput() {
        try {
            stopTextInputMethod?.invoke(null)
        } catch (_: Throwable) {
        }
    }

    fun setTextInputRect(x: Int, y: Int, w: Int, h: Int) {
        try {
            setTextInputRectMethod?.invoke(null, x, y, w, h)
        } catch (_: Throwable) {
        }
    }
}

/**
 * ## SdlReflectionImePort
 *
 * Connects native SDL IME composition events to the UI engine using an in-memory Reflection proxy on `SdlInput.stringEditEvents`.
 * Bypasses Arc Scene2D (`arc.scene.ui.TextField`) and avoids hijacking `Core.scene.keyboardFocus`.
 * Routes IME composition candidates directly to the active text field while passing through events to Mindustry when idle.
 *
 * @param hostProvider Non-null provider lambda returning the ambient [PlatformHost] facade.
 *
 * @see ImePort
 * @see PlatformHost
 */
class SdlReflectionImePort(
    private val hostProvider: () -> PlatformHost,
) : ImePort {

    private var activeCompositionCallback: ((String) -> Unit)? = null
    private var activeClearCallback: (() -> Unit)? = null
    private var isTextInputActive: Boolean = false
    private var isHookInstalled: Boolean = false

    // --- IME SESSION LIFECYCLE ---

    override fun startSession(
        globalX: Float,
        globalY: Float,
        width: Float,
        height: Float,
        initialText: String,
        cursorPosition: Int,
        onCompositionChanged: (composition: String) -> Unit,
        onCompositionCleared: () -> Unit,
    ) {
        if (!SdlNativeHelper.isAvailable) return

        ensureHook()
        activeCompositionCallback = onCompositionChanged
        activeClearCallback = onCompositionCleared

        try {
            if (!isTextInputActive) {
                SdlNativeHelper.startTextInput()
                isTextInputActive = true
            }
            updateTextInputRect(globalX, globalY, width, height)
        } catch (sdlError: Throwable) {
            Log.warn("[NekoMod] SDL_StartTextInput invocation failed", sdlError)
        }
    }

    override fun syncSession(
        globalX: Float,
        globalY: Float,
        width: Float,
        height: Float,
        text: String,
        cursorPosition: Int,
    ) {
        if (!isTextInputActive) return

        updateTextInputRect(globalX, globalY, width, height)
    }

    override fun stopSession() {
        if (!isTextInputActive) return

        try {
            SdlNativeHelper.stopTextInput()
            isTextInputActive = false
        } catch (sdlError: Throwable) {
            Log.warn("[NekoMod] SDL_StopTextInput invocation failed", sdlError)
        }

        activeCompositionCallback = null
        activeClearCallback = null
    }

    fun clearComposition() {
        val clearCb = activeClearCallback
        activeCompositionCallback?.invoke("")

        if (isTextInputActive) {
            try {
                SdlNativeHelper.stopTextInput()
                SdlNativeHelper.startTextInput()
            } catch (sdlError: Throwable) {
                Log.warn("[NekoMod] Restarting text input failed during clearComposition", sdlError)
            }
        }

        clearCb?.invoke()
    }

    // --- SDL RECT SYNCHRONIZATION ---

    private fun updateTextInputRect(globalX: Float, globalY: Float, width: Float, height: Float) {
        try {
            val portHeight = hostProvider().window.height
            val screenHeight = when {
                portHeight > 0.0f -> portHeight.toInt()
                else -> 1080
            }
            val sdlY = screenHeight - 1 - (globalY + height).toInt()
            SdlNativeHelper.setTextInputRect(globalX.toInt(), sdlY, width.toInt(), height.toInt())
        } catch (rectError: Throwable) {
            Log.warn("[NekoMod] SDL_SetTextInputRect invocation failed", rectError)
        }
    }

    // --- REFLECTION HOOK ---

    private fun ensureHook() {
        if (isHookInstalled) return

        val input = Core.input ?: return
        try {
            val stringEditEventsField = findField(input.javaClass, "stringEditEvents")
            if (stringEditEventsField == null) {
                Log.warn("[NekoMod] Unable to locate 'stringEditEvents' field on SdlInput.")
                return
            }
            stringEditEventsField.isAccessible = true

            @Suppress("UNCHECKED_CAST")
            val originalSeq = stringEditEventsField.get(input) as? Seq<Any>
            if (originalSeq == null) {
                Log.warn("[NekoMod] 'stringEditEvents' field on SdlInput is null or incompatible.")
                return
            }

            val proxySeq = object : Seq<Any>() {
                override fun add(value: Any): Seq<Any> {
                    val compositionCallback = activeCompositionCallback ?: return originalSeq.add(value)
                    val text = extractEditText(value) ?: return originalSeq.add(value)

                    when {
                        text.isNotEmpty() -> compositionCallback(text)
                        else -> activeClearCallback?.invoke()
                    }

                    // Swallowed: prevent Arc Scene2D from receiving candidate events
                    return this
                }

                override fun clear(): Seq<Any> {
                    originalSeq.clear()
                    return super.clear()
                }
            }

            stringEditEventsField.set(input, proxySeq)
            isHookInstalled = true
            Log.info("[NekoMod] SdlReflectionImePort installed successfully.")
        } catch (hookError: Throwable) {
            Log.err("[NekoMod] Failed to install SdlReflectionImePort hook", hookError)
        }
    }

    private fun extractEditText(editEvent: Any): String? {
        val field = resolveTextField(editEvent.javaClass)
        if (field == null) {
            Log.warn("[NekoMod] Unable to locate 'text' field on SdlInput.EditEvent.")
            return null
        }

        return try {
            field.get(editEvent) as? String
        } catch (fieldReadError: Throwable) {
            Log.warn("[NekoMod] Failed to read EditEvent.text field", fieldReadError)
            null
        }
    }

    private fun resolveTextField(eventClass: Class<*>): Field? {
        val cached = cachedTextField
        if (cached != null) return cached

        val resolved = findField(eventClass, "text") ?: return null
        resolved.isAccessible = true
        cachedTextField = resolved
        return resolved
    }

    companion object {
        private var cachedTextField: Field? = null

        private fun findField(clazz: Class<*>, name: String): Field? {
            var current: Class<*>? = clazz
            while (current != null && current != Any::class.java) {
                try {
                    return current.getDeclaredField(name)
                } catch (_: NoSuchFieldException) {
                    current = current.superclass
                }
            }
            return null
        }
    }
}
