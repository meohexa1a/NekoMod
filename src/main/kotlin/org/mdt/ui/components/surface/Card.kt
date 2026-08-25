@file:Suppress("FunctionName", "unused")

package org.mdt.ui.components.surface

import androidx.compose.runtime.Composable
import arc.graphics.Color
import org.mdt.ui.components.layout.Box
import org.mdt.ui.compose.BoxScope
import org.mdt.ui.compose.UIModifier
import org.mdt.ui.compose.backdrop
import org.mdt.ui.compose.background
import org.mdt.ui.compose.border
import org.mdt.ui.compose.cornerRadius
import org.mdt.ui.compose.pad
import org.mdt.ui.compose.shadow

/**
 * ## Card
 *
 * Glassmorphic surface container with rounded corners, subtle shadow, and frosted backdrop blur.
 */
@Composable
fun Card(
    modifier: UIModifier = UIModifier,
    backgroundColor: Color = Color.valueOf("1e2030").a(0.85f),
    borderColor: Color = Color.valueOf("363a4f").a(0.8f),
    radius: Float = 12f,
    padding: Float = 16f,
    enableBackdrop: Boolean = true,
    content: @Composable BoxScope.() -> Unit
) {
    val base = UIModifier
        .cornerRadius(radius)
        .background(backgroundColor)
        .border(1f, borderColor)
        .shadow(Color.black.a(0.4f), offsetX = 0f, offsetY = -4f, blur = 16f, spread = 2f)
        .pad(padding)

    val styledModifier = if (enableBackdrop) {
        base.backdrop(blur = true, blurRadius = 12f)
    } else base

    Box(
        modifier = styledModifier.then(modifier),
        content = content
    )
}
