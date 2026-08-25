@file:Suppress("FunctionName", "unused")

package org.mdt.ui.components.display.progress

import androidx.compose.runtime.Composable
import androidx.compose.runtime.ComposeNode
import arc.graphics.Color
import org.mdt.ui.compose.NodeApplier
import org.mdt.ui.compose.UIModifier

/**
 * ## ProgressColors
 *
 * Color palette state for [ProgressBar].
 */
data class ProgressColors(
    val track: Color = Color.valueOf("181926"),
    val fill: Color = Color.valueOf("2563eb")
) {
    companion object {
        val Default = ProgressColors()
        val Primary = ProgressColors(track = Color.valueOf("181926"), fill = Color.valueOf("2563eb"))
        val Success = ProgressColors(track = Color.valueOf("181926"), fill = Color.valueOf("059669"))
        val Danger = ProgressColors(track = Color.valueOf("181926"), fill = Color.valueOf("dc2626"))
        val Warning = ProgressColors(track = Color.valueOf("181926"), fill = Color.valueOf("d97706"))
    }
}

/**
 * ## ProgressBar
 *
 * Declarative progress indicator displaying completion percentage between `0.0f` and `1.0f`.
 *
 * @param progress Progress ratio (clamped to 0.0f..1.0f).
 * @param modifier Chainable [UIModifier].
 * @param barHeight Thickness of the progress bar in pixels.
 * @param colors Color styling palette ([ProgressColors]).
 */
@Composable
fun ProgressBar(
    progress: Float,
    modifier: UIModifier = UIModifier,
    barHeight: Float = 6f,
    colors: ProgressColors = ProgressColors.Default
) {
    ComposeNode<ProgressBarNode, NodeApplier>(
        factory = {
            val node = ProgressBarNode()
            node.progress = progress
            node.barHeight = barHeight
            node.trackColor.set(colors.track)
            node.fillColor.set(colors.fill)
            modifier.applyTo(node)
            node
        },
        update = {
            set(progress) { this.progress = it }
            set(barHeight) { this.barHeight = it; invalidateLayout() }
            set(colors) {
                this.trackColor.set(it.track)
                this.fillColor.set(it.fill)
            }
            set(modifier) {
                it.applyTo(this)
                invalidateLayout()
            }
        }
    )
}
