package org.mdt.core.ui.node

import arc.graphics.g2d.TextureRegion
import org.mdt.core.ui.layout.BoxMeasurePolicy
import org.mdt.core.ui.layout.MeasurePolicy
import org.mdt.core.ui.render.UIBatch
import org.mdt.core.ui.unit.Color

/**
 * ## LayoutNode
 *
 * Core primitive container Virtual DOM node.
 * Unifies flex/box layout delegation with direct [UIBatch.drawBox] GPU rendering.
 * Unstyled by default (zero default background, border, or corner radius).
 *
 * See: docs/layout-engine/layout_engine_en.md
 */
open class LayoutNode : UINode() {

    // --- PROPERTIES & VISUAL STYLING (Unstyled by default) ---

    var measurePolicy: MeasurePolicy = BoxMeasurePolicy
        set(value) {
            if (field != value) {
                field = value
                invalidateLayout()
            }
        }

    // Direct visual styling fields (Set strictly via UIModifiers or explicit composable properties)
    var radius: Float = 0.0f
    var color: Color = Color.Clear
    var borderWidth: Float = 0.0f
    var borderColor: Color = Color.Clear
    var isGlass: Boolean = false
    var region: TextureRegion? = null

    // --- SIZING & INTRINSIC MEASUREMENT ---

    override fun getPrefWidth(): Float {
        if (width >= 0.0f) return width

        val measuredContentWidth = measurePolicy.measureWidth(this)
        val baseWidth = if (minWidth >= 0.0f) maxOf(measuredContentWidth, minWidth) else measuredContentWidth
        return baseWidth + padL + padR
    }

    override fun getPrefHeight(availableWidth: Float): Float {
        if (height >= 0.0f) return height

        val availableInnerWidth = when {
            availableWidth >= 0.0f -> maxOf(0.0f, availableWidth - padL - padR)
            width >= 0.0f -> maxOf(0.0f, width - padL - padR)
            bounds.width > 0.0f -> maxOf(0.0f, bounds.width - padL - padR)
            else -> -1.0f
        }

        val measuredContentHeight = measurePolicy.measureHeight(this, availableInnerWidth)
        val baseHeight = if (minHeight >= 0.0f) maxOf(measuredContentHeight, minHeight) else measuredContentHeight
        return baseHeight + padT + padB
    }

    // --- LAYOUT ENGINE ---

    override fun layout() {
        val computedWidth = if (bounds.width > 0.0f) bounds.width else getPrefWidth()
        val computedHeight = if (bounds.height > 0.0f) bounds.height else getPrefHeight()

        if (bounds.width != computedWidth || bounds.height != computedHeight) {
            setSize(computedWidth, computedHeight)
        }

        val availableWidth = maxOf(0.0f, bounds.width - padL - padR)
        val availableHeight = maxOf(0.0f, bounds.height - padT - padB)
        val innerX = bounds.x + padL
        val innerY = bounds.y + padB

        measurePolicy.layout(this, innerX, innerY, availableWidth, availableHeight)

        isLayoutDirty = false
        for (i in 0 until children.size) {
            val child = children[i]
            if (child.visible) child.layout()
        }
    }

    // --- RENDERING ---

    override fun drawSelf() {
        if (color.alpha > 0.001f || borderWidth > 0.001f || region != null || isGlass) {
            UIBatch.drawBox(
                x = bounds.x,
                y = bounds.y,
                width = bounds.width,
                height = bounds.height,
                region = region,
                radius = radius,
                color = color,
                borderWidth = borderWidth,
                borderColor = borderColor,
                isGlass = isGlass
            )
        }
    }
}
