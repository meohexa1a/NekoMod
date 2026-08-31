// [AGENT INVARIANT] Synchronously update @property, @param, and @see KDocs when modifying this file.

package org.mdt.core.ui.input

import arc.input.InputProcessor
import arc.input.KeyCode
import org.mdt.core.platform.PlatformHost
import org.mdt.core.ui.node.CanvasNode
import org.mdt.core.ui.node.InputNode
import org.mdt.core.ui.node.UINode

/**
 * ## EngineInputProcessor
 *
 * Routes pointer clicks, drags, hover state, wheel scrolling, and keyboard events into the virtual UI tree.
 * Uses a 3-pass event pipeline: `INITIAL` (tunneling), `MAIN` (bubbling), and `FINAL` (cursor/hover update).
 * Coordinates keyboard focus and OS IME sessions with [org.mdt.core.platform.PlatformHost].
 *
 * @property canvas Target root virtual screen node ([CanvasNode]).
 * @property hoveredNode Virtual node currently under the pointer cursor.
 * @property pressedNode Virtual node currently receiving active pointer drag/press gestures.
 * @property focusedNode Virtual node currently holding keyboard input focus.
 *
 * @see CanvasNode
 * @see PointerEvent
 * @see InputNode
 * @see org.mdt.core.platform.PlatformHost
 */
class EngineInputProcessor(
    val canvas: CanvasNode,
    private val hostProvider: () -> PlatformHost,
) : InputProcessor {

    private val host: PlatformHost get() = hostProvider()

    // --- PROPERTIES & REUSABLE BUFFERS ---

    private val hitPathBuffer = ArrayList<UINode>(16)

    var hoveredNode: UINode? = null
        private set
    var pressedNode: UINode? = null
        private set
    var focusedNode: UINode? = null
        private set

    private var prevMouseX: Float = 0.0f
    private var prevMouseY: Float = 0.0f
    private var isPointerPressed: Boolean = false

    private var lastClickTime: Long = 0L
    private var lastClickNode: UINode? = null

    private var pendingSingleClickNode: UINode? = null
    private var pendingSingleClickTime: Long = 0L
    private val doubleClickTimeout: Long = 280L

    private fun toLocalY(screenY: Int): Float = screenY.toFloat()

    /**
     * Called every frame to flush pending single clicks whose double-click window has expired.
     */
    fun update() {
        val node = pendingSingleClickNode ?: return
        val now = host.system.nowMillis()
        if (now - pendingSingleClickTime >= doubleClickTimeout) {
            pendingSingleClickNode = null
            if (node.parent != null) {
                node.onClick?.invoke()
            }
        }
    }

    // --- 3-PASS DISPATCH PIPELINE ---

    private fun dispatch3Pass(change: PointerInputChange, type: PointerEventType): Boolean {
        hitPathBuffer.clear()
        canvas.buildHitPath(change.x, change.y, hitPathBuffer)
        if (hitPathBuffer.isEmpty()) return false

        val event = PointerEvent(change, type, PointerEventPass.INITIAL)

        // Pass 1: INITIAL (Tunneling: Root -> Leaf)
        event.pass = PointerEventPass.INITIAL
        for (i in hitPathBuffer.indices) {
            hitPathBuffer[i].dispatchPointerEvent(event, PointerEventPass.INITIAL)
            if (event.isConsumed) break
        }

        // Pass 2: MAIN (Bubbling: Leaf -> Root)
        event.pass = PointerEventPass.MAIN
        for (i in hitPathBuffer.indices.reversed()) {
            hitPathBuffer[i].dispatchPointerEvent(event, PointerEventPass.MAIN)
            if (event.isConsumed) break
        }

        // Pass 3: FINAL (Cleanup: Root -> Leaf)
        event.pass = PointerEventPass.FINAL
        for (i in hitPathBuffer.indices) {
            hitPathBuffer[i].dispatchPointerEvent(event, PointerEventPass.FINAL)
        }

        return event.isConsumed || hitPathBuffer.size > 1
    }

    // --- FOCUS & IME MANAGEMENT ---

    fun requestFocus(node: UINode?) {
        if (focusedNode === node) return

        val previous = focusedNode
        if (previous is InputNode) {
            previous.editState.clearComposition()
            host.ime.stopSession()
        }

        previous?.let { it.isFocused = false }
        focusedNode = node
        node?.let { it.isFocused = true }

        if (node is InputNode) {
            val globalPos = node.localToGlobal(0.0f, 0.0f)
            host.ime.startSession(
                globalX = globalPos.x,
                globalY = globalPos.y,
                width = node.bounds.width,
                height = node.bounds.height,
                initialText = node.editState.text,
                cursorPosition = node.editState.cursor,
                onCompositionChanged = { candidate ->
                    node.editState.setComposition(candidate)
                    node.invalidateLayout()
                },
                onCompositionCleared = {
                    node.editState.clearComposition()
                    node.invalidateLayout()
                },
            )
        }
    }

    fun clearFocus() = requestFocus(null)

    /** Synchronizes native OS IME candidate position, size, text, and cursor bounds for [node]. */
    fun syncIme(node: InputNode) {
        if (focusedNode === node) {
            val globalPos = node.localToGlobal(0.0f, 0.0f)
            host.ime.syncSession(
                globalX = globalPos.x,
                globalY = globalPos.y,
                width = node.bounds.width,
                height = node.bounds.height,
                text = node.editState.text,
                cursorPosition = node.editState.cursor,
            )
        }
    }

    // --- NODE MATCHING HELPERS ---

    private fun findFocusableNode(hit: UINode): UINode? {
        if (hit.isFocusable) return hit

        for (i in hit.children.indices) {
            val child = hit.children[i]
            if (child.visible && child.isFocusable) return child
        }

        var current: UINode? = hit.parent
        while (current != null && current !== canvas) {
            if (current.isFocusable) return current
            current = current.parent
        }
        return null
    }

    private fun isDescendantOrSelf(child: UINode?, ancestor: UINode?): Boolean {
        if (child == null || ancestor == null) return false

        var current: UINode? = child
        while (current != null) {
            if (current === ancestor) return true
            current = current.parent
        }
        return false
    }

    // --- POINTER & TOUCH EVENTS ---

    override fun touchDown(screenX: Int, screenY: Int, pointer: Int, button: KeyCode): Boolean {
        val clickX = screenX.toFloat()
        val clickY = toLocalY(screenY)
        val now = host.system.nowMillis()

        isPointerPressed = true
        val change = PointerInputChange(
            id = pointer.toLong(),
            uptimeMillis = now,
            x = clickX,
            y = clickY,
            prevX = clickX,
            prevY = clickY,
            pressed = true,
            prevPressed = false,
            button = button,
        )
        prevMouseX = clickX
        prevMouseY = clickY

        val hitNode = canvas.hitTest(clickX, clickY)
        val validHitNode = if (hitNode !== canvas) hitNode else null

        if (validHitNode != null) {
            pressedNode = validHitNode

            val focusTarget = findFocusableNode(validHitNode)
            if (focusTarget != null) {
                requestFocus(focusTarget)
            } else {
                clearFocus()
            }

            validHitNode.dispatchPointerDown(PointerEvent(change, PointerEventType.Press))
        } else {
            clearFocus()
            pressedNode = null
        }

        val handled = dispatch3Pass(change, PointerEventType.Press)
        return handled || validHitNode != null
    }

    override fun touchUp(screenX: Int, screenY: Int, pointer: Int, button: KeyCode): Boolean {
        val clickX = screenX.toFloat()
        val clickY = toLocalY(screenY)
        val now = host.system.nowMillis()

        val change = PointerInputChange(
            id = pointer.toLong(),
            uptimeMillis = now,
            x = clickX,
            y = clickY,
            prevX = prevMouseX,
            prevY = prevMouseY,
            pressed = false,
            prevPressed = isPointerPressed,
            button = button,
        )
        isPointerPressed = false
        prevMouseX = clickX
        prevMouseY = clickY

        val hitNode = canvas.hitTest(clickX, clickY)
        val pressed = pressedNode
        pressedNode = null

        dispatch3Pass(change, PointerEventType.Release)

        if (pressed != null) {
            pressed.dispatchPointerUp(PointerEvent(change, PointerEventType.Release))

            val isInsideTarget = hitNode === pressed || isDescendantOrSelf(hitNode, pressed)
            if (isInsideTarget) {
                handleClickResolution(pressed, now)
            }

            return true
        }

        return hitNode != null && hitNode !== canvas
    }

    private fun handleClickResolution(pressed: UINode, now: Long) {
        val hasDoubleClick = pressed.onDoubleClick != null
        val isDoubleClickWindow = lastClickNode === pressed && (now - lastClickTime < doubleClickTimeout)

        when {
            hasDoubleClick && isDoubleClickWindow -> {
                pendingSingleClickNode = null
                lastClickTime = 0L
                lastClickNode = null
                pressed.onDoubleClick?.invoke()
            }

            hasDoubleClick -> {
                lastClickNode = pressed
                lastClickTime = now
                if (pressed.onClick != null) {
                    pendingSingleClickNode = pressed
                    pendingSingleClickTime = now
                }
            }

            else -> {
                pendingSingleClickNode?.let { previousPending ->
                    if (previousPending !== pressed && previousPending.parent != null) {
                        previousPending.onClick?.invoke()
                    }
                }
                pendingSingleClickNode = null
                pressed.onClick?.invoke()
                lastClickTime = now
                lastClickNode = pressed
            }
        }
    }

    override fun touchDragged(screenX: Int, screenY: Int, pointer: Int): Boolean {
        val dragX = screenX.toFloat()
        val dragY = toLocalY(screenY)
        val now = host.system.nowMillis()

        val change = PointerInputChange(
            id = pointer.toLong(),
            uptimeMillis = now,
            x = dragX,
            y = dragY,
            prevX = prevMouseX,
            prevY = prevMouseY,
            pressed = true,
            prevPressed = true,
            button = KeyCode.mouseLeft,
        )
        prevMouseX = dragX
        prevMouseY = dragY

        val pressed = pressedNode
        pressed?.dispatchPointerDrag(PointerEvent(change, PointerEventType.Drag))

        return dispatch3Pass(change, PointerEventType.Drag) || pressed != null
    }

    override fun mouseMoved(screenX: Int, screenY: Int): Boolean {
        val moveX = screenX.toFloat()
        val moveY = toLocalY(screenY)
        val now = host.system.nowMillis()

        val change = PointerInputChange(
            id = 0L,
            uptimeMillis = now,
            x = moveX,
            y = moveY,
            prevX = prevMouseX,
            prevY = prevMouseY,
            pressed = isPointerPressed,
            prevPressed = isPointerPressed,
            button = KeyCode.mouseLeft,
        )
        prevMouseX = moveX
        prevMouseY = moveY

        dispatch3Pass(change, PointerEventType.Move)

        val hitNode = canvas.hitTest(moveX, moveY)
        val target = if (hitNode !== canvas) hitNode else null

        if (target !== hoveredNode) {
            hoveredNode?.let {
                it.isHovered = false
                it.onPointerExit?.invoke()
                it.onHover?.invoke(false)
            }
            target?.let {
                it.isHovered = true
                it.onPointerEnter?.invoke()
                it.onHover?.invoke(true)
            }
            hoveredNode = target

            // Update System Mouse Cursor with clean when
            when {
                target == null -> host.window.restoreCursor()
                target.cursor != null -> host.window.setCursor(target.cursor)
                target.onClick != null || target.pointerFilters.isNotEmpty() -> host.window.setCursorHand()
                else -> host.window.restoreCursor()
            }
        }

        return target != null
    }

    // --- SCROLL EVENTS ---

    override fun scrolled(amountX: Float, amountY: Float): Boolean {
        val scrollX = host.input.mouseX
        val scrollY = host.input.mouseY
        val now = host.system.nowMillis()

        val change = PointerInputChange(
            id = 0L,
            uptimeMillis = now,
            x = scrollX,
            y = scrollY,
            scrollX = amountX,
            scrollY = amountY,
        )

        val handled3Pass = dispatch3Pass(change, PointerEventType.Scroll)
        if (handled3Pass) return true

        val hitNode = canvas.hitTest(scrollX, scrollY) ?: hoveredNode ?: return false
        val event = ScrollEvent(amountX, amountY)

        var current: UINode? = hitNode
        while (current != null) {
            if (current is org.mdt.core.ui.node.LayoutNode && current.scrollable) {
                if (current.handleScrollEvent(event)) return true
            }
            if (current.onScroll != null) {
                current.onScroll!!.invoke(event)
                if (event.isConsumed) return true
            }
            current = current.parent
        }

        return false
    }

    // --- KEYBOARD EVENTS ---

    override fun keyDown(keyCode: KeyCode): Boolean {
        val pureKey = Key.fromArcKeyCode(keyCode)
        return focusedNode?.dispatchKeyDown(pureKey) ?: false
    }

    override fun keyUp(keyCode: KeyCode): Boolean {
        val pureKey = Key.fromArcKeyCode(keyCode)
        return focusedNode?.dispatchKeyUp(pureKey) ?: false
    }

    override fun keyTyped(character: Char): Boolean =
        focusedNode?.dispatchKeyTyped(character) ?: false
}

