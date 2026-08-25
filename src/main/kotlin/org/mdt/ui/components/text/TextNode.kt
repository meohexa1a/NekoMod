package org.mdt.ui.components.text

import arc.graphics.Color
import arc.graphics.g2d.Draw
import arc.graphics.g2d.GlyphLayout
import mindustry.ui.Fonts
import org.mdt.ui.components.layout.LayoutNode
import org.mdt.ui.render.EngineRenderer

/**
 * ## TextNode
 *
 * Virtual DOM node rendering BMFont glyphs with caching and typography configurations.
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

    init {
        this.text = text
    }

    override fun getPrefWidth(): Float {
        if (width >= 0f) return width
        val f = Fonts.def
        val oldSX = f.scaleX
        val oldSY = f.scaleY
        f.data.setScale(textVisuals.fontScale, textVisuals.fontScale)

        layoutHelper.setText(f, text)
        val textW = layoutHelper.width

        f.data.setScale(oldSX, oldSY)
        return (if (minWidth >= 0f) maxOf(textW, minWidth) else textW) + padL + padR
    }

    override fun getPrefHeight(): Float {
        if (height >= 0f) return height
        val f = Fonts.def
        val oldSX = f.scaleX
        val oldSY = f.scaleY
        f.data.setScale(textVisuals.fontScale, textVisuals.fontScale)

        layoutHelper.setText(f, text)
        val textH = layoutHelper.height

        f.data.setScale(oldSX, oldSY)
        return (if (minHeight >= 0f) maxOf(textH, minHeight) else textH) + padT + padB
    }

    override fun drawSelf(renderer: EngineRenderer) {
        super.drawSelf(renderer)
        if (text.isEmpty()) return

        val innerX = bounds.x + padL
        val innerY = bounds.y + padB
        val innerH = bounds.height - padT - padB

        val f = Fonts.def
        val oldSX = f.scaleX
        val oldSY = f.scaleY
        f.data.setScale(textVisuals.fontScale, textVisuals.fontScale)

        f.color = textVisuals.color
        val capH = f.data.capHeight
        val drawY = innerY + (innerH + capH) * 0.5f

        f.draw(text, innerX, drawY)

        f.data.setScale(oldSX, oldSY)
        Draw.color(Color.white)
    }

    companion object {
        private val layoutHelper = GlyphLayout()
    }
}
