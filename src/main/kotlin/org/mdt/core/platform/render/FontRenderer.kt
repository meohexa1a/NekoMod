package org.mdt.core.platform.render

import arc.graphics.g2d.Font
import arc.graphics.g2d.GlyphLayout
import arc.util.Align
import org.mdt.core.ui.unit.Color

/**
 * ## FontRenderer [BMFont Rasterizer & Measurement Engine]
 *
 * > 🤖 **AGENT DIRECTIVE:** When modifying this class or related logic, synchronously update this KDoc:
 * > 1) Keep Purpose accurate. 2) Update Key Rules & Checklist [x]/[ ]. 3) Maintain Related Files map.
 *
 * ### 1. Purpose
 * - Emits bitmap font (BMFont) glyph quads directly into [UIBatch] for 1-Draw-Call rendering.
 * - Measures text dimensions, performs multi-line text wrapping, and truncates text with ellipsis.
 *
 * ### 2. Key Rules & Checklist
 * - [x] Draw BMFonts at integer scale (natural `1.0f`), avoiding fractional scaling.
 * - [x] `getPrefHeight` clamps height to at least one `font.lineHeight`.
 * - [x] `truncateWithEllipsis` truncates string and appends `...` within target width.
 * - [x] `draw` maps glyph coordinates and texture UVs into [UIBatch.drawGlyph].
 *
 * ### 3. Related Files
 * - GPU Batcher: `src/main/kotlin/org/mdt/core/platform/render/UIBatch.kt`
 * - Text Node: `src/main/kotlin/org/mdt/core/ui/node/TextNode.kt`
 * - Composable Text: `src/main/kotlin/org/mdt/ui/components/text/Text.kt`
 */
object FontRenderer {

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
     * Draws [text] directly into [UIBatch] with optional [ellipsis] truncation or [wrap].
     */
    fun draw(
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
