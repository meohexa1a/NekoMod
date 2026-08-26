package org.mdt.ui.components.input.textfield

import arc.Core
import arc.input.KeyCode
import arc.math.Mathf

/**
 * ## TextEditState
 *
 * Headless string manipulation state machine for text inputs.
 * Manages character insertion, bounds clamping, cursor navigation,
 * selection ranges, OS clipboard, and blink timers.
 *
 * See: docs/complex-challenges/complex_challenges_en.md
 */
class TextEditState(
    var onTextChange: ((String) -> Unit)? = null
) {
    var text: String = ""
        private set

    var cursor: Int = 0
        private set

    var selectionStart: Int = -1
        private set

    var isFocused: Boolean = false

    var cursorVisible: Boolean = true
        private set

    private var blinkTimer: Float = 0f
    private val blinkInterval: Float = 0.32f

    fun setText(newText: String) {
        if (text != newText) {
            text = newText
            cursor = cursor.coerceIn(0, text.length)
            selectionStart = -1
            resetBlink()
        }
    }

    fun resetBlink() {
        blinkTimer = 0f
        cursorVisible = true
    }

    fun updateBlink(delta: Float) {
        if (!isFocused) {
            cursorVisible = false
            return
        }
        blinkTimer += delta
        if (blinkTimer >= blinkInterval) {
            blinkTimer -= blinkInterval
            cursorVisible = !cursorVisible
        }
    }

    fun hasSelection(): Boolean = selectionStart != -1 && selectionStart != cursor

    fun getSelectionRange(): Pair<Int, Int>? {
        if (!hasSelection()) return null
        val start = minOf(selectionStart, cursor).coerceIn(0, text.length)
        val end = maxOf(selectionStart, cursor).coerceIn(0, text.length)
        return Pair(start, end)
    }

    fun getSelectedText(): String {
        val range = getSelectionRange() ?: return ""
        return text.substring(range.first, range.second)
    }

    fun selectAll() {
        if (text.isEmpty()) return
        selectionStart = 0
        cursor = text.length
        resetBlink()
    }

    @Suppress("unused")
    fun clearSelection() {
        selectionStart = -1
    }

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

    // =========================================================================
    // I. Character & String Insertion
    // =========================================================================

    fun insert(character: Char) {
        if (character < ' ' && character != '\t') return

        deleteSelection()
        val safeCursor = cursor.coerceIn(0, text.length)
        val newText = text.substring(0, safeCursor) + character + text.substring(safeCursor)
        cursor = safeCursor + 1
        text = newText
        onTextChange?.invoke(text)
        resetBlink()
    }

    fun insert(textToInsert: String) {
        if (textToInsert.isEmpty()) return

        deleteSelection()
        val safeCursor = cursor.coerceIn(0, text.length)
        val newText = text.substring(0, safeCursor) + textToInsert + text.substring(safeCursor)
        cursor = safeCursor + textToInsert.length
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

    // =========================================================================
    // II. Cursor Navigation & Selection Range
    // =========================================================================

    fun moveLeft(extendSelection: Boolean = false) {
        if (extendSelection) {
            if (selectionStart == -1) selectionStart = cursor
            cursor = (cursor - 1).coerceAtLeast(0)
        } else {
            if (hasSelection()) {
                val range = getSelectionRange()!!
                cursor = range.first
                selectionStart = -1
            } else {
                cursor = (cursor - 1).coerceAtLeast(0)
            }
        }
        resetBlink()
    }

    fun moveRight(extendSelection: Boolean = false) {
        if (extendSelection) {
            if (selectionStart == -1) selectionStart = cursor
            cursor = (cursor + 1).coerceAtMost(text.length)
        } else {
            if (hasSelection()) {
                val range = getSelectionRange()!!
                cursor = range.second
                selectionStart = -1
            } else {
                cursor = (cursor + 1).coerceAtMost(text.length)
            }
        }
        resetBlink()
    }

    fun moveToStart(extendSelection: Boolean = false) {
        if (extendSelection) {
            if (selectionStart == -1) selectionStart = cursor
        } else {
            selectionStart = -1
        }
        cursor = 0
        resetBlink()
    }

    fun moveToEnd(extendSelection: Boolean = false) {
        if (extendSelection) {
            if (selectionStart == -1) selectionStart = cursor
        } else {
            selectionStart = -1
        }
        cursor = text.length
        resetBlink()
    }

    fun moveCursor(index: Int, extendSelection: Boolean = false) {
        val clampedIndex = index.coerceIn(0, text.length)
        if (extendSelection) {
            if (selectionStart == -1) selectionStart = cursor
        } else {
            selectionStart = -1
        }
        cursor = clampedIndex
        resetBlink()
    }

    // =========================================================================
    // III. Clipboard Operations
    // =========================================================================

    fun copy() {
        val selectedText = getSelectedText()
        if (selectedText.isNotEmpty() && Core.app != null) {
            Core.app.clipboardText = selectedText
        }
    }

    fun cut() {
        val selectedText = getSelectedText()
        if (selectedText.isNotEmpty()) {
            if (Core.app != null) Core.app.clipboardText = selectedText
            deleteSelection()
        }
    }

    fun paste() {
        if (Core.app != null) {
            val clipboardText = Core.app.clipboardText
            if (!clipboardText.isNullOrEmpty()) insert(clipboardText)
        }
    }

    // =========================================================================
    // IV. Keyboard Event Processors
    // =========================================================================

    fun onKeyTyped(character: Char): Boolean {
        if (!isFocused) return false

        if (character >= ' ' || character == '\t') {
            insert(character)
            return true
        }
        return false
    }

    fun onKeyDown(key: KeyCode): Boolean {
        if (!isFocused) return false

        val isCtrl = Core.input != null && Core.input.ctrl()
        val isShift = Core.input != null && Core.input.shift()

        return when (key) {
            KeyCode.backspace -> backspace()
            KeyCode.del -> delete()
            KeyCode.left -> { moveLeft(isShift); true }
            KeyCode.right -> { moveRight(isShift); true }
            KeyCode.home -> { moveToStart(isShift); true }
            KeyCode.end -> { moveToEnd(isShift); true }
            KeyCode.a -> if (isCtrl) { selectAll(); true } else false
            KeyCode.c -> if (isCtrl) { copy(); true } else false
            KeyCode.v -> if (isCtrl) { paste(); true } else false
            KeyCode.x -> if (isCtrl) { cut(); true } else false
            else -> false
        }
    }
}
