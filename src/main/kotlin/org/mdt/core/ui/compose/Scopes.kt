package org.mdt.core.ui.compose

import org.mdt.core.ui.UINode
import org.mdt.core.ui.layout.Alignment
import org.mdt.core.ui.layout.HorizontalAlign
import org.mdt.core.ui.layout.SizeFlags
import org.mdt.core.ui.layout.VerticalAlign

// =========================================================================
// SCOPE-SPECIFIC TYPED MODIFIERS
// =========================================================================

data class BoxAlignModifier(val alignment: Alignment) : UIModifier.Element {
    override fun applyTo(node: UINode) {
        when (alignment.horizontal) {
            HorizontalAlign.START -> node.sizeFlagsHorizontal = SizeFlags.SHRINK_BEGIN
            HorizontalAlign.CENTER -> node.sizeFlagsHorizontal = SizeFlags.SHRINK_CENTER
            HorizontalAlign.END -> node.sizeFlagsHorizontal = SizeFlags.SHRINK_END
            HorizontalAlign.FILL -> node.sizeFlagsHorizontal = SizeFlags.FILL
        }
        when (alignment.vertical) {
            VerticalAlign.TOP -> node.sizeFlagsVertical = SizeFlags.SHRINK_BEGIN
            VerticalAlign.CENTER -> node.sizeFlagsVertical = SizeFlags.SHRINK_CENTER
            VerticalAlign.BOTTOM -> node.sizeFlagsVertical = SizeFlags.SHRINK_END
            VerticalAlign.FILL -> node.sizeFlagsVertical = SizeFlags.FILL
        }
    }
}

data class RowAlignModifier(val alignment: VerticalAlign) : UIModifier.Element {
    override fun applyTo(node: UINode) {
        when (alignment) {
            VerticalAlign.TOP -> node.sizeFlagsVertical = SizeFlags.SHRINK_BEGIN
            VerticalAlign.CENTER -> node.sizeFlagsVertical = SizeFlags.SHRINK_CENTER
            VerticalAlign.BOTTOM -> node.sizeFlagsVertical = SizeFlags.SHRINK_END
            VerticalAlign.FILL -> node.sizeFlagsVertical = SizeFlags.FILL
        }
    }
}

data class ColumnAlignModifier(val alignment: HorizontalAlign) : UIModifier.Element {
    override fun applyTo(node: UINode) {
        when (alignment) {
            HorizontalAlign.START -> node.sizeFlagsHorizontal = SizeFlags.SHRINK_BEGIN
            HorizontalAlign.CENTER -> node.sizeFlagsHorizontal = SizeFlags.SHRINK_CENTER
            HorizontalAlign.END -> node.sizeFlagsHorizontal = SizeFlags.SHRINK_END
            HorizontalAlign.FILL -> node.sizeFlagsHorizontal = SizeFlags.FILL
        }
    }
}

// =========================================================================
// COMPOSE SCOPE INTERFACES
// =========================================================================

@UIDslMarker
interface BoxScope {
    fun UIModifier.align(alignment: Alignment): UIModifier = then(BoxAlignModifier(alignment))

    companion object Instance : BoxScope
}

@UIDslMarker
interface RowScope {
    fun UIModifier.weight(weight: Float): UIModifier = then(WeightModifier(weight))

    fun UIModifier.align(alignment: VerticalAlign): UIModifier = then(RowAlignModifier(alignment))

    companion object Instance : RowScope
}

@UIDslMarker
interface ColumnScope {
    fun UIModifier.weight(weight: Float): UIModifier = then(WeightModifier(weight))

    fun UIModifier.align(alignment: HorizontalAlign): UIModifier = then(ColumnAlignModifier(alignment))

    companion object Instance : ColumnScope
}

@UIDslMarker
interface GridScope {
    companion object Instance : GridScope
}
