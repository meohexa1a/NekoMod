@file:Suppress("unused")

package org.mdt.core.ui.compose

import arc.graphics.Color
import arc.graphics.g2d.TextureRegion
import mindustry.graphics.Pal
import org.mdt.core.ui.PointerEvent
import org.mdt.core.ui.UINode
import org.mdt.ui.components.layout.BoxVisuals
import org.mdt.ui.components.layout.LayoutNode
import org.mdt.ui.components.layout.ScaleMode
import org.mdt.ui.components.text.TextNode
import org.mdt.ui.components.text.TextVisuals
import org.mdt.core.ui.layout.LayoutPreset
import org.mdt.core.ui.layout.SizeFlags

/**
 * ## UIModifier
 *
 * Immutable, type-safe modifier chain representing layout rules, visual styles,
 * and input gestures applied to virtual [UINode] instances.
 *
 * Designed with Typed Modifier Elements for Zero-GC execution and fast recomposition skipping.
 *
 * See: docs/compose-dsl/compose_dsl_en.md
 */
interface UIModifier {

    /** Applies this modifier's configuration onto the target [UINode]. */
    fun applyTo(node: UINode)

    /** Chains this modifier with another [UIModifier]. */
    fun then(other: UIModifier): UIModifier = if (other === None) this else CombinedModifier(this, other)

    /**
     * Single atomic modifier element with structural equality.
     */
    interface Element : UIModifier

    /** Empty modifier representing no-op. */
    companion object None : UIModifier {
        override fun applyTo(node: UINode) {}
        override fun toString(): String = "Modifier.None"
    }
}

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
// TYPED MODIFIER ELEMENTS (Zero-GC, Value-based, Compose Recomposition Safe)
// =========================================================================

data class PaddingModifier(val l: Float, val t: Float, val r: Float, val b: Float) : UIModifier.Element {
    override fun applyTo(node: UINode) = node.pad(l, t, r, b)
}

data class MarginModifier(val l: Float, val t: Float, val r: Float, val b: Float) : UIModifier.Element {
    override fun applyTo(node: UINode) = node.margin(l, t, r, b)
}

data class SizeModifier(val w: Float = -1f, val h: Float = -1f) : UIModifier.Element {
    override fun applyTo(node: UINode) {
        if (w >= 0f) node.width = w
        if (h >= 0f) node.height = h
    }
}

data class MinSizeModifier(val minW: Float = -1f, val minH: Float = -1f) : UIModifier.Element {
    override fun applyTo(node: UINode) {
        if (minW >= 0f) node.minWidth = minW
        if (minH >= 0f) node.minHeight = minH
    }
}

data class MaxSizeModifier(val maxW: Float = -1f, val maxH: Float = -1f) : UIModifier.Element {
    override fun applyTo(node: UINode) {
        if (maxW >= 0f) node.maxWidth = maxW
        if (maxH >= 0f) node.maxHeight = maxH
    }
}

data class FillModifier(val h: Boolean = true, val v: Boolean = true) : UIModifier.Element {
    override fun applyTo(node: UINode) {
        if (h) node.sizeFlagsHorizontal = node.sizeFlagsHorizontal or SizeFlags.FILL
        if (v) node.sizeFlagsVertical = node.sizeFlagsVertical or SizeFlags.FILL
    }
}

data class ExpandModifier(val h: Boolean = true, val v: Boolean = true, val ratio: Float = 1f) : UIModifier.Element {
    override fun applyTo(node: UINode) {
        if (h) node.sizeFlagsHorizontal = node.sizeFlagsHorizontal or SizeFlags.EXPAND
        if (v) node.sizeFlagsVertical = node.sizeFlagsVertical or SizeFlags.EXPAND
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
            it.fillColor.set(color)
            it.backgroundMode = BoxVisuals.BackgroundMode.COLOR
        }
    }
}

data class RadiusModifier(val tl: Float, val tr: Float, val br: Float, val bl: Float) : UIModifier.Element {
    override fun applyTo(node: UINode) {
        (node as? LayoutNode)?.ensureVisuals()?.radius(tl, tr, br, bl)
    }
}

data class BorderModifier(
    val width: Float,
    val color: Color,
    val style: BoxVisuals.BorderStyle = BoxVisuals.BorderStyle.SOLID
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
            vis.blur = blur
            vis.blurRadius = blurRadius
            vis.backdropWeight = weight
            vis.backdropBlend = blend
            vis.backdropTint.set(tint)
            vis.blurIterations = iterations
            vis.backgroundMode = BoxVisuals.BackgroundMode.BACKDROP
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
// FLUENT MODIFIER EXTENSION BUILDERS
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
fun UIModifier.size(w: Float, h: Float): UIModifier = then(SizeModifier(w, h))

/** Sets explicit desired width in pixels. */
fun UIModifier.width(w: Float): UIModifier = then(SizeModifier(w = w, h = -1f))

/** Sets explicit desired height in pixels. */
fun UIModifier.height(h: Float): UIModifier = then(SizeModifier(w = -1f, h = h))

/** Sets minimum dimensions constraint in pixels. */
fun UIModifier.minSize(w: Float, h: Float): UIModifier = then(MinSizeModifier(w, h))

/** Sets minimum width constraint in pixels. */
fun UIModifier.minWidth(w: Float): UIModifier = then(MinSizeModifier(minW = w, minH = -1f))

/** Sets minimum height constraint in pixels. */
fun UIModifier.minHeight(h: Float): UIModifier = then(MinSizeModifier(minW = -1f, minH = h))

/** Sets maximum dimensions constraint in pixels. */
fun UIModifier.maxSize(w: Float, h: Float): UIModifier = then(MaxSizeModifier(w, h))

/** Sets maximum width constraint in pixels. */
fun UIModifier.maxWidth(w: Float): UIModifier = then(MaxSizeModifier(maxW = w, maxH = -1f))

/** Sets maximum height constraint in pixels. */
fun UIModifier.maxHeight(h: Float): UIModifier = then(MaxSizeModifier(maxW = -1f, maxH = h))

/** Fills container slot in specified directions. */
fun UIModifier.fill(h: Boolean = true, v: Boolean = true): UIModifier = then(FillModifier(h, v))

/** Fills horizontal allocated slot. */
fun UIModifier.fillMaxWidth(): UIModifier = then(FillModifier(h = true, v = false))

/** Fills vertical allocated slot. */
fun UIModifier.fillMaxHeight(): UIModifier = then(FillModifier(h = false, v = true))

/** Fills both horizontal and vertical allocated slot. */
fun UIModifier.fillMaxSize(): UIModifier = then(FillModifier(h = true, v = true))

/** Expands to consume available free space in container. */
fun UIModifier.expand(h: Boolean = true, v: Boolean = true, ratio: Float = 1f): UIModifier =
    then(ExpandModifier(h, v, ratio))

/** Flex weight ratio allocating available space proportionally in [Row] or [Column]. */
fun UIModifier.weight(ratio: Float): UIModifier = then(WeightModifier(ratio))

/** Sets screen anchoring preset. */
fun UIModifier.anchor(preset: LayoutPreset): UIModifier = then(AnchorPresetModifier(preset))

/** Anchors child to fill a horizontal fraction of the parent (0.0..1.0). */
fun UIModifier.anchorFillWidth(fraction: Float): UIModifier =
    then(AnchorSpanModifier(0f, 0f, fraction.coerceIn(0f, 1f), 1f))

/** Sets solid background color. */
fun UIModifier.background(color: Color): UIModifier = then(BackgroundModifier(color))

/** Sets uniform corner radius on all 4 corners (pixels). */
fun UIModifier.radius(all: Float): UIModifier = then(RadiusModifier(all, all, all, all))

/** Shorthand alias for [radius]. */
fun UIModifier.cornerRadius(all: Float): UIModifier = radius(all)

/** Sets corner radius individually for each corner (pixels). */
fun UIModifier.radius(tl: Float, tr: Float, br: Float, bl: Float): UIModifier =
    then(RadiusModifier(tl, tr, br, bl))

/** Sets border stroke styling. */
fun UIModifier.border(
    width: Float,
    color: Color = Color.white,
    style: BoxVisuals.BorderStyle = BoxVisuals.BorderStyle.SOLID
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
