@file:Suppress("unused", "FunctionName")

package org.mdt.core.ui.compose

import arc.graphics.Color
import arc.graphics.g2d.TextureRegion
import mindustry.graphics.Pal
import org.mdt.core.ui.UINode
import org.mdt.core.ui.input.PointerEvent
import org.mdt.core.ui.layout.LayoutPreset
import org.mdt.core.ui.layout.SizeFlags
import org.mdt.ui.components.display.image.ScaleMode
import org.mdt.ui.components.layout.BoxVisuals
import org.mdt.ui.components.layout.LayoutNode
import org.mdt.ui.components.text.TextNode
import org.mdt.ui.components.text.TextVisuals

/**
 * ## UIModifier
 *
 * Immutable, type-safe modifier chain representing layout constraints, visual styles,
 * and input gesture listeners applied onto virtual [UINode] instances.
 *
 * Designed with Typed Modifier Elements for Zero-GC execution and fast Compose recomposition skipping.
 *
 * See: docs/compose-dsl/compose_dsl_en.md
 */
interface UIModifier {

    /** Applies this modifier's rules onto the target [node]. */
    fun applyTo(node: UINode)

    /** Chains this modifier with another [other] modifier. */
    fun then(other: UIModifier): UIModifier = if (other === None) this else CombinedModifier(this, other)

    /** Single atomic modifier element supporting value-based structural equality. */
    interface Element : UIModifier

    /** Empty modifier singleton representing a no-op chain start. */
    companion object None : UIModifier {
        override fun applyTo(node: UINode) {}
        override fun toString(): String = "Modifier.None"
    }
}

/** Top-level modifier factory returning an empty [UIModifier]. */
fun Modifier(): UIModifier = UIModifier

/** Ambient top-level accessor returning an empty [UIModifier]. */
val Modifier: UIModifier get() = UIModifier

/** Fluent modifier configuration builder. */
fun Modifier(block: UIModifier.() -> UIModifier): UIModifier = UIModifier.block()

/** Chains two modifiers sequentially with structural equality support. */
class CombinedModifier(val outer: UIModifier, val inner: UIModifier) : UIModifier {
    override fun applyTo(node: UINode) {
        outer.applyTo(node)
        inner.applyTo(node)
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is CombinedModifier) return false
        return outer == other.outer && inner == other.inner
    }

    override fun hashCode(): Int = 31 * outer.hashCode() + inner.hashCode()

    override fun toString(): String = "CombinedModifier($outer -> $inner)"
}

// =========================================================================
// I. Typed Modifier Elements (Zero-GC, Value-based Equality)
// =========================================================================

data class PaddingModifier(val left: Float, val top: Float, val right: Float, val bottom: Float) : UIModifier.Element {
    override fun applyTo(node: UINode) = node.pad(left, top, right, bottom)
}

data class MarginModifier(val left: Float, val top: Float, val right: Float, val bottom: Float) : UIModifier.Element {
    override fun applyTo(node: UINode) = node.margin(left, top, right, bottom)
}

data class SizeModifier(val width: Float = -1f, val height: Float = -1f) : UIModifier.Element {
    override fun applyTo(node: UINode) {
        if (width >= 0f) node.width = width
        if (height >= 0f) node.height = height
    }
}

data class MinSizeModifier(val minWidth: Float = -1f, val minHeight: Float = -1f) : UIModifier.Element {
    override fun applyTo(node: UINode) {
        if (minWidth >= 0f) node.minWidth = minWidth
        if (minHeight >= 0f) node.minHeight = minHeight
    }
}

data class MaxSizeModifier(val maxWidth: Float = -1f, val maxHeight: Float = -1f) : UIModifier.Element {
    override fun applyTo(node: UINode) {
        if (maxWidth >= 0f) node.maxWidth = maxWidth
        if (maxHeight >= 0f) node.maxHeight = maxHeight
    }
}

data class FillModifier(val horizontal: Boolean = true, val vertical: Boolean = true) : UIModifier.Element {
    override fun applyTo(node: UINode) {
        if (horizontal) node.sizeFlagsHorizontal = node.sizeFlagsHorizontal or SizeFlags.FILL
        if (vertical) node.sizeFlagsVertical = node.sizeFlagsVertical or SizeFlags.FILL
    }
}

data class ExpandModifier(val horizontal: Boolean = true, val vertical: Boolean = true, val ratio: Float = 1f) : UIModifier.Element {
    override fun applyTo(node: UINode) {
        if (horizontal) node.sizeFlagsHorizontal = node.sizeFlagsHorizontal or SizeFlags.EXPAND
        if (vertical) node.sizeFlagsVertical = node.sizeFlagsVertical or SizeFlags.EXPAND
        node.stretchRatio = ratio
    }
}

data class WeightModifier(val ratio: Float) : UIModifier.Element {
    override fun applyTo(node: UINode) {
        node.sizeFlagsHorizontal = node.sizeFlagsHorizontal or SizeFlags.EXPAND_FILL
        node.sizeFlagsVertical = node.sizeFlagsVertical or SizeFlags.EXPAND_FILL
        node.stretchRatio = ratio
    }
}

data class AnchorPresetModifier(val preset: LayoutPreset) : UIModifier.Element {
    override fun applyTo(node: UINode) = node.anchorData.setPreset(preset)
}

data class AnchorSpanModifier(val left: Float, val bottom: Float, val right: Float, val top: Float) : UIModifier.Element {
    override fun applyTo(node: UINode) {
        node.anchorData.setAnchors(left, bottom, right, top)
        node.anchorData.setOffsets(0f, 0f, 0f, 0f)
    }
}

data class BackgroundModifier(val color: Color) : UIModifier.Element {
    override fun applyTo(node: UINode) {
        (node as? LayoutNode)?.ensureVisuals()?.let {
            it.background.color.set(color)
            it.background.mode = org.mdt.ui.components.layout.BackgroundFill.Mode.COLOR
        }
    }
}

data class ProgressModifier(val fraction: Float, val color: Color) : UIModifier.Element {
    override fun applyTo(node: UINode) {
        (node as? LayoutNode)?.ensureVisuals()?.progress(fraction, color)
    }
}

data class RadiusModifier(val topStart: Float, val topEnd: Float, val bottomEnd: Float, val bottomStart: Float) : UIModifier.Element {
    override fun applyTo(node: UINode) {
        (node as? LayoutNode)?.ensureVisuals()?.radius(topStart, topEnd, bottomEnd, bottomStart)
    }
}

data class BorderModifier(
    val width: Float,
    val color: Color,
    val style: org.mdt.ui.components.layout.Border.Style = org.mdt.ui.components.layout.Border.Style.SOLID
) : UIModifier.Element {
    override fun applyTo(node: UINode) {
        (node as? LayoutNode)?.ensureVisuals()?.border(width, color, style)
    }
}

data class ShadowModifier(
    val color: Color,
    val offsetX: Float = 0f,
    val offsetY: Float = 0f,
    val blur: Float = 8f,
    val spread: Float = 4f
) : UIModifier.Element {
    override fun applyTo(node: UINode) {
        (node as? LayoutNode)?.ensureVisuals()?.shadow(color, offsetX, offsetY, blur, spread)
    }
}

data class GlowModifier(val color: Color, val spread: Float = 6f, val blur: Float = 12f) : UIModifier.Element {
    override fun applyTo(node: UINode) {
        (node as? LayoutNode)?.ensureVisuals()?.glow(color, spread, blur)
    }
}

data class BackdropModifier(
    val blur: Boolean = true,
    val blurRadius: Float = 12f,
    val weight: Float = 0.8f,
    val blend: Float = 0.8f,
    val tint: Color = Color.white,
    val iterations: Int = 2
) : UIModifier.Element {
    override fun applyTo(node: UINode) {
        (node as? LayoutNode)?.ensureVisuals()?.let { vis ->
            vis.backdrop.enabled = blur
            vis.backdrop.blurRadius = blurRadius
            vis.backdrop.weight = weight
            vis.backdrop.blend = blend
            vis.backdrop.tint.set(tint)
            vis.backdrop.iterations = iterations
            vis.background.mode = org.mdt.ui.components.layout.BackgroundFill.Mode.BACKDROP
        }
    }
}

data class TextureModifier(
    val region: TextureRegion,
    val scaleMode: ScaleMode = ScaleMode.FIT,
    val tint: Color = Color.white
) : UIModifier.Element {
    override fun applyTo(node: UINode) {
        (node as? LayoutNode)?.ensureVisuals()?.texture(region, scaleMode, tint)
    }
}

data class OpacityModifier(val opacity: Float) : UIModifier.Element {
    override fun applyTo(node: UINode) {
        (node as? LayoutNode)?.ensureVisuals()?.let { it.opacity = opacity }
    }
}

data class ClipModifier(val clip: Boolean) : UIModifier.Element {
    override fun applyTo(node: UINode) {
        node.clip = clip
    }
}

data class ClickableModifier(
    val onClick: () -> Unit,
    val onPressStateChanged: ((Boolean) -> Unit)? = null
) : UIModifier.Element {
    override fun applyTo(node: UINode) {
        node.onClick = onClick
        if (onPressStateChanged != null) {
            val prevDown = node.onPointerDown
            val prevUp = node.onPointerUp
            node.onPointerDown = {
                prevDown?.invoke(it)
                onPressStateChanged.invoke(true)
            }
            node.onPointerUp = {
                prevUp?.invoke(it)
                onPressStateChanged.invoke(false)
            }
        }
    }
}

data class HoverableModifier(val onHover: (Boolean) -> Unit) : UIModifier.Element {
    override fun applyTo(node: UINode) {
        node.onHover = onHover
    }
}

data class PointerDownModifier(val onDown: (PointerEvent) -> Unit) : UIModifier.Element {
    override fun applyTo(node: UINode) {
        node.onPointerDown = onDown
    }
}

data class PointerUpModifier(val onUp: (PointerEvent) -> Unit) : UIModifier.Element {
    override fun applyTo(node: UINode) {
        node.onPointerUp = onUp
    }
}

data class PointerDragModifier(val onDrag: (PointerEvent) -> Unit) : UIModifier.Element {
    override fun applyTo(node: UINode) {
        node.onPointerDrag = onDrag
    }
}

data class TouchableModifier(val touchable: Boolean) : UIModifier.Element {
    override fun applyTo(node: UINode) {
        node.touchable = touchable
    }
}

class StyleModifier(val block: BoxVisuals.() -> Unit) : UIModifier.Element {
    override fun applyTo(node: UINode) {
        (node as? LayoutNode)?.ensureVisuals()?.apply(block)
    }
}

class TextStyleModifier(val block: TextVisuals.() -> Unit) : UIModifier.Element {
    override fun applyTo(node: UINode) {
        (node as? TextNode)?.textVisuals?.apply(block)
    }
}

class CustomModifier(val block: (UINode) -> Unit) : UIModifier.Element {
    override fun applyTo(node: UINode) = block(node)
}

// =========================================================================
// II. Fluent Modifier Extension Builders
// =========================================================================

/** Sets equal inward padding on all 4 sides of the node. */
fun UIModifier.pad(all: Float): UIModifier = then(PaddingModifier(all, all, all, all))

/** Sets inward padding on horizontal and vertical axes. */
fun UIModifier.pad(horizontal: Float = 0f, vertical: Float = 0f): UIModifier =
    then(PaddingModifier(horizontal, vertical, horizontal, vertical))

/** Sets inward padding individually for each side. */
fun UIModifier.pad(left: Float = 0f, top: Float = 0f, right: Float = 0f, bottom: Float = 0f): UIModifier =
    then(PaddingModifier(left, top, right, bottom))

/** Sets equal outward margin on all 4 sides of the node. */
fun UIModifier.margin(all: Float): UIModifier = then(MarginModifier(all, all, all, all))

/** Sets outward margin on horizontal and vertical axes. */
fun UIModifier.margin(horizontal: Float = 0f, vertical: Float = 0f): UIModifier =
    then(MarginModifier(horizontal, vertical, horizontal, vertical))

/** Sets outward margin individually for each side. */
fun UIModifier.margin(left: Float = 0f, top: Float = 0f, right: Float = 0f, bottom: Float = 0f): UIModifier =
    then(MarginModifier(left, top, right, bottom))

/** Sets equal width and height dimensions in pixels. */
fun UIModifier.size(all: Float): UIModifier = then(SizeModifier(all, all))

/** Sets explicit desired dimensions (width, height) in pixels. */
fun UIModifier.size(width: Float, height: Float): UIModifier = then(SizeModifier(width, height))

/** Sets explicit desired width in pixels. */
fun UIModifier.width(width: Float): UIModifier = then(SizeModifier(width = width, height = -1f))

/** Sets explicit desired height in pixels. */
fun UIModifier.height(height: Float): UIModifier = then(SizeModifier(width = -1f, height = height))

/** Sets minimum dimensions constraint in pixels. */
fun UIModifier.minSize(minWidth: Float, minHeight: Float): UIModifier = then(MinSizeModifier(minWidth, minHeight))

/** Sets minimum width constraint in pixels. */
fun UIModifier.minWidth(minWidth: Float): UIModifier = then(MinSizeModifier(minWidth = minWidth, minHeight = -1f))

/** Sets minimum height constraint in pixels. */
fun UIModifier.minHeight(minHeight: Float): UIModifier = then(MinSizeModifier(minWidth = -1f, minHeight = minHeight))

/** Sets maximum dimensions constraint in pixels. */
fun UIModifier.maxSize(maxWidth: Float, maxHeight: Float): UIModifier = then(MaxSizeModifier(maxWidth, maxHeight))

/** Sets maximum width constraint in pixels. */
fun UIModifier.maxWidth(maxWidth: Float): UIModifier = then(MaxSizeModifier(maxWidth = maxWidth, maxHeight = -1f))

/** Sets maximum height constraint in pixels. */
fun UIModifier.maxHeight(maxHeight: Float): UIModifier = then(MaxSizeModifier(maxWidth = -1f, maxHeight = maxHeight))

/** Fills container slot in specified directions. */
fun UIModifier.fill(horizontal: Boolean = true, vertical: Boolean = true): UIModifier =
    then(FillModifier(horizontal, vertical))

/** Fills horizontal allocated slot. */
fun UIModifier.fillMaxWidth(): UIModifier = then(FillModifier(horizontal = true, vertical = false))

/** Fills vertical allocated slot. */
fun UIModifier.fillMaxHeight(): UIModifier = then(FillModifier(horizontal = false, vertical = true))

/** Fills both horizontal and vertical allocated slot. */
fun UIModifier.fillMaxSize(): UIModifier = then(FillModifier(horizontal = true, vertical = true))

/** Expands to consume available free space in container. */
fun UIModifier.expand(horizontal: Boolean = true, vertical: Boolean = true, ratio: Float = 1f): UIModifier =
    then(ExpandModifier(horizontal, vertical, ratio))

/** Flex weight ratio allocating available space proportionally in [Row] or [Column]. */
fun UIModifier.weight(ratio: Float): UIModifier = then(WeightModifier(ratio))

/** Sets screen anchoring preset. */
fun UIModifier.anchor(preset: LayoutPreset): UIModifier = then(AnchorPresetModifier(preset))

/** Anchors child to fill a horizontal fraction of the parent (0.0..1.0). */
fun UIModifier.anchorFillWidth(fraction: Float): UIModifier =
    then(AnchorSpanModifier(0f, 0f, fraction.coerceIn(0f, 1f), 1f))

/** Sets solid background color. */
fun UIModifier.background(color: Color): UIModifier = then(BackgroundModifier(color))

/** Sets progress fill fraction and active fill color within the SDF capsule. */
fun UIModifier.progress(fraction: Float, color: Color): UIModifier = then(ProgressModifier(fraction, color))

/** Sets uniform corner radius on all 4 corners (pixels). */
fun UIModifier.radius(all: Float): UIModifier = then(RadiusModifier(all, all, all, all))

/** Shorthand alias for [radius]. */
fun UIModifier.cornerRadius(all: Float): UIModifier = radius(all)

/** Sets corner radius individually for each corner (pixels). */
fun UIModifier.radius(topLeft: Float, topRight: Float, bottomRight: Float, bottomLeft: Float): UIModifier =
    then(RadiusModifier(topLeft, topRight, bottomRight, bottomLeft))

/** Sets border stroke styling. */
fun UIModifier.border(
    width: Float,
    color: Color = Color.white,
    style: org.mdt.ui.components.layout.Border.Style = org.mdt.ui.components.layout.Border.Style.SOLID
): UIModifier = then(BorderModifier(width, color, style))

/** Sets outer drop shadow. */
fun UIModifier.shadow(
    color: Color = Pal.shadow,
    offsetX: Float = 0f,
    offsetY: Float = 0f,
    blur: Float = 8f,
    spread: Float = 4f
): UIModifier = then(ShadowModifier(color, offsetX, offsetY, blur, spread))

/** Sets outer glow effect. */
fun UIModifier.glow(color: Color, spread: Float = 6f, blur: Float = 12f): UIModifier =
    then(GlowModifier(color, spread, blur))

/** Enables and configures 2-pass Gaussian backdrop blur. */
fun UIModifier.backdrop(
    blur: Boolean = true,
    blurRadius: Float = 12f,
    weight: Float = 0.8f,
    blend: Float = 0.8f,
    tint: Color = Color.white,
    iterations: Int = 2
): UIModifier = then(BackdropModifier(blur, blurRadius, weight, blend, tint, iterations))

/** Sets texture fill with [ScaleMode] on [LayoutNode]. */
fun UIModifier.texture(
    region: TextureRegion,
    scaleMode: ScaleMode = ScaleMode.FIT,
    tint: Color = Color.white
): UIModifier = then(TextureModifier(region, scaleMode, tint))

/** Sets visual opacity (0.0f = fully transparent, 1.0f = fully opaque). */
fun UIModifier.opacity(value: Float): UIModifier = then(OpacityModifier(value))

/** Clips child content to this node's rectangular bounding box. */
fun UIModifier.clip(clip: Boolean = true): UIModifier = then(ClipModifier(clip))

/** Shorthand alias for [clip]. */
fun UIModifier.clipToBounds(): UIModifier = clip(true)

/** Attaches click action with optional press state callback. */
fun UIModifier.clickable(
    onPressStateChanged: ((Boolean) -> Unit)? = null,
    onClick: () -> Unit
): UIModifier = then(ClickableModifier(onClick, onPressStateChanged))

/** Attaches simple click callback. */
fun UIModifier.onClick(block: () -> Unit): UIModifier = clickable(onClick = block)

/** Attaches hover listener. */
fun UIModifier.hoverable(onHover: (Boolean) -> Unit): UIModifier = then(HoverableModifier(onHover))

/** Attaches hover state change listener. */
fun UIModifier.onHover(block: (Boolean) -> Unit): UIModifier = hoverable(block)

/** Attaches pointer down listener. */
fun UIModifier.onPointerDown(block: (PointerEvent) -> Unit): UIModifier = then(PointerDownModifier(block))

/** Attaches pointer up listener. */
fun UIModifier.onPointerUp(block: (PointerEvent) -> Unit): UIModifier = then(PointerUpModifier(block))

/** Attaches pointer drag listener. */
fun UIModifier.onPointerDrag(block: (PointerEvent) -> Unit): UIModifier = then(PointerDragModifier(block))

/** Toggles touchability/hit-testability of the node. */
fun UIModifier.touchable(touchable: Boolean): UIModifier = then(TouchableModifier(touchable))

/** Inline style block allowing direct configuration of [BoxVisuals] properties. */
fun UIModifier.style(block: BoxVisuals.() -> Unit): UIModifier = then(StyleModifier(block))

/** Inline typography style block allowing direct configuration of [TextVisuals] properties. */
fun UIModifier.textStyle(block: TextVisuals.() -> Unit): UIModifier = then(TextStyleModifier(block))

/** Appends a custom lambda mutation onto the modifier chain. */
fun UIModifier.custom(block: (UINode) -> Unit): UIModifier = then(CustomModifier(block))
