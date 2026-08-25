package org.mdt.ui.compose

import arc.graphics.Color
import mindustry.graphics.Pal
import org.mdt.ui.core.PointerEvent
import org.mdt.ui.core.UINode
import org.mdt.ui.layout.LayoutPreset
import org.mdt.ui.layout.SizeFlags
import org.mdt.ui.widgets.BoxNode
import org.mdt.ui.widgets.BoxVisuals

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
    if (it is BoxNode) {
        it.visuals.fillColor.set(color)
        it.visuals.backgroundMode = BoxVisuals.BackgroundMode.SOLID
    }
})

/** Sets uniform corner radius on all 4 corners (pixels). */
fun UIModifier.radius(all: Double): UIModifier = then(CustomModifier {
    if (it is BoxNode) it.visuals.radius(all)
})

/** Sets corner radius individually for each corner (pixels). */
fun UIModifier.radius(tl: Double, tr: Double, br: Double, bl: Double): UIModifier = then(CustomModifier {
    if (it is BoxNode) it.visuals.radius(tl, tr, br, bl)
})

/** Sets border stroke styling. */
fun UIModifier.border(
    width: Double,
    color: Color = Color.white,
    style: BoxVisuals.BorderStyle = BoxVisuals.BorderStyle.SOLID
): UIModifier = then(CustomModifier {
    if (it is BoxNode) it.visuals.border(width, color, style)
})

/** Sets inner drop shadow. */
fun UIModifier.shadow(color: Color = Pal.shadow, spread: Double = 4.0, blur: Double = 8.0): UIModifier = then(CustomModifier {
    if (it is BoxNode) it.visuals.shadow(color, spread, blur)
})

/** Sets outer glow effect. */
fun UIModifier.glow(color: Color, spread: Double = 6.0, blur: Double = 12.0): UIModifier = then(CustomModifier {
    if (it is BoxNode) it.visuals.glow(color, spread, blur)
})

/**
 * Enables and configures parameterized 2-pass Gaussian backdrop blur.
 *
 * @param blur Whether backdrop blur is active.
 * @param radius Kernel blur radius in pixels (e.g. 2f for subtle, 16f for frosted glass).
 * @param weight Alpha weight of the blurred texture (0.0 to 1.0).
 * @param blend Blending curve exponent.
 * @param tint Tint color applied to the blurred backdrop texture.
 * @param iterations Number of ping-pong blur passes (1 to 4).
 */
fun UIModifier.backdrop(
    blur: Boolean = true,
    radius: Float = 4f,
    weight: Double = 0.8,
    blend: Double = 0.8,
    tint: Color = Color.white,
    iterations: Int = 2
): UIModifier = then(CustomModifier {
    if (it is BoxNode) it.visuals.backdrop(blur, radius, weight, blend, tint, iterations)
})

/** Sets visual opacity (0.0 = fully transparent, 1.0 = fully opaque). */
fun UIModifier.opacity(value: Double): UIModifier = then(CustomModifier {
    if (it is BoxNode) it.visuals.opacity = value
})

// --- INTERACTIVITY ---

/** Attaches single-click callback. */
fun UIModifier.onClick(block: () -> Unit): UIModifier = then(CustomModifier {
    it.onClick = block
})

/** Attaches double-click callback. */
fun UIModifier.onDoubleClick(block: () -> Unit): UIModifier = then(CustomModifier {
    it.onDoubleClick = block
})

/** Attaches hover state change listener. */
fun UIModifier.onHover(block: (Boolean) -> Unit): UIModifier = then(CustomModifier {
    it.onHover = block
})

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
