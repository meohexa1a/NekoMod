@file:Suppress("FunctionName", "unused")

package org.mdt.ui.components.surface

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import arc.graphics.Color
import org.mdt.ui.components.layout.Box
import org.mdt.ui.components.text.Text
import org.mdt.ui.compose.UIModifier
import org.mdt.ui.compose.background
import org.mdt.ui.compose.border
import org.mdt.ui.compose.clickable
import org.mdt.ui.compose.cornerRadius
import org.mdt.ui.compose.hoverable
import org.mdt.ui.compose.pad
import org.mdt.ui.compose.shadow

data class ButtonColors(
    val background: Color,
    val text: Color,
    val hover: Color,
    val active: Color,
    val border: Color = Color.clear
) {
    companion object {
        val Default = ButtonColors(
            background = Color.valueOf("24273a"),
            text = Color.valueOf("cad3f5"),
            hover = Color.valueOf("363a4f"),
            active = Color.valueOf("494d64"),
            border = Color.valueOf("494d64")
        )
        val Primary = ButtonColors(
            background = Color.valueOf("2563eb"),
            text = Color.white,
            hover = Color.valueOf("1d4ed8"),
            active = Color.valueOf("1e40af")
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
 * ## Button
 *
 * Interactive clickable button with stateful hover & active color transitions.
 */
@Composable
fun Button(
    text: String,
    onClick: () -> Unit,
    modifier: UIModifier = UIModifier,
    colors: ButtonColors = ButtonColors.Default,
    radius: Float = 6f,
    fontScale: Float = 0.9f
) {
    var isHovered by remember { mutableStateOf(false) }
    var isPressed by remember { mutableStateOf(false) }

    val bgColor = when {
        isPressed -> colors.active
        isHovered -> colors.hover
        else -> colors.background
    }

    Box(
        modifier = UIModifier
            .cornerRadius(radius)
            .background(bgColor)
            .border(1f, colors.border)
            .pad(left = 12f, right = 12f, top = 6f, bottom = 6f)
            .clickable(
                onClick = onClick,
                onPressStateChanged = { isPressed = it }
            )
            .hoverable { isHovered = it }
            .then(modifier)
    ) {
        Text(
            text = text,
            color = colors.text,
            scale = fontScale
        )
    }
}
