package org.hubdustry.core.compose.modifier

import arc.graphics.Color
import arc.graphics.g2d.Font
import org.hubdustry.core.layout.LayoutNode

// ─── Public Text Modifiers ────────────────────────────────────────

/**
 * Đặt màu chữ (text color) cho Text element.
 */
fun Modifier.textColor(color: Color): Modifier =
    this.then(TextColorModifier(color = color))

/**
 * Đặt kiểu chữ BMFont ([Font]) cho Text element.
 */
fun Modifier.font(font: Font): Modifier =
    this.then(FontModifier(font = font))

// ─── Internal Modifier Elements ────────────────────────────────────

internal data class TextColorModifier(
    val color: Color
) : Modifier.Element {
    override fun applyTo(node: LayoutNode) {
        node.textColor = color
    }
}

internal data class FontModifier(
    val font: Font
) : Modifier.Element {
    override fun applyTo(node: LayoutNode) {
        node.font = font
    }
}

