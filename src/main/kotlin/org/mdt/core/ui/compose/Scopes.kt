package org.mdt.core.ui.compose

import org.mdt.core.ui.layout.Alignment
import org.mdt.core.ui.layout.HorizontalAlign
import org.mdt.core.ui.layout.SizeFlags
import org.mdt.core.ui.layout.VerticalAlign
import org.mdt.core.ui.node.UINode

/**
 * ## UIDslMarker [Compose DSL Marker]
 *
 * Scoping annotation ensuring Compose UI builder lambdas do not unintentionally
 * cross-access nested modifier scopes.
 */
@DslMarker
annotation class UIDslMarker

// --- SCOPE-SPECIFIC TYPED MODIFIERS ---

/**
 * ## BoxAlignModifier [Scope Alignment Modifier]
 *
 * Typed modifier element applying 2D content alignment within a `Box` container.
 */
data class BoxAlignModifier(val alignment: Alignment) : UIModifier.Element {
    override fun applyTo(node: UINode) {
        when (alignment.horizontal) {
            HorizontalAlign.START -> if ((node.sizeFlagsHorizontal and SizeFlags.FILL) == 0) node.sizeFlagsHorizontal = SizeFlags.SHRINK_BEGIN
            HorizontalAlign.CENTER -> if ((node.sizeFlagsHorizontal and SizeFlags.FILL) == 0) node.sizeFlagsHorizontal = SizeFlags.SHRINK_CENTER
            HorizontalAlign.END -> if ((node.sizeFlagsHorizontal and SizeFlags.FILL) == 0) node.sizeFlagsHorizontal = SizeFlags.SHRINK_END
            HorizontalAlign.FILL -> node.sizeFlagsHorizontal = SizeFlags.FILL
        }
        when (alignment.vertical) {
            VerticalAlign.TOP -> if ((node.sizeFlagsVertical and SizeFlags.FILL) == 0) node.sizeFlagsVertical = SizeFlags.SHRINK_BEGIN
            VerticalAlign.CENTER -> if ((node.sizeFlagsVertical and SizeFlags.FILL) == 0) node.sizeFlagsVertical = SizeFlags.SHRINK_CENTER
            VerticalAlign.BOTTOM -> if ((node.sizeFlagsVertical and SizeFlags.FILL) == 0) node.sizeFlagsVertical = SizeFlags.SHRINK_END
            VerticalAlign.FILL -> node.sizeFlagsVertical = SizeFlags.FILL
        }
    }
}

/**
 * ## RowAlignModifier [Scope Alignment Modifier]
 *
 * Typed modifier element applying vertical cross-axis alignment within a `Row` container.
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
 * ## ColumnAlignModifier [Scope Alignment Modifier]
 *
 * Typed modifier element applying horizontal cross-axis alignment within a `Column` container.
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
 * ## BoxScope [Composable Container Scope]
 *
 * ### 1. 📖 Feature Specification & Core Architecture:
 * - Scope receiver restricting child modifier extensions inside [org.mdt.ui.components.layout.Box].
 * - Provides 2D child content positioning via `align(Alignment)`.
 *
 * ### 2. ⚡ Invariants & Non-Negotiable Rules:
 * - **Rule 1 (Zero-GC Chaining):** Extension functions must append typed [BoxAlignModifier] via `.then()`.
 *
 * ### 3. 🔗 Related Files & Subsystem Map:
 * - 🎨 **Composable Box:** `src/main/kotlin/org/mdt/ui/components/layout/Box.kt`
 * - 📐 **Measure Policy:** `src/main/kotlin/org/mdt/core/ui/layout/MeasurePolicy.kt`
 *
 * ### 4. ✅ Behavioral Verification Checklist:
 * - [x] `align(Alignment)` accurately translates horizontal/vertical alignment into size flags without overriding `FILL`.
 */
@UIDslMarker
interface BoxScope {
    /** Aligns child node within the parent box boundaries. */
    fun UIModifier.align(alignment: Alignment): UIModifier = then(BoxAlignModifier(alignment))

    companion object Instance : BoxScope
}

/**
 * ## RowScope [Composable Container Scope]
 *
 * ### 1. 📖 Feature Specification & Core Architecture:
 * - Scope receiver restricting child modifier extensions inside [org.mdt.ui.components.layout.Row].
 * - Provides horizontal flex proportional weight distribution and vertical cross-axis alignment.
 *
 * ### 2. ⚡ Invariants & Non-Negotiable Rules:
 * - **Rule 1 (Zero-GC Chaining):** `weight(Float)` and `align(VerticalAlign)` append typed modifiers via `.then()`.
 *
 * ### 3. 🔗 Related Files & Subsystem Map:
 * - 🎨 **Composable Row:** `src/main/kotlin/org/mdt/ui/components/layout/FlexLayouts.kt`
 * - 📐 **Measure Policy:** `src/main/kotlin/org/mdt/core/ui/layout/MeasurePolicy.kt`
 *
 * ### 4. ✅ Behavioral Verification Checklist:
 * - [x] `weight(Float)` sets `SizeFlags.EXPAND_FILL` and assigns proportional `stretchRatio`.
 * - [x] `align(VerticalAlign)` positions child along row vertical cross-axis.
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
 * ## ColumnScope [Composable Container Scope]
 *
 * ### 1. 📖 Feature Specification & Core Architecture:
 * - Scope receiver restricting child modifier extensions inside [org.mdt.ui.components.layout.Column].
 * - Provides vertical flex proportional weight distribution and horizontal cross-axis alignment.
 *
 * ### 2. ⚡ Invariants & Non-Negotiable Rules:
 * - **Rule 1 (Zero-GC Chaining):** `weight(Float)` and `align(HorizontalAlign)` append typed modifiers via `.then()`.
 *
 * ### 3. 🔗 Related Files & Subsystem Map:
 * - 🎨 **Composable Column:** `src/main/kotlin/org/mdt/ui/components/layout/FlexLayouts.kt`
 * - 📐 **Measure Policy:** `src/main/kotlin/org/mdt/core/ui/layout/MeasurePolicy.kt`
 *
 * ### 4. ✅ Behavioral Verification Checklist:
 * - [x] `weight(Float)` sets `SizeFlags.EXPAND_FILL` and assigns proportional `stretchRatio`.
 * - [x] `align(HorizontalAlign)` positions child along column horizontal cross-axis.
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
 * ## GridScope [Composable Container Scope]
 *
 * Scope receiver restricting child modifier extensions inside grid containers.
 */
@UIDslMarker
interface GridScope {
    companion object Instance : GridScope
}

