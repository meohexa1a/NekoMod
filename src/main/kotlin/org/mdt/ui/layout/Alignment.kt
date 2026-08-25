@file:Suppress("unused")

package org.mdt.ui.layout

enum class ArrangementType {
    START,
    CENTER,
    END,
    SPACE_BETWEEN,
    SPACE_AROUND,
    SPACE_EVENLY
}

data class Arrangement(
    val type: ArrangementType = ArrangementType.START,
    val spacing: Float = 0f
) {
    companion object {
        val Start = Arrangement(ArrangementType.START, 0f)
        val Center = Arrangement(ArrangementType.CENTER, 0f)
        val End = Arrangement(ArrangementType.END, 0f)
        val SpaceBetween = Arrangement(ArrangementType.SPACE_BETWEEN, 0f)
        val SpaceAround = Arrangement(ArrangementType.SPACE_AROUND, 0f)
        val SpaceEvenly = Arrangement(ArrangementType.SPACE_EVENLY, 0f)

        fun spacedBy(space: Float, alignment: ArrangementType = ArrangementType.START) =
            Arrangement(alignment, space)
    }
}

enum class HorizontalAlign {
    START,
    CENTER,
    END,
    FILL
}

enum class VerticalAlign {
    TOP,
    CENTER,
    BOTTOM,
    FILL
}

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
