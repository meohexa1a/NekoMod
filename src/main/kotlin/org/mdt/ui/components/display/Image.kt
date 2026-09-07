// [AGENT INVARIANT] Synchronously update @property, @param, and @see KDocs when modifying this file.

@file:Suppress("FunctionName", "unused")

package org.mdt.ui.components.display

import androidx.compose.runtime.Composable
import arc.graphics.g2d.TextureRegion
import org.mdt.core.platform.LocalPlatformHost
import org.mdt.core.ui.modifier.UIModifier
import org.mdt.core.ui.modifier.background
import org.mdt.core.ui.modifier.texture
import org.mdt.core.ui.unit.Color
import org.mdt.ui.components.layout.Box
import org.mdt.ui.theme.LocalContentColor

/**
 * ## Image
 *
 * Declarative image composable rendering TextureRegions from the game atlas.
 * Automatically tints according to [LocalContentColor] if [tint] is [Color.Unspecified].
 *
 * @param region Texture region to draw.
 * @param modifier Chainable [UIModifier].
 * @param tint Color tint applied to the texture region.
 *
 * @see LocalContentColor
 */
@Composable
fun Image(
    region: TextureRegion,
    modifier: UIModifier = UIModifier,
    tint: Color = Color.Unspecified
) {
    val resolvedTint = when {
        tint.isSpecified -> tint
        else -> LocalContentColor.current
    }

    Box(
        modifier = modifier
            .texture(region)
            .background(resolvedTint)
    )
}

/**
 * ## Image (Name Overload)
 *
 * Declarative image composable resolving TextureRegions dynamically by name from [LocalPlatformHost].
 *
 * @param name Atlas texture region name identifier.
 * @param modifier Chainable [UIModifier].
 * @param tint Color tint applied to the texture region.
 */
@Composable
fun Image(
    name: String,
    modifier: UIModifier = UIModifier,
    tint: Color = Color.Unspecified
) {
    val host = LocalPlatformHost.current
    val region = host.assets.resolveAtlasRegion(name) ?: host.assets.resolveFallbackRegion()
    Image(region = region, modifier = modifier, tint = tint)
}
