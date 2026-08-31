// [AGENT ARCHITECTURE & INVARIANTS]
// - Domain Role: BMFont Text Virtual DOM Node.
// - Operating Mechanism: Calculates baseline layout, wraps lines within available width, and renders glyphs via [UIBatch.drawText].
// - Invariants: Draw BMFonts at integer scale (1.0f); text mutations trigger `invalidateLayout()`.
// - Dependencies: [FontMeasurer], [UIBatch], [LayoutNode].
// - Directive: Synchronously update @property, @param, and @see KDocs when modifying this file.

package org.mdt.core.ui.node

import arc.graphics.g2d.Font
import arc.util.Align
import org.mdt.core.platform.PlatformHost
import org.mdt.core.platform.render.FontMeasurer
import org.mdt.core.platform.render.UIBatch
import org.mdt.core.platform.unit.Color

/**
 * ## TextNode
 *
 * Virtual DOM node that renders bitmap fonts (BMFont) using [org.mdt.core.platform.render.UIBatch].
 * Supports text wrapping, ellipsis truncation, alignment, and intrinsic text size measurements via [FontMeasurer].
 *
 * @param text Initial text string content.
 *
 * @property text Current text string rendered by this node.
 * @property textColor Text rendering color.
 * @property font BMFont instance used for measurement and drawing.
 * @property wrap Whether text should automatically wrap to multiple lines within available width.
 * @property ellipsis Whether overflowing text should be truncated with an ellipsis (`...`).
 * @property align Text alignment flag inside line bounds (e.g., `arc.util.Align.left`, `arc.util.Align.center`).
 *
 * @see LayoutNode
 * @see FontMeasurer
 * @see org.mdt.ui.components.text.Text
 */
open class TextNode(
    text: String = "",
    private val hostProvider: () -> PlatformHost = { PlatformHost.NoOp }
) : LayoutNode() {

    val fontMeasurer: FontMeasurer get() = hostProvider().render.fontMeasurer

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

    val activeFont: Font? get() = font ?: host.assets.resolveDefaultFont()

    var textColor: Color = Color.White
    var align: Int = Align.left
    var wrap: Boolean = false
        set(value) {
            if (field != value) {
                field = value
                invalidateLayout()
            }
        }
    var ellipsis: Boolean = false
        set(value) {
            if (field != value) {
                field = value
                invalidateLayout()
            }
        }

    override fun getPrefWidth(): Float {
        if (width >= 0.0f) return width
        if (wrap) return maxOf(0.0f, minWidth) + padL + padR

        val currentFont = activeFont ?: return padL + padR
        val measuredWidth = fontMeasurer.getPrefWidth(currentFont, text, 0.0f, false)
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
        val measuredHeight = fontMeasurer.getPrefHeight(currentFont, text, maxOf(0.0f, targetWidth), align, wrap)
        val baseHeight = if (minHeight >= 0.0f) maxOf(measuredHeight, minHeight) else measuredHeight
        return baseHeight + padT + padB
    }

    override fun drawSelf(batch: UIBatch) {
        super.drawSelf(batch)
        if (text.isEmpty()) return
        val currentFont = activeFont ?: return

        val innerX = bounds.x + padL
        val innerY = bounds.y + padB
        val innerWidth = maxOf(0.0f, bounds.width - padL - padR)
        val innerHeight = maxOf(0.0f, bounds.height - padT - padB)

        val capHeight = currentFont.data.capHeight
        val drawY = when {
            wrap -> innerY + innerHeight - currentFont.data.ascent
            else -> innerY + (innerHeight + capHeight) * 0.5f
        }

        batch.drawText(
            font = currentFont,
            text = text,
            x = innerX,
            y = drawY,
            targetWidth = innerWidth,
            align = align,
            wrap = wrap,
            ellipsis = ellipsis,
            color = textColor
        )
    }
}
