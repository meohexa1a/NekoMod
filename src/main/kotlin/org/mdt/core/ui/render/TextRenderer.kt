@file:Suppress("unused")

package org.mdt.core.ui.render

import arc.graphics.g2d.Font
import arc.graphics.g2d.GlyphLayout
import arc.util.Tmp
import mindustry.ui.Fonts
import org.mdt.core.ui.Rect
import org.mdt.ui.components.text.TextVisuals

class TextRenderer {
    private var prefSizeInvalid = true
    private var prefWidth = 0f
    private var prefHeight = 0f

    private fun font(): Font = Fonts.def

    fun invalidate() {
        prefSizeInvalid = true
    }

    fun getPrefWidth(text: String, visuals: TextVisuals, containerWidth: Float): Float {
        if (prefSizeInvalid) scaleAndComputePrefSize(text, visuals, containerWidth)
        return prefWidth
    }

    fun getPrefHeight(text: String, visuals: TextVisuals, containerWidth: Float): Float {
        if (prefSizeInvalid) scaleAndComputePrefSize(text, visuals, containerWidth)
        return prefHeight
    }

    private fun scaleAndComputePrefSize(text: String, visuals: TextVisuals, containerWidth: Float) {
        val f = font()
        val oldSX = f.scaleX
        val oldSY = f.scaleY
        val scaled = visuals.fontScaleX != 1.0f || visuals.fontScaleY != 1.0f
        if (scaled) f.data.setScale(visuals.fontScaleX, visuals.fontScaleY)

        prefSizeInvalid = false
        if (visuals.wrap && visuals.ellipsis == null) {
            val w = if (containerWidth <= 0) Float.MAX_VALUE else containerWidth
            prefSizeLayout.setText(f, text, visuals.color.toArcColor(Tmp.c1), w, visuals.lineAlign, true)
        } else {
            prefSizeLayout.setText(f, text, 0, text.length, visuals.color.toArcColor(Tmp.c1), 0f, visuals.lineAlign, visuals.wrap, visuals.ellipsis)
        }
        prefWidth = prefSizeLayout.width
        prefHeight = prefSizeLayout.height

        if (scaled) f.data.setScale(oldSX, oldSY)
    }

    fun draw(text: String, visuals: TextVisuals, bounds: Rect) {
        if (text.isEmpty()) return
        val f = font()
        val oldSX = f.scaleX
        val oldSY = f.scaleY
        val scaled = visuals.fontScaleX != 1.0f || visuals.fontScaleY != 1.0f
        if (scaled) f.data.setScale(visuals.fontScaleX, visuals.fontScaleY)

        f.color = visuals.color.toArcColor(Tmp.c1)

        val textH = getPrefHeight(text, visuals, bounds.width)
        // In BMFont bottom-up OpenGL, baseline is at bottom of box + vertical centering padding + capHeight + baselineOffset
        val baselineY = bounds.y + (bounds.height - textH) * 0.5f + f.data.capHeight + visuals.baselineOffset

        if (visuals.wrap && visuals.ellipsis == null) {
            f.draw(text, bounds.x, baselineY, bounds.width, visuals.labelAlign, true)
        } else {
            f.draw(text, bounds.x, baselineY, 0, text.length, bounds.width, visuals.labelAlign, false, visuals.ellipsis)
        }

        if (scaled) f.data.setScale(oldSX, oldSY)
    }

    companion object {
        private val prefSizeLayout = GlyphLayout()
    }
}
