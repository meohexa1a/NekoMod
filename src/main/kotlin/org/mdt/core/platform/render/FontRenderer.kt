// [AGENT ARCHITECTURE & INVARIANTS]
// - Domain Role: BMFont Glyph Rasterization & Layout Pipeline.
// - Operating Mechanism: Batches individual glyph runs into [UIBatch]; zero-allocation measurement via reused [GlyphLayout].
// - Invariants: Draw BMFonts at integer scale (1.0f); baseline math uses OpenGL bottom-left coordinates.
// - Dependencies: [UIBatch], [TextNode], [InputNode], [Color].
// - Directive: Synchronously update @property, @param, and @see KDocs when modifying this file.

package org.mdt.core.platform.render

import arc.graphics.g2d.Font
import arc.graphics.g2d.GlyphLayout
import arc.util.Align

/**
 * ## FontRenderer
 *
 * Emits bitmap font (BMFont) glyph quads directly into [UIBatch] for 1-Draw-Call GPU batch rendering.
 * Measures text dimensions, performs multi-line text wrapping, and truncates text with ellipsis.
 *
 * @see UIBatch
 * @see org.mdt.core.ui.node.TextNode
 * @see org.mdt.ui.components.text.Text
 */
class FontRenderer {

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

    /**
     * Truncates [text] to fit within [availableWidth] by appending an ellipsis (`...`).
     */
    fun truncateWithEllipsis(
        font: Font,
        text: String,
        availableWidth: Float
    ): String {
        if (text.isEmpty() || availableWidth <= 0.0f) return ""

        val totalWidth = getPrefWidth(font, text, 0.0f, false)
        if (totalWidth <= availableWidth) return text

        val ellipsis = "..."
        val ellipsisWidth = getPrefWidth(font, ellipsis, 0.0f, false)
        if (ellipsisWidth >= availableWidth) return ellipsis

        val maxTextWidth = availableWidth - ellipsisWidth
        var low = 0
        var high = text.length
        var bestFit = 0

        while (low <= high) {
            val mid = (low + high) ushr 1
            val sub = text.substring(0, mid)
            val subWidth = getPrefWidth(font, sub, 0.0f, false)
            if (subWidth <= maxTextWidth) {
                bestFit = mid
                low = mid + 1
            } else {
                high = mid - 1
            }
        }

        return text.substring(0, bestFit) + ellipsis
    }

    // --- RENDERING ---

    /**
     * Draws [text] directly into [batch] with optional [ellipsis] truncation or [wrap].
     */
    fun draw(
        batch: UIBatch,
        font: Font,
        text: CharSequence,
        x: Float,
        y: Float,
        targetWidth: Float = 0.0f,
        align: Int = Align.left,
        wrap: Boolean = false,
        ellipsis: Boolean = false,
        color: Color = Color.White
    ) {
        if (text.isEmpty()) return

        val textToRender = when {
            ellipsis && !wrap && targetWidth > 0.0f -> truncateWithEllipsis(font, text.toString(), targetWidth)
            else -> text
        }

        layoutHelper.setText(font, textToRender, color.toArcColor(arcColorHelper), targetWidth, align, wrap)

        val scaleX = font.data.scaleX
        val scaleY = font.data.scaleY

        for (run in layoutHelper.runs) {
            val glyphs = run.glyphs
            val xAdvances = run.xAdvances
            var currentX = x + run.x
            val currentY = y + run.y
            val runColor = Color.fromArc(run.color)
            val glyphCount = glyphs.size

            for (i in 0 until glyphCount) {
                val glyph = glyphs.get(i)
                currentX += xAdvances.get(i)

                val drawX = currentX + glyph.xoffset * scaleX
                val drawY = currentY + glyph.yoffset * scaleY
                val glyphWidth = glyph.width * scaleX
                val glyphHeight = glyph.height * scaleY
                val fontRegion = when {
                    glyph.page < font.regions.size -> font.regions.get(glyph.page)
                    else -> font.regions.first()
                }
                val fontTexture = fontRegion.texture

                batch.drawGlyph(
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
