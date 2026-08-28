package org.mdt.core.ui.render

import arc.graphics.g2d.Font
import arc.graphics.g2d.GlyphLayout
import arc.util.Align
import org.mdt.core.ui.unit.Color

/**
 * ## UIFontDrawer
 *
 * Direct, high-performance BMFont glyph emitter for NekoMod.
 * Reuses Java's [GlyphLayout] for text measurement, word-wrapping, and color tag parsing,
 * while emitting glyph quads directly into [UIBatch] without invoking Arc's `FontCache` or `Draw.batch`.
 *
 * See: docs/rendering-shaders/rendering_shaders_en.md
 */
object UIFontDrawer {

    private val layoutHelper = GlyphLayout()
    private val arcColorHelper = arc.graphics.Color()

    // --- INTRINSIC MEASUREMENT ---

    /**
     * Measures the preferred width of [text] with the given [font].
     */
    fun getPrefWidth(
        font: Font,
        text: CharSequence,
        targetWidth: Float = 0.0f,
        wrap: Boolean = false
    ): Float {
        if (text.isEmpty()) return 0.0f

        layoutHelper.setText(font, text, arc.graphics.Color.white, targetWidth, Align.left, wrap)
        return layoutHelper.width
    }

    /**
     * Measures the preferred height of [text] with the given [font].
     */
    fun getPrefHeight(
        font: Font,
        text: CharSequence,
        targetWidth: Float = 0.0f,
        align: Int = Align.left,
        wrap: Boolean = false
    ): Float {
        if (text.isEmpty()) return 0.0f

        layoutHelper.setText(font, text, arc.graphics.Color.white, targetWidth, align, wrap)
        return maxOf(font.lineHeight, layoutHelper.height)
    }

    // --- RENDERING ---

    /**
     * Draws [text] directly into [UIBatch].
     */
    fun draw(
        font: Font,
        text: CharSequence,
        x: Float,
        y: Float,
        targetWidth: Float = 0.0f,
        align: Int = Align.left,
        wrap: Boolean = false,
        color: Color = Color.White
    ) {
        if (text.isEmpty()) return

        layoutHelper.setText(font, text, color.toArcColor(arcColorHelper), targetWidth, align, wrap)

        val scaleX = font.data.scaleX
        val scaleY = font.data.scaleY

        for (run in layoutHelper.runs) {
            val glyphs = run.glyphs
            val xAdvances = run.xAdvances
            var currentX = x + run.x
            val currentY = y + run.y
            val runColor = Color.fromArc(run.color)

            for (i in 0 until glyphs.size) {
                val glyph = glyphs[i]
                currentX += xAdvances[i]

                val drawX = currentX + glyph.xoffset * scaleX
                val drawY = currentY + glyph.yoffset * scaleY
                val glyphWidth = glyph.width * scaleX
                val glyphHeight = glyph.height * scaleY
                val fontRegion = if (glyph.page < font.regions.size) font.regions.get(glyph.page) else font.regions.first()
                val fontTexture = fontRegion.texture

                UIBatch.drawGlyph(
                    x = drawX,
                    y = drawY,
                    width = glyphWidth,
                    height = glyphHeight,
                    uvMinU = glyph.u,
                    uvMinV = glyph.v,
                    uvMaxU = glyph.u2,
                    uvMaxV = glyph.v2,
                    color = runColor,
                    fontTexture = fontTexture
                )
            }
        }
    }
}
