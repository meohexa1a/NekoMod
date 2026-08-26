package org.mdt.ui.components.display.image

import androidx.compose.runtime.*
import arc.graphics.Color
import arc.graphics.g2d.TextureRegion
import org.mdt.core.common.RequestBuilder
import org.mdt.core.common.TextureHandle
import org.mdt.core.engine.EngineContext
import org.mdt.core.engine.image.ImageSource
import org.mdt.core.ui.compose.UIModifier
import org.mdt.core.ui.compose.texture
import org.mdt.ui.components.layout.Box

/**
 * ## ScaleMode
 *
 * Scaling behavior for texture regions inside UI containers and image widgets.
 *
 * - [FIT]: Scales the image proportionally so it fits completely within container bounds.
 * - [CROP]: Scales the image proportionally to completely fill container bounds, cropping overflow.
 * - [STRETCH]: Non-uniformly stretches the image to exactly match container dimensions.
 * - [CENTER]: Displays the image at its natural 1:1 pixel size centered in the container.
 */
enum class ScaleMode {
    FIT,
    CROP,
    STRETCH,
    CENTER
}

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
    val imageService = EngineContext.current.image
    var region by remember(source) { mutableStateOf(imageService.fallbackRegion()) }

    DisposableEffect(source) {
        var currentHandle: TextureHandle? = null
        imageService.load(source) { loadedRegion, handle, _ ->
            region = loadedRegion
            currentHandle?.release()
            currentHandle = handle
        }
        onDispose {
            currentHandle?.release()
            currentHandle = null
        }
    }

    Box(
        modifier = UIModifier
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
        modifier = UIModifier
            .texture(region, scaleMode, tint)
            .then(modifier)
    )
}
