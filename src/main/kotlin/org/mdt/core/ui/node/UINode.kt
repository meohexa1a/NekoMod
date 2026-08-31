// [AGENT ARCHITECTURE & INVARIANTS]
// - Domain Role: Virtual DOM Base Node for UI Hierarchy, Layout & Hit-Testing.
// - Operating Mechanism: Tree hierarchy traversal, parent/child lifecycle, input filtering, and coordinate transformations.
// - Invariants: Float coordinates with OpenGL bottom-left origin; zero-allocation hit tests.
// - Dependencies: [LayoutNode], [CanvasNode], [InputNode], [TextNode], [UIBatch], [EngineInputProcessor].
// - Directive: Synchronously update @property, @param, and @see KDocs when modifying this file.

package org.mdt.core.ui.node

import org.mdt.core.platform.PlatformHost
import org.mdt.core.platform.render.UIBatch
import org.mdt.core.ui.input.CursorIcon
import org.mdt.core.ui.input.EngineInputProcessor
import org.mdt.core.ui.input.Key
import org.mdt.core.ui.input.KeyEvent
import org.mdt.core.ui.input.PointerEvent
import org.mdt.core.ui.input.PointerEventPass
import org.mdt.core.ui.input.PointerInputFilter
import org.mdt.core.ui.input.ScrollEvent
import org.mdt.core.ui.modifier.UIModifier
import org.mdt.core.ui.unit.AnchorData
import org.mdt.core.ui.unit.Offset
import org.mdt.core.ui.unit.Rect

/**
 * ## UINode
 *
 * Base virtual DOM element managing hierarchy relationships ([parent], [children]), box bounds,
 * margins, paddings, pointer/keyboard event filters, and OpenGL coordinate transformations.
 *
 * @property id Unique query identifier in the Virtual DOM tree.
 * @property name Human-readable display label for debugging.
 * @property parent Parent node owning this node in the UI hierarchy.
 * @property children List of child nodes belonging to this node.
 * @property bounds Absolute screen boundary rectangle in OpenGL bottom-left coordinates.
 * @property width Fixed desired width in pixels (`-1.0f` for intrinsic content sizing).
 * @property height Fixed desired height in pixels (`-1.0f` for intrinsic content sizing).
 * @property minWidth Minimum width bound in pixels (`-1.0f` for unconstrained).
 * @property minHeight Minimum height bound in pixels (`-1.0f` for unconstrained).
 * @property maxWidth Maximum width bound in pixels (`-1.0f` for unconstrained).
 * @property maxHeight Maximum height bound in pixels (`-1.0f` for unconstrained).
 * @property visible Whether this node is visible and consumes layout space.
 * @property touchable Whether this node can receive pointer events during hit testing.
 * @property isFocusable Whether this node can accept keyboard focus.
 * @property isFocused Whether this node currently holds keyboard focus.
 * @property opacity Alpha transparency multiplier in range `0.0f..1.0f`.
 * @property zIndex Sorting index for drawing order.
 * @property clip Whether child rendering is scissor-clipped to this node's bounds.
 * @property isLayoutDirty Dirty flag indicating layout recalculation is required.
 * @property cursor Custom mouse cursor displayed when pointer hovers over this node.
 * @property hitTestBehavior Hit-testing transparency behavior mode ([HitTestBehavior]).
 * @property tag Generic user payload attached to this node.
 *
 * @see LayoutNode
 * @see InputNode
 * @see TextNode
 * @see CanvasNode
 */
open class UINode {

    // --- PLATFORM ACCESS ---

    private var cachedHost: PlatformHost? = null
    private var cachedInputProcessor: EngineInputProcessor? = null

    val host: PlatformHost
        get() {
            cachedHost?.let { return it }
            var current: UINode? = this
            while (current != null) {
                if (current is CanvasNode) {
                    val resolved = current.hostProvider()
                    cachedHost = resolved
                    return resolved
                }
                current = current.parent
            }
            return PlatformHost.NoOp
        }

    val activeInputProcessor: EngineInputProcessor?
        get() {
            cachedInputProcessor?.let { return it }
            var current: UINode? = this
            while (current != null) {
                if (current is CanvasNode) {
                    val processor = current.inputProcessor
                    cachedInputProcessor = processor
                    return processor
                }
                current = current.parent
            }
            return null
        }

    // --- IDENTITY & HIERARCHY ---

    /** Unique query identifier in the Virtual DOM tree. */
    var id: String = ""

    /** Human-readable display label (e.g. "HeroCard", "Action Row"). */
    var name: String = ""

    /** Parent node owning this node in the UI hierarchy. */
    var parent: UINode? = null
        internal set

    /** List of child nodes belonging to this node. */
    val children: ArrayList<UINode> = object : ArrayList<UINode>() {
        override fun add(element: UINode): Boolean {
            element.parent = this@UINode
            element.onAttached()
            return super.add(element)
        }

        override fun add(index: Int, element: UINode) {
            element.parent = this@UINode
            element.onAttached()
            super.add(index, element)
        }

        override fun addAll(elements: Collection<UINode>): Boolean {
            for (element in elements) {
                element.parent = this@UINode
                element.onAttached()
            }
            return super.addAll(elements)
        }

        override fun addAll(index: Int, elements: Collection<UINode>): Boolean {
            for (element in elements) {
                element.parent = this@UINode
                element.onAttached()
            }
            return super.addAll(index, elements)
        }

        override fun remove(element: UINode): Boolean {
            if (element.parent === this@UINode) {
                element.onDetached()
                element.parent = null
            }
            return super.remove(element)
        }

        override fun removeAt(index: Int): UINode {
            val removed = super.removeAt(index)
            if (removed.parent === this@UINode) {
                removed.onDetached()
                removed.parent = null
            }
            return removed
        }

        override fun clear() {
            for (child in this) {
                if (child.parent === this@UINode) {
                    child.onDetached()
                    child.parent = null
                }
            }
            super.clear()
        }
    }

    /**
     * Absolute screen boundary rectangle.
     * Uses OpenGL bottom-left origin: (x=0, y=0) at bottom-left screen corner.
     */
    val bounds = Rect()

    // --- SIZING & SIZE FLAGS ---

    /** Horizontal size flags for container slot allocation. Default: 0 (Hug content). */
    var sizeFlagsHorizontal: Int = 0
        set(value) {
            if (field != value) {
                field = value
                invalidateLayout()
            }
        }

    /** Vertical size flags for container slot allocation. Default: 0 (Hug content). */
    var sizeFlagsVertical: Int = 0
        set(value) {
            if (field != value) {
                field = value
                invalidateLayout()
            }
        }

    /** Weight ratio for distributing excess container space when [org.mdt.core.ui.unit.SizeFlags.EXPAND] is set. */
    var stretchRatio: Float = 1.0f
        set(value) {
            if (field != value) {
                field = value
                invalidateLayout()
            }
        }

    /** Anchor and offset data for absolute/relative screen anchoring. */
    val anchorData: AnchorData = AnchorData()

    /** Fixed desired width (-1f for auto size). */
    var width: Float = -1.0f
        set(value) {
            if (field != value) {
                field = value
                invalidateLayout()
            }
        }

    /** Fixed desired height (-1f for auto size). */
    var height: Float = -1.0f
        set(value) {
            if (field != value) {
                field = value
                invalidateLayout()
            }
        }

    /** Minimum allowed width (-1f for unconstrained). */
    var minWidth: Float = -1.0f
        set(value) {
            if (field != value) {
                field = value
                invalidateLayout()
            }
        }

    /** Minimum allowed height (-1f for unconstrained). */
    var minHeight: Float = -1.0f
        set(value) {
            if (field != value) {
                field = value
                invalidateLayout()
            }
        }

    /** Maximum allowed width (-1f for unconstrained). */
    var maxWidth: Float = -1.0f
        set(value) {
            if (field != value) {
                field = value
                invalidateLayout()
            }
        }

    /** Maximum allowed height (-1f for unconstrained). */
    var maxHeight: Float = -1.0f
        set(value) {
            if (field != value) {
                field = value
                invalidateLayout()
            }
        }

    // --- BOX MODEL (MARGIN & PADDING) ---

    /** Outward margin on the left side (pixels). */
    var marginL: Float = 0.0f
        set(value) {
            if (field != value) {
                field = value
                invalidateLayout()
            }
        }

    /** Outward margin on the top side (pixels). */
    var marginT: Float = 0.0f
        set(value) {
            if (field != value) {
                field = value
                invalidateLayout()
            }
        }

    /** Outward margin on the right side (pixels). */
    var marginR: Float = 0.0f
        set(value) {
            if (field != value) {
                field = value
                invalidateLayout()
            }
        }

    /** Outward margin on the bottom side (pixels). */
    var marginB: Float = 0.0f
        set(value) {
            if (field != value) {
                field = value
                invalidateLayout()
            }
        }

    // Semantic margin aliases
    var marginLeft: Float
        get() = marginL
        set(value) {
            marginL = value
        }
    var marginTop: Float
        get() = marginT
        set(value) {
            marginT = value
        }
    var marginRight: Float
        get() = marginR
        set(value) {
            marginR = value
        }
    var marginBottom: Float
        get() = marginB
        set(value) {
            marginB = value
        }

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
        set(value) {
            if (field != value) {
                field = value
                invalidateLayout()
            }
        }

    /** Inward padding on the top side (pixels). */
    var padT: Float = 0.0f
        set(value) {
            if (field != value) {
                field = value
                invalidateLayout()
            }
        }

    /** Inward padding on the right side (pixels). */
    var padR: Float = 0.0f
        set(value) {
            if (field != value) {
                field = value
                invalidateLayout()
            }
        }

    /** Inward padding on the bottom side (pixels). */
    var padB: Float = 0.0f
        set(value) {
            if (field != value) {
                field = value
                invalidateLayout()
            }
        }

    // Semantic padding aliases
    var paddingLeft: Float
        get() = padL
        set(value) {
            padL = value
        }
    var paddingTop: Float
        get() = padT
        set(value) {
            padT = value
        }
    var paddingRight: Float
        get() = padR
        set(value) {
            padR = value
        }
    var paddingBottom: Float
        get() = padB
        set(value) {
            padB = value
        }

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
        internal set(value) {
            if (field != value) {
                field = value
                onFocusChanged(value)
            }
        }

    /** Lifecycle callback triggered when keyboard focus state changes. */
    protected open fun onFocusChanged(focused: Boolean) {}

    /** Alpha rendering opacity in range 0.0f..1.0f. */
    var opacity: Float = 1.0f

    /** Explicit Z-index rendering order sorting value. */
    var zIndex: Float = 0.0f

    /** Whether child rendering should be clipped to this node's bounding box. */
    var clip: Boolean = false

    /** Dirty flag indicating layout recalculation is required. */
    var isLayoutDirty: Boolean = true

    /** Generic user payload attached to this node. */
    var tag: Any? = null

    /** Invoked on single left click. */
    var onClick: (() -> Unit)? = null

    /** Invoked on double click within a double-click threshold. */
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
    var cursor: CursorIcon? = null

    /** Invoked on scroll wheel action. */
    var onScroll: ((ScrollEvent) -> Unit)? = null

    /** Invoked when a character is typed while focused. Return true to consume. */
    var onKeyTyped: ((Char) -> Boolean)? = null

    /** Invoked when a key is pressed while focused. Return true to consume. */
    var onKeyDown: ((Key) -> Boolean)? = null

    /** Invoked when a key is released while focused. Return true to consume. */
    var onKeyUp: ((Key) -> Boolean)? = null

    /** Whether pointer is currently hovering over this node. */
    var isHovered: Boolean = false
        internal set(value) {
            if (field != value) {
                field = value
                onHover?.invoke(value)
            }
        }

    // --- INTRINSIC EVENT HANDLERS (OVERRIDDEN BY LEAF NODES LIKE INPUTNODE) ---

    open fun handlePointerDown(event: PointerEvent): Boolean = false
    open fun handlePointerDrag(event: PointerEvent): Boolean = false
    open fun handlePointerUp(event: PointerEvent): Boolean = false
    open fun handleKeyDown(key: Key): Boolean = false
    open fun handleKeyUp(key: Key): Boolean = false
    open fun handleKeyTyped(character: Char): Boolean = false

    // --- EVENT DISPATCH METHODS ---

    open fun dispatchPointerDown(event: PointerEvent): Boolean {
        onPointerDown?.invoke(event)
        val intrinsicHandled = handlePointerDown(event)
        return event.isConsumed || intrinsicHandled
    }

    open fun dispatchPointerDrag(event: PointerEvent): Boolean {
        onPointerDrag?.invoke(event)
        val intrinsicHandled = handlePointerDrag(event)
        return event.isConsumed || intrinsicHandled
    }

    open fun dispatchPointerUp(event: PointerEvent): Boolean {
        onPointerUp?.invoke(event)
        val intrinsicHandled = handlePointerUp(event)
        return event.isConsumed || intrinsicHandled
    }

    open fun dispatchKeyDown(key: Key): Boolean {
        if (onKeyDown?.invoke(key) == true) return true
        return handleKeyDown(key)
    }

    open fun dispatchKeyUp(key: Key): Boolean {
        if (onKeyUp?.invoke(key) == true) return true
        return handleKeyUp(key)
    }

    open fun dispatchKeyTyped(character: Char): Boolean {
        if (onKeyTyped?.invoke(character) == true) return true
        return handleKeyTyped(character)
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
        val baseWidth = when {
            width >= 0.0f -> width
            minWidth >= 0.0f -> minWidth
            else -> 0.0f
        }
        return baseWidth + padL + padR
    }

    /** Computes preferred height including inward padding factoring in optional [availableWidth] constraints. */
    open fun getPrefHeight(availableWidth: Float = -1.0f): Float {
        val baseHeight = when {
            height >= 0.0f -> height
            minHeight >= 0.0f -> minHeight
            else -> 0.0f
        }
        return baseHeight + padT + padB
    }

    /** Executes layout pass for this node and its children. */
    open fun layout() {
        isLayoutDirty = false
        for (i in children.indices) {
            val child = children[i]
            if (child.visible) {
                child.layout()
            }
        }
    }

    // --- MODIFIER PIPELINE & RESET ---

    /** Active modifier chain applied to this node. */
    var modifier: UIModifier = UIModifier
        set(value) {
            field = value
            resetModifiers()
            value.applyTo(this)
            invalidateLayout()
        }

    /**
     * Resets all visual styling, layout constraints, and event listener slots to their pristine defaults.
     * Invoked before applying a new modifier chain during Compose recomposition.
     */
    open fun resetModifiers() {
        sizeFlagsHorizontal = 0
        sizeFlagsVertical = 0
        stretchRatio = 1.0f
        width = -1.0f
        height = -1.0f
        minWidth = -1.0f
        minHeight = -1.0f
        maxWidth = -1.0f
        maxHeight = -1.0f
        anchorData.reset()

        marginL = 0.0f
        marginT = 0.0f
        marginR = 0.0f
        marginB = 0.0f
        padL = 0.0f
        padT = 0.0f
        padR = 0.0f
        padB = 0.0f

        visible = true
        opacity = 1.0f
        zIndex = 0.0f
        clip = false

        hitTestBehavior = HitTestBehavior.TRANSLUCENT
        cursor = null
        touchable = true
        isFocusable = false
        onClick = null
        onDoubleClick = null
        onHover = null
        onPointerDown = null
        onPointerUp = null
        onPointerDrag = null
        onPointerEnter = null
        onPointerExit = null
        onScroll = null
        onKeyDown = null
        pointerFilters.clear()
        tag = null
    }

    // --- RENDERING & DRAW ---

    /** Renders this node and its children directly to [batch]. */
    open fun draw(batch: UIBatch) {
        if (!visible) return

        val shouldClip = clip && bounds.width > 0.0f && bounds.height > 0.0f
        if (shouldClip) {
            batch.pushClip(bounds.x, bounds.y, bounds.width, bounds.height)
        }

        drawSelf(batch)
        drawChildren(batch)

        if (shouldClip) {
            batch.popClip()
        }
    }

    /** Renders the visual representation of this node. */
    protected open fun drawSelf(batch: UIBatch) {}

    /** Renders all visible children respecting [zIndex] ordering. */
    protected open fun drawChildren(batch: UIBatch) {
        val size = children.size
        if (size == 0) return

        var hasVaryingZ = false
        val firstZ = children[0].zIndex
        for (i in 1 until size) {
            if (children[i].zIndex != firstZ) {
                hasVaryingZ = true
                break
            }
        }

        if (!hasVaryingZ) {
            for (i in 0 until size) {
                val child = children[i]
                if (child.visible) child.draw(batch)
            }
            return
        }

        val sorted = children.sortedWith(compareBy { it.zIndex })
        for (i in sorted.indices) {
            val child = sorted[i]
            if (child.visible) child.draw(batch)
        }
    }

    /** Registered composable multi-listener pointer filters. */
    val pointerFilters: ArrayList<PointerInputFilter> = ArrayList(2)

    fun addPointerFilter(filter: PointerInputFilter) {
        if (!pointerFilters.contains(filter)) {
            pointerFilters.add(filter)
        }
    }

    /** Compatibility alias for [addPointerFilter]. */
    fun addPointerInputFilter(filter: PointerInputFilter) = addPointerFilter(filter)

    fun removePointerFilter(filter: PointerInputFilter) {
        pointerFilters.remove(filter)
    }

    fun clearPointerFilters() {
        pointerFilters.clear()
    }

    /**
     * Dispatches [event] through all registered [pointerFilters] and legacy callbacks during [pass].
     */
    open fun dispatchPointerEvent(event: PointerEvent, pass: PointerEventPass) {
        for (i in pointerFilters.indices) {
            if (event.isConsumed) break
            pointerFilters[i].onPointerEvent(event, pass, this)
        }
    }

    // --- HIT TESTING & FOCUS ---

    /** Hit testing behavior mode. Default: [HitTestBehavior.TRANSLUCENT]. */
    var hitTestBehavior: HitTestBehavior = HitTestBehavior.TRANSLUCENT

    /**
     * Traverses the Virtual DOM tree to assemble the active [path] of nodes covering ([pointX], [pointY]).
     * Returns true if this node or any of its descendants claimed the hit path.
     */
    open fun buildHitPath(pointX: Float, pointY: Float, path: ArrayList<UINode>): Boolean {
        if (!visible || !touchable || hitTestBehavior == HitTestBehavior.NONE) return false
        if (!bounds.contains(pointX, pointY)) return false

        path.add(this)

        val size = children.size
        var hasVaryingZ = false
        if (size > 1) {
            val firstZ = children[0].zIndex
            for (i in 1 until size) {
                if (children[i].zIndex != firstZ) {
                    hasVaryingZ = true
                    break
                }
            }
        }

        val handled = when {
            hasVaryingZ -> {
                val sorted = children.sortedWith(compareByDescending { it.zIndex })
                var childHandled = false
                for (i in sorted.indices) {
                    if (sorted[i].buildHitPath(pointX, pointY, path)) {
                        childHandled = true
                        break
                    }
                }
                childHandled
            }

            else -> {
                var childHandled = false
                for (i in children.indices.reversed()) {
                    if (children[i].buildHitPath(pointX, pointY, path)) {
                        childHandled = true
                        break
                    }
                }
                childHandled
            }
        }

        if (handled || isInteractiveOrOpaque()) {
            return true
        }

        path.removeAt(path.size - 1)
        return false
    }

    /** Performs hit testing for screen coordinate ([pointX], [pointY]). */
    open fun hitTest(pointX: Float, pointY: Float): UINode? {
        if (!visible || !touchable || hitTestBehavior == HitTestBehavior.NONE) return null
        if (!bounds.contains(pointX, pointY)) return null

        val size = children.size
        var hasVaryingZ = false
        if (size > 1) {
            val firstZ = children[0].zIndex
            for (i in 1 until size) {
                if (children[i].zIndex != firstZ) {
                    hasVaryingZ = true
                    break
                }
            }
        }

        if (hasVaryingZ) {
            val sorted = children.sortedWith(compareByDescending { it.zIndex })
            for (i in sorted.indices) {
                val hit = sorted[i].hitTest(pointX, pointY)
                if (hit != null) return hit
            }
        } else {
            for (i in children.indices.reversed()) {
                val hit = children[i].hitTest(pointX, pointY)
                if (hit != null) return hit
            }
        }

        return when {
            isInteractiveOrOpaque() -> this
            else -> null
        }
    }

    /** Returns true if this node absorbs pointer hit-testing or responds to pointer events. */
    open fun isInteractiveOrOpaque(): Boolean =
        hitTestBehavior == HitTestBehavior.OPAQUE ||
            pointerFilters.isNotEmpty() ||
            onClick != null ||
            onDoubleClick != null ||
            onPointerDown != null ||
            onPointerUp != null ||
            onPointerDrag != null ||
            onHover != null ||
            onPointerEnter != null ||
            onPointerExit != null ||
            onScroll != null ||
            isFocusable ||
            cursor != null

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
        for (i in children.indices) {
            val child = children[i]
            child.onDetached()
            child.parent = null
        }
        children.clear()
        invalidateLayout()
    }

    fun moveChild(from: Int, to: Int, count: Int = 1) {
        if (from == to || count <= 0 || from >= children.size) return

        val dest = when {
            to > from -> to - count
            else -> to
        }
        val safeDest = dest.coerceIn(0, children.size - count)
        val moved = ArrayList<UINode>(count)
        repeat(count) {
            moved.add(children.removeAt(from))
        }
        children.addAll(safeDest, moved)
        invalidateLayout()
    }

    open fun onAttached() {
        cachedHost = parent?.host
        cachedInputProcessor = parent?.activeInputProcessor
    }

    open fun onDetached() {
        if (activeInputProcessor?.focusedNode === this) {
            activeInputProcessor?.clearFocus()
        }
        cachedHost = null
        cachedInputProcessor = null
    }

    fun requestFocus() {
        if (!isFocusable || isFocused) return

        activeInputProcessor?.requestFocus(this)
    }

    fun clearFocus() {
        if (!isFocused) return

        activeInputProcessor?.clearFocus()
    }

    // --- COORDINATE TRANSFORMATIONS ---

    fun localToGlobal(localX: Float, localY: Float): Offset = Offset(bounds.x + localX, bounds.y + localY)
    fun globalToLocal(globalX: Float, globalY: Float): Offset = Offset(globalX - bounds.x, globalY - bounds.y)
}

/**
 * ## HitTestBehavior [Pointer Hit-Testing Strategy]
 *
 * Defines how pointer hit testing evaluates this node and its children during event dispatch passes.
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
