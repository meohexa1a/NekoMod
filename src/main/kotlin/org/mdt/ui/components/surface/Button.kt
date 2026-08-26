@file:Suppress("FunctionName", "unused")

package org.mdt.ui.components.surface

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import arc.graphics.Color
import arc.graphics.g2d.Font
import arc.util.Align
import mindustry.ui.Fonts
import org.mdt.ui.components.layout.Box
import org.mdt.ui.components.text.Text
import org.mdt.core.ui.compose.*

/**
 * ## ButtonColors
 *
 * Visual color palette state for [Button].
 */
data class ButtonColors(
    val background: Color,
    val text: Color,
    val hover: Color,
    val active: Color,
    val border: Color = Color.clear
) {
    companion object {
        val Default = ButtonColors(
            background = Color.valueOf("1a1c2e").a(0.85f),
            text = Color.valueOf("cad3f5"),
            hover = Color.valueOf("282b45"),
            active = Color.valueOf("3b82f6"),
            border = Color.valueOf("363a4f")
        )
        val Ghost = ButtonColors(
            background = Color.clear,
            text = Color.valueOf("cad3f5"),
            hover = Color.valueOf("2563eb").a(0.2f),
            active = Color.valueOf("2563eb").a(0.4f),
            border = Color.clear
        )
        val Primary = ButtonColors(
            background = Color.valueOf("2563eb"),
            text = Color.white,
            hover = Color.valueOf("1d4ed8"),
            active = Color.valueOf("1e40af"),
            border = Color.valueOf("60a5fa").a(0.4f)
        )
        val Success = ButtonColors(
            background = Color.valueOf("059669"),
            text = Color.white,
            hover = Color.valueOf("047857"),
            active = Color.valueOf("065f46")
        )
        val Danger = ButtonColors(
            background = Color.valueOf("dc2626"),
            text = Color.white,
            hover = Color.valueOf("b91c1c"),
            active = Color.valueOf("991b1b")
        )
    }
}

/**
 * ## Button (Slot-based)
 *
 * Core declarative clickable button component supporting arbitrary custom composable content.
 */
@Composable
fun Button(
    onClick: () -> Unit,
    modifier: UIModifier = UIModifier,
    colors: ButtonColors = ButtonColors.Default,
    content: @Composable () -> Unit
) {
    var isHovered by remember { mutableStateOf(false) }
    var isPressed by remember { mutableStateOf(false) }

    val bgColor = when {
        isPressed -> colors.active
        isHovered -> colors.hover
        else -> colors.background
    }

    val currentBorder = if (isHovered && colors.border != Color.clear) {
        colors.border.cpy().mul(1.3f)
    } else colors.border

    Box(
        modifier = UIModifier
            .radius(8f)
            .background(bgColor)
            .border(1f, currentBorder)
            .shadow(if (isHovered) Color.black.a(0.35f) else Color.clear, blur = 8f, spread = 1f)
            .pad(horizontal = 16f, vertical = 10f)
            .clickable(
                onClick = onClick,
                onPressStateChanged = { isPressed = it }
            )
            .hoverable { isHovered = it }
            .then(modifier)
    ) {
        content()
    }
}

/**
 * ## Button (Text overload)
 *
 * Convenient declarative text button.
 */
@Composable
fun Button(
    text: String,
    onClick: () -> Unit,
    modifier: UIModifier = UIModifier,
    colors: ButtonColors = ButtonColors.Default,
    radius: Float = 8f,
    font: Font = Fonts.def,
    fontScale: Float = 1.0f,
    paddingV: Float = 10f,
    paddingH: Float = 16f,
    align: Int = Align.left,
    textModifier: UIModifier = UIModifier
) {
    Button(
        onClick = onClick,
        colors = colors,
        modifier = UIModifier
            .radius(radius)
            .pad(horizontal = paddingH, vertical = paddingV)
            .then(modifier)
    ) {
        Text(
            text = text,
            color = colors.text,
            font = font,
            scale = fontScale,
            align = align,
            modifier = textModifier
        )
    }
}
