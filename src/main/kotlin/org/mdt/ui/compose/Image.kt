@file:Suppress("FunctionName")

package org.mdt.ui.compose

import androidx.compose.runtime.Composable
import androidx.compose.runtime.ComposeNode
import arc.graphics.Color
import org.mdt.core.image.ImageSource
import org.mdt.ui.widgets.ImageNode
import org.mdt.ui.widgets.ScaleMode

/**
 * ## Image
 *
 * Declarative image composable supporting remote URLs, asset paths, and TextureRegions.
 *
 * @param source Image source (URL String, Asset String, or TextureRegion).
 * @param modifier Chainable [UIModifier].
 * @param scaleMode Aspect scaling mode ([ScaleMode.FIT], [ScaleMode.CROP], [ScaleMode.STRETCH]).
 * @param tint Color tint applied to the image.
 */
@Composable
fun Image(
    source: Any,
    modifier: UIModifier = UIModifier,
    scaleMode: ScaleMode = ScaleMode.FIT,
    tint: Color = Color.white
) {
    val imgSource = ImageSource.of(source)

    ComposeNode<ImageNode, NodeApplier>(
        factory = { ImageNode() },
        update = {
            set(imgSource) { this.source = it }
            set(scaleMode) { this.scaleMode = it }
            set(tint) { this.tint.set(it) }
            set(modifier) {
                it.applyTo(this)
                invalidateLayout()
            }
        }
    )
}
