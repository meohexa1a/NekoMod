package org.mdt.core.ui

import arc.input.KeyCode
import arc.math.geom.Vec2
import org.mdt.core.ui.input.PointerEvent
import org.mdt.core.ui.input.ScrollEvent
import org.mdt.core.ui.layout.AnchorData
import org.mdt.core.ui.layout.SizeFlags
import org.mdt.core.ui.render.EngineRenderer
import org.mdt.core.ui.render.ScissorStack

/**
 * ## UINode (Virtual UI Node)
 *
 * Base class for all elements in the declarative Virtual DOM UI tree.
 * Manages parent-child hierarchy, Box Model (Margin & Padding),
 * sizing computations, event dispatching, and coordinate transformations.
 *
 * See: docs/architecture/architecture_en.md
 * See: docs/layout-engine/layout_engine_en.md
 */
open class UINode {

    /** Unique query identifier in the Virtual DOM tree. */
    var id: String = ""

    /** Human-readable display label (e.g. "HeroCard", "Action Row"). */
    var name: String = ""

    /** Parent node owning this node in the UI hierarchy. */
    var parent: UINode? = null
        internal set

    /** List of child nodes belonging to this node. */
    val children = ArrayList<UINode>()

    /**
     * Absolute screen boundary rectangle.
     * Uses OpenGL bottom-left origin: (x=0, y=0) at bottom-left screen corner.
     */
    val bounds = Rect()

    // ==========================================
    // SIZE FLAGS & SIZING
    // ==========================================

    /** Horizontal size flags for container slot allocation. Default: 0 (Hug content). */
    var sizeFlagsHorizontal: Int = 0

    /** Vertical size flags for container slot allocation. Default: 0 (Hug content). */
    var sizeFlagsVertical: Int = 0

    /** Weight ratio for distributing excess container space when [SizeFlags.EXPAND] is set. */
    var stretchRatio: Float = 1f

    /** Anchor and offset data for absolute/relative screen anchoring. */
    val anchorData: AnchorData = AnchorData()

    /** Fixed desired width (-1f for auto size). */
    var width: Float = -1f
    /** Fixed desired height (-1f for auto size). */
    var height: Float = -1f
    /** Minimum allowed width (-1f for unconstrained). */
    var minWidth: Float = -1f
    /** Minimum allowed height (-1f for unconstrained). */
    var minHeight: Float = -1f
    /** Maximum allowed width (-1f for unconstrained). */
    var maxWidth: Float = -1f
    /** Maximum allowed height (-1f for unconstrained). */
    var maxHeight: Float = -1f

    // ==========================================
    // BOX MODEL: MARGIN (Outward Spacing)
    // ==========================================

    /** Outward margin on the left side (pixels). */
    var marginL: Float = 0f
    /** Outward margin on the top side (pixels). */
    var marginT: Float = 0f
    /** Outward margin on the right side (pixels). */
    var marginR: Float = 0f
    /** Outward margin on the bottom side (pixels). */
    var marginB: Float = 0f

    /** Sets equal margin on all 4 sides. */
    fun margin(all: Float) = margin(all, all, all, all)

    /** Sets margin on horizontal and vertical axes. */
    fun margin(horizontal: Float = 0f, vertical: Float = 0f) = margin(horizontal, vertical, horizontal, vertical)

    /** Sets margin individually for each side. */
    fun margin(left: Float = 0f, top: Float = 0f, right: Float = 0f, bottom: Float = 0f) {
        marginL = left
        marginT = top
        marginR = right
        marginB = bottom
        invalidateLayout()
    }

    // ==========================================
    // BOX MODEL: PADDING (Inward Spacing)
    // ==========================================

    /** Inward padding on the left side (pixels). */
    var padL: Float = 0f
    /** Inward padding on the top side (pixels). */
    var padT: Float = 0f
    /** Inward padding on the right side (pixels). */
    var padR: Float = 0f
    /** Inward padding on the bottom side (pixels). */
    var padB: Float = 0f

    /** Sets equal padding on all 4 sides. */
    fun pad(all: Float) = pad(all, all, all, all)

    /** Sets padding on horizontal and vertical axes. */
    fun pad(horizontal: Float = 0f, vertical: Float = 0f) = pad(horizontal, vertical, horizontal, vertical)

    /** Sets padding individually for each side. */
    fun pad(left: Float = 0f, top: Float = 0f, right: Float = 0f, bottom: Float = 0f) {
        padL = left
        padT = top
        padR = right
        padB = bottom
        invalidateLayout()
    }

    // ==========================================
    // STATE & LIFECYCLE
    // ==========================================

    /** Visibility flag. When false, the node is hidden and does not consume layout space. */
    var visible: Boolean = true
        set(value) {
            if (field != value) {
                field = value
                invalidateLayout()
            }
        }

    /** Whether this node can receive pointer events (hit testing). */
    var touchable: Boolean = true

    /** Whether this node can accept keyboard focus. */
    var isFocusable: Boolean = false

    /** Whether this node currently holds keyboard focus. */
    var isFocused: Boolean = false
        internal set

    /** Whether child rendering should be clipped to this node's bounding box. */
    var clip: Boolean = false

    /** Dirty flag indicating layout recalculation is required. */
    var isLayoutDirty: Boolean = true

    /** Generic user payload or AST DOM Element tag attached to this node. */
    var tag: Any? = null

    // ==========================================
    // EVENT LISTENERS
    // ==========================================

    /** Invoked on single left click. */
    var onClick: (() -> Unit)? = null

    /** Invoked on double click within double-click threshold. */
    var onDoubleClick: (() -> Unit)? = null

    /** Invoked on pointer press down. */
    var onPointerDown: ((PointerEvent) -> Unit)? = null

    /** Invoked continuously while dragging a pressed node. */
    var onPointerDrag: ((PointerEvent) -> Unit)? = null

    /** Invoked on pointer release up. */
    var onPointerUp: ((PointerEvent) -> Unit)? = null

    /** Invoked when pointer enters node bounds. */
    var onPointerEnter: (() -> Unit)? = null

    /** Invoked when pointer exits node bounds. */
    var onPointerExit: (() -> Unit)? = null

    /** Invoked when hover state changes. */
    var onHover: ((Boolean) -> Unit)? = null

    /** Custom mouse cursor displayed when pointer hovers over this node. */
    var cursor: arc.Graphics.Cursor? = null

    /** Invoked on scroll wheel action. */
    var onScroll: ((ScrollEvent) -> Unit)? = null

    /** Invoked when a character is typed while focused. Return true to consume. */
    var onKeyTyped: ((Char) -> Boolean)? = null

    /** Invoked when a key is pressed while focused. Return true to consume. */
    var onKeyDown: ((KeyCode) -> Boolean)? = null

    /** Invoked when a key is released while focused. Return true to consume. */
    var onKeyUp: ((KeyCode) -> Boolean)? = null

    /** Whether pointer is currently hovering over this node. */
    var isHovered: Boolean = false
        internal set(value) {
            if (field != value) {
                field = value
                onHover?.invoke(value)
            }
        }

    /** Marks this node and its ancestors as dirty to trigger layout recalculation. */
    fun invalidateLayout() {
        isLayoutDirty = true
        parent?.invalidateLayout()
    }

    /** Sets node boundary rectangle. */
    fun setBounds(x: Float, y: Float, width: Float, height: Float) = bounds.set(x, y, width, height)

    /** Sets node position (x, y). */
    fun setPosition(x: Float, y: Float) {
        bounds.x = x
        bounds.y = y
    }

    /** Sets node dimensions (width, height). */
    fun setSize(width: Float, height: Float) {
        bounds.width = width
        bounds.height = height
    }

    /** Computes preferred width including inward padding. */
    open fun getPrefWidth(): Float {
        val base = if (width >= 0f) width else if (minWidth >= 0f) minWidth else 0f
        return base + padL + padR
    }

    /** Computes preferred height including inward padding. */
    open fun getPrefHeight(): Float {
        val base = if (height >= 0f) height else if (minHeight >= 0f) minHeight else 0f
        return base + padT + padB
    }

    /** Executes layout pass for this node and its children. */
    open fun layout() {
        isLayoutDirty = false
        for (child in children) {
            if (child.visible) child.layout()
        }
    }

    /** Renders this node and its children to the GPU. */
    open fun draw(renderer: EngineRenderer) {
        if (!visible) return

        val shouldClip = clip && bounds.width > 0f && bounds.height > 0f
        var pushed = false
        if (shouldClip) {
            pushed = ScissorStack.push(bounds)
            if (!pushed) {
                // Completely clipped outside visible bounds
                ScissorStack.pop()
                return
            }
        }

        drawSelf(renderer)
        drawChildren(renderer)

        if (pushed) ScissorStack.pop()
    }

    /** Renders the visual representation of this node. */
    protected open fun drawSelf(renderer: EngineRenderer) {}

    /** Renders all visible children in bottom-up order. */
    protected open fun drawChildren(renderer: EngineRenderer) {
        for (child in children) {
            if (child.visible) child.draw(renderer)
        }
    }

    /**
     * Performs hit testing for screen coordinate (px, py).
     * Children are evaluated in top-to-bottom order (frontmost child first).
     */
    open fun hitTest(px: Float, py: Float): UINode? {
        if (!visible || !touchable) return null

        for (i in children.indices.reversed()) {
            val child = children[i]
            val hit = child.hitTest(px, py)
            if (hit != null) return hit
        }

        return if (bounds.contains(px, py)) this else null
    }

    // ==========================================
    // LIFECYCLE HOOKS
    // ==========================================

    /** Invoked when this node is attached to an active UI tree. */
    open fun onAttached() {}

    /** Invoked when this node is detached from the UI tree. */
    open fun onDetached() {}

    /** Requests keyboard focus. */
    fun requestFocus() {
        if (!isFocusable || isFocused) return
        isFocused = true
    }

    /** Clears keyboard focus. */
    fun clearFocus() {
        if (!isFocused) return
        isFocused = false
    }

    // ==========================================
    // COORDINATE TRANSFORMS
    // ==========================================

    /** Converts local coordinates (lx, ly) relative to this node into global screen coordinates. */
    fun localToGlobal(lx: Float, ly: Float): Vec2 {
        return Vec2(bounds.x + lx, bounds.y + ly)
    }

    /** Converts global screen coordinates (gx, gy) into local coordinates within this node. */
    fun globalToLocal(gx: Float, gy: Float): Vec2 {
        return Vec2(gx - bounds.x, gy - bounds.y)
    }

    /** Returns global bounding rectangle in screen space. */
    fun getGlobalBounds(): Rect {
        return Rect(bounds.x, bounds.y, bounds.width, bounds.height)
    }

    /** Returns root [CanvasNode] if attached to tree. */
    fun getCanvas(): CanvasNode? {
        var cur: UINode? = this
        while (cur != null) {
            if (cur is CanvasNode) return cur
            cur = cur.parent
        }
        return null
    }

    // ==========================================
    // TREE MANIPULATION
    // ==========================================

    fun addChild(child: UINode) {
        addChildAt(children.size, child)
    }

    fun addChildAt(index: Int, child: UINode) {
        child.parent?.removeChild(child)
        child.parent = this
        val safeIndex = index.coerceIn(0, children.size)
        children.add(safeIndex, child)
        child.onAttached()
        invalidateLayout()
    }

    fun removeChild(child: UINode): Boolean {
        val removed = children.remove(child)
        if (removed) {
            child.onDetached()
            child.parent = null
            invalidateLayout()
        }
        return removed
    }

    fun removeChildAt(index: Int): UINode {
        val child = children.removeAt(index)
        child.onDetached()
        child.parent = null
        invalidateLayout()
        return child
    }

    fun clearChildren() {
        for (child in children) {
            child.onDetached()
            child.parent = null
        }
        children.clear()
        invalidateLayout()
    }

    fun moveChild(from: Int, to: Int, count: Int = 1) {
        if (from == to || count <= 0 || from >= children.size) return
        val dest = if (to > from) to - count else to
        val safeDest = dest.coerceIn(0, children.size - count)
        val moved = ArrayList<UINode>(count)
        repeat(count) {
            moved.add(children.removeAt(from))
        }
        children.addAll(safeDest, moved)
        invalidateLayout()
    }

    fun findNodeById(targetId: String): UINode? {
        if (this.id == targetId || this.name == targetId) return this
        for (child in children) {
            val found = child.findNodeById(targetId)
            if (found != null) return found
        }
        return null
    }

    fun dumpTree(indent: String = ""): String {
        val sb = StringBuilder()
        val name = this.javaClass.simpleName.ifEmpty { "Node" }
        sb.append("$indent$name bounds=(${bounds.x.toInt()}, ${bounds.y.toInt()}, ${bounds.width.toInt()}x${bounds.height.toInt()}) pref=(${getPrefWidth().toInt()}x${getPrefHeight().toInt()})\n")
        for (child in children) {
            sb.append(child.dumpTree("$indent  "))
        }
        return sb.toString()
    }
}
