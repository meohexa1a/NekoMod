// [AGENT INVARIANT] Synchronously update @property, @param, and @see KDocs when modifying this file.

package org.mdt.core.platform.render

import arc.graphics.g2d.Font
import arc.graphics.g2d.GlyphLayout
import arc.util.Align

/**
 * ## FontMeasurer
 *
 * Measures bitmap font (BMFont) text dimensions, calculates multi-line wrapping bounds,
 * and performs binary-search text truncation with ellipsis.
 *
 * @see UIBatch
 * @see org.mdt.core.ui.node.TextNode
 * @see org.mdt.core.ui.node.InputNode
 */
class FontMeasurer {

    private val layoutHelper = GlyphLayout()

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
}