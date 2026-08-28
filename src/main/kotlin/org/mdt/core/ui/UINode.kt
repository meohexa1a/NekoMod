package org.mdt.core.ui

import arc.input.KeyCode
import arc.math.geom.Vec2
import org.mdt.core.ui.input.PointerEvent
import org.mdt.core.ui.input.ScrollEvent
import org.mdt.core.ui.layout.AnchorData
import org.mdt.core.ui.render.UIBatch

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

    // --- IDENTITY & HIERARCHY ---

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

    // --- SIZING & SIZE FLAGS ---

    /** Horizontal size flags for container slot allocation. Default: 0 (Hug content). */
    var sizeFlagsHorizontal: Int = 0

    /** Vertical size flags for container slot allocation. Default: 0 (Hug content). */
    var sizeFlagsVertical: Int = 0

    /** Weight ratio for distributing excess container space when [org.mdt.core.ui.layout.SizeFlags.EXPAND] is set. */
    var stretchRatio: Float = 1.0f

    /** Anchor and offset data for absolute/relative screen anchoring. */
    val anchorData: AnchorData = AnchorData()

    /** Fixed desired width (-1f for auto size). */
    var width: Float = -1.0f

    /** Fixed desired height (-1f for auto size). */
    var height: Float = -1.0f

    /** Minimum allowed width (-1f for unconstrained). */
    var minWidth: Float = -1.0f

    /** Minimum allowed height (-1f for unconstrained). */
    var minHeight: Float = -1.0f

    /** Maximum allowed width (-1f for unconstrained). */
    var maxWidth: Float = -1.0f

    /** Maximum allowed height (-1f for unconstrained). */
    var maxHeight: Float = -1.0f

    // --- BOX MODEL (MARGIN & PADDING) ---

    /** Outward margin on the left side (pixels). */
    var marginL: Float = 0.0f
    /** Outward margin on the top side (pixels). */
    var marginT: Float = 0.0f
    /** Outward margin on the right side (pixels). */
    var marginR: Float = 0.0f
    /** Outward margin on the bottom side (pixels). */
    var marginB: Float = 0.0f

    // Semantic margin aliases
    var marginLeft: Float
        get() = marginL
        set(value) { marginL = value }
    var marginTop: Float
        get() = marginT
        set(value) { marginT = value }
    var marginRight: Float
        get() = marginR
        set(value) { marginR = value }
    var marginBottom: Float
        get() = marginB
        set(value) { marginB = value }

    fun margin(all: Float) = margin(all, all, all, all)

    fun margin(horizontal: Float = 0.0f, vertical: Float = 0.0f) =
        margin(horizontal, vertical, horizontal, vertical)

    fun margin(left: Float = 0.0f, top: Float = 0.0f, right: Float = 0.0f, bottom: Float = 0.0f) {
        marginL = left
        marginT = top
        marginR = right
        marginB = bottom
        invalidateLayout()
    }

    /** Inward padding on the left side (pixels). */
    var padL: Float = 0.0f
    /** Inward padding on the top side (pixels). */
    var padT: Float = 0.0f
    /** Inward padding on the right side (pixels). */
    var padR: Float = 0.0f
    /** Inward padding on the bottom side (pixels). */
    var padB: Float = 0.0f

    // Semantic padding aliases
    var paddingLeft: Float
        get() = padL
        set(value) { padL = value }
    var paddingTop: Float
        get() = padT
        set(value) { padT = value }
    var paddingRight: Float
        get() = padR
        set(value) { padR = value }
    var paddingBottom: Float
        get() = padB
        set(value) { padB = value }

    fun pad(all: Float) = pad(all, all, all, all)

    fun pad(horizontal: Float = 0.0f, vertical: Float = 0.0f) =
        pad(horizontal, vertical, horizontal, vertical)

    fun pad(left: Float = 0.0f, top: Float = 0.0f, right: Float = 0.0f, bottom: Float = 0.0f) {
        padL = left
        padT = top
        padR = right
        padB = bottom
        invalidateLayout()
    }

    // --- STATE & EVENT LISTENERS ---

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

    /** Generic user payload attached to this node. */
    var tag: Any? = null

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

    // --- LAYOUT & INTRINSIC MEASUREMENT ---

    /** Marks this node and its ancestors as dirty to trigger layout recalculation. */
    fun invalidateLayout() {
        isLayoutDirty = true
        parent?.invalidateLayout()
    }

    fun setBounds(x: Float, y: Float, width: Float, height: Float) = bounds.set(x, y, width, height)

    fun setPosition(x: Float, y: Float) {
        bounds.x = x
        bounds.y = y
    }

    fun setSize(width: Float, height: Float) {
        bounds.width = width
        bounds.height = height
    }

    /** Computes preferred width including inward padding. */
    open fun getPrefWidth(): Float {
        val base = if (width >= 0.0f) width else if (minWidth >= 0.0f) minWidth else 0.0f
        return base + padL + padR
    }

    /** Computes preferred height including inward padding factoring in optional [availableWidth] constraints. */
    open fun getPrefHeight(availableWidth: Float = -1.0f): Float {
        val base = if (height >= 0.0f) height else if (minHeight >= 0.0f) minHeight else 0.0f
        return base + padT + padB
    }

    /** Executes layout pass for this node and its children. */
    open fun layout() {
        isLayoutDirty = false
        for (i in 0 until children.size) {
            val child = children[i]
            if (child.visible) child.layout()
        }
    }

    // --- RENDERING & DRAW ---

    /** Renders this node and its children directly to [UIBatch]. */
    open fun draw() {
        if (!visible) return

        val shouldClip = clip && bounds.width > 0.0f && bounds.height > 0.0f
        if (shouldClip) {
            UIBatch.pushClip(bounds.x, bounds.y, bounds.width, bounds.height)
        }

        drawSelf()
        drawChildren()

        if (shouldClip) {
            UIBatch.popClip()
        }
    }

    /** Renders the visual representation of this node. */
    protected open fun drawSelf() {}

    /** Renders all visible children in order. */
    protected open fun drawChildren() {
        for (i in 0 until children.size) {
            val child = children[i]
            if (child.visible) child.draw()
        }
    }

    // --- HIT TESTING & FOCUS ---

    /** Hit testing behavior mode. Default: [HitTestBehavior.TRANSLUCENT]. */
    var hitTestBehavior: HitTestBehavior = HitTestBehavior.TRANSLUCENT

    /** Performs hit testing for screen coordinate ([pointX], [pointY]). */
    open fun hitTest(pointX: Float, pointY: Float): UINode? {
        if (!visible || !touchable || hitTestBehavior == HitTestBehavior.NONE) return null

        if (!bounds.contains(pointX, pointY)) return null

        for (i in children.indices.reversed()) {
            val child = children[i]
            val hit = child.hitTest(pointX, pointY)
            if (hit != null) return hit
        }

        return when (hitTestBehavior) {
            HitTestBehavior.OPAQUE -> this
            HitTestBehavior.TRANSLUCENT -> null
            HitTestBehavior.NONE -> null
        }
    }

    // --- TREE MANIPULATION ---

    fun addChild(child: UINode) = addChildAt(children.size, child)

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
        for (i in 0 until children.size) {
            val child = children[i]
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

    open fun onAttached() {}
    open fun onDetached() {}

    fun requestFocus() {
        if (!isFocusable || isFocused) return

        isFocused = true
    }

    fun clearFocus() {
        if (!isFocused) return

        isFocused = false
    }

    // --- COORDINATE TRANSFORMATIONS ---

    fun localToGlobal(localX: Float, localY: Float): Vec2 = Vec2(bounds.x + localX, bounds.y + localY)
    fun globalToLocal(globalX: Float, globalY: Float): Vec2 = Vec2(globalX - bounds.x, globalY - bounds.y)
}

/**
 * ## HitTestBehavior
 *
 * Defines how pointer hit testing evaluates this node and its children.
 *
 * See: docs/ui-engine/ui_engine_en.md
 */
enum class HitTestBehavior {
    /**
     * Consumes pointer hit testing within its bounding box.
     * Prevents underlying layers/gameplay from receiving pointer events.
     * Default for interactive controls (Button, Card, Slider, TextField, Modal).
     */
    OPAQUE,

    /**
     * Only intercepts pointer events if one of its children is hit.
     * Transparent/empty space within its bounds passes through to underlying gameplay.
     * Default for layout containers (Box, Column, Row, CanvasNode).
     */
    TRANSLUCENT,

    /**
     * Completely transparent to hit testing (never intercepts).
     * Used for inert spacers or decorative overlays.
     */
    NONE
}
