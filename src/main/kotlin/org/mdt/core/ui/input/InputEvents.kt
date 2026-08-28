package org.mdt.core.ui.input

import arc.input.KeyCode

// --- POINTER EVENTS ---

/**
 * ## PointerEvent [Pointer Interaction Event]
 *
 * ### 1. 📖 Feature Specification & Core Architecture:
 * - Represents a pointer/touch interaction event dispatched across the Virtual DOM tree.
 * - Conveys 2D screen coordinate values ([x], [y]) in OpenGL bottom-left origin ($y=0$ bottom).
 * - Tracks triggering [button] and [pointer] finger index with a mutable [isConsumed] cancellation flag.
 *
 * ### 2. ⚡ Invariants & Non-Negotiable Rules:
 * - **Rule 1 (Float Everywhere):** Coordinates [x] and [y] use float pixels.
 *
 * ### 3. 🔗 Related Files & Subsystem Map:
 * - 🎮 **Router:** `src/main/kotlin/org/mdt/core/ui/input/EngineInputProcessor.kt`
 * - 🌲 **Target Node:** `src/main/kotlin/org/mdt/core/ui/node/UINode.kt`
 *
 * ### 4. ✅ Behavioral Verification Checklist:
 * - [x] `screenX` and `screenY` accessors match `x` and `y`.
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
 * ## ScrollEvent [Scroll Gesture Event]
 *
 * ### 1. 📖 Feature Specification & Core Architecture:
 * - Represents a hardware mouse wheel or touch gesture scroll event.
 * - Conveys [amountX] and [amountY] scroll offsets bubbling upwards through the UI tree.
 *
 * ### 2. ⚡ Invariants & Non-Negotiable Rules:
 * - **Rule 1 (Event Bubbling):** Traverses ancestor chain until a node consumes the event via `isConsumed = true`.
 *
 * ### 3. 🔗 Related Files & Subsystem Map:
 * - 🎮 **Router:** `src/main/kotlin/org/mdt/core/ui/input/EngineInputProcessor.kt`
 * - 🌲 **Target Node:** `src/main/kotlin/org/mdt/core/ui/node/UINode.kt`
 *
 * ### 4. ✅ Behavioral Verification Checklist:
 * - [x] Bubbling halts immediately once `isConsumed` is set to `true`.
 */
class ScrollEvent(
    val amountX: Float,
    val amountY: Float,
    var isConsumed: Boolean = false
)

// --- KEYBOARD EVENTS ---

/**
 * ## KeyEvent [Physical Keyboard Event]
 *
 * Represents a physical keyboard key press or release event.
 */
class KeyEvent(
    val keyCode: KeyCode,
    var isConsumed: Boolean = false
)
