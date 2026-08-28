package org.mdt.core.ui.input

import arc.input.KeyCode
import org.mdt.core.ui.EngineRuntime

/**
 * ## TextEditState [Headless Text Editing State Machine]
 *
 * ### 1. 📖 Feature Specification & Core Architecture:
 * - Headless string manipulation state machine managing interactive text editing across UI components.
 * - Handles text insertion, deletion, backspace, selection ranges (`selectionStart`..`cursor`), and word/line boundaries.
 * - Maintains active IME composition candidate buffers (`compositionText`) and computes effective cursor positions.
 * - Manages caret blink timer cycles (0.32s interval) with auto-reset on typing or navigation.
 * - Bridges to OS clipboard (`copy`, `cut`, `paste`) exclusively via [EngineRuntime.host].
 *
 * ### 2. ⚡ Invariants & Non-Negotiable Rules:
 * - **Rule 1 (Zero Platform Bypass):** Clipboard operations and modifier keys (Ctrl, Shift) MUST route through [EngineRuntime.host].
 * - **Rule 2 (IME Double-Backspace Protection):** `onKeyTyped` strictly ignores `\b` (characters $< 32$) to prevent double-deletion. Backspace is handled exclusively in `onKeyDown`.
 *
 * ### 3. 🔗 Related Files & Subsystem Map:
 * - 🌲 **Target Virtual Node:** `src/main/kotlin/org/mdt/core/ui/node/InputNode.kt`
 * - 🎮 **Focus Coordinator:** `src/main/kotlin/org/mdt/core/ui/input/EngineInputProcessor.kt`
 * - 🎨 **Composable UI:** `src/main/kotlin/org/mdt/ui/components/input/TextField.kt`
 * - 🔌 **Platform Host:** `src/main/kotlin/org/mdt/core/engine/PlatformHost.kt`
 *
 * ### 4. ✅ Behavioral Verification Checklist:
 * - [x] `insert(Char)` filters non-printable control characters except `\t` and `\n` (in multiline mode).
 * - [x] `backspace()` and `delete()` delete active selection if present, else delete single adjacent character.
 * - [x] `selectAll()` sets selection from 0 to `text.length`.
 * - [x] `selectWordAt(index)` expands selection to whitespace boundaries.
 * - [x] Shortcut keys (Ctrl+A, Ctrl+C, Ctrl+V, Ctrl+X) execute corresponding clipboard/selection actions.
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

    var compositionText: String = ""
        private set

    var isFocused: Boolean = false
    var isMultiline: Boolean = false

    var cursorVisible: Boolean = true
        private set

    private var blinkTimer: Float = 0.0f
    private val blinkInterval: Float = 0.32f

    fun hasComposition(): Boolean = compositionText.isNotEmpty()

    fun getDisplayText(): String {
        if (compositionText.isEmpty()) return text
        val safeCursor = cursor.coerceIn(0, text.length)
        return text.substring(0, safeCursor) + compositionText + text.substring(safeCursor)
    }

    fun getEffectiveCursor(): Int {
        if (compositionText.isEmpty()) return cursor
        return (cursor + compositionText.length).coerceIn(0, getDisplayText().length)
    }

    fun getCompositionRange(): Pair<Int, Int>? {
        if (compositionText.isEmpty()) return null
        val safeCursor = cursor.coerceIn(0, text.length)
        return Pair(safeCursor, safeCursor + compositionText.length)
    }

    fun setComposition(candidate: String) {
        if (compositionText != candidate) {
            compositionText = candidate
            resetBlink()
        }
    }

    fun clearComposition() {
        if (compositionText.isNotEmpty()) {
            compositionText = ""
            resetBlink()
        }
    }

    fun setText(newText: String, newCursor: Int = -1) {
        if (text != newText || newCursor != -1) {
            text = newText
            cursor = if (newCursor >= 0) newCursor.coerceIn(0, text.length) else cursor.coerceIn(0, text.length)
            selectionStart = -1
            compositionText = ""
            resetBlink()
        }
    }

    fun resetBlink() {
        blinkTimer = 0.0f
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

    fun setSelection(start: Int, end: Int) {
        selectionStart = start.coerceIn(0, text.length)
        cursor = end.coerceIn(0, text.length)
        resetBlink()
    }

    fun selectWordAt(index: Int) {
        if (text.isEmpty()) return
        val safeIndex = index.coerceIn(0, text.length - 1)
        var start = safeIndex
        var end = safeIndex
        while (start > 0 && !text[start - 1].isWhitespace()) start--
        while (end < text.length && !text[end].isWhitespace()) end++
        setSelection(start, end)
    }

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

    // --- CHARACTER & STRING INSERTION ---

    fun insert(character: Char) {
        // Filter out control characters except tab and newline (if multiline)
        if (character < ' ' && character != '\t' && (!isMultiline || character != '\n')) return

        clearComposition()
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

        clearComposition()
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

    // --- CURSOR NAVIGATION ---

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

    // --- CLIPBOARD OPERATIONS ---

    fun copy() {
        val selectedText = getSelectedText()
        if (selectedText.isNotEmpty()) {
            EngineRuntime.host.setClipboard(selectedText)
        }
    }

    fun cut() {
        val selectedText = getSelectedText()
        if (selectedText.isNotEmpty()) {
            EngineRuntime.host.setClipboard(selectedText)
            deleteSelection()
        }
    }

    fun paste() {
        val clipboardText = EngineRuntime.host.getClipboard()
        if (clipboardText.isNotEmpty()) {
            insert(clipboardText)
        }
    }

    // --- KEYBOARD EVENT PROCESSORS ---

    /**
     * Processes printable character typing.
     * Enforces Rule 10: Ignores '\b' backspace characters to prevent double-deletion with Vietnamese IME.
     */
    fun onKeyTyped(character: Char): Boolean {
        if (!isFocused) return false

        if (character >= ' ' || character == '\t' || (isMultiline && character == '\n')) {
            insert(character)
            return true
        }
        return false
    }

    fun onKeyDown(key: KeyCode): Boolean {
        if (!isFocused) return false

        val isCtrl = EngineRuntime.host.isCtrlPressed
        val isShift = EngineRuntime.host.isShiftPressed

        return when (key) {
            KeyCode.backspace -> backspace()
            KeyCode.del -> delete()
            KeyCode.enter -> if (isMultiline) { insert('\n'); true } else false
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
