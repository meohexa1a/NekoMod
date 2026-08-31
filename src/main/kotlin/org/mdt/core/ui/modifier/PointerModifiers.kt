// [AGENT INVARIANT] Synchronously update @property, @param, and @see KDocs when modifying this file.

@file:Suppress("unused")

package org.mdt.core.ui.modifier

import org.mdt.core.ui.input.CursorIcon
import org.mdt.core.ui.input.PointerEvent
import org.mdt.core.ui.input.PointerEventPass
import org.mdt.core.ui.input.PointerEventType
import org.mdt.core.ui.input.PointerInputFilter
import org.mdt.core.ui.input.ScrollEvent
import org.mdt.core.ui.node.HitTestBehavior
import org.mdt.core.ui.node.UINode
import kotlin.math.hypot

// --- TYPED POINTER & INPUT MODIFIER ELEMENTS ---

/**
 * ## ClickableModifier
 *
 * Attaches a primary click interaction listener and press state tracking to a [UINode].
 *
 * @property onClick Action callback invoked upon pointer click release within node bounds.
 * @property onPressStateChanged Callback tracking pressed state transitions (`true` on down, `false` on release/exit).
 */
data class ClickableModifier(
    val onClick: () -> Unit,
    val onPressStateChanged: ((Boolean) -> Unit)? = null,
) : UIModifier.Element {
    override fun applyTo(node: UINode) {
        node.hitTestBehavior = HitTestBehavior.OPAQUE
        node.cursor = CursorIcon.HAND
        node.onClick = onClick
        if (onPressStateChanged != null) {
            node.onPointerDown = { onPressStateChanged.invoke(true) }
            node.onPointerUp = { onPressStateChanged.invoke(false) }
            node.onPointerExit = { onPressStateChanged.invoke(false) }
        }
    }
}

/**
 * ## DoubleClickModifier
 *
 * Attaches a double-click gesture listener to a [UINode].
 *
 * @property onDoubleClick Action callback invoked on double click.
 */
data class DoubleClickModifier(val onDoubleClick: () -> Unit) : UIModifier.Element {
    override fun applyTo(node: UINode) {
        node.onDoubleClick = onDoubleClick
    }
}

/**
 * ## HoverableModifier
 *
 * Attaches pointer hover detection to a [UINode].
 *
 * @property onHover Callback receiving `true` when cursor enters node bounds, and `false` when it exits.
 */
data class HoverableModifier(val onHover: (Boolean) -> Unit) : UIModifier.Element {
    override fun applyTo(node: UINode) {
        node.onHover = onHover
    }
}

/**
 * ## DraggableModifier
 *
 * Attaches continuous drag gesture tracking to a [UINode].
 *
 * @property onDrag Callback receiving delta coordinates `(dx, dy)` during dragging.
 * @property onDragStart Optional callback invoked when dragging initiates.
 * @property onDragEnd Optional callback invoked when dragging completes.
 * @property onDragCancel Optional callback invoked when dragging is canceled.
 * @property touchSlop Minimum distance threshold in pixels required to trigger dragging.
 */
data class DraggableModifier(
    val onDrag: (dx: Float, dy: Float) -> Unit,
    val onDragStart: ((Float, Float) -> Unit)? = null,
    val onDragEnd: (() -> Unit)? = null,
    val onDragCancel: (() -> Unit)? = null,
    val touchSlop: Float = 4.0f,
) : UIModifier.Element {
    override fun applyTo(node: UINode) {
        node.hitTestBehavior = HitTestBehavior.OPAQUE
        var isDragging = false
        var totalDistance = 0.0f
        var startX = 0.0f
        var startY = 0.0f

        node.onPointerDown = { event ->
            isDragging = false
            totalDistance = 0.0f
            startX = event.x
            startY = event.y
        }
        node.onPointerDrag = { event ->
            if (event.change.pressed) {
                totalDistance += hypot(event.dx, event.dy)
                if (!isDragging && totalDistance >= touchSlop) {
                    isDragging = true
                    onDragStart?.invoke(startX, startY)
                }
                if (isDragging) {
                    onDrag(event.dx, event.dy)
                    event.consume()
                }
            }
        }
        node.onPointerUp = { event ->
            if (isDragging) {
                isDragging = false
                onDragEnd?.invoke()
                event.consume()
            }
        }
    }
}

/**
 * ## PointerInputModifier
 *
 * Attaches a raw [PointerInputFilter] to intercept events across [PointerEventPass] phases.
 *
 * @property filter The custom filter instance.
 */
data class PointerInputModifier(val filter: PointerInputFilter) : UIModifier.Element {
    override fun applyTo(node: UINode) = node.addPointerFilter(filter)
}

/**
 * ## PointerDownModifier
 *
 * Intercepts [PointerEventType.Press] events on a [UINode].
 *
 * @property onDown Callback receiving pointer press events.
 */
data class PointerDownModifier(val onDown: (PointerEvent) -> Unit) : UIModifier.Element {
    override fun applyTo(node: UINode) {
        node.onPointerDown = onDown
    }
}

/**
 * ## PointerUpModifier
 *
 * Intercepts [PointerEventType.Release] events on a [UINode].
 *
 * @property onUp Callback receiving pointer release events.
 */
data class PointerUpModifier(val onUp: (PointerEvent) -> Unit) : UIModifier.Element {
    override fun applyTo(node: UINode) {
        node.onPointerUp = onUp
    }
}

/**
 * ## PointerDragModifier
 *
 * Intercepts [PointerEventType.Drag] events on a [UINode].
 *
 * @property onDrag Callback receiving active pointer dragging events.
 */
data class PointerDragModifier(val onDrag: (PointerEvent) -> Unit) : UIModifier.Element {
    override fun applyTo(node: UINode) {
        node.onPointerDrag = onDrag
    }
}

/**
 * ## ScrollModifier
 *
 * Intercepts scroll gestures on a [UINode].
 *
 * @property onScroll Callback receiving scroll gesture events.
 */
data class ScrollModifier(val onScroll: (ScrollEvent) -> Unit) : UIModifier.Element {
    override fun applyTo(node: UINode) {
        node.onScroll = onScroll
    }
}

/**
 * ## TouchableModifier
 *
 * Configures hit-testing visibility on a [UINode].
 *
 * @property touchable Whether the node and its children receive input events.
 */
data class TouchableModifier(val touchable: Boolean) : UIModifier.Element {
    override fun applyTo(node: UINode) {
        node.touchable = touchable
    }
}

/**
 * ## FocusableModifier
 *
 * Configures keyboard focus eligibility on a [UINode].
 *
 * @property focusable Whether this node can gain input focus.
 */
data class FocusableModifier(val focusable: Boolean = true) : UIModifier.Element {
    override fun applyTo(node: UINode) {
        node.isFocusable = focusable
    }
}

/**
 * ## HitTestBehaviorModifier
 *
 * Sets hit-testing transparency mode ([HitTestBehavior]) on a [UINode].
 *
 * @property behavior Hit-testing behavior mode ([HitTestBehavior.OPAQUE], [HitTestBehavior.TRANSLUCENT], etc.).
 */
data class HitTestBehaviorModifier(val behavior: HitTestBehavior) : UIModifier.Element {
    override fun applyTo(node: UINode) {
        node.hitTestBehavior = behavior
    }
}

/**
 * ## TagModifier
 *
 * Attaches arbitrary debug metadata object to a [UINode].
 *
 * @property tag Metadata tag object.
 */
data class TagModifier(val tag: Any?) : UIModifier.Element {
    override fun applyTo(node: UINode) {
        node.tag = tag
    }
}

// --- FLUENT EXTENSION FUNCTIONS ---

fun UIModifier.clickable(
    onPressStateChanged: ((Boolean) -> Unit)? = null,
    onClick: () -> Unit,
): UIModifier = then(ClickableModifier(onClick, onPressStateChanged))

fun UIModifier.onClick(block: () -> Unit): UIModifier = clickable(onClick = block)
fun UIModifier.onDoubleClick(block: () -> Unit): UIModifier = then(DoubleClickModifier(block))
fun UIModifier.hoverable(onHover: (Boolean) -> Unit): UIModifier = then(HoverableModifier(onHover))
fun UIModifier.onHover(block: (Boolean) -> Unit): UIModifier = hoverable(block)

fun UIModifier.pointerInput(filter: PointerInputFilter): UIModifier = then(PointerInputModifier(filter))
fun UIModifier.draggable(
    onDragStart: ((Float, Float) -> Unit)? = null,
    onDragEnd: (() -> Unit)? = null,
    onDragCancel: (() -> Unit)? = null,
    touchSlop: Float = 4.0f,
    onDrag: (dx: Float, dy: Float) -> Unit,
): UIModifier = then(
    DraggableModifier(
        onDragStart = onDragStart,
        onDrag = onDrag,
        onDragEnd = onDragEnd,
        onDragCancel = onDragCancel,
        touchSlop = touchSlop,
    ),
)

fun UIModifier.onPointerDown(block: (PointerEvent) -> Unit): UIModifier = then(PointerDownModifier(block))
fun UIModifier.onPointerUp(block: (PointerEvent) -> Unit): UIModifier = then(PointerUpModifier(block))
fun UIModifier.onPointerDrag(block: (PointerEvent) -> Unit): UIModifier = then(PointerDragModifier(block))
fun UIModifier.onScroll(block: (ScrollEvent) -> Unit): UIModifier = then(ScrollModifier(block))

/**
 * ## KeyDownModifier
 *
 * Attaches a keyboard key down listener to a [UINode].
 *
 * @property onKeyDown Key press handler returning `true` to consume the event.
 */
data class KeyDownModifier(val onKeyDown: (org.mdt.core.ui.input.Key) -> Boolean) : UIModifier.Element {
    override fun applyTo(node: UINode) {
        node.onKeyDown = onKeyDown
    }
}

/**
 * ## CursorModifier
 *
 * Configures the mouse cursor displayed when pointer hovers over a [UINode].
 *
 * @property cursor Cursor icon to display.
 */
data class CursorModifier(val cursor: CursorIcon) : UIModifier.Element {
    override fun applyTo(node: UINode) {
        node.cursor = cursor
    }
}

/**
 * ## ConsumePointerModifier
 *
 * Intercepts and consumes pointer events to prevent them from bubbling to underlying background scrims.
 */
data class ConsumePointerModifier(val dummy: Unit = Unit) : UIModifier.Element {
    override fun applyTo(node: UINode) {
        node.hitTestBehavior = HitTestBehavior.OPAQUE
        node.onPointerDown = { it.consume() }
        node.onClick = { /* consumes click event without action */ }
    }
}

fun UIModifier.consumePointer(): UIModifier = then(ConsumePointerModifier())

fun UIModifier.onKeyDown(block: (org.mdt.core.ui.input.Key) -> Boolean): UIModifier = then(KeyDownModifier(block))
fun UIModifier.cursor(cursor: CursorIcon): UIModifier = then(CursorModifier(cursor))
fun UIModifier.focusable(focusable: Boolean = true): UIModifier = then(FocusableModifier(focusable))
fun UIModifier.hitTestBehavior(behavior: HitTestBehavior): UIModifier = then(HitTestBehaviorModifier(behavior))
fun UIModifier.opaque(): UIModifier = hitTestBehavior(HitTestBehavior.OPAQUE)
fun UIModifier.tag(tag: Any?): UIModifier = then(TagModifier(tag))
