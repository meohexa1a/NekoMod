package org.mdt.ui.widgets

import arc.Core
import org.mdt.ui.core.Rect
import org.mdt.ui.core.UINode
import org.mdt.ui.layout.SizeFlags
import org.mdt.ui.render.EngineRenderer
import org.mdt.ui.render.TextRenderer

/**
 * ## TextNode
 *
 * BMFont text rendering node with Mindustry localization bundle resolution (`$key` or `@key`),
 * auto preferred size computation factoring in Box Model Padding, and OpenGL baseline alignment.
 *
 * See: docs/architecture/architecture_en.md
 */
class TextNode(text: String = "") : UINode() {
    /** Typography visual styling properties (Color, Scale, Alignment, Wrap, Ellipsis). */
    val visuals = TextVisuals()

    /** BMFont layout calculator and glyph drawer. */
    val renderer = TextRenderer()

    private var _text: String = ""

    /** Text content string. Automatically resolves localization keys prefixed with `$` or `@`. */
    var text: String
        get() = _text
        set(value) {
            val resolved = if (value.isNotEmpty() && Core.bundle != null) {
                val c = value[0]
                if (c == '$' || c == '@') {
                    val key = value.substring(1)
                    if (Core.bundle.has(key)) Core.bundle[key, value] else value
                } else value
            } else value

            if (resolved != _text) {
                _text = resolved
                renderer.invalidate()
                invalidateLayout()
            }
        }

    init {
        sizeFlagsHorizontal = SizeFlags.SHRINK_BEGIN
        sizeFlagsVertical = SizeFlags.SHRINK_CENTER
        this.text = text
    }

    override fun getPrefWidth(): Float {
        val availInnerW = if (bounds.width > 0f) maxOf(0f, bounds.width - padL - padR) else 0f
        val textPrefW = renderer.getPrefWidth(_text, visuals, availInnerW)
        val contentW = if (width >= 0f) width else if (minWidth >= 0f) maxOf(textPrefW, minWidth) else textPrefW
        return contentW + padL + padR
    }

    override fun getPrefHeight(): Float {
        val availInnerW = if (bounds.width > 0f) maxOf(0f, bounds.width - padL - padR) else 0f
        val textPrefH = renderer.getPrefHeight(_text, visuals, availInnerW)
        val contentH = if (height >= 0f) height else if (minHeight >= 0f) maxOf(textPrefH, minHeight) else textPrefH
        return contentH + padT + padB
    }

    override fun drawSelf(renderer: EngineRenderer) {
        if (_text.isNotEmpty()) {
            val innerBounds = Rect(
                bounds.x + padL,
                bounds.y + padB,
                maxOf(0f, bounds.width - padL - padR),
                maxOf(0f, bounds.height - padT - padB)
            )
            this.renderer.draw(_text, visuals, innerBounds)
        }
    }
}
