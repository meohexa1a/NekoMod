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

        val f = textVisuals.font
        val oldSX = f.scaleX
        val oldSY = f.scaleY
        val isScaled = textVisuals.fontScaleX != 1.0f || textVisuals.fontScaleY != 1.0f
        if (isScaled) f.data.setScale(textVisuals.fontScaleX, textVisuals.fontScaleY)

        layoutHelper.setText(f, text)
        val textW = layoutHelper.width

        if (isScaled) f.data.setScale(oldSX, oldSY)
        return (if (minWidth >= 0f) maxOf(textW, minWidth) else textW) + padL + padR
    }

    override fun getPrefHeight(): Float {
        if (height >= 0f) return height
        val f = textVisuals.font
        val oldSX = f.scaleX
        val oldSY = f.scaleY
        val isScaled = textVisuals.fontScaleX != 1.0f || textVisuals.fontScaleY != 1.0f
        if (isScaled) f.data.setScale(textVisuals.fontScaleX, textVisuals.fontScaleY)

        val targetW = if (bounds.width > 0f) bounds.width - padL - padR else (if (width > 0f) width - padL - padR else 0f)
        val textH = if (textVisuals.wrap && targetW > 0f) {
            layoutHelper.setText(f, text, textVisuals.color.toArcColor(Tmp.c1), targetW, textVisuals.align, true)
            layoutHelper.height
        } else {
            layoutHelper.setText(f, text)
            maxOf(f.lineHeight, layoutHelper.height)
        }

        if (isScaled) f.data.setScale(oldSX, oldSY)
        return (if (minHeight >= 0f) maxOf(textH, minHeight) else textH) + padT + padB
    }

    override fun drawSelf(renderer: EngineRenderer) {
        super.drawSelf(renderer)
        if (text.isEmpty()) return

        val innerX = bounds.x + padL
        val innerY = bounds.y + padB
        val innerW = bounds.width - padL - padR
        val innerH = bounds.height - padT - padB

        val f = textVisuals.font
        val oldSX = f.scaleX
        val oldSY = f.scaleY
        val isScaled = textVisuals.fontScaleX != 1.0f || textVisuals.fontScaleY != 1.0f
        if (isScaled) f.data.setScale(textVisuals.fontScaleX, textVisuals.fontScaleY)

        f.color = textVisuals.color.toArcColor(Tmp.c1)

        val capH = f.data.capHeight
        if (textVisuals.wrap && innerW > 0f) {
            layoutHelper.setText(f, text, textVisuals.color.toArcColor(Tmp.c1), innerW, textVisuals.align, true)
            val drawY = innerY + (innerH + layoutHelper.height) * 0.5f
            f.draw(text, innerX, drawY, innerW, textVisuals.align, true)
        } else {
            val drawY = innerY + (innerH + capH) * 0.5f
            if (innerW > 0f) {
                f.draw(text, innerX, drawY, 0, text.length, innerW, textVisuals.align, false, textVisuals.ellipsis)
            } else {
                f.draw(text, innerX, drawY)
            }
        }

        if (isScaled) f.data.setScale(oldSX, oldSY)
        Draw.color()
    }

    companion object {
        private val layoutHelper = GlyphLayout()
    }
}
