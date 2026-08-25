package org.mdt.ui.widgets

import org.mdt.ui.core.UINode
import org.mdt.ui.layout.GodotLayout
import org.mdt.ui.render.BoxRenderer
import org.mdt.ui.render.EngineRenderer

/**
 * ## BoxNode
 *
 * Multi-purpose rectangular node supporting SDF background drawing, borders, drop shadows,
 * backdrop Gaussian blur, and anchor/container layout.
 *
 * See: docs/architecture/architecture_en.md
 * See: docs/rendering-shaders/rendering_shaders_en.md
 */
open class BoxNode : UINode() {
    /** Visual styling configuration (Fill, Radius, Border, Shadow, Backdrop Blur). */
    val visuals = BoxVisuals()

    override fun getPrefWidth(): Float {
        if (width >= 0f) return width
        val visibleChildren = children.filter { it.visible }
        var maxChildW = 0f
        for (child in visibleChildren) {
            maxChildW = maxOf(maxChildW, GodotLayout.getChildMinWidth(child))
        }
        val contentW = if (minWidth >= 0f) maxOf(maxChildW, minWidth) else maxChildW
        return contentW + padL + padR
    }

    override fun getPrefHeight(): Float {
        if (height >= 0f) return height
        val visibleChildren = children.filter { it.visible }
        var maxChildH = 0f
        for (child in visibleChildren) {
            maxChildH = maxOf(maxChildH, GodotLayout.getChildMinHeight(child))
        }
        val contentH = if (minHeight >= 0f) maxOf(maxChildH, minHeight) else maxChildH
        return contentH + padT + padB
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

        for (child in children) {
            if (!child.visible) continue
            val anchor = child.anchorData
            if (anchor.anchorLeft != 0f || anchor.anchorTop != 0f || anchor.anchorRight != 0f || anchor.anchorBottom != 0f ||
                anchor.offsetLeft != 0f || anchor.offsetTop != 0f || anchor.offsetRight != 0f || anchor.offsetBottom != 0f) {
                GodotLayout.layoutSingleAnchor(child, innerX, innerY, availW, availH)
            } else {
                GodotLayout.fitChildInRect(child, innerX, innerY, availW, availH)
            }
        }

        isLayoutDirty = false
        for (child in children) {
            if (child.visible) {
                child.layout()
            }
        }
    }

    override fun drawSelf(renderer: EngineRenderer) {
        if (visuals.opacity > 0.0f) {
            BoxRenderer.draw(bounds.x, bounds.y, bounds.width, bounds.height, visuals, renderer.blurProcessor)
        }
    }
}
