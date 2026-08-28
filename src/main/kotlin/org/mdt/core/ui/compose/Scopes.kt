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
 *
 * See: docs/compose-dsl/compose_dsl_en.md
 */
@DslMarker
annotation class UIDslMarker

// --- SCOPE-SPECIFIC TYPED MODIFIERS ---

/**
 * ## BoxAlignModifier
 *
 * Typed modifier element applying box content alignment.
 *
 * See: docs/compose-dsl/compose_dsl_en.md
 */
data class BoxAlignModifier(val alignment: Alignment) : UIModifier.Element {
    override fun applyTo(node: UINode) {
        when (alignment.horizontal) {
            HorizontalAlign.START -> node.sizeFlagsHorizontal = SizeFlags.SHRINK_BEGIN
            HorizontalAlign.CENTER -> if ((node.sizeFlagsHorizontal and SizeFlags.FILL) == 0) node.sizeFlagsHorizontal = SizeFlags.SHRINK_CENTER
            HorizontalAlign.END -> node.sizeFlagsHorizontal = SizeFlags.SHRINK_END
            HorizontalAlign.FILL -> node.sizeFlagsHorizontal = SizeFlags.FILL
        }
        when (alignment.vertical) {
            VerticalAlign.TOP -> node.sizeFlagsVertical = SizeFlags.SHRINK_BEGIN
            VerticalAlign.CENTER -> if ((node.sizeFlagsVertical and SizeFlags.FILL) == 0) node.sizeFlagsVertical = SizeFlags.SHRINK_CENTER
            VerticalAlign.BOTTOM -> node.sizeFlagsVertical = SizeFlags.SHRINK_END
            VerticalAlign.FILL -> node.sizeFlagsVertical = SizeFlags.FILL
        }
    }
}

/**
 * ## RowAlignModifier
 *
 * Typed modifier element applying row vertical cross-alignment.
 *
 * See: docs/compose-dsl/compose_dsl_en.md
 */
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

/**
 * ## ColumnAlignModifier
 *
 * Typed modifier element applying column horizontal cross-alignment.
 *
 * See: docs/compose-dsl/compose_dsl_en.md
 */
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

// --- COMPOSE SCOPE INTERFACES ---

/**
 * ## BoxScope
 *
 * Scope receiver for children inside a `Box` container.
 *
 * See: docs/compose-dsl/compose_dsl_en.md
 */
@UIDslMarker
interface BoxScope {
    /** Aligns child node within the parent box boundaries. */
    fun UIModifier.align(alignment: Alignment): UIModifier = then(BoxAlignModifier(alignment))

    companion object Instance : BoxScope
}

/**
 * ## RowScope
 *
 * Scope receiver for children inside a horizontal `Row` container.
 *
 * See: docs/compose-dsl/compose_dsl_en.md
 */
@UIDslMarker
interface RowScope {
    /** Allocates available horizontal flex space proportionally by [weight] ratio. */
    fun UIModifier.weight(weight: Float): UIModifier = then(WeightModifier(weight))

    /** Aligns child node along the vertical cross-axis of the row. */
    fun UIModifier.align(alignment: VerticalAlign): UIModifier = then(RowAlignModifier(alignment))

    companion object Instance : RowScope
}

/**
 * ## ColumnScope
 *
 * Scope receiver for children inside a vertical `Column` container.
 *
 * See: docs/compose-dsl/compose_dsl_en.md
 */
@UIDslMarker
interface ColumnScope {
    /** Allocates available vertical flex space proportionally by [weight] ratio. */
    fun UIModifier.weight(weight: Float): UIModifier = then(WeightModifier(weight))

    /** Aligns child node along the horizontal cross-axis of the column. */
    fun UIModifier.align(alignment: HorizontalAlign): UIModifier = then(ColumnAlignModifier(alignment))

    companion object Instance : ColumnScope
}

/**
 * ## GridScope
 *
 * Scope receiver for children inside a `Grid` container.
 *
 * See: docs/compose-dsl/compose_dsl_en.md
 */
@UIDslMarker
interface GridScope {
    companion object Instance : GridScope
}
