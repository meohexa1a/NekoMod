@file:Suppress("FunctionName", "unused")

package org.mdt.ui.components.display

import androidx.compose.runtime.*
import kotlinx.coroutines.delay
import org.mdt.core.ui.compose.*
import org.mdt.core.ui.layout.Alignment
import org.mdt.core.ui.unit.Color
import org.mdt.ui.components.layout.Box
import org.mdt.ui.components.text.Text

/**
 * ## TooltipBox [Declarative Self-Managed Tooltip Composable]
 *
 * ### 1. 📖 Feature Specification & Core Architecture:
 * - Declarative, self-contained tooltip wrapper managing hover recognition, delay timer, and overlay presentation.
 * - Encapsulates its own local state (isHovered, delay ~350ms) using coroutines and standard Compose snapshot state.
 * - Renders a stylized floating bubble with rounded corners, subtle border, and text styling.
 *
 * ### 2. ⚡ Invariants & Non-Negotiable Rules:
 * - **Rule 1 (Declarative State Isolation):** State is scoped locally to the Composable, leaving Core UI 100% agnostic of tooltips.
 * - **Rule 2 (UIModifier Chaining):** Callers can append custom modifiers via .then(modifier).
 *
 * ### 3. 🔗 Related Files & Subsystem Map:
 * - 🎨 **Composable Box:** src/main/kotlin/org/mdt/ui/components/layout/Box.kt
 * - 🎨 **Composable Text:** src/main/kotlin/org/mdt/ui/components/text/Text.kt
 * - 🌲 **Virtual Node:** src/main/kotlin/org/mdt/core/ui/node/LayoutNode.kt
 *
 * ### 4. ✅ Behavioral Verification Checklist:
 * - [x] Hovering over target content starts delay timer (350ms) before revealing tooltip.
 * - [x] Exiting hover area immediately conceals tooltip bubble.
 * - [x] Custom tooltip composable slot or plain text string overload supported.
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
        if (isHovered) {
            delay(delayMs)
            isVisible = true
        } else {
            isVisible = false
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
