@file:Suppress("FunctionName", "unused")

package org.mdt.ui.components.display.image

import androidx.compose.runtime.Composable
import androidx.compose.runtime.ComposeNode
import arc.graphics.Color
import arc.graphics.g2d.TextureRegion
import org.mdt.core.image.ImageSource
import org.mdt.ui.compose.NodeApplier
import org.mdt.ui.compose.UIModifier

/**
 * ## Image
 *
 * Declarative image composable supporting remote URLs, sprite atlas regions,
 * and local files.
 *
 * @param source Image origin ([ImageSource] or String URL/Atlas name).
 * @param modifier Chainable [UIModifier].
 * @param scaleMode Image scaling behavior ([ScaleMode]).
 * @param tint Tint color multiplied with the image.
 */
@Composable
fun Image(
    source: ImageSource,
    modifier: UIModifier = UIModifier,
    scaleMode: ScaleMode = ScaleMode.FIT,
    tint: Color = Color.white
) {
    ComposeNode<ImageNode, NodeApplier>(
        factory = {
            val node = ImageNode()
            node.source = source
            node.scaleMode = scaleMode
            node.tintColor.set(tint)
            modifier.applyTo(node)
            node
        },
        update = {
            set(source) { this.source = it }
            set(scaleMode) { this.scaleMode = it; invalidateLayout() }
            set(tint) { this.tintColor.set(it) }
            set(modifier) {
                it.applyTo(this)
                invalidateLayout()
            }
        }
    )
}

/**
 * Convenience overload accepting a String identifier (URL, Atlas name, or file path).
 */
@Composable
fun Image(
    source: String,
    modifier: UIModifier = UIModifier,
    scaleMode: ScaleMode = ScaleMode.FIT,
    tint: Color = Color.white
) {
    Image(
        source = ImageSource.of(source),
        modifier = modifier,
        scaleMode = scaleMode,
        tint = tint
    )
}

/**
 * Convenience overload accepting a raw [TextureRegion].
 */
@Composable
fun Image(
    region: TextureRegion,
    modifier: UIModifier = UIModifier,
    scaleMode: ScaleMode = ScaleMode.FIT,
    tint: Color = Color.white
) {
    ComposeNode<ImageNode, NodeApplier>(
        factory = {
            val node = ImageNode()
            node.setRegion(region)
            node.scaleMode = scaleMode
            node.tintColor.set(tint)
            modifier.applyTo(node)
            node
        },
        update = {
            set(region) { this.setRegion(it) }
            set(scaleMode) { this.scaleMode = it; invalidateLayout() }
            set(tint) { this.tintColor.set(it) }
            set(modifier) {
                it.applyTo(this)
                invalidateLayout()
            }
        }
    )
}
