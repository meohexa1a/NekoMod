package org.mdt.core.ui.input

import arc.Core
import arc.input.InputProcessor
import arc.input.KeyCode
import arc.util.Time
import org.mdt.core.ui.CanvasNode
import org.mdt.core.ui.UINode

/**
 * ## EngineInputProcessor
 *
 * Core Arc [InputProcessor] implementation routing hardware pointer, scroll,
 * and keyboard events into the declarative [UINode] tree.
 *
 * See: docs/ui-engine/ui_engine_en.md
 */
class EngineInputProcessor(val canvas: CanvasNode) : InputProcessor {

    var hoveredNode: UINode? = null
    var pressedNode: UINode? = null
    var focusedNode: UINode? = null
        private set

    private var lastClickTime: Long = 0
    private var lastClickNode: UINode? = null

    private fun toLocalY(screenY: Int): Float = screenY.toFloat()

    // =========================================================================
    // I. Focus Management
    // =========================================================================

    fun requestFocus(node: UINode?) {
        if (focusedNode === node) return

        focusedNode?.let { it.isFocused = false }
        focusedNode = node
        node?.let { it.isFocused = true }
    }

    fun clearFocus() = requestFocus(null)

    // =========================================================================
    // II. Hit Testing & Node Matching
    // =========================================================================

    private fun findActionableNode(hit: UINode?): UINode? {
        var current = hit
        while (current != null && current !== canvas) {
            if (current.onClick != null || current.onDoubleClick != null ||
                current.onPointerDown != null || current.onPointerUp != null ||
                current.onPointerDrag != null || current.onHover != null ||
                current.onPointerEnter != null || current.onPointerExit != null ||
                current.onScroll != null || current.isFocusable
            ) {
                return current
            }
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

    // =========================================================================
    // III. Pointer & Touch Events
    // =========================================================================

    override fun touchDown(screenX: Int, screenY: Int, pointer: Int, button: KeyCode): Boolean {
        val clickX = screenX.toFloat()
        val clickY = toLocalY(screenY)

        val hitNode = canvas.hitTest(clickX, clickY)
        if (hitNode != null && hitNode !== canvas) {
            val actionable = findActionableNode(hitNode)
            if (actionable != null) {
                pressedNode = actionable

                if (actionable.isFocusable) {
                    requestFocus(actionable)
                } else {
                    clearFocus()
                }

                val event = PointerEvent(clickX, clickY, pointer, button)
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
        val clickX = screenX.toFloat()
        val clickY = toLocalY(screenY)

        val hitNode = canvas.hitTest(clickX, clickY)
        val pressed = pressedNode
        pressedNode = null

        if (pressed != null) {
            val event = PointerEvent(clickX, clickY, pointer, button)
            pressed.onPointerUp?.invoke(event)

            if (isDescendantOrSelf(hitNode, pressed) || hitNode === pressed) {
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

        return hitNode != null && hitNode !== canvas
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

        return mouseMoved(screenX, screenY)
    }

    override fun mouseMoved(screenX: Int, screenY: Int): Boolean {
        val moveX = screenX.toFloat()
        val moveY = toLocalY(screenY)

        val hitNode = canvas.hitTest(moveX, moveY)
        val actionable = if (hitNode !== canvas) findActionableNode(hitNode) else null

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

        return hitNode != null && hitNode !== canvas
    }

    // =========================================================================
    // IV. Scroll Events
    // =========================================================================

    override fun scrolled(amountX: Float, amountY: Float): Boolean {
        val scrollX = if (Core.input != null) Core.input.mouseX().toFloat() else 0f
        val scrollY = if (Core.input != null) toLocalY(Core.input.mouseY()) else 0f

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

    // =========================================================================
    // V. Keyboard Events
    // =========================================================================

    override fun keyDown(keyCode: KeyCode): Boolean = focusedNode?.onKeyDown?.invoke(keyCode) ?: false

    override fun keyUp(keyCode: KeyCode): Boolean = focusedNode?.onKeyUp?.invoke(keyCode) ?: false

    override fun keyTyped(character: Char): Boolean = focusedNode?.onKeyTyped?.invoke(character) ?: false
}
