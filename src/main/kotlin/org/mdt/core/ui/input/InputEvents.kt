package org.mdt.core.ui.input

import arc.input.KeyCode
import org.mdt.core.ui.node.UINode

// --- PASS & EVENT TYPE ENUMS ---

/**
 * ## PointerEventPass [3-Pass Event Propagation Phases]
 *
 * Defines the three-phase propagation lifecycle for pointer events:
 * 1. [INITIAL]: Tunneling from Root down to Leaf (Parent-first). Used by Modal backdrops and outer containers to preview and intercept events.
 * 2. [MAIN]: Bubbling from Leaf up to Root (Child-first). Primary interaction handling where buttons, sliders, and text inputs consume events.
 * 3. [FINAL]: Post-processing and cleanup pass for hover state tracking, cursor synchronization, and clearing finished gestures.
 */
enum class PointerEventPass {
    INITIAL,
    MAIN,
    FINAL
}

/**
 * ## PointerEventType [Pointer Physical Action Type]
 *
 * Categorizes the physical action driving a pointer event.
 */
enum class PointerEventType {
    Press,
    Release,
    Move,
    Drag,
    Scroll,
    Enter,
    Exit,
    Cancel
}

// --- POINTER INPUT CHANGE ---

/**
 * ## PointerInputChange [Pointer State Change]
 *
 * > 🤖 **AGENT DIRECTIVE:** When modifying this class or related logic, synchronously update this KDoc:
 * > 1) Keep Purpose accurate. 2) Update Key Rules & Checklist [x]/[ ]. 3) Maintain Related Files map.
 *
 * ### 1. Purpose
 * - Holds the state changes for a pointer (mouse/finger position, deltas, buttons, scroll) in a frame.
 * - Tracks whether the event has been consumed by a UI node via [isConsumed].
 *
 * ### 2. Key Rules & Checklist
 * - [x] All positions and deltas use `Float` in OpenGL bottom-left screen coordinates (`y=0` bottom).
 * - [x] Calling `consume()` marks `isConsumed = true` to stop propagation.
 * - [x] `dx` and `dy` return `(x - prevX)` and `(y - prevY)` accurately.
 *
 * ### 3. Related Files
 * - Master Input Processor: `src/main/kotlin/org/mdt/core/ui/input/EngineInputProcessor.kt`
 * - Virtual Node: `src/main/kotlin/org/mdt/core/ui/node/UINode.kt`
 */
class PointerInputChange(
    val id: Long = 0L,
    val uptimeMillis: Long = 0L,
    val x: Float,
    val y: Float,
    val prevX: Float = x,
    val prevY: Float = y,
    val pressed: Boolean = false,
    val prevPressed: Boolean = false,
    val button: KeyCode = KeyCode.mouseLeft,
    val scrollX: Float = 0.0f,
    val scrollY: Float = 0.0f,
    var isConsumed: Boolean = false
) {
    /** Horizontal position delta from previous frame. */
    val dx: Float get() = x - prevX

    /** Vertical position delta from previous frame. */
    val dy: Float get() = y - prevY

    /** Marks this pointer change as consumed, preventing lower/subsequent handlers from processing it. */
    fun consume() {
        isConsumed = true
    }
}

// --- POINTER EVENT ---

/**
 * ## PointerEvent [Unified Pointer Pipeline Event Envelope]
 *
 * Dispatched through the 3-Pass pipeline containing the active [change], [type], and current [pass].
 */
class PointerEvent(
    val change: PointerInputChange,
    val type: PointerEventType,
    var pass: PointerEventPass = PointerEventPass.MAIN
) {
    val x: Float get() = change.x
    val y: Float get() = change.y
    val prevX: Float get() = change.prevX
    val prevY: Float get() = change.prevY
    val dx: Float get() = change.dx
    val dy: Float get() = change.dy
    val button: KeyCode get() = change.button
    val scrollX: Float get() = change.scrollX
    val scrollY: Float get() = change.scrollY
    val isConsumed: Boolean get() = change.isConsumed

    fun consume() = change.consume()
}

// --- POINTER INPUT FILTER CONTRACT ---

/**
 * ## PointerInputFilter [Composable Multi-Listener Input Contract]
 *
 * Attachable filter receiving pointer events across [PointerEventPass.INITIAL], [PointerEventPass.MAIN], and [PointerEventPass.FINAL].
 */
interface PointerInputFilter {
    /**
     * Invoked when a pointer event passes through [node].
     *
     * @param event The active pointer event envelope.
     * @param pass The current propagation phase ([PointerEventPass.INITIAL], [PointerEventPass.MAIN], [PointerEventPass.FINAL]).
     * @param node The [UINode] this filter is attached to.
     */
    fun onPointerEvent(event: PointerEvent, pass: PointerEventPass, node: UINode)
}

// --- SCROLL & KEYBOARD COMPATIBILITY EVENTS ---

/**
 * ## ScrollEvent [Scroll Gesture Event]
 */
class ScrollEvent(
    val amountX: Float,
    val amountY: Float,
    var isConsumed: Boolean = false
)

/**
 * ## KeyEvent [Physical Keyboard Event]
 */
class KeyEvent(
    val keyCode: KeyCode,
    var isConsumed: Boolean = false
)
