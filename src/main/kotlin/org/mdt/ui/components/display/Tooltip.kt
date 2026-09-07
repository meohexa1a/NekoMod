// [AGENT INVARIANT] Synchronously update @property, @param, and @see KDocs when modifying this file.

@file:Suppress("FunctionName", "unused")

package org.mdt.ui.components.display

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import kotlin.time.Duration.Companion.milliseconds
import kotlinx.coroutines.delay
import org.mdt.core.ui.layout.BoxScope
import org.mdt.core.ui.modifier.UIModifier
import org.mdt.core.ui.modifier.align
import org.mdt.core.ui.modifier.margin
import org.mdt.core.ui.modifier.onHover
import org.mdt.core.ui.unit.Alignment
import org.mdt.core.ui.unit.Color
import org.mdt.core.ui.unit.Insets
import org.mdt.ui.components.layout.Box
import org.mdt.ui.components.surface.Surface
import org.mdt.ui.components.text.Text
import org.mdt.ui.theme.ThemeTokens

/**
 * ## TooltipBox
 *
 * Declarative tooltip container that shows a floating tooltip [Surface] overlay after hovering for [delayMs].
 *
 * @param tooltip Composable slot rendered inside the floating tooltip surface.
 * @param modifier Chainable [UIModifier].
 * @param delayMs Delay duration in milliseconds before tooltip appears.
 * @param content Composable slot representing the anchor control.
 *
 * @see Surface
 */
@Composable
fun TooltipBox(
    tooltip: @Composable BoxScope.() -> Unit,
    modifier: UIModifier = UIModifier,
    delayMs: Long = 350L,
    content: @Composable BoxScope.() -> Unit
) {
    var isHovered by remember { mutableStateOf(false) }
    var isVisible by remember { mutableStateOf(false) }

    LaunchedEffect(isHovered) {
        when {
            isHovered -> {
                delay(delayMs.milliseconds)
                isVisible = true
            }
            else -> isVisible = false
        }
    }

    Box(
        modifier = modifier.onHover { isHovered = it }
    ) {
        content()

        if (isVisible) {
            Surface(
                modifier = UIModifier
                    .align(Alignment.TopCenter)
                    .margin(top = 28.0f),
                color = Color(0.08f, 0.08f, 0.12f, 0.92f),
                contentColor = ThemeTokens.textPrimary,
                radius = 8.0f,
                borderWidth = 1.0f,
                borderColor = Color(1.0f, 1.0f, 1.0f, 0.15f),
                isGlass = true,
                contentPadding = Insets(left = 8.0f, top = 4.0f, right = 8.0f, bottom = 4.0f),
                content = tooltip
            )
        }
    }
}

/**
 * Convenience overload of [TooltipBox] displaying a simple text label.
 */
@Composable
fun TooltipBox(
    text: String,
    modifier: UIModifier = UIModifier,
    delayMs: Long = 350L,
    content: @Composable BoxScope.() -> Unit
) {
    TooltipBox(
        tooltip = {
            Text(text = text)
        },
        modifier = modifier,
        delayMs = delayMs,
        content = content
    )
}
