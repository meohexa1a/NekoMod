package org.mdt.core.ui.node

import arc.graphics.g2d.Font
import arc.util.Align
import org.mdt.core.ui.EngineRuntime
import org.mdt.core.ui.render.UIFontDrawer
import org.mdt.core.ui.unit.Color

/**
 * ## TextNode
 *
 * Core primitive Virtual DOM node rendering BMFont text glyphs directly through [UIFontDrawer] and [org.mdt.core.ui.render.UIBatch].
 * Unstyled by default.
 *
 * See: docs/design-system/design_system_en.md
 */
open class TextNode(
    text: String = ""
) : LayoutNode() {

    init {
        hitTestBehavior = HitTestBehavior.TRANSLUCENT
    }

    var text: String = text
        set(value) {
            if (field != value) {
                field = value
                invalidateLayout()
            }
        }

    var font: Font? = null
        set(value) {
            if (field != value) {
                field = value
                invalidateLayout()
            }
        }

    val activeFont: Font? get() = font ?: EngineRuntime.host.resolveDefaultFont()

    var textColor: Color = Color.White
    var align: Int = Align.left
    var wrap: Boolean = false
        set(value) {
            if (field != value) {
                field = value
                invalidateLayout()
            }
        }

    override fun getPrefWidth(): Float {
        if (width >= 0.0f) return width
        if (wrap) return (if (minWidth >= 0.0f) minWidth else 0.0f) + padL + padR

        val currentFont = activeFont ?: return padL + padR
        val measuredWidth = UIFontDrawer.getPrefWidth(currentFont, text, 0.0f, false)
        val baseWidth = if (minWidth >= 0.0f) maxOf(measuredWidth, minWidth) else measuredWidth
        return baseWidth + padL + padR
    }

    override fun getPrefHeight(availableWidth: Float): Float {
        if (height >= 0.0f) return height

        val currentFont = activeFont ?: return padT + padB
        val targetWidth = when {
            availableWidth >= 0.0f -> availableWidth - padL - padR
            bounds.width > 0.0f -> bounds.width - padL - padR
            width > 0.0f -> width - padL - padR
            parent != null && parent!!.bounds.width > 0.0f -> parent!!.bounds.width - parent!!.padL - parent!!.padR - padL - padR
            else -> 0.0f
        }
        val measuredHeight = UIFontDrawer.getPrefHeight(currentFont, text, maxOf(0.0f, targetWidth), align, wrap)
        val baseHeight = if (minHeight >= 0.0f) maxOf(measuredHeight, minHeight) else measuredHeight
        return baseHeight + padT + padB
    }

    override fun drawSelf() {
        super.drawSelf()
        if (text.isEmpty()) return
        val currentFont = activeFont ?: return

        val innerX = bounds.x + padL
        val innerY = bounds.y + padB
        val innerWidth = maxOf(0.0f, bounds.width - padL - padR)
        val innerHeight = maxOf(0.0f, bounds.height - padT - padB)

        val capHeight = currentFont.data.capHeight
        val drawY = if (wrap) {
            innerY + innerHeight - currentFont.data.ascent
        } else {
            innerY + (innerHeight + capHeight) * 0.5f
        }

        UIFontDrawer.draw(
            font = currentFont,
            text = text,
            x = innerX,
            y = drawY,
            targetWidth = innerWidth,
            align = align,
            wrap = wrap,
            color = textColor
        )
    }
}
