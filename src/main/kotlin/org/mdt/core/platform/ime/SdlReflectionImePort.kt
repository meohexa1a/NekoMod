// [AGENT INVARIANT] Synchronously update @property, @param, and @see KDocs when modifying this file.

package org.mdt.core.platform.ime

import arc.Core
import arc.backend.sdl.jni.SDL
import arc.struct.Seq
import arc.util.Log
import java.lang.reflect.Field

/**
 * ## SdlReflectionImePort
 *
 * Connects native SDL IME composition events to the UI engine using an in-memory Reflection proxy on `SdlInput.stringEditEvents`.
 * Bypasses Arc Scene2D (`arc.scene.ui.TextField`) and avoids hijacking `Core.scene.keyboardFocus`.
 * Routes IME composition candidates directly to the active text field while passing through events to Mindustry when idle.
 *
 * @see ImePort
 * @see PlatformHost
 */
class SdlReflectionImePort : ImePort {

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
        onCompositionCleared: () -> Unit
    ) {
        ensureHook()
        activeCompositionCallback = onCompositionChanged
        activeClearCallback = onCompositionCleared

        try {
            if (!isTextInputActive) {
                SDL.SDL_StartTextInput()
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
        cursorPosition: Int
    ) {
        if (isTextInputActive) {
            updateTextInputRect(globalX, globalY, width, height)
        }
    }

    override fun stopSession() {
        val clearCb = activeClearCallback
        activeCompositionCallback = null
        activeClearCallback = null

        try {
            if (isTextInputActive) {
                SDL.SDL_StopTextInput()
                isTextInputActive = false
            }
        } catch (sdlError: Throwable) {
            Log.warn("[NekoMod] SDL_StopTextInput invocation failed", sdlError)
        }

        clearCb?.invoke()
    }

    // --- SDL RECT SYNCHRONIZATION ---

    private fun updateTextInputRect(globalX: Float, globalY: Float, width: Float, height: Float) {
        try {
            val screenHeight = when {
                Core.graphics != null && Core.graphics.height > 0 -> Core.graphics.height
                else -> 1080
            }
            val sdlY = screenHeight - 1 - (globalY + height).toInt()
            SDL.SDL_SetTextInputRect(globalX.toInt(), sdlY, width.toInt(), height.toInt())
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
