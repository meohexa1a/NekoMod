package org.mdt.ui.input

import arc.Core
import arc.input.KeyCode

/**
 * ## TextEditState
 *
 * Headless, zero-allocation text editing state machine managing cursor positioning,
 * selection ranges, text insertion/deletion, clipboard operations, and cursor blink cycles.
 *
 * Designed as a decoupled model for [org.mdt.ui.widgets.TextFieldNode].
 */
class TextEditState(
    initialText: String = "",
    var onTextChange: ((String) -> Unit)? = null
) {
    var text: String = initialText
        private set

    /** 0-based character index of the active editing cursor. */
    var cursor: Int = initialText.length
        private set

    /** Selection anchor index (-1 when no selection is active). */
    var selectionStart: Int = -1
        private set

    /** Blink countdown timer in seconds. */
    var blinkTime: Float = 0f
        private set

    /** Whether the blinking cursor indicator is currently visible. */
    var cursorVisible: Boolean = true
        private set

    var isFocused: Boolean = false
        set(value) {
            field = value
            if (value) {
                resetBlink()
            } else {
                selectionStart = -1
            }
        }

    fun setText(newText: String) {
        if (text != newText) {
            text = newText
            cursor = cursor.coerceIn(0, text.length)
            selectionStart = -1
            resetBlink()
        }
    }

    fun resetBlink() {
        blinkTime = 0.32f
        cursorVisible = true
    }

    fun updateBlink(delta: Float) {
        if (!isFocused) {
            cursorVisible = false
            return
        }
        blinkTime -= delta
        if (blinkTime <= 0f) {
            blinkTime = 0.32f
            cursorVisible = !cursorVisible
        }
    }

    fun hasSelection(): Boolean = selectionStart != -1 && selectionStart != cursor

    fun getSelectionRange(): Pair<Int, Int>? {
        if (!hasSelection()) return null
        val min = minOf(selectionStart, cursor).coerceIn(0, text.length)
        val max = maxOf(selectionStart, cursor).coerceIn(0, text.length)
        return min to max
    }

    fun getSelectedText(): String? {
        val range = getSelectionRange() ?: return null
        return text.substring(range.first, range.second)
    }

    fun clearSelection() {
        selectionStart = -1
    }

    fun selectAll() {
        if (text.isNotEmpty()) {
            selectionStart = 0
            cursor = text.length
            resetBlink()
        }
    }

    /**
     * Deletes the currently selected text range if active.
     * @return True if a selection was deleted.
     */
    fun deleteSelection(): Boolean {
        val range = getSelectionRange() ?: return false
        val newText = text.substring(0, range.first) + text.substring(range.second)
        cursor = range.first
        selectionStart = -1
        text = newText
        onTextChange?.invoke(text)
        resetBlink()
        return true
    }

    fun insert(c: Char) {
        if (c < ' ' && c != '\t') return
        deleteSelection()
        val safeCursor = cursor.coerceIn(0, text.length)
        val newText = text.substring(0, safeCursor) + c + text.substring(safeCursor)
        cursor = safeCursor + 1
        text = newText
        onTextChange?.invoke(text)
        resetBlink()
    }

    fun insert(str: String) {
        if (str.isEmpty()) return
        deleteSelection()
        val safeCursor = cursor.coerceIn(0, text.length)
        val newText = text.substring(0, safeCursor) + str + text.substring(safeCursor)
        cursor = safeCursor + str.length
        text = newText
        onTextChange?.invoke(text)
        resetBlink()
    }

    fun backspace(): Boolean {
        if (deleteSelection()) return true
        if (cursor > 0 && text.isNotEmpty()) {
            val safeCursor = cursor.coerceIn(1, text.length)
            val newText = text.substring(0, safeCursor - 1) + text.substring(safeCursor)
            cursor = safeCursor - 1
            text = newText
            onTextChange?.invoke(text)
            resetBlink()
            return true
        }
        return false
    }

    fun delete(): Boolean {
        if (deleteSelection()) return true
        if (cursor < text.length && text.isNotEmpty()) {
            val safeCursor = cursor.coerceIn(0, text.length - 1)
            val newText = text.substring(0, safeCursor) + text.substring(safeCursor + 1)
            text = newText
            onTextChange?.invoke(text)
            resetBlink()
            return true
        }
        return false
    }

    fun moveCursor(index: Int, extendSelection: Boolean = false) {
        val target = index.coerceIn(0, text.length)
        if (extendSelection) {
            if (selectionStart == -1) selectionStart = cursor
        } else {
            selectionStart = -1
        }
        cursor = target
        resetBlink()
    }

    fun moveLeft(extendSelection: Boolean = false) {
        if (!extendSelection && hasSelection()) {
            val range = getSelectionRange()!!
            cursor = range.first
            selectionStart = -1
            resetBlink()
        } else {
            moveCursor(cursor - 1, extendSelection)
        }
    }

    fun moveRight(extendSelection: Boolean = false) {
        if (!extendSelection && hasSelection()) {
            val range = getSelectionRange()!!
            cursor = range.second
            selectionStart = -1
            resetBlink()
        } else {
            moveCursor(cursor + 1, extendSelection)
        }
    }

    fun moveToStart(extendSelection: Boolean = false) = moveCursor(0, extendSelection)
    fun moveToEnd(extendSelection: Boolean = false) = moveCursor(text.length, extendSelection)

    fun copy() {
        val sel = getSelectedText()
        if (!sel.isNullOrEmpty()) {
            Core.app.clipboardText = sel
        }
    }

    fun cut() {
        val sel = getSelectedText()
        if (!sel.isNullOrEmpty()) {
            Core.app.clipboardText = sel
            deleteSelection()
        }
    }

    fun paste() {
        val clip = Core.app.clipboardText
        if (!clip.isNullOrEmpty()) {
            // Remove carriage returns
            val sanitized = clip.replace("\r", "")
            insert(sanitized)
        }
    }

    /**
     * Handles printable character insertion. Control keys are handled in onKeyDown.
     */
    fun onKeyTyped(char: Char): Boolean {
        if (!isFocused) return false
        // Control characters (< 32, including \b) are handled by onKeyDown
        if (char >= ' ' || char == '\t') {
            insert(char)
            return true
        }
        return false
    }

    fun onKeyDown(key: KeyCode): Boolean {
        if (!isFocused) return false
        val isCtrl = Core.input.ctrl()
        val isShift = Core.input.shift()

        return when (key) {
            KeyCode.backspace -> backspace()
            KeyCode.del -> delete()
            KeyCode.left -> {
                moveLeft(isShift)
                true
            }
            KeyCode.right -> {
                moveRight(isShift)
                true
            }
            KeyCode.home -> {
                moveToStart(isShift)
                true
            }
            KeyCode.end -> {
                moveToEnd(isShift)
                true
            }
            KeyCode.a -> if (isCtrl) {
                selectAll()
                true
            } else false
            KeyCode.c -> if (isCtrl) {
                copy()
                true
            } else false
            KeyCode.v -> if (isCtrl) {
                paste()
                true
            } else false
            KeyCode.x -> if (isCtrl) {
                cut()
                true
            } else false
            else -> false
        }
    }
}
