@file:Suppress("unused", "FunctionName")

package org.mdt.core.ui.compose

import arc.graphics.g2d.TextureRegion
import org.mdt.core.ui.UINode
import org.mdt.core.ui.graphics.Color
import org.mdt.core.ui.input.PointerEvent
import org.mdt.core.ui.input.ScrollEvent
import org.mdt.core.ui.layout.LayoutPreset
import org.mdt.core.ui.layout.SizeFlags
import org.mdt.ui.components.layout.LayoutNode

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

/**
 * ## CombinedModifier
 *
 * Chains two modifiers sequentially with structural equality support.
 *
 * See: docs/compose-dsl/compose_dsl_en.md
 */
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

// --- TYPED MODIFIER ELEMENTS (ZERO-GC, VALUE-BASED EQUALITY) ---

data class PaddingModifier(val left: Float, val top: Float, val right: Float, val bottom: Float) : UIModifier.Element {
    override fun applyTo(node: UINode) = node.pad(left, top, right, bottom)
}

data class MarginModifier(val left: Float, val top: Float, val right: Float, val bottom: Float) : UIModifier.Element {
    override fun applyTo(node: UINode) = node.margin(left, top, right, bottom)
}

data class SizeModifier(val width: Float = -1.0f, val height: Float = -1.0f) : UIModifier.Element {
    override fun applyTo(node: UINode) {
        if (width >= 0.0f) node.width = width
        if (height >= 0.0f) node.height = height
    }
}

data class MinSizeModifier(val minWidth: Float = -1.0f, val minHeight: Float = -1.0f) : UIModifier.Element {
    override fun applyTo(node: UINode) {
        if (minWidth >= 0.0f) node.minWidth = minWidth
        if (minHeight >= 0.0f) node.minHeight = minHeight
    }
}

data class MaxSizeModifier(val maxWidth: Float = -1.0f, val maxHeight: Float = -1.0f) : UIModifier.Element {
    override fun applyTo(node: UINode) {
        if (maxWidth >= 0.0f) node.maxWidth = maxWidth
        if (maxHeight >= 0.0f) node.maxHeight = maxHeight
    }
}

data class FillModifier(val horizontal: Boolean = true, val vertical: Boolean = true) : UIModifier.Element {
    override fun applyTo(node: UINode) {
        if (horizontal) node.sizeFlagsHorizontal = node.sizeFlagsHorizontal or SizeFlags.FILL
        if (vertical) node.sizeFlagsVertical = node.sizeFlagsVertical or SizeFlags.FILL
    }
}

data class ExpandModifier(val horizontal: Boolean = true, val vertical: Boolean = true, val ratio: Float = 1.0f) : UIModifier.Element {
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

data class BackgroundModifier(val color: Color) : UIModifier.Element {
    override fun applyTo(node: UINode) {
        (node as? LayoutNode)?.color = color
    }
}

data class RadiusModifier(val radius: Float) : UIModifier.Element {
    override fun applyTo(node: UINode) {
        (node as? LayoutNode)?.radius = radius
    }
}

data class BorderModifier(val width: Float, val color: Color) : UIModifier.Element {
    override fun applyTo(node: UINode) {
        (node as? LayoutNode)?.let {
            it.borderWidth = width
            it.borderColor = color
        }
    }
}

data class GlassModifier(val isGlass: Boolean = true) : UIModifier.Element {
    override fun applyTo(node: UINode) {
        (node as? LayoutNode)?.isGlass = isGlass
    }
}

data class TextureModifier(val region: TextureRegion) : UIModifier.Element {
    override fun applyTo(node: UINode) {
        (node as? LayoutNode)?.region = region
    }
}

data class ClipModifier(val clip: Boolean) : UIModifier.Element {
    override fun applyTo(node: UINode) {
        node.clip = clip
    }
}

data class HitTestBehaviorModifier(val behavior: org.mdt.core.ui.HitTestBehavior) : UIModifier.Element {
    override fun applyTo(node: UINode) {
        node.hitTestBehavior = behavior
    }
}

data class ClickableModifier(
    val onClick: () -> Unit,
    val onPressStateChanged: ((Boolean) -> Unit)? = null
) : UIModifier.Element {
    override fun applyTo(node: UINode) {
        node.onClick = onClick
        node.hitTestBehavior = org.mdt.core.ui.HitTestBehavior.OPAQUE
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

data class ScrollModifier(val onScroll: (ScrollEvent) -> Unit) : UIModifier.Element {
    override fun applyTo(node: UINode) {
        node.onScroll = onScroll
    }
}

data class TouchableModifier(val touchable: Boolean) : UIModifier.Element {
    override fun applyTo(node: UINode) {
        node.touchable = touchable
    }
}

data class KeyDownModifier(val onKeyDown: (arc.input.KeyCode) -> Boolean) : UIModifier.Element {
    override fun applyTo(node: UINode) {
        node.isFocusable = true
        node.onKeyDown = onKeyDown
    }
}

data class FocusableModifier(val focusable: Boolean) : UIModifier.Element {
    override fun applyTo(node: UINode) {
        node.isFocusable = focusable
    }
}

data class CursorModifier(val cursor: arc.Graphics.Cursor) : UIModifier.Element {
    override fun applyTo(node: UINode) {
        node.cursor = cursor
    }
}

// --- FLUENT MODIFIER EXTENSION BUILDERS ---

fun UIModifier.pad(all: Float): UIModifier = then(PaddingModifier(all, all, all, all))
fun UIModifier.pad(horizontal: Float = 0.0f, vertical: Float = 0.0f): UIModifier =
    then(PaddingModifier(horizontal, vertical, horizontal, vertical))
fun UIModifier.pad(left: Float = 0.0f, top: Float = 0.0f, right: Float = 0.0f, bottom: Float = 0.0f): UIModifier =
    then(PaddingModifier(left, top, right, bottom))

fun UIModifier.margin(all: Float): UIModifier = then(MarginModifier(all, all, all, all))
fun UIModifier.margin(horizontal: Float = 0.0f, vertical: Float = 0.0f): UIModifier =
    then(MarginModifier(horizontal, vertical, horizontal, vertical))
fun UIModifier.margin(left: Float = 0.0f, top: Float = 0.0f, right: Float = 0.0f, bottom: Float = 0.0f): UIModifier =
    then(MarginModifier(left, top, right, bottom))

fun UIModifier.size(all: Float): UIModifier = then(SizeModifier(all, all))
fun UIModifier.size(width: Float, height: Float): UIModifier = then(SizeModifier(width, height))
fun UIModifier.width(width: Float): UIModifier = then(SizeModifier(width = width, height = -1.0f))
fun UIModifier.height(height: Float): UIModifier = then(SizeModifier(width = -1.0f, height = height))

fun UIModifier.minSize(minWidth: Float, minHeight: Float): UIModifier = then(MinSizeModifier(minWidth, minHeight))
fun UIModifier.minWidth(minWidth: Float): UIModifier = then(MinSizeModifier(minWidth = minWidth, minHeight = -1.0f))
fun UIModifier.minHeight(minHeight: Float): UIModifier = then(MinSizeModifier(minWidth = -1.0f, minHeight = minHeight))

fun UIModifier.maxSize(maxWidth: Float, maxHeight: Float): UIModifier = then(MaxSizeModifier(maxWidth, maxHeight))
fun UIModifier.maxWidth(maxWidth: Float): UIModifier = then(MaxSizeModifier(maxWidth = maxWidth, maxHeight = -1.0f))
fun UIModifier.maxHeight(maxHeight: Float): UIModifier = then(MaxSizeModifier(maxWidth = -1.0f, maxHeight = maxHeight))

fun UIModifier.fill(horizontal: Boolean = true, vertical: Boolean = true): UIModifier = then(FillModifier(horizontal, vertical))
fun UIModifier.fillMaxWidth(): UIModifier = then(FillModifier(horizontal = true, vertical = false))
fun UIModifier.fillMaxHeight(): UIModifier = then(FillModifier(horizontal = false, vertical = true))
fun UIModifier.fillMaxSize(): UIModifier = then(FillModifier(horizontal = true, vertical = true))

fun UIModifier.expand(horizontal: Boolean = true, vertical: Boolean = true, ratio: Float = 1.0f): UIModifier =
    then(ExpandModifier(horizontal, vertical, ratio))
fun UIModifier.weight(ratio: Float): UIModifier = then(WeightModifier(ratio))

fun UIModifier.anchor(preset: LayoutPreset): UIModifier = then(AnchorPresetModifier(preset))

fun UIModifier.background(color: Color): UIModifier = then(BackgroundModifier(color))
fun UIModifier.radius(radius: Float): UIModifier = then(RadiusModifier(radius))
fun UIModifier.cornerRadius(radius: Float): UIModifier = radius(radius)
fun UIModifier.border(width: Float, color: Color = Color.White): UIModifier = then(BorderModifier(width, color))
fun UIModifier.glass(isGlass: Boolean = true): UIModifier = then(GlassModifier(isGlass))
fun UIModifier.texture(region: TextureRegion): UIModifier = then(TextureModifier(region))

fun UIModifier.clip(clip: Boolean = true): UIModifier = then(ClipModifier(clip))
fun UIModifier.clipToBounds(): UIModifier = clip(true)

fun UIModifier.clickable(
    onPressStateChanged: ((Boolean) -> Unit)? = null,
    onClick: () -> Unit
): UIModifier = then(ClickableModifier(onClick, onPressStateChanged))

fun UIModifier.onClick(block: () -> Unit): UIModifier = clickable(onClick = block)
fun UIModifier.hoverable(onHover: (Boolean) -> Unit): UIModifier = then(HoverableModifier(onHover))
fun UIModifier.onHover(block: (Boolean) -> Unit): UIModifier = hoverable(block)

fun UIModifier.onPointerDown(block: (PointerEvent) -> Unit): UIModifier = then(PointerDownModifier(block))
fun UIModifier.onPointerUp(block: (PointerEvent) -> Unit): UIModifier = then(PointerUpModifier(block))
fun UIModifier.onPointerDrag(block: (PointerEvent) -> Unit): UIModifier = then(PointerDragModifier(block))
fun UIModifier.onScroll(block: (ScrollEvent) -> Unit): UIModifier = then(ScrollModifier(block))

fun UIModifier.touchable(touchable: Boolean): UIModifier = then(TouchableModifier(touchable))
fun UIModifier.onKeyDown(block: (arc.input.KeyCode) -> Boolean): UIModifier = then(KeyDownModifier(block))
fun UIModifier.focusable(focusable: Boolean = true): UIModifier = then(FocusableModifier(focusable))
fun UIModifier.cursor(cursor: arc.Graphics.Cursor): UIModifier = then(CursorModifier(cursor))
fun UIModifier.hitTestBehavior(behavior: org.mdt.core.ui.HitTestBehavior): UIModifier = then(HitTestBehaviorModifier(behavior))
fun UIModifier.opaque(): UIModifier = hitTestBehavior(org.mdt.core.ui.HitTestBehavior.OPAQUE)
fun UIModifier.translucent(): UIModifier = hitTestBehavior(org.mdt.core.ui.HitTestBehavior.TRANSLUCENT)
