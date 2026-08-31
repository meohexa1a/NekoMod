// [AGENT ARCHITECTURE & INVARIANTS]
// - Domain Role: Visual Styling Element Modifiers & Fluent Builders.
// - Operating Mechanism: Applies background colors, textures, corner radii, borders, frosted glass, drop shadows, opacity, and clipping onto [LayoutNode].
// - Invariants: Float colors, radii, opacities; single-draw-call compatibility in [UIBatch].
// - Dependencies: [UIModifier], [LayoutNode], [UIBatch], [SceneBlur].
// - Directive: Synchronously update @property, @param, and @see KDocs when modifying this file.

@file:Suppress("unused")

package org.mdt.core.ui.modifier

import arc.graphics.g2d.TextureRegion
import org.mdt.core.ui.node.LayoutNode
import org.mdt.core.ui.node.UINode
import org.mdt.core.ui.unit.Color

// --- TYPED DRAW MODIFIER ELEMENTS ---

/**
 * ## TextureModifier
 *
 * Sets background texture atlas region on a [LayoutNode].
 *
 * @property region Texture region to render.
 */
data class TextureModifier(val region: TextureRegion) : UIModifier.Element {
    override fun applyTo(node: UINode) {
        if (node is LayoutNode) node.region = region
    }
}

/**
 * ## BackgroundModifier
 *
 * Sets background fill color on a [LayoutNode].
 *
 * @property color Fill color applied to the node background.
 */
data class BackgroundModifier(val color: Color) : UIModifier.Element {
    override fun applyTo(node: UINode) {
        if (node is LayoutNode) node.color = color
    }
}

/**
 * ## RadiusModifier
 *
 * Sets uniform rounded corner radius on a [LayoutNode].
 *
 * @property radius Corner radius in pixels.
 */
data class RadiusModifier(val radius: Float) : UIModifier.Element {
    override fun applyTo(node: UINode) {
        if (node is LayoutNode) node.radius = radius
    }
}

/**
 * ## BorderModifier
 *
 * Sets border stroke outline on a [LayoutNode].
 *
 * @property width Stroke width in pixels.
 * @property color Stroke outline color.
 */
data class BorderModifier(val width: Float, val color: Color) : UIModifier.Element {
    override fun applyTo(node: UINode) {
        if (node is LayoutNode) {
            node.borderWidth = width
            node.borderColor = color
        }
    }
}

/**
 * ## GlassModifier
 *
 * Enables Dual-Kawase frosted glass background blur sampling on a [LayoutNode].
 *
 * @property isGlass Whether frosted glass blur capture is active for this node.
 */
data class GlassModifier(val isGlass: Boolean = true) : UIModifier.Element {
    override fun applyTo(node: UINode) {
        if (node is LayoutNode) node.isGlass = isGlass
    }
}

/**
 * ## ShadowModifier
 *
 * Sets drop-shadow properties on a [LayoutNode].
 *
 * @property radius Shadow blur radius in pixels.
 * @property color Shadow color and opacity.
 * @property offsetX Horizontal shadow offset in pixels.
 * @property offsetY Vertical shadow offset in pixels.
 */
data class ShadowModifier(
    val radius: Float,
    val color: Color,
    val offsetX: Float = 0.0f,
    val offsetY: Float = 0.0f,
) : UIModifier.Element {
    override fun applyTo(node: UINode) {
        if (node is LayoutNode) {
            node.shadowRadius = radius
            node.shadowColor = color
            node.shadowOffsetX = offsetX
            node.shadowOffsetY = offsetY
        }
    }
}

/**
 * ## OpacityModifier
 *
 * Sets composite rendering opacity factor on a [UINode].
 *
 * @property opacity Alpha transparency multiplier in range `0.0f..1.0f`.
 */
data class OpacityModifier(val opacity: Float) : UIModifier.Element {
    override fun applyTo(node: UINode) {
        node.opacity = opacity
    }
}

/**
 * ## ZIndexModifier
 *
 * Sets explicit Z-index rendering order on a [UINode].
 *
 * @property zIndex Sorting index for drawing order.
 */
data class ZIndexModifier(val zIndex: Float) : UIModifier.Element {
    override fun applyTo(node: UINode) {
        node.zIndex = zIndex
    }
}

/**
 * ## ClipModifier
 *
 * Configures scissor clipping to node bounds on a [UINode].
 *
 * @property clip Whether rendering of children should be scissor-clipped to this node's bounds.
 */
data class ClipModifier(val clip: Boolean = true) : UIModifier.Element {
    override fun applyTo(node: UINode) {
        node.clip = clip
    }
}

// --- FLUENT EXTENSION FUNCTIONS ---

fun UIModifier.texture(region: TextureRegion): UIModifier = then(TextureModifier(region))

fun UIModifier.background(color: Color): UIModifier = then(BackgroundModifier(color))
fun UIModifier.background(hex: String): UIModifier = background(Color.parse(hex))

fun UIModifier.radius(radius: Float): UIModifier = then(RadiusModifier(radius))
fun UIModifier.radius(radius: Int): UIModifier = radius(radius.toFloat())
fun UIModifier.cornerRadius(radius: Float): UIModifier = radius(radius)
fun UIModifier.cornerRadius(radius: Int): UIModifier = radius(radius.toFloat())

fun UIModifier.border(width: Float, color: Color): UIModifier = then(BorderModifier(width, color))
fun UIModifier.border(width: Float, hex: String): UIModifier = border(width, Color.parse(hex))
fun UIModifier.border(width: Int, color: Color): UIModifier = border(width.toFloat(), color)
fun UIModifier.border(width: Int, hex: String): UIModifier = border(width.toFloat(), Color.parse(hex))

fun UIModifier.glass(isGlass: Boolean = true): UIModifier = then(GlassModifier(isGlass))

fun UIModifier.shadow(
    radius: Float = 8.0f,
    color: Color = Color(0.0f, 0.0f, 0.0f, 0.35f),
    offsetX: Float = 0.0f,
    offsetY: Float = -2.0f,
): UIModifier = then(ShadowModifier(radius, color, offsetX, offsetY))

fun UIModifier.opacity(opacity: Float): UIModifier = then(OpacityModifier(opacity))
fun UIModifier.alpha(alpha: Float): UIModifier = opacity(alpha)

fun UIModifier.zIndex(zIndex: Float): UIModifier = then(ZIndexModifier(zIndex))
fun UIModifier.zIndex(zIndex: Int): UIModifier = zIndex(zIndex.toFloat())

fun UIModifier.clip(clip: Boolean = true): UIModifier = then(ClipModifier(clip))
fun UIModifier.clipToBounds(): UIModifier = clip(true)
