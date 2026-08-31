// [AGENT INVARIANT] Synchronously update @property, @param, and @see KDocs when modifying this file.

package org.mdt.ui.components.surface

import androidx.compose.runtime.Composable
import org.mdt.core.ui.unit.Color
import org.mdt.core.ui.modifier.UIModifier
import org.mdt.core.ui.modifier.background
import org.mdt.core.ui.modifier.border
import org.mdt.core.ui.modifier.glass
import org.mdt.core.ui.modifier.opaque
import org.mdt.core.ui.modifier.pad
import org.mdt.core.ui.modifier.radius
import org.mdt.core.ui.layout.BoxScope
import org.mdt.ui.components.layout.Box
import org.mdt.ui.theme.CardDefaults

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
    color: Color = CardDefaults.color,
    radius: Float = CardDefaults.radius,
    borderWidth: Float = CardDefaults.borderWidth,
    borderColor: Color = CardDefaults.borderColor,
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
