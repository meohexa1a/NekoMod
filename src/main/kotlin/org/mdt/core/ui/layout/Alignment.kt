@file:Suppress("unused")

package org.mdt.core.ui.layout

// --- ARRANGEMENT DEFINITIONS ---

/**
 * ## ArrangementType
 *
 * Types of child distribution along the flexbox main axis.
 *
 * See: docs/layout-engine/layout_engine_en.md
 */
enum class ArrangementType {
    START,
    CENTER,
    END,
    SPACE_BETWEEN,
    SPACE_AROUND,
    SPACE_EVENLY
}

/**
 * ## Arrangement
 *
 * Spatial spacing and alignment configuration for linear layouts ([RowMeasurePolicy] and [ColumnMeasurePolicy]).
 *
 * See: docs/layout-engine/layout_engine_en.md
 */
data class Arrangement(
    val type: ArrangementType = ArrangementType.START,
    val spacing: Float = 0.0f
) {
    companion object {
        val Start = Arrangement(ArrangementType.START, 0.0f)
        val Center = Arrangement(ArrangementType.CENTER, 0.0f)
        val End = Arrangement(ArrangementType.END, 0.0f)
        val SpaceBetween = Arrangement(ArrangementType.SPACE_BETWEEN, 0.0f)
        val SpaceAround = Arrangement(ArrangementType.SPACE_AROUND, 0.0f)
        val SpaceEvenly = Arrangement(ArrangementType.SPACE_EVENLY, 0.0f)

        fun spacedBy(space: Float, alignment: ArrangementType = ArrangementType.START) =
            Arrangement(alignment, space)
    }
}

// --- ALIGNMENT DEFINITIONS ---

/**
 * ## HorizontalAlign
 *
 * Horizontal alignment placement for cross-axis slot positioning.
 *
 * See: docs/layout-engine/layout_engine_en.md
 */
enum class HorizontalAlign {
    START,
    CENTER,
    END,
    FILL
}

/**
 * ## VerticalAlign
 *
 * Vertical alignment placement for cross-axis slot positioning.
 *
 * See: docs/layout-engine/layout_engine_en.md
 */
enum class VerticalAlign {
    TOP,
    CENTER,
    BOTTOM,
    FILL
}

/**
 * ## Alignment
 *
 * 2D box content alignment configuration combining horizontal and vertical alignments.
 *
 * See: docs/layout-engine/layout_engine_en.md
 */
data class Alignment(
    val horizontal: HorizontalAlign = HorizontalAlign.START,
    val vertical: VerticalAlign = VerticalAlign.CENTER
) {
    companion object {
        val TopStart = Alignment(HorizontalAlign.START, VerticalAlign.TOP)
        val TopCenter = Alignment(HorizontalAlign.CENTER, VerticalAlign.TOP)
        val TopEnd = Alignment(HorizontalAlign.END, VerticalAlign.TOP)

        val CenterStart = Alignment(HorizontalAlign.START, VerticalAlign.CENTER)
        val Center = Alignment(HorizontalAlign.CENTER, VerticalAlign.CENTER)
        val CenterEnd = Alignment(HorizontalAlign.END, VerticalAlign.CENTER)

        val BottomStart = Alignment(HorizontalAlign.START, VerticalAlign.BOTTOM)
        val BottomCenter = Alignment(HorizontalAlign.CENTER, VerticalAlign.BOTTOM)
        val BottomEnd = Alignment(HorizontalAlign.END, VerticalAlign.BOTTOM)

        val Fill = Alignment(HorizontalAlign.FILL, VerticalAlign.FILL)
    }
}
