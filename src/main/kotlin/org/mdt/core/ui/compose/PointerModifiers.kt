// [AGENT INVARIANT] Synchronously update @property, @param, and @see KDocs when modifying this file.

@file:Suppress("unused")

package org.mdt.core.ui.compose

import arc.Graphics.Cursor
import arc.input.KeyCode
import org.mdt.core.ui.input.KeyEvent
import org.mdt.core.ui.input.PointerEvent
import org.mdt.core.ui.input.PointerEventPass
import org.mdt.core.ui.input.PointerEventType
import org.mdt.core.ui.input.PointerInputFilter
import org.mdt.core.ui.input.ScrollEvent
import org.mdt.core.ui.node.HitTestBehavior
import org.mdt.core.ui.node.UINode

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
    val onPressStateChanged: ((Boolean) -> Unit)? = null
) : UIModifier.Element {
    override fun applyTo(node: UINode) {
        node.hitTestBehavior = HitTestBehavior.OPAQUE
        node.cursor = Cursor.SystemCursor.hand

        node.addPointerInputFilter(object : PointerInputFilter {
            private var isPointerDownInside = false

            override fun onPointerEvent(event: PointerEvent, pass: PointerEventPass, node: UINode) {
                if (pass != PointerEventPass.MAIN) return

                when (event.type) {
                    PointerEventType.Press -> {
                        isPointerDownInside = true
                        onPressStateChanged?.invoke(true)
                        event.consume()
                    }
                    PointerEventType.Release -> {
                        if (isPointerDownInside) {
                            isPointerDownInside = false
                            onPressStateChanged?.invoke(false)
                            onClick()
                            event.consume()
                        }
                    }
                    PointerEventType.Exit, PointerEventType.Cancel -> {
                        if (isPointerDownInside) {
                            isPointerDownInside = false
                            onPressStateChanged?.invoke(false)
                        }
                    }
                    else -> {}
                }
            }
        })
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
        node.addPointerInputFilter(object : PointerInputFilter {
            override fun onPointerEvent(event: PointerEvent, pass: PointerEventPass, node: UINode) {
                if (pass == PointerEventPass.FINAL && event.type == PointerEventType.Enter) {
                    onHover(true)
                } else if (pass == PointerEventPass.FINAL && event.type == PointerEventType.Exit) {
                    onHover(false)
                }
            }
        })
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
    val touchSlop: Float = 4.0f
) : UIModifier.Element {
    override fun applyTo(node: UINode) {
        node.addPointerInputFilter(object : PointerInputFilter {
            private var isDragging = false
            private var totalDistance = 0.0f

            override fun onPointerEvent(event: PointerEvent, pass: PointerEventPass, node: UINode) {
                if (pass != PointerEventPass.MAIN) return

                when (event.type) {
                    PointerEventType.Press -> {
                        isDragging = false
                        totalDistance = 0.0f
                    }
                    PointerEventType.Drag, PointerEventType.Move -> {
                        if (event.change.pressed) {
                            totalDistance += kotlin.math.hypot(event.dx, event.dy)
                            if (!isDragging && totalDistance >= touchSlop) {
                                isDragging = true
                                onDragStart?.invoke(event.x, event.y)
                            }
                            if (isDragging) {
                                onDrag(event.dx, event.dy)
                                event.consume()
                            }
                        }
                    }
                    PointerEventType.Release -> {
                        if (isDragging) {
                            isDragging = false
                            onDragEnd?.invoke()
                            event.consume()
                        }
                    }
                    PointerEventType.Cancel -> {
                        if (isDragging) {
                            isDragging = false
                            onDragCancel?.invoke()
                        }
                    }
                    else -> {}
                }
            }
        })
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
    override fun applyTo(node: UINode) = node.addPointerInputFilter(filter)
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
        node.addPointerInputFilter(object : PointerInputFilter {
            override fun onPointerEvent(event: PointerEvent, pass: PointerEventPass, node: UINode) {
                if (pass == PointerEventPass.MAIN && event.type == PointerEventType.Press) {
                    onDown(event)
                }
            }
        })
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
        node.addPointerInputFilter(object : PointerInputFilter {
            override fun onPointerEvent(event: PointerEvent, pass: PointerEventPass, node: UINode) {
                if (pass == PointerEventPass.MAIN && event.type == PointerEventType.Release) {
                    onUp(event)
                }
            }
        })
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
        node.addPointerInputFilter(object : PointerInputFilter {
            override fun onPointerEvent(event: PointerEvent, pass: PointerEventPass, node: UINode) {
                if (pass == PointerEventPass.MAIN && event.type == PointerEventType.Drag) {
                    onDrag(event)
                }
            }
        })
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
 * ## KeyDownModifier
 *
 * Intercepts physical key down events when the [UINode] has input focus.
 *
 * @property onKeyDown Key handler callback returning `true` if consumed.
 */
data class KeyDownModifier(val onKeyDown: (KeyCode) -> Boolean) : UIModifier.Element {
    override fun applyTo(node: UINode) {
        node.onKeyDown = onKeyDown
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
 * ## CursorModifier
 *
 * Configures OS/Game mouse cursor icon displayed when hovering over a [UINode].
 *
 * @property cursor System or custom mouse cursor icon.
 */
data class CursorModifier(val cursor: Cursor) : UIModifier.Element {
    override fun applyTo(node: UINode) {
        node.cursor = cursor
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
    onClick: () -> Unit
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
    onDrag: (dx: Float, dy: Float) -> Unit
): UIModifier = then(
    DraggableModifier(
        onDragStart = onDragStart,
        onDrag = onDrag,
        onDragEnd = onDragEnd,
        onDragCancel = onDragCancel,
        touchSlop = touchSlop
    )
)

fun UIModifier.onPointerDown(block: (PointerEvent) -> Unit): UIModifier = then(PointerDownModifier(block))
fun UIModifier.onPointerUp(block: (PointerEvent) -> Unit): UIModifier = then(PointerUpModifier(block))
fun UIModifier.onPointerDrag(block: (PointerEvent) -> Unit): UIModifier = then(PointerDragModifier(block))
fun UIModifier.onScroll(block: (ScrollEvent) -> Unit): UIModifier = then(ScrollModifier(block))

fun UIModifier.touchable(touchable: Boolean): UIModifier = then(TouchableModifier(touchable))
fun UIModifier.onKeyDown(block: (KeyCode) -> Boolean): UIModifier = then(KeyDownModifier(block))
fun UIModifier.focusable(focusable: Boolean = true): UIModifier = then(FocusableModifier(focusable))
fun UIModifier.cursor(cursor: Cursor): UIModifier = then(CursorModifier(cursor))
fun UIModifier.hitTestBehavior(behavior: HitTestBehavior): UIModifier = then(HitTestBehaviorModifier(behavior))
fun UIModifier.opaque(): UIModifier = hitTestBehavior(HitTestBehavior.OPAQUE)
fun UIModifier.tag(tag: Any?): UIModifier = then(TagModifier(tag))
