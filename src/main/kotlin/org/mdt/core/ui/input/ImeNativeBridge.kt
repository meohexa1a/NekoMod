package org.mdt.core.ui.input

import arc.Core
import arc.backend.sdl.jni.SDL
import arc.math.geom.Vec2
import arc.scene.ui.TextField
import org.mdt.ui.components.input.textfield.TextFieldNode

/**
 * ## ImeNativeBridge
 *
 * Industrial-standard IME Native Bridge coordinating OS IME candidate window
 * positioning, pre-edit composition buffer (`SDL_EVENT_TEXT_EDIT`), and committed text.
 *
 * See: docs/complex-challenges/complex_challenges_en.md
 */
object ImeNativeBridge {
    private var isTextInputActive: Boolean = false
    private var activeField: TextFieldNode? = null

    /** Headless receiver for Arc's SdlInput to receive SDL_EVENT_TEXT_EDIT composition events. */
    private val bridgeElement = object : TextField("") {
        override fun localToStageCoordinates(vec: Vec2): Vec2 {
            val field = activeField ?: return vec.setZero()
            val globalPos = field.localToGlobal(0f, 0f)
            return vec.set(globalPos.x, globalPos.y)
        }

        override fun setSelection(selectionStart: Int, selectionEnd: Int) {
            super.setSelection(selectionStart, selectionEnd)
            val field = activeField ?: return
            if (imeData != null) {
                val fullText = this.text ?: ""
                val start = minOf(selectionStart, selectionEnd).coerceIn(0, fullText.length)
                val end = maxOf(selectionStart, selectionEnd).coerceIn(0, fullText.length)
                if (end > start) {
                    val candidate = fullText.substring(start, end)
                    field.editState.setComposition(candidate)
                    field.invalidateLayout()
                }
            }
        }

        override fun clearSelection() {
            super.clearSelection()
            if (imeData == null) {
                activeField?.editState?.clearComposition()
                activeField?.invalidateLayout()
            }
        }

        override fun setText(str: String?) {
            super.setText(str)
            if (imeData == null) {
                activeField?.editState?.clearComposition()
                activeField?.invalidateLayout()
            }
        }
    }

    /** Attaches [node] to the active IME session. */
    fun attach(node: TextFieldNode) {
        activeField = node
        bridgeElement.text = node.editState.text
        bridgeElement.cursorPosition = node.editState.cursor
        val globalPos = node.localToGlobal(0f, 0f)
        bridgeElement.setPosition(globalPos.x, globalPos.y)
        bridgeElement.setSize(node.bounds.width, node.bounds.height)

        if (Core.scene != null && Core.scene.keyboardFocus !== bridgeElement) {
            Core.scene.keyboardFocus = bridgeElement
        }

        startTextInput(globalPos.x, globalPos.y, node.bounds.width, node.bounds.height)
    }

    /** Synchronizes screen position and size for the active [node]. */
    fun sync(node: TextFieldNode) {
        if (activeField === node) {
            bridgeElement.text = node.editState.text
            bridgeElement.cursorPosition = node.editState.cursor
            val globalPos = node.localToGlobal(0f, 0f)
            bridgeElement.setPosition(globalPos.x, globalPos.y)
            bridgeElement.setSize(node.bounds.width, node.bounds.height)

            if (Core.scene != null && Core.scene.keyboardFocus !== bridgeElement) {
                Core.scene.keyboardFocus = bridgeElement
            }

            updateTextInputRect(globalPos.x, globalPos.y, node.bounds.width, node.bounds.height)
        }
    }

    /** Detaches [node] when focus is lost. */
    fun detach(node: TextFieldNode) {
        if (activeField === node) {
            activeField = null
            node.editState.clearComposition()
            if (Core.scene != null && Core.scene.keyboardFocus === bridgeElement) {
                Core.scene.keyboardFocus = null
            }
            stopTextInput()
        }
    }

    /** Starts native OS text input and positions the IME candidate window at [globalX, globalY, width, height]. */
    fun startTextInput(globalX: Float, globalY: Float, width: Float, height: Float) {
        try {
            if (!isTextInputActive) {
                SDL.SDL_StartTextInput()
                isTextInputActive = true
            }
            updateTextInputRect(globalX, globalY, width, height)
        } catch (_: Throwable) {
            // Safe fallback on non-SDL platforms or test environments
        }
    }

    /** Updates the screen rectangle for the native IME candidate box. */
    fun updateTextInputRect(globalX: Float, globalY: Float, width: Float, height: Float) {
        try {
            if (!isTextInputActive) return
            val screenHeight = if (Core.graphics != null && Core.graphics.height > 0) Core.graphics.height else 1080
            // SDL uses top-left coordinates: Y is inverted from OpenGL bottom-left
            val sdlY = screenHeight - 1 - (globalY + height).toInt()
            SDL.SDL_SetTextInputRect(globalX.toInt(), sdlY, width.toInt(), height.toInt())
        } catch (_: Throwable) {
            // Safe fallback
        }
    }

    /** Stops native OS text input session. */
    fun stopTextInput() {
        try {
            if (isTextInputActive) {
                SDL.SDL_StopTextInput()
                isTextInputActive = false
            }
        } catch (_: Throwable) {
            // Safe fallback
        }
    }
}
