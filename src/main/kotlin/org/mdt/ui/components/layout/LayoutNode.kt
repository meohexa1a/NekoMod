package org.mdt.ui.components.layout

import org.mdt.ui.core.UINode
import org.mdt.ui.layout.policy.BoxMeasurePolicy
import org.mdt.ui.layout.policy.MeasurePolicy
import org.mdt.ui.render.BoxRenderer
import org.mdt.ui.render.EngineRenderer

/**
 * ## LayoutNode
 *
 * Unified, high-performance Virtual DOM container node inspired by Jetpack Compose's `LayoutNode`.
 * Replaces heavy inheritance hierarchies with pure composition:
 * - Measurement & layout behavior is delegated to [MeasurePolicy].
 * - Visual styling ([BoxVisuals]) is completely lazy/nullable (zero memory & GPU overhead for layout-only nodes).
 *
 * See: docs/layout-engine/layout_engine_en.md
 * See: docs/architecture/architecture_en.md
 */
open class LayoutNode : UINode() {

    /** Measurement and positioning strategy. */
    var measurePolicy: MeasurePolicy = BoxMeasurePolicy
        set(value) {
            if (field != value) {
                field = value
                invalidateLayout()
            }
        }

    /** Visual styling configuration (allocated on-demand only when visual styling is attached). */
    var visuals: BoxVisuals? = null
        protected set

    /**
     * Lazily creates and returns the [BoxVisuals] styling configuration for this node.
     */
    fun ensureVisuals(): BoxVisuals {
        var current = visuals
        if (current == null) {
            current = BoxVisuals()
            visuals = current
        }
        return current
    }

    override fun getPrefWidth(): Float {
        if (width >= 0f) return width
        val contentW = measurePolicy.measureWidth(this)
        val baseW = if (minWidth >= 0f) maxOf(contentW, minWidth) else contentW
        return baseW + padL + padR
    }

    override fun getPrefHeight(): Float {
        if (height >= 0f) return height
        val contentH = measurePolicy.measureHeight(this)
        val baseH = if (minHeight >= 0f) maxOf(contentH, minHeight) else contentH
        return baseH + padT + padB
    }

    override fun layout() {
        val w = if (bounds.width > 0f) bounds.width else getPrefWidth()
        val h = if (bounds.height > 0f) bounds.height else getPrefHeight()
        if (bounds.width != w || bounds.height != h) {
            setSize(w, h)
        }

        val availW = maxOf(0f, bounds.width - padL - padR)
        val availH = maxOf(0f, bounds.height - padT - padB)
        val innerX = bounds.x + padL
        val innerY = bounds.y + padB

        measurePolicy.layout(this, innerX, innerY, availW, availH)

        isLayoutDirty = false
        for (child in children) {
            if (child.visible) {
                child.layout()
            }
        }
    }

    override fun drawSelf(renderer: EngineRenderer) {
        val vis = visuals
        if (vis != null && vis.isVisible()) {
            BoxRenderer.draw(bounds.x, bounds.y, bounds.width, bounds.height, vis, renderer.blurProcessor)
        }
    }
}
