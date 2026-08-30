// [AGENT INVARIANT] Synchronously update @property, @param, and @see KDocs when modifying this file.

@file:Suppress("FunctionName", "unused")

package org.mdt.ui.components.display

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import kotlinx.coroutines.delay
import org.mdt.core.ui.compose.UIModifier
import org.mdt.core.ui.compose.align
import org.mdt.core.ui.compose.background
import org.mdt.core.ui.compose.border
import org.mdt.core.ui.compose.glass
import org.mdt.core.ui.compose.margin
import org.mdt.core.ui.compose.onHover
import org.mdt.core.ui.compose.pad
import org.mdt.core.ui.compose.radius
import org.mdt.core.ui.layout.Alignment
import org.mdt.core.ui.unit.Color
import org.mdt.ui.components.layout.Box
import org.mdt.ui.components.text.Text
import kotlin.time.Duration.Companion.milliseconds

/**
 * ## TooltipBox
 *
 * Declarative tooltip container that shows a floating tooltip overlay after hovering for [delayMs].
 */
@Composable
fun TooltipBox(
    tooltip: @Composable () -> Unit,
    modifier: UIModifier = UIModifier,
    delayMs: Long = 350L,
    content: @Composable () -> Unit
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
        modifier = UIModifier
            .onHover { isHovered = it }
            .then(modifier)
    ) {
        content()

        if (isVisible) {
            Box(
                modifier = UIModifier
                    .align(Alignment.TopCenter)
                    .margin(top = 28.0f)
                    .background(Color(0.08f, 0.08f, 0.12f, 0.92f))
                    .radius(8.0f)
                    .border(1.0f, Color(1.0f, 1.0f, 1.0f, 0.15f))
                    .pad(8.0f, 4.0f)
                    .glass(true)
            ) {
                tooltip()
            }
        }
    }
}

/**
 * Convenience overload of [TooltipBox] displaying simple string [text].
 */
@Composable
fun TooltipBox(
    text: String,
    modifier: UIModifier = UIModifier,
    delayMs: Long = 350L,
    content: @Composable () -> Unit
) {
    TooltipBox(
        tooltip = {
            Text(
                text = text,
                color = Color(0.92f, 0.92f, 0.95f, 1.0f)
            )
        },
        modifier = modifier,
        delayMs = delayMs,
        content = content
    )
}
