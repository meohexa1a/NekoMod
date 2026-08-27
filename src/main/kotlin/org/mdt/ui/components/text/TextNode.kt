package org.mdt.ui.components.text

import arc.graphics.g2d.Draw
import arc.graphics.g2d.GlyphLayout
import arc.util.Tmp
import org.mdt.core.ui.render.EngineRenderer
import org.mdt.ui.components.layout.LayoutNode

/**
 * ## TextNode
 *
 * Virtual DOM node rendering BMFont glyphs with typography configurations,
 * multi-line wrapping, and font selection. Uses Anuke's pooled [Tmp.c1] for zero-allocation rendering.
 *
 * See: docs/architecture/architecture_en.md
 */
open class TextNode(
    text: String = ""
) : LayoutNode() {

    val textVisuals = TextVisuals()

    var text: String
        get() = textVisuals.text
        set(value) {
            if (textVisuals.text != value) {
                textVisuals.text = value
                invalidateLayout()
            }
        }

    var ellipsis: String?
        get() = textVisuals.ellipsis
        set(value) {
            if (textVisuals.ellipsis != value) {
                textVisuals.ellipsis = value
                invalidateLayout()
            }
        }

    init {
        this.text = text
    }

    override fun getPrefWidth(): Float {
        if (width >= 0f) return width

        if (textVisuals.wrap) {
            // When wrapping is enabled, return 0 or minimum width to avoid expanding container beyond bounds
            return (if (minWidth >= 0f) minWidth else 0f) + padL + padR
        }

        val font = textVisuals.font
        val oldScaleX = font.scaleX
        val oldScaleY = font.scaleY
        val isScaled = textVisuals.fontScaleX != 1.0f || textVisuals.fontScaleY != 1.0f
        if (isScaled) font.data.setScale(textVisuals.fontScaleX, textVisuals.fontScaleY)

        layoutHelper.setText(font, text)
        val textWidth = layoutHelper.width

        if (isScaled) font.data.setScale(oldScaleX, oldScaleY)
        return (if (minWidth >= 0f) maxOf(textWidth, minWidth) else textWidth) + padL + padR
    }

    override fun getPrefHeight(): Float {
        if (height >= 0f) return height

        val font = textVisuals.font
        val oldScaleX = font.scaleX
        val oldScaleY = font.scaleY
        val isScaled = textVisuals.fontScaleX != 1.0f || textVisuals.fontScaleY != 1.0f
        if (isScaled) font.data.setScale(textVisuals.fontScaleX, textVisuals.fontScaleY)

        val targetWidth = if (bounds.width > 0f) {
            bounds.width - padL - padR
        } else if (width > 0f) {
            width - padL - padR
        } else {
            // Walk up parent hierarchy to find first ancestor with defined width for multi-line measurement
            var curParent = parent
            var foundWidth = 0f
            while (curParent != null) {
                val parentBoundWidth = if (curParent.bounds.width > 0f) curParent.bounds.width else curParent.width
                if (parentBoundWidth > 0f) {
                    foundWidth = maxOf(0f, parentBoundWidth - curParent.padL - curParent.padR)
                    break
                }
                curParent = curParent.parent
            }
            if (foundWidth > 0f) foundWidth - padL - padR else 0f
        }

        val textHeight = if (textVisuals.wrap && targetWidth > 0f) {
            layoutHelper.setText(font, text, textVisuals.color.toArcColor(Tmp.c1), targetWidth, textVisuals.align, true)
            layoutHelper.height
        } else {
            layoutHelper.setText(font, text)
            maxOf(font.lineHeight, layoutHelper.height)
        }

        if (isScaled) font.data.setScale(oldScaleX, oldScaleY)
        return (if (minHeight >= 0f) maxOf(textHeight, minHeight) else textHeight) + padT + padB
    }

    override fun drawSelf(renderer: EngineRenderer) {
        super.drawSelf(renderer)
        if (text.isEmpty()) return

        val innerX = bounds.x + padL
        val innerY = bounds.y + padB
        val innerWidth = bounds.width - padL - padR
        val innerHeight = bounds.height - padT - padB

        val font = textVisuals.font
        val oldScaleX = font.scaleX
        val oldScaleY = font.scaleY
        val isScaled = textVisuals.fontScaleX != 1.0f || textVisuals.fontScaleY != 1.0f
        if (isScaled) font.data.setScale(textVisuals.fontScaleX, textVisuals.fontScaleY)

        font.color = textVisuals.color.toArcColor(Tmp.c1)

        val capHeight = font.data.capHeight
        if (textVisuals.wrap && innerWidth > 0f) {
            layoutHelper.setText(font, text, textVisuals.color.toArcColor(Tmp.c1), innerWidth, textVisuals.align, true)
            val drawY = innerY + (innerHeight + layoutHelper.height) * 0.5f
            font.draw(text, innerX, drawY, innerWidth, textVisuals.align, true)
        } else {
            val drawY = innerY + (innerHeight + capHeight) * 0.5f
            if (innerWidth > 0f) {
                font.draw(text, innerX, drawY, 0, text.length, innerWidth, textVisuals.align, false, textVisuals.ellipsis)
            } else {
                font.draw(text, innerX, drawY)
            }
        }

        if (isScaled) font.data.setScale(oldScaleX, oldScaleY)
        Draw.color()
    }

    companion object {
        private val layoutHelper = GlyphLayout()
    }
}
