package org.mdt.ui.core

import arc.input.KeyCode

class PointerEvent(
    val x: Float,
    val y: Float,
    val pointer: Int = 0,
    val button: KeyCode = KeyCode.mouseLeft,
    var isConsumed: Boolean = false
)

class ScrollEvent(
    val amountX: Float,
    val amountY: Float,
    var isConsumed: Boolean = false
)

class KeyEvent(
    val keyCode: KeyCode,
    var isConsumed: Boolean = false
)
