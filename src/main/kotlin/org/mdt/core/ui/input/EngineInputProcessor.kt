package org.mdt.core.ui.input

import arc.input.InputProcessor
import arc.input.KeyCode
import org.mdt.core.ui.CanvasNode
import org.mdt.core.ui.EngineRuntime
import org.mdt.core.ui.UINode

/**
 * ## EngineInputProcessor
 *
 * Core Arc [InputProcessor] routing hardware pointer, scroll,
 * and keyboard events into the declarative [UINode] tree.
 *
 * Seamlessly passes unhandled events through to Mindustry gameplay.
 *
 * See: docs/ui-engine/ui_engine_en.md
 */
class EngineInputProcessor(val canvas: CanvasNode) : InputProcessor {

    // --- PROPERTIES & STATE ---

    var hoveredNode: UINode? = null
    var pressedNode: UINode? = null
    var focusedNode: UINode? = null
        private set

    private var lastClickTime: Long = 0
    private var lastClickNode: UINode? = null

    private fun toLocalY(screenY: Int): Float = screenY.toFloat()

    // --- FOCUS MANAGEMENT ---

    fun requestFocus(node: UINode?) {
        if (focusedNode === node) return

        focusedNode?.let { it.isFocused = false }
        focusedNode = node
        node?.let { it.isFocused = true }
    }

    fun clearFocus() = requestFocus(null)

    // --- HIT TESTING & NODE MATCHING ---

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

        val hitNode = canvas.hitTest(clickX, clickY)

        if (hitNode != null && hitNode !== canvas) {
            pressedNode = hitNode

            if (hitNode.isFocusable) {
                requestFocus(hitNode)
            } else {
                clearFocus()
            }

            val event = PointerEvent(clickX, clickY, pointer, button)
            hitNode.onPointerDown?.invoke(event)
            return true
        }

        clearFocus()
        pressedNode = null
        return false
    }

    override fun touchUp(screenX: Int, screenY: Int, pointer: Int, button: KeyCode): Boolean {
        val clickX = screenX.toFloat()
        val clickY = toLocalY(screenY)

        val hitNode = canvas.hitTest(clickX, clickY)
        val pressed = pressedNode
        pressedNode = null

        if (pressed != null) {
            val event = PointerEvent(clickX, clickY, pointer, button)
            pressed.onPointerUp?.invoke(event)

            if (isDescendantOrSelf(hitNode, pressed) || hitNode === pressed) {
                val now = EngineRuntime.host.nowMillis()
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

        return false
    }

    override fun touchDragged(screenX: Int, screenY: Int, pointer: Int): Boolean {
        val dragX = screenX.toFloat()
        val dragY = toLocalY(screenY)

        val pressed = pressedNode
        if (pressed != null) {
            val event = PointerEvent(dragX, dragY, pointer, KeyCode.mouseLeft)
            pressed.onPointerDrag?.invoke(event)
            return true
        }

        return false
    }

    override fun mouseMoved(screenX: Int, screenY: Int): Boolean {
        val moveX = screenX.toFloat()
        val moveY = toLocalY(screenY)

        val hitNode = canvas.hitTest(moveX, moveY)
        val target = if (hitNode !== canvas) hitNode else null

        if (target !== hoveredNode) {
            hoveredNode?.let {
                it.isHovered = false
                it.onPointerExit?.invoke()
            }
            target?.let {
                it.isHovered = true
                it.onPointerEnter?.invoke()
            }
            hoveredNode = target

            // Update System Mouse Cursor
            if (target != null) {
                if (target.cursor != null) {
                    EngineRuntime.host.setCursor(target.cursor)
                } else if (target.onClick != null) {
                    EngineRuntime.host.setCursorHand()
                } else {
                    EngineRuntime.host.restoreCursor()
                }
            } else {
                EngineRuntime.host.restoreCursor()
            }
        }

        return target != null
    }

    // --- SCROLL EVENTS ---

    override fun scrolled(amountX: Float, amountY: Float): Boolean {
        val scrollX = EngineRuntime.host.mouseX
        val scrollY = EngineRuntime.host.mouseY

        val hitNode = canvas.hitTest(scrollX, scrollY) ?: hoveredNode ?: return false

        val event = ScrollEvent(amountX, amountY)

        var current: UINode? = hitNode
        while (current != null) {
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
        // 1. Try focused node
        focusedNode?.let { start ->
            var current: UINode? = start
            while (current != null) {
                if (current.onKeyDown?.invoke(keyCode) == true) return true
                current = current.parent
            }
        }

        // 2. Try hovered node
        hoveredNode?.let { start ->
            var current: UINode? = start
            while (current != null) {
                if (current.onKeyDown?.invoke(keyCode) == true) return true
                current = current.parent
            }
        }

        return false
    }

    override fun keyUp(keyCode: KeyCode): Boolean {
        focusedNode?.let { start ->
            var current: UINode? = start
            while (current != null) {
                if (current.onKeyUp?.invoke(keyCode) == true) return true
                current = current.parent
            }
        }

        hoveredNode?.let { start ->
            var current: UINode? = start
            while (current != null) {
                if (current.onKeyUp?.invoke(keyCode) == true) return true
                current = current.parent
            }
        }

        return false
    }

    override fun keyTyped(character: Char): Boolean {
        focusedNode?.let { start ->
            var current: UINode? = start
            while (current != null) {
                if (current.onKeyTyped?.invoke(character) == true) return true
                current = current.parent
            }
        }
        return false
    }
}
