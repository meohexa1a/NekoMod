package org.mdt.core.ui.input

import arc.input.KeyCode

// --- POINTER EVENTS ---

/**
 * ## PointerEvent
 *
 * Represents a pointer/mouse interaction event dispatched across the Virtual DOM tree.
 *
 * @param x Screen X coordinate in pixels.
 * @param y Screen Y coordinate in pixels (bottom-left origin).
 * @param pointer Pointer / touch finger identifier.
 * @param button Key code for the triggering mouse button.
 * @param isConsumed Whether this event has been intercepted and consumed by a node.
 *
 * See: docs/ui-engine/ui_engine_en.md
 */
class PointerEvent(
    val x: Float,
    val y: Float,
    val pointer: Int = 0,
    val button: KeyCode = KeyCode.mouseLeft,
    var isConsumed: Boolean = false
) {
    val screenX: Float get() = x
    val screenY: Float get() = y
}

// --- SCROLL EVENTS ---

/**
 * ## ScrollEvent
 *
 * Represents a mouse wheel or gesture scroll event.
 *
 * @param amountX Horizontal scroll delta.
 * @param amountY Vertical scroll delta.
 * @param isConsumed Whether this event has been intercepted and consumed.
 *
 * See: docs/ui-engine/ui_engine_en.md
 */
class ScrollEvent(
    val amountX: Float,
    val amountY: Float,
    var isConsumed: Boolean = false
)

// --- KEYBOARD EVENTS ---

/**
 * ## KeyEvent
 *
 * Represents a physical keyboard key press or release event.
 *
 * @param keyCode KeyCode enumeration value.
 * @param isConsumed Whether this event has been intercepted and consumed.
 *
 * See: docs/ui-engine/ui_engine_en.md
 */
class KeyEvent(
    val keyCode: KeyCode,
    var isConsumed: Boolean = false
)
