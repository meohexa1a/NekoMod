package org.hubdustry.core.compose.input

import arc.input.KeyCode

/**
 * Interface đón nhận các sự kiện bàn phím và IME cho các thành phần nhập liệu.
 */
interface KeyboardInputHandler {
    /**
     * Nhận ký tự đã gõ (bao gồm ký tự Unicode và điều khiển cơ bản).
     */
    fun onKeyTyped(character: Char): Boolean

    /**
     * Nhận sự kiện nhấn phím xuống (bao gồm phím mũi tên, backspace, delete, escape, enter).
     */
    fun onKeyDown(keyCode: KeyCode?): Boolean

    /**
     * Nhận sự kiện nhả phím lên.
     */
    fun onKeyUp(keyCode: KeyCode?): Boolean = false

    /**
     * Thông báo component đã mất tiêu điểm bàn phím (Focus Lost).
     * Dùng để dọn dẹp visual state (tắt con trỏ nhấp nháy, đóng session IME).
     */
    fun onFocusLost() {}
}
