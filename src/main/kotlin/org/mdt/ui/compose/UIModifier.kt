@file:Suppress("unused")

package org.mdt.ui.compose

import arc.graphics.Color
import mindustry.graphics.Pal
import org.mdt.ui.core.PointerEvent
import org.mdt.ui.core.UINode
import org.mdt.ui.layout.LayoutPreset
import org.mdt.ui.layout.SizeFlags
import org.mdt.ui.components.layout.BoxVisuals
import org.mdt.ui.components.layout.LayoutNode

/**
 * ## UIModifier
 *
 * Immutable, chainable modifier interface representing layout parameters,
 * visual styles, and event listeners applied to virtual [UINode] instances.
 *
 * See: docs/compose-dsl/compose_dsl_en.md
 */
interface UIModifier {
    /** Applies this modifier's configuration onto the target [UINode]. */
    fun applyTo(node: UINode)

    /** Chains this modifier with another [UIModifier]. */
    fun then(other: UIModifier): UIModifier {
        return if (other === None) this else CombinedModifier(this, other)
    }

    /** Empty modifier representing no-op. */
    companion object None : UIModifier {
        override fun applyTo(node: UINode) {}
    }
}

/** Chains two modifiers sequentially. */
class CombinedModifier(private val outer: UIModifier, private val inner: UIModifier) : UIModifier {
    override fun applyTo(node: UINode) {
        outer.applyTo(node)
        inner.applyTo(node)
    }
}

/** Custom modifier wrapping an arbitrary lambda mutation. */
class CustomModifier(private val block: (UINode) -> Unit) : UIModifier {
    override fun applyTo(node: UINode) {
        block(node)
    }
}

// ==========================================
// FLUENT MODIFIER EXTENSIONS
// ==========================================

/** Appends a custom lambda mutation onto the modifier chain. */
fun UIModifier.custom(block: (UINode) -> Unit): UIModifier = then(CustomModifier(block))

// --- PADDING (Inward Spacing) ---

/** Sets equal inward padding on all 4 sides of the node. */
fun UIModifier.pad(all: Float): UIModifier = then(CustomModifier {
    it.pad(all)
})

/** Sets inward padding on horizontal and vertical axes. */
fun UIModifier.pad(horizontal: Float = 0f, vertical: Float = 0f): UIModifier = then(CustomModifier {
    it.pad(horizontal, vertical)
})

/** Sets inward padding individually for each side. */
fun UIModifier.pad(left: Float = 0f, top: Float = 0f, right: Float = 0f, bottom: Float = 0f): UIModifier = then(CustomModifier {
    it.pad(left, top, right, bottom)
})

// --- MARGIN (Outward Spacing) ---

/** Sets equal outward margin on all 4 sides of the node. */
fun UIModifier.margin(all: Float): UIModifier = then(CustomModifier {
    it.margin(all)
})

/** Sets outward margin on horizontal and vertical axes. */
fun UIModifier.margin(horizontal: Float = 0f, vertical: Float = 0f): UIModifier = then(CustomModifier {
    it.margin(horizontal, vertical)
})

/** Sets outward margin individually for each side. */
fun UIModifier.margin(left: Float = 0f, top: Float = 0f, right: Float = 0f, bottom: Float = 0f): UIModifier = then(CustomModifier {
    it.margin(left, top, right, bottom)
})

// --- SIZING & CONSTRAINTS ---

/** Sets explicit desired dimensions (width, height) in pixels. */
fun UIModifier.size(w: Float, h: Float): UIModifier = then(CustomModifier {
    it.width = w
    it.height = h
})

/** Sets explicit desired width in pixels. */
fun UIModifier.width(w: Float): UIModifier = then(CustomModifier {
    it.width = w
})

/** Sets explicit desired height in pixels. */
fun UIModifier.height(h: Float): UIModifier = then(CustomModifier {
    it.height = h
})

/** Sets minimum dimensions constraint in pixels. */
fun UIModifier.minSize(w: Float, h: Float): UIModifier = then(CustomModifier {
    it.minWidth = w
    it.minHeight = h
})

/** Sets minimum width constraint in pixels. */
fun UIModifier.minWidth(w: Float): UIModifier = then(CustomModifier {
    it.minWidth = w
})

/** Sets minimum height constraint in pixels. */
fun UIModifier.minHeight(h: Float): UIModifier = then(CustomModifier {
    it.minHeight = h
})

/** Sets maximum dimensions constraint in pixels. */
fun UIModifier.maxSize(w: Float, h: Float): UIModifier = then(CustomModifier {
    it.maxWidth = w
    it.maxHeight = h
})

/** Locks node to rigid fixed dimensions and disables auto container expansion. */
fun UIModifier.fixed(w: Float = -1f, h: Float = -1f): UIModifier = then(CustomModifier {
    if (w >= 0f) { it.width = w; it.minWidth = w; it.maxWidth = w; it.sizeFlagsHorizontal = SizeFlags.SHRINK_BEGIN }
    if (h >= 0f) { it.height = h; it.minHeight = h; it.maxHeight = h; it.sizeFlagsVertical = SizeFlags.SHRINK_BEGIN }
})

// --- SIZE FLAGS & ALIGNMENT ---

/** Fills allocated container slot. */
fun UIModifier.fill(h: Boolean = true, v: Boolean = true): UIModifier = then(CustomModifier {
    if (h) it.sizeFlagsHorizontal = it.sizeFlagsHorizontal or SizeFlags.FILL
    if (v) it.sizeFlagsVertical = it.sizeFlagsVertical or SizeFlags.FILL
})

/** Fills horizontal allocated slot. */
fun UIModifier.fillMaxWidth(): UIModifier = fill(h = true, v = false)

/** Fills vertical allocated slot. */
fun UIModifier.fillMaxHeight(): UIModifier = fill(h = false, v = true)

/** Expands to consume available free space in container. */
fun UIModifier.expand(h: Boolean = true, v: Boolean = true): UIModifier = then(CustomModifier {
    if (h) it.sizeFlagsHorizontal = it.sizeFlagsHorizontal or SizeFlags.EXPAND
    if (v) it.sizeFlagsVertical = it.sizeFlagsVertical or SizeFlags.EXPAND
})

/** Expands to consume free space and fills the entire allocated slot. */
fun UIModifier.expandFill(h: Boolean = true, v: Boolean = true): UIModifier = then(CustomModifier {
    if (h) it.sizeFlagsHorizontal = it.sizeFlagsHorizontal or SizeFlags.EXPAND_FILL
    if (v) it.sizeFlagsVertical = it.sizeFlagsVertical or SizeFlags.EXPAND_FILL
})

/** Sets stretch weight ratio for container free space distribution. */
fun UIModifier.weight(ratio: Float): UIModifier = then(CustomModifier {
    it.sizeFlagsHorizontal = it.sizeFlagsHorizontal or SizeFlags.EXPAND_FILL
    it.sizeFlagsVertical = it.sizeFlagsVertical or SizeFlags.EXPAND_FILL
    it.stretchRatio = ratio
})

/** Sets screen anchoring preset (e.g. [LayoutPreset.CENTER], [LayoutPreset.TOP_LEFT]). */
fun UIModifier.anchor(preset: LayoutPreset): UIModifier = then(CustomModifier {
    it.anchorData.setPreset(preset)
})

// --- VISUALS & STYLING ---

/** Sets solid background color. */
fun UIModifier.background(color: Color): UIModifier = then(CustomModifier {
    (it as? LayoutNode)?.ensureVisuals()?.let { vis ->
        vis.fillColor.set(color)
        vis.backgroundMode = BoxVisuals.BackgroundMode.COLOR
    }
})

/** Sets uniform corner radius on all 4 corners (pixels). */
fun UIModifier.radius(all: Float): UIModifier = then(CustomModifier {
    (it as? LayoutNode)?.ensureVisuals()?.radius(all)
})

/** Shorthand alias for [radius]. */
fun UIModifier.cornerRadius(all: Float): UIModifier = radius(all)

/** Sets corner radius individually for each corner (pixels). */
fun UIModifier.radius(tl: Float, tr: Float, br: Float, bl: Float): UIModifier = then(CustomModifier {
    (it as? LayoutNode)?.ensureVisuals()?.radius(tl, tr, br, bl)
})

/** Sets border stroke styling. */
fun UIModifier.border(
    width: Float,
    color: Color = Color.white,
    style: BoxVisuals.BorderStyle = BoxVisuals.BorderStyle.SOLID
): UIModifier = then(CustomModifier {
    (it as? LayoutNode)?.ensureVisuals()?.border(width, color, style)
})

/** Sets outer drop shadow. */
fun UIModifier.shadow(
    color: Color = Pal.shadow,
    offsetX: Float = 0f,
    offsetY: Float = 0f,
    blur: Float = 8f,
    spread: Float = 4f
): UIModifier = then(CustomModifier {
    (it as? LayoutNode)?.ensureVisuals()?.shadow(color, offsetX, offsetY, blur, spread)
})

/** Sets outer glow effect. */
fun UIModifier.glow(color: Color, spread: Float = 6f, blur: Float = 12f): UIModifier = then(CustomModifier {
    (it as? LayoutNode)?.ensureVisuals()?.glow(color, spread, blur)
})

/**
 * Enables and configures parameterized 2-pass Gaussian backdrop blur.
 */
fun UIModifier.backdrop(
    blur: Boolean = true,
    blurRadius: Float = 12f,
    weight: Float = 0.8f,
    blend: Float = 0.8f,
    tint: Color = Color.white,
    iterations: Int = 2
): UIModifier = then(CustomModifier {
    (it as? LayoutNode)?.ensureVisuals()?.let { vis ->
        vis.blur = blur
        vis.blurRadius = blurRadius
        vis.backdropWeight = weight
        vis.backdropBlend = blend
        vis.backdropTint.set(tint)
        vis.blurIterations = iterations
        vis.backgroundMode = BoxVisuals.BackgroundMode.BACKDROP
    }
})

/** Sets visual opacity (0.0f = fully transparent, 1.0f = fully opaque). */
fun UIModifier.opacity(value: Float): UIModifier = then(CustomModifier {
    (it as? LayoutNode)?.ensureVisuals()?.opacity = value
})

/** Clips child content to this node's rectangular bounding box. */
fun UIModifier.clip(clip: Boolean = true): UIModifier = then(CustomModifier {
    it.clip = clip
})

/** Shorthand alias for [clip]. */
fun UIModifier.clipToBounds(): UIModifier = clip(true)

// --- INTERACTIVITY ---

/** Attaches click action with optional press state callback. */
fun UIModifier.clickable(
    onPressStateChanged: ((Boolean) -> Unit)? = null,
    onClick: () -> Unit
): UIModifier = then(CustomModifier { target ->
    target.onClick = onClick
    if (onPressStateChanged != null) {
        val prevDown = target.onPointerDown
        val prevUp = target.onPointerUp
        target.onPointerDown = {
            prevDown?.invoke(it)
            onPressStateChanged(true)
        }
        target.onPointerUp = {
            prevUp?.invoke(it)
            onPressStateChanged(false)
        }
    }
})

/** Attaches simple click callback. */
fun UIModifier.onClick(block: () -> Unit): UIModifier = clickable(onClick = block)

/** Attaches hover listener. */
fun UIModifier.hoverable(onHover: (Boolean) -> Unit): UIModifier = then(CustomModifier {
    it.onHover = onHover
})

/** Attaches double-click callback. */
fun UIModifier.onDoubleClick(block: () -> Unit): UIModifier = then(CustomModifier {
    it.onDoubleClick = block
})

/** Attaches hover state change listener. */
fun UIModifier.onHover(block: (Boolean) -> Unit): UIModifier = hoverable(block)

/** Attaches pointer down listener. */
fun UIModifier.onPointerDown(block: (PointerEvent) -> Unit): UIModifier = then(CustomModifier {
    it.onPointerDown = block
})

/** Attaches pointer up listener. */
fun UIModifier.onPointerUp(block: (PointerEvent) -> Unit): UIModifier = then(CustomModifier {
    it.onPointerUp = block
})

/** Toggles touchability/hit-testability of the node. */
fun UIModifier.touchable(touchable: Boolean): UIModifier = then(CustomModifier {
    it.touchable = touchable
})
