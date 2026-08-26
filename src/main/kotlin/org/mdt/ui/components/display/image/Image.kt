@file:Suppress("FunctionName", "unused")

package org.mdt.ui.components.display.image

import androidx.compose.runtime.*
import arc.graphics.Color
import arc.graphics.g2d.TextureRegion
import org.mdt.core.cache.TextureHandle
import org.mdt.core.image.ImageLoader
import org.mdt.core.image.ImageSource
import org.mdt.core.net.RequestBuilder
import org.mdt.ui.components.layout.Box
import org.mdt.ui.components.layout.ScaleMode
import org.mdt.core.ui.compose.UIModifier
import org.mdt.core.ui.compose.texture

/**
 * ## Image
 *
 * Pure declarative image composable using the unified GPU SDF [Box] primitive.
 * Supports remote URLs (with auth headers / request templates), sprite atlas regions,
 * local files, and direct [TextureRegion] instances with full SDF rounded corners,
 * borders, shadows, and glow.
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
    var region by remember(source) { mutableStateOf(ImageLoader.fallbackRegion()) }

    DisposableEffect(source) {
        var currentHandle: TextureHandle? = null
        ImageLoader.load(source) { reg, handle, _ ->
            region = reg
            currentHandle?.release()
            currentHandle = handle
        }
        onDispose {
            currentHandle?.release()
            currentHandle = null
        }
    }

    Box(
        modifier = modifier
            .texture(region, scaleMode, tint)
            .then(modifier)
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
 * Convenience overload accepting a remote URL with an HTTP request builder template.
 */
@Composable
fun Image(
    url: String,
    modifier: UIModifier = UIModifier,
    scaleMode: ScaleMode = ScaleMode.FIT,
    tint: Color = Color.white,
    configureRequest: RequestBuilder.() -> Unit
) {
    Image(
        source = ImageSource.Url(url, configureRequest),
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
    Box(
        modifier = modifier
            .texture(region, scaleMode, tint)
            .then(modifier)
    )
}
