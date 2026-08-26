package org.mdt.core.ui.compose

import org.mdt.core.ui.UINode
import org.mdt.core.ui.layout.Alignment
import org.mdt.core.ui.layout.HorizontalAlign
import org.mdt.core.ui.layout.SizeFlags
import org.mdt.core.ui.layout.VerticalAlign

/**
 * ## UIDslMarker
 *
 * Scoping annotation ensuring Compose UI builder lambdas do not unintentionally
 * cross-access nested modifier scopes.
 */
@DslMarker
annotation class UIDslMarker

// =========================================================================
// I. Scope-Specific Typed Modifiers
// =========================================================================

/** Typed modifier element applying box content alignment. */
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

/** Typed modifier element applying row vertical cross-alignment. */
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

/** Typed modifier element applying column horizontal cross-alignment. */
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
// II. Compose Scope Interfaces
// =========================================================================

/** Scope receiver for children inside a `Box` container. */
@UIDslMarker
interface BoxScope {
    /** Aligns child node within the parent box boundaries. */
    fun UIModifier.align(alignment: Alignment): UIModifier = then(BoxAlignModifier(alignment))

    companion object Instance : BoxScope
}

/** Scope receiver for children inside a horizontal `Row` container. */
@UIDslMarker
interface RowScope {
    /** Allocates available horizontal flex space proportionally by [weight] ratio. */
    fun UIModifier.weight(weight: Float): UIModifier = then(WeightModifier(weight))

    /** Aligns child node along the vertical cross-axis of the row. */
    fun UIModifier.align(alignment: VerticalAlign): UIModifier = then(RowAlignModifier(alignment))

    companion object Instance : RowScope
}

/** Scope receiver for children inside a vertical `Column` container. */
@UIDslMarker
interface ColumnScope {
    /** Allocates available vertical flex space proportionally by [weight] ratio. */
    fun UIModifier.weight(weight: Float): UIModifier = then(WeightModifier(weight))

    /** Aligns child node along the horizontal cross-axis of the column. */
    fun UIModifier.align(alignment: HorizontalAlign): UIModifier = then(ColumnAlignModifier(alignment))

    companion object Instance : ColumnScope
}

/** Scope receiver for children inside a `Grid` container. */
@UIDslMarker
interface GridScope {
    companion object Instance : GridScope
}
