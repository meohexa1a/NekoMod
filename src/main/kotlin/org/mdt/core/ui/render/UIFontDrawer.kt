package org.mdt.core.ui.render

import arc.graphics.g2d.Font
import arc.graphics.g2d.GlyphLayout
import arc.util.Align
import org.mdt.core.ui.unit.Color

/**
 * ## UIFontDrawer [Direct BMFont Glyph Emitter & Measurement Helper]
 *
 * ### 1. 📖 Feature Specification & Core Architecture:
 * - Direct, high-performance BMFont glyph emitter for NekoMod.
 * - Reuses static [GlyphLayout] for text bounds measurement, word-wrapping, and color markup parsing.
 * - Emits individual glyph quads directly into [UIBatch] without invoking Arc's `FontCache` or `Draw.batch`.
 * - Supports binary-search single-line ellipsis truncation (`truncateWithEllipsis`) and multi-line wrapping.
 *
 * ### 2. ⚡ Invariants & Non-Negotiable Rules:
 * - **Rule 1 (Typography & Scale):** Draw BMFonts at natural scale ($1.0f$). Do not apply fractional float scale to bitmap fonts.
 * - **Rule 2 (1-Draw-Call Batching):** Pushes glyphs into active [UIBatch] pass with texture binding validation.
 *
 * ### 3. 🔗 Related Files & Subsystem Map:
 * - ⚡ **GPU Batcher:** `src/main/kotlin/org/mdt/core/ui/render/UIBatch.kt`
 * - 🌲 **Virtual Node:** `src/main/kotlin/org/mdt/core/ui/node/TextNode.kt`, `src/main/kotlin/org/mdt/core/ui/node/InputNode.kt`
 * - 🎨 **Composable Text:** `src/main/kotlin/org/mdt/ui/components/display/text/Text.kt`
 * - 🔌 **Platform Host:** `src/main/kotlin/org/mdt/core/engine/PlatformHost.kt`
 *
 * ### 4. ✅ Behavioral Verification Checklist:
 * - [x] `getPrefWidth` calculates string width without text wrapping when `wrap = false`.
 * - [x] `getPrefHeight` clamps height to at least `font.lineHeight`.
 * - [x] `truncateWithEllipsis` truncates string and appends `...` within target width.
 * - [x] `draw` maps runs and glyph offsets into [UIBatch.drawGlyph].
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

        val textToRender = if (ellipsis && !wrap && targetWidth > 0.0f) {
            truncateWithEllipsis(font, text.toString(), targetWidth)
        } else {
            text
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

            for ((i, element) in glyphs.withIndex()) {
                val glyph = element
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
