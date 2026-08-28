@file:Suppress("FunctionName", "unused")

package org.mdt.ui.components.surface

import androidx.compose.runtime.Composable
import org.mdt.core.ui.compose.*
import org.mdt.core.ui.unit.Color
import org.mdt.ui.components.layout.Box

/**
 * ## Card
 *
 * Container surface for groupings of related UI information with rounded corners, borders, and optional frosted glassmorphism.
 * Intercepts pointer events (opaque) to prevent clicking through to gameplay.
 *
 * See: docs/components-guide/components_guide_en.md
 */
@Composable
fun Card(
    modifier: UIModifier = UIModifier,
    color: Color = Color(0.12f, 0.12f, 0.18f, 0.45f),
    radius: Float = 16.0f,
    borderWidth: Float = 1.0f,
    borderColor: Color = Color(1.0f, 1.0f, 1.0f, 0.18f),
    isGlass: Boolean = true,
    content: @Composable BoxScope.() -> Unit
) {
    Box(
        modifier = UIModifier
            .opaque()
            .background(color)
            .radius(radius)
            .border(borderWidth, borderColor)
            .glass(isGlass)
            .pad(16.0f)
            .then(modifier),
        content = content
    )
}
