package org.hubdustry.core.compose.foundation.text

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import arc.Core
import arc.input.KeyCode

/**
 * ## TextFieldState
 *
 * Bộ máy trạng thái thuần túy (Headless State Machine) điều khiển văn bản cho Text Field.
 * Chịu trách nhiệm quản lý bộ đệm chuỗi, con trỏ (cursor), khoảng bôi đen (selection),
 * chuỗi ứng viên IME (composition candidate), clipboard hệ điều hành và nhấp nháy con trỏ.
 *
 * Tích hợp snapshot state của Compose (`mutableStateOf`) giúp tự động kích hoạt
 * recomposition một cách trung thực và chính xác khi trạng thái văn bản thay đổi.
 *
 * Tuân thủ quy tắc 1.1 (Active Self-Validating Entity) và 8.2 (Never-Throw Runtime).
 */
class TextFieldState(
    initialText: String = "",
    var onTextChange: ((String) -> Unit)? = null
) {
    private var _text by mutableStateOf(initialText)
    val text: String get() = _text

    private var _selection by mutableStateOf(TextRange(initialText.length))
    val selection: TextRange get() = _selection

    private var _composition by mutableStateOf("")
    val composition: String get() = _composition

    var isFocused: Boolean by mutableStateOf(false)

    var isSingleLine: Boolean = true

    private var _cursorVisible by mutableStateOf(true)
    var cursorVisible: Boolean
        get() = _cursorVisible
        internal set(value) { _cursorVisible = value }

    private var blinkTimer: Float = 0f
    private val blinkInterval: Float = 0.5f

    val cursor: Int
        get() = selection.end

    fun hasComposition(): Boolean = composition.isNotEmpty()

    fun hasSelection(): Boolean = !selection.collapsed

    /**
     * Trả về toàn bộ chuỗi đang hiển thị, bao gồm cả chuỗi ứng viên IME (nếu có)
     * được chèn ngay tại vị trí con trỏ hiện tại.
     */
    fun getDisplayText(): String {
        if (composition.isEmpty()) return text
        val safePos = cursor.coerceIn(0, text.length)
        return text.substring(0, safePos) + composition + text.substring(safePos)
    }

    /**
     * Vị trí con trỏ thực tế trên chuỗi hiển thị ([getDisplayText]).
     */
    fun getEffectiveCursor(): Int {
        if (composition.isEmpty()) return cursor
        return (cursor + composition.length).coerceIn(0, getDisplayText().length)
    }

    /**
     * Trả về khoảng ký tự của chuỗi candidate IME trên [getDisplayText].
     */
    fun getCompositionRange(): TextRange? {
        if (composition.isEmpty()) return null
        val safePos = cursor.coerceIn(0, text.length)
        return TextRange(safePos, safePos + composition.length)
    }

    /**
     * Cập nhật chuỗi ứng viên IME (chưa chốt commit vào văn bản chính).
     */
    fun setComposition(candidate: String) {
        if (composition != candidate) {
            _composition = candidate
            resetBlink()
        }
    }

    /**
     * Xóa bỏ chuỗi ứng viên IME mà không ghi vào văn bản chính.
     */
    fun clearComposition() {
        if (composition.isNotEmpty()) {
            _composition = ""
            resetBlink()
        }
    }

    /**
     * Chốt (commit) chuỗi ứng viên IME vào văn bản chính tại vị trí con trỏ.
     */
    fun commitComposition() {
        if (composition.isNotEmpty()) {
            insertText(composition)
            _composition = ""
        }
    }

    /**
     * Thiết lập văn bản mới và cập nhật vị trí con trỏ.
     */
    fun setText(newText: String, newCursor: Int = -1) {
        _text = newText
        val safeCursor = if (newCursor >= 0) newCursor.coerceIn(0, newText.length) else newText.length
        _selection = TextRange(safeCursor)
        _composition = ""
        resetBlink()
    }

    /**
     * Đặt khoảng bôi đen (selection) mới.
     */
    fun setSelection(start: Int, end: Int = start) {
        _selection = TextRange(start, end).coerceIn(0, text.length)
        resetBlink()
    }

    /**
     * Chọn toàn bộ văn bản.
     */
    fun selectAll() {
        if (text.isNotEmpty()) {
            _selection = TextRange(0, text.length)
            resetBlink()
        }
    }

    /**
     * Đặt con trỏ tại vị trí [index].
     */
    fun setCursor(index: Int) {
        val safeIndex = index.coerceIn(0, text.length)
        _selection = TextRange(safeIndex, safeIndex)
        resetBlink()
    }

    /**
     * Chèn một chuỗi [str] vào vị trí hiện tại (thay thế selection nếu đang bôi đen).
     */
    fun insertText(str: String) {
        if (str.isEmpty()) return
        val min = selection.min
        val max = selection.max
        val newText = text.substring(0, min) + str + text.substring(max)
        val newCursor = min + str.length
        _text = newText
        _selection = TextRange(newCursor)
        _composition = ""
        resetBlink()
        onTextChange?.invoke(newText)
    }

    /**
     * Xử lý ký tự committed nhận được từ bàn phím (`InputListener.keyTyped`).
     */
    fun onKeyTyped(c: Char): Boolean {
        if (!isFocused) return false

        // Loại bỏ phím điều khiển BackSpace, DEL (127) hoặc ký tự không in được
        if (c == '\b' || c.code == 127 || (c.code < 32 && c != '\t' && c != '\n')) {
            return false
        }

        if (c == '\r' || c == '\n') {
            if (isSingleLine) return false
            insertText("\n")
            return true
        }

        insertText(c.toString())
        return true
    }

    /**
     * Xử lý các phím chức năng (Xóa, Điều hướng, Copy, Cut, Paste).
     */
    fun onKeyDown(keycode: KeyCode?): Boolean {
        if (!isFocused || keycode == null) return false

        val isShift = try { Core.input?.shift() == true } catch (_: Throwable) { false }
        val isCtrl = try { Core.input?.ctrl() == true } catch (_: Throwable) { false }

        return when (keycode) {
            KeyCode.backspace -> {
                handleBackspace()
                true
            }
            KeyCode.del -> {
                handleDelete()
                true
            }
            KeyCode.left -> {
                handleMoveLeft(isShift, isCtrl)
                true
            }
            KeyCode.right -> {
                handleMoveRight(isShift, isCtrl)
                true
            }
            KeyCode.home -> {
                handleHome(isShift)
                true
            }
            KeyCode.end -> {
                handleEnd(isShift)
                true
            }
            KeyCode.a -> {
                if (isCtrl) {
                    selectAll()
                    true
                } else false
            }
            KeyCode.c -> {
                if (isCtrl) {
                    copyToClipboard()
                    true
                } else false
            }
            KeyCode.x -> {
                if (isCtrl) {
                    cutToClipboard()
                    true
                } else false
            }
            KeyCode.v -> {
                if (isCtrl) {
                    pasteFromClipboard()
                    true
                } else false
            }
            else -> false
        }
    }

    private fun handleBackspace() {
        if (hasSelection()) {
            deleteSelection()
            return
        }
        if (cursor > 0) {
            val prev = cursor - 1
            val newText = text.substring(0, prev) + text.substring(cursor)
            _text = newText
            _selection = TextRange(prev)
            resetBlink()
            onTextChange?.invoke(newText)
        }
    }

    private fun handleDelete() {
        if (hasSelection()) {
            deleteSelection()
            return
        }
        if (cursor < text.length) {
            val newText = text.substring(0, cursor) + text.substring(cursor + 1)
            _text = newText
            resetBlink()
            onTextChange?.invoke(newText)
        }
    }

    private fun deleteSelection() {
        val min = selection.min
        val max = selection.max
        val newText = text.substring(0, min) + text.substring(max)
        _text = newText
        _selection = TextRange(min)
        resetBlink()
        onTextChange?.invoke(newText)
    }

    private fun handleMoveLeft(extendSelection: Boolean, byWord: Boolean) {
        val target = if (byWord) findPreviousWordBoundary(cursor) else (cursor - 1).coerceAtLeast(0)
        val anchor = if (extendSelection) selection.start else target
        _selection = TextRange(anchor, target)
        resetBlink()
    }

    private fun handleMoveRight(extendSelection: Boolean, byWord: Boolean) {
        val target = if (byWord) findNextWordBoundary(cursor) else (cursor + 1).coerceAtMost(text.length)
        val anchor = if (extendSelection) selection.start else target
        _selection = TextRange(anchor, target)
        resetBlink()
    }

    private fun handleHome(extendSelection: Boolean) {
        val anchor = if (extendSelection) selection.start else 0
        _selection = TextRange(anchor, 0)
        resetBlink()
    }

    private fun handleEnd(extendSelection: Boolean) {
        val anchor = if (extendSelection) selection.start else text.length
        _selection = TextRange(anchor, text.length)
        resetBlink()
    }

    fun selectWordAt(index: Int) {
        if (text.isEmpty()) return
        val safeIndex = index.coerceIn(0, text.length)
        var start = safeIndex
        while (start > 0 && !text[start - 1].isWhitespace()) start--
        var end = safeIndex
        while (end < text.length && !text[end].isWhitespace()) end++
        _selection = TextRange(start, end)
        resetBlink()
    }

    private fun findPreviousWordBoundary(from: Int): Int {
        var i = (from - 1).coerceAtLeast(0)
        while (i > 0 && text[i].isWhitespace()) i--
        while (i > 0 && !text[i - 1].isWhitespace()) i--
        return i
    }

    private fun findNextWordBoundary(from: Int): Int {
        var i = from.coerceAtMost(text.length)
        while (i < text.length && text[i].isWhitespace()) i++
        while (i < text.length && !text[i].isWhitespace()) i++
        return i
    }

    // ── CLIPBOARD OPERATIONS ─────────────────────────────────────────────────

    fun copyToClipboard() {
        if (!hasSelection()) return
        val selectedText = text.substring(selection.min, selection.max)
        try {
            Core.app?.setClipboardText(selectedText)
        } catch (_: Throwable) {
        }
    }

    fun cutToClipboard() {
        if (!hasSelection()) return
        copyToClipboard()
        deleteSelection()
    }

    fun pasteFromClipboard() {
        val clipboardContent: String = try {
            Core.app?.getClipboardText()
        } catch (_: Throwable) {
            null
        } ?: return

        if (clipboardContent.isNotEmpty()) {
            val sanitized = if (isSingleLine) clipboardContent.replace("\n", "").replace("\r", "") else clipboardContent
            insertText(sanitized)
        }
    }

    // ── BLINK LOGIC ──────────────────────────────────────────────────────────

    fun resetBlink() {
        blinkTimer = 0f
        _cursorVisible = true
    }

    fun updateBlink(deltaSeconds: Float) {
        if (!isFocused) {
            _cursorVisible = false
            return
        }
        blinkTimer += deltaSeconds
        if (blinkTimer >= blinkInterval) {
            blinkTimer -= blinkInterval
            _cursorVisible = !_cursorVisible
        }
    }
}
