package org.mdt.core.ui.node

import arc.graphics.g2d.Font
import arc.util.Align
import org.mdt.core.ui.EngineRuntime
import org.mdt.core.platform.render.FontRenderer
import org.mdt.core.ui.unit.Color

/**
 * ## TextNode [BMFont Text Virtual Node]
 *
 * > 🤖 **AGENT DIRECTIVE:** When modifying this class or related logic, synchronously update this KDoc:
 * > 1) Keep Purpose accurate. 2) Update Key Rules & Checklist [x]/[ ]. 3) Maintain Related Files map.
 *
 * ### 1. Purpose
 * - Virtual DOM node that renders bitmap fonts (BMFont) using [FontRenderer] and [org.mdt.core.platform.render.UIBatch].
 * - Supports text wrapping, ellipsis truncation, alignment, and intrinsic text size measurements.
 *
 * ### 2. Key Rules & Checklist
 * - [x] Draw bitmap fonts at integer scale (natural `scale = 1.0f`), avoiding fractional scaling.
 * - [x] Calculate vertical baseline correctly using OpenGL bottom-left coordinates.
 * - [x] Changing `text`, `font`, `wrap`, or `ellipsis` must trigger `invalidateLayout()`.
 * - [x] Pass container inner width during height calculation when `wrap = true`.
 *
 * ### 3. Related Files
 * - Composable UI: `src/main/kotlin/org/mdt/ui/components/text/Text.kt`
 * - Font Drawer: `src/main/kotlin/org/mdt/core/platform/render/FontRenderer.kt`
 * - GPU Batcher: `src/main/kotlin/org/mdt/core/platform/render/UIBatch.kt`
 */
open class TextNode(
    text: String = ""
) : LayoutNode() {

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

    val activeFont: Font? get() = font ?: EngineRuntime.host.resolveDefaultFont()

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
        val measuredWidth = FontRenderer.getPrefWidth(currentFont, text, 0.0f, false)
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
        val measuredHeight = FontRenderer.getPrefHeight(currentFont, text, maxOf(0.0f, targetWidth), align, wrap)
        val baseHeight = if (minHeight >= 0.0f) maxOf(measuredHeight, minHeight) else measuredHeight
        return baseHeight + padT + padB
    }

    override fun drawSelf() {
        super.drawSelf()
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

        FontRenderer.draw(
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
