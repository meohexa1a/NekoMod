package org.mdt.ui.input

import arc.input.InputProcessor
import arc.input.KeyCode
import arc.util.Time
import org.mdt.ui.core.CanvasNode
import org.mdt.ui.core.PointerEvent
import org.mdt.ui.core.ScrollEvent
import org.mdt.ui.core.UINode

class EngineInputProcessor(val canvas: CanvasNode) : InputProcessor {
    var hoveredNode: UINode? = null
    var pressedNode: UINode? = null
    var focusedNode: UINode? = null
        private set

    private var lastClickTime: Long = 0
    private var lastClickNode: UINode? = null

    private fun toLocalY(screenY: Int): Float = screenY.toFloat()

    fun requestFocus(node: UINode?) {
        if (focusedNode === node) return
        focusedNode?.let {
            it.isFocused = false
        }
        focusedNode = node
        node?.let {
            it.isFocused = true
        }
    }

    fun clearFocus() {
        requestFocus(null)
    }

    private fun findActionableNode(hit: UINode?): UINode? {
        var cur = hit
        while (cur != null && cur !== canvas) {
            if (cur.onClick != null || cur.onDoubleClick != null || cur.onPointerDown != null || cur.onPointerUp != null || cur.isFocusable) {
                return cur
            }
            cur = cur.parent
        }
        return null
    }

    private fun isDescendantOrSelf(child: UINode?, ancestor: UINode?): Boolean {
        if (child == null || ancestor == null) return false
        var cur: UINode? = child
        while (cur != null) {
            if (cur === ancestor) return true
            cur = cur.parent
        }
        return false
    }

    override fun touchDown(screenX: Int, screenY: Int, pointer: Int, button: KeyCode): Boolean {
        val x = screenX.toFloat()
        val y = toLocalY(screenY)

        val hit = canvas.hitTest(x, y)
        if (hit != null && hit !== canvas) {
            val actionable = findActionableNode(hit)
            if (actionable != null) {
                pressedNode = actionable
                // Focus handling
                if (actionable.isFocusable) {
                    requestFocus(actionable)
                } else {
                    clearFocus()
                }

                val event = PointerEvent(x, y, pointer, button)
                actionable.onPointerDown?.invoke(event)
                return true
            }
            clearFocus()
            return true
        }

        clearFocus()
        pressedNode = null
        return false
    }

    override fun touchUp(screenX: Int, screenY: Int, pointer: Int, button: KeyCode): Boolean {
        val x = screenX.toFloat()
        val y = toLocalY(screenY)

        val hit = canvas.hitTest(x, y)
        val pressed = pressedNode
        pressedNode = null

        if (pressed != null) {
            val event = PointerEvent(x, y, pointer, button)
            pressed.onPointerUp?.invoke(event)

            if (isDescendantOrSelf(hit, pressed) || hit === pressed) {
                val now = Time.millis()
                if (lastClickNode === pressed && now - lastClickTime < 350) {
                    pressed.onDoubleClick?.invoke()
                    lastClickTime = 0
                    lastClickNode = null
                } else {
                    pressed.onClick?.invoke()
                    lastClickTime = now
                    lastClickNode = pressed
                }
            }
            return true
        }

        return hit != null && hit !== canvas
    }

    override fun touchDragged(screenX: Int, screenY: Int, pointer: Int): Boolean {
        val x = screenX.toFloat()
        val y = toLocalY(screenY)

        val pressed = pressedNode
        if (pressed != null) {
            val event = PointerEvent(x, y, pointer, arc.input.KeyCode.mouseLeft)
            pressed.onPointerDrag?.invoke(event)
            return true
        }

        return mouseMoved(screenX, screenY)
    }

    override fun mouseMoved(screenX: Int, screenY: Int): Boolean {
        val x = screenX.toFloat()
        val y = toLocalY(screenY)

        val hit = canvas.hitTest(x, y)
        val actionable = if (hit !== canvas) findActionableNode(hit) else null

        if (actionable !== hoveredNode) {
            hoveredNode?.let {
                it.isHovered = false
                it.onPointerExit?.invoke()
            }
            actionable?.let {
                it.isHovered = true
                it.onPointerEnter?.invoke()
            }
            hoveredNode = actionable
        }

        return hit != null && hit !== canvas
    }

    override fun scrolled(amountX: Float, amountY: Float): Boolean {
        val node = hoveredNode ?: return false
        val event = ScrollEvent(amountX, amountY)

        var cur: UINode? = node
        while (cur != null) {
            if (cur.onScroll != null) {
                cur.onScroll!!.invoke(event)
                if (event.isConsumed) return true
            }
            cur = cur.parent
        }

        return false
    }

    override fun keyDown(keyCode: KeyCode): Boolean {
        val focused = focusedNode ?: return false
        return focused.onKeyDown?.invoke(keyCode) ?: false
    }

    override fun keyUp(keyCode: KeyCode): Boolean {
        val focused = focusedNode ?: return false
        return focused.onKeyUp?.invoke(keyCode) ?: false
    }

    override fun keyTyped(character: Char): Boolean {
        val focused = focusedNode ?: return false
        return focused.onKeyTyped?.invoke(character) ?: false
    }
}
