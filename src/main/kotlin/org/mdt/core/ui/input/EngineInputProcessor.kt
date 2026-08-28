package org.mdt.core.ui.input

import arc.input.InputProcessor
import arc.input.KeyCode
import org.mdt.core.ui.EngineRuntime
import org.mdt.core.ui.node.CanvasNode
import org.mdt.core.ui.node.InputNode
import org.mdt.core.ui.node.UINode

/**
 * ## EngineInputProcessor [Hardware Input Event Router & Focus Coordinator]
 *
 * ### 1. 📖 Feature Specification & Core Architecture:
 * - Master [InputProcessor] routing hardware pointer down/up/drag, mouse enter/exit, mouse wheel scroll, and keyboard events into the virtual [UINode] tree.
 * - Dispatches pointer events via 2D hit testing on [canvas] using bottom-left screen coordinates.
 * - Serves as the single authoritative Focus & IME Session Coordinator ([requestFocus], [clearFocus], [syncIme]):
 *   - Automatically activates OS native IME session via [org.mdt.core.engine.PlatformHost] when an [InputNode] is focused.
 *   - Gracefully terminates IME session when focus is cleared or shifted.
 * - Supports double-click recognition (280ms threshold) and flushes delayed single-click actions smoothly.
 * - Seamlessly passes unhandled events through to underlying Mindustry gameplay (`return false`).
 *
 * ### 2. ⚡ Invariants & Non-Negotiable Rules:
 * - **Rule 1 (Event-Driven Drag Routing):** Route continuous drag gestures via `onPointerDrag` in `touchDragged`; NEVER poll hardware in frame render loops.
 * - **Rule 2 (Cursor Management via Host):** Mouse cursor updates (`setCursorHand`, `setCursor`, `restoreCursor`) MUST execute exclusively through [EngineRuntime.host].
 * - **Rule 3 (Single Focus & IME Authority):** IME lifecycle MUST be coordinated exclusively through `requestFocus` / `clearFocus` / `syncIme`.
 *
 * ### 3. 🔗 Related Files & Subsystem Map:
 * - 🌲 **Root Virtual Node:** `src/main/kotlin/org/mdt/core/ui/node/CanvasNode.kt`
 * - 🌲 **Text Input Node:** `src/main/kotlin/org/mdt/core/ui/node/InputNode.kt`
 * - 🎛️ **Event POJOs:** `src/main/kotlin/org/mdt/core/ui/input/InputEvents.kt`
 * - 🔌 **Platform Host:** `src/main/kotlin/org/mdt/core/engine/PlatformHost.kt`
 * - ⚙️ **Runtime Orchestrator:** `src/main/kotlin/org/mdt/core/ui/EngineRuntime.kt`
 *
 * ### 4. ✅ Behavioral Verification Checklist:
 * - [x] `touchDown` returns `true` when a non-canvas node is hit and assigns focus target.
 * - [x] `requestFocus` automatically launches IME session for [InputNode] and terminates previous session.
 * - [x] `clearFocus` cleanly stops native IME session.
 * - [x] `touchUp` differentiates between single-click and double-click without dropping events.
 * - [x] `mouseMoved` updates hovered node, fires enter/exit callbacks, and updates hardware cursor.
 * - [x] `scrolled` bubbles scroll events up ancestor hierarchy until consumed.
 * - [x] `keyDown`, `keyUp`, `keyTyped` dispatch to currently focused node.
 */
class EngineInputProcessor(val canvas: CanvasNode) : InputProcessor {

    // --- PROPERTIES & STATE ---

    var hoveredNode: UINode? = null
    var pressedNode: UINode? = null
    var focusedNode: UINode? = null
        private set

    private var lastClickTime: Long = 0
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
        val now = EngineRuntime.host.nowMillis()
        if (now - pendingSingleClickTime >= doubleClickTimeout) {
            pendingSingleClickNode = null
            node.onClick?.invoke()
        }
    }

    // --- FOCUS & IME MANAGEMENT ---

    fun requestFocus(node: UINode?) {
        if (focusedNode === node) return

        val previous = focusedNode
        if (previous is InputNode) {
            previous.editState.clearComposition()
            EngineRuntime.host.stopImeSession()
        }

        previous?.let { it.isFocused = false }
        focusedNode = node
        node?.let { it.isFocused = true }

        if (node is InputNode) {
            val globalPos = node.localToGlobal(0.0f, 0.0f)
            EngineRuntime.host.startImeSession(
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
                }
            )
        }
    }

    fun clearFocus() = requestFocus(null)

    /** Synchronizes native OS IME candidate position, size, text, and cursor bounds for [node]. */
    fun syncIme(node: InputNode) {
        if (focusedNode === node) {
            val globalPos = node.localToGlobal(0.0f, 0.0f)
            EngineRuntime.host.syncImeSession(
                globalX = globalPos.x,
                globalY = globalPos.y,
                width = node.bounds.width,
                height = node.bounds.height,
                text = node.editState.text,
                cursorPosition = node.editState.cursor
            )
        }
    }

    // --- HIT TESTING & NODE MATCHING ---

    private fun findFocusableNode(hit: UINode): UINode? {
        if (hit.isFocusable) return hit

        // 1. Check direct children
        for (i in hit.children.indices) {
            val child = hit.children[i]
            if (child.visible && child.isFocusable) return child
        }

        // 2. Check parent chain
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

        val hitNode = canvas.hitTest(clickX, clickY)

        if (hitNode != null && hitNode !== canvas) {
            pressedNode = hitNode

            val focusTarget = findFocusableNode(hitNode)
            if (focusTarget != null) {
                requestFocus(focusTarget)
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

                if (pressed.onDoubleClick != null) {
                    val isDouble = lastClickNode === pressed && (now - lastClickTime < doubleClickTimeout)
                    if (isDouble) {
                        // Cancel pending single-click and execute double-click
                        pendingSingleClickNode = null
                        lastClickTime = 0L
                        lastClickNode = null
                        pressed.onDoubleClick?.invoke()
                    } else {
                        // First click on node with double-click capability: delay single-click
                        lastClickNode = pressed
                        lastClickTime = now
                        if (pressed.onClick != null) {
                            pendingSingleClickNode = pressed
                            pendingSingleClickTime = now
                        }
                    }
                } else {
                    // Flush any pending single-click on another node
                    val prev = pendingSingleClickNode
                    if (prev != null) {
                        pendingSingleClickNode = null
                        prev.onClick?.invoke()
                    }
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

    override fun keyDown(keyCode: KeyCode): Boolean = focusedNode?.onKeyDown?.invoke(keyCode) ?: false

    override fun keyUp(keyCode: KeyCode): Boolean = focusedNode?.onKeyUp?.invoke(keyCode) ?: false

    override fun keyTyped(character: Char): Boolean = focusedNode?.onKeyTyped?.invoke(character) ?: false
}
