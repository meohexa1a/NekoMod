@file:Suppress("FunctionName", "unused")

package org.mdt.ui.components.display

import androidx.compose.runtime.Composable
import arc.graphics.g2d.TextureRegion
import org.mdt.core.engine.LocalPlatformHost
import org.mdt.core.ui.compose.UIModifier
import org.mdt.core.ui.compose.background
import org.mdt.core.ui.compose.texture
import org.mdt.core.ui.graphics.Color
import org.mdt.ui.components.layout.Box

/**
 * ## Image
 *
 * Declarative image composable rendering TextureRegions from the game atlas.
 *
 * See: docs/components-guide/components_guide_en.md
 */
@Composable
fun Image(
    region: TextureRegion,
    modifier: UIModifier = UIModifier,
    tint: Color = Color.White
) {
    Box(
        modifier = UIModifier
            .texture(region)
            .background(tint)
            .then(modifier)
    )
}

/**
 * ## Image (Name Overload)
 *
 * Declarative image composable resolving TextureRegions dynamically by name from [LocalPlatformHost].
 *
 * See: docs/components-guide/components_guide_en.md
 */
@Composable
fun Image(
    name: String,
    modifier: UIModifier = UIModifier,
    tint: Color = Color.White
) {
    val host = LocalPlatformHost.current
    val region = host.resolveAtlasRegion(name) ?: host.resolveFallbackRegion()
    Image(region = region, modifier = modifier, tint = tint)
}
