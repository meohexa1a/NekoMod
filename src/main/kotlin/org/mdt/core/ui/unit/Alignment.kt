// [AGENT INVARIANT] Synchronously update @property, @param, and @see KDocs when modifying this file.

@file:Suppress("unused")

package org.mdt.core.ui.unit

// --- ARRANGEMENT DEFINITIONS ---

/**
 * ## ArrangementType [Flex Main-Axis Distribution]
 *
 * Types of child distribution along the flexbox main axis.
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
 * ## Arrangement [Flex Spatial Arrangement Configuration]
 *
 * Spatial spacing and alignment configuration for linear layouts ([RowMeasurePolicy] and [ColumnMeasurePolicy]).
 */
data class Arrangement(
    val type: ArrangementType = ArrangementType.START,
    val spacing: Float = 0.0f,
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
 * ## HorizontalAlign [Horizontal Cross-Axis Alignment]
 *
 * Horizontal alignment placement for cross-axis slot positioning.
 */
enum class HorizontalAlign {
    START,
    CENTER,
    END,
    FILL
}

/**
 * ## VerticalAlign [Vertical Cross-Axis Alignment]
 *
 * Vertical alignment placement for cross-axis slot positioning.
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
 * Defines 2D positioning ([horizontal] and [vertical]) for aligning child elements within layout containers.
 * Provides standard static presets ([Center], [TopStart], [BottomEnd], [Fill]).
 *
 * @property horizontal Horizontal axis alignment placement ([HorizontalAlign]).
 * @property vertical Vertical axis alignment placement ([VerticalAlign]).
 *
 * @see HorizontalAlign
 * @see VerticalAlign
 * @see org.mdt.core.ui.layout.BoxMeasurePolicy
 */
data class Alignment(
    val horizontal: HorizontalAlign = HorizontalAlign.START,
    val vertical: VerticalAlign = VerticalAlign.CENTER,
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
