// [AGENT INVARIANT] Synchronously update @property, @param, and @see KDocs when modifying this file.

package org.mdt.core.ui.layout

import org.mdt.core.ui.modifier.BoxAlignModifier
import org.mdt.core.ui.modifier.ColumnAlignModifier
import org.mdt.core.ui.modifier.HorizontalWeightModifier
import org.mdt.core.ui.modifier.RowAlignModifier
import org.mdt.core.ui.modifier.UIModifier
import org.mdt.core.ui.modifier.VerticalWeightModifier
import org.mdt.core.ui.unit.Alignment
import org.mdt.core.ui.unit.HorizontalAlign
import org.mdt.core.ui.unit.VerticalAlign

/**
 * ## RowScope
 *
 * Scope interface providing row-specific layout modifiers for horizontal flex distribution.
 *
 * @see org.mdt.ui.components.layout.Row
 */
interface RowScope {
    /**
     * Proportions main-axis (horizontal) space according to [weight].
     * Leaves cross-axis (vertical) sizing untouched to avoid cross-axis distortion.
     *
     * @param weight Relative weight fraction for proportional space allocation.
     */
    fun UIModifier.weight(weight: Float): UIModifier = then(HorizontalWeightModifier(weight))

    /**
     * Proportions main-axis (horizontal) space according to integer [weight].
     *
     * @param weight Relative integer weight for proportional space allocation.
     */
    fun UIModifier.weight(weight: Int): UIModifier = weight(weight.toFloat())

    /**
     * Configures vertical cross-axis alignment for this child within the Row.
     *
     * @param alignment Vertical alignment mode ([VerticalAlign.Top], [VerticalAlign.Center], [VerticalAlign.Bottom]).
     */
    fun UIModifier.align(alignment: VerticalAlign): UIModifier = then(RowAlignModifier(alignment))
}

/**
 * ## ColumnScope
 *
 * Scope interface providing column-specific layout modifiers for vertical flex distribution.
 *
 * @see org.mdt.ui.components.layout.Column
 */
interface ColumnScope {
    /**
     * Proportions main-axis (vertical) space according to [weight].
     * Leaves cross-axis (horizontal) sizing untouched to avoid cross-axis distortion.
     *
     * @param weight Relative weight fraction for proportional space allocation.
     */
    fun UIModifier.weight(weight: Float): UIModifier = then(VerticalWeightModifier(weight))

    /**
     * Proportions main-axis (vertical) space according to integer [weight].
     *
     * @param weight Relative integer weight for proportional space allocation.
     */
    fun UIModifier.weight(weight: Int): UIModifier = weight(weight.toFloat())

    /**
     * Configures horizontal cross-axis alignment for this child within the Column.
     *
     * @param alignment Horizontal alignment mode ([HorizontalAlign.Left], [HorizontalAlign.Center], [HorizontalAlign.Right]).
     */
    fun UIModifier.align(alignment: HorizontalAlign): UIModifier = then(ColumnAlignModifier(alignment))
}

/**
 * ## BoxScope
 *
 * Scope interface providing 2D box-specific alignment and fill modifiers.
 *
 * @see org.mdt.ui.components.layout.Box
 */
interface BoxScope {
    /**
     * Configures 2D spatial alignment within the Box container.
     *
     * @param alignment 2D alignment preset ([Alignment.Center], [Alignment.TopStart], etc.).
     */
    fun UIModifier.align(alignment: Alignment): UIModifier = then(BoxAlignModifier(alignment))
}

/** Singleton zero-GC implementation for [RowScope]. */
object RowScopeInstance : RowScope

/** Singleton zero-GC implementation for [ColumnScope]. */
object ColumnScopeInstance : ColumnScope

/** Singleton zero-GC implementation for [BoxScope]. */
object BoxScopeInstance : BoxScope
