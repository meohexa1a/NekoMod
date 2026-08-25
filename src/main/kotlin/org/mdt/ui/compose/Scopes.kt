package org.mdt.ui.compose

import org.mdt.ui.layout.Alignment
import org.mdt.ui.layout.HorizontalAlign
import org.mdt.ui.layout.SizeFlags
import org.mdt.ui.layout.VerticalAlign

@UIDslMarker
interface BoxScope {
    fun UIModifier.align(alignment: Alignment): UIModifier = then(CustomModifier {
        when (alignment.horizontal) {
            HorizontalAlign.START -> it.sizeFlagsHorizontal = SizeFlags.SHRINK_BEGIN
            HorizontalAlign.CENTER -> it.sizeFlagsHorizontal = SizeFlags.SHRINK_CENTER
            HorizontalAlign.END -> it.sizeFlagsHorizontal = SizeFlags.SHRINK_END
            HorizontalAlign.FILL -> it.sizeFlagsHorizontal = SizeFlags.FILL
        }
        when (alignment.vertical) {
            VerticalAlign.TOP -> it.sizeFlagsVertical = SizeFlags.SHRINK_BEGIN
            VerticalAlign.CENTER -> it.sizeFlagsVertical = SizeFlags.SHRINK_CENTER
            VerticalAlign.BOTTOM -> it.sizeFlagsVertical = SizeFlags.SHRINK_END
            VerticalAlign.FILL -> it.sizeFlagsVertical = SizeFlags.FILL
        }
    })

    companion object Instance : BoxScope
}

@UIDslMarker
interface RowScope {
    fun UIModifier.weight(weight: Float): UIModifier = then(CustomModifier {
        it.sizeFlagsHorizontal = it.sizeFlagsHorizontal or SizeFlags.EXPAND_FILL
        it.stretchRatio = weight
    })

    fun UIModifier.align(alignment: VerticalAlign): UIModifier = then(CustomModifier {
        when (alignment) {
            VerticalAlign.TOP -> it.sizeFlagsVertical = SizeFlags.SHRINK_BEGIN
            VerticalAlign.CENTER -> it.sizeFlagsVertical = SizeFlags.SHRINK_CENTER
            VerticalAlign.BOTTOM -> it.sizeFlagsVertical = SizeFlags.SHRINK_END
            VerticalAlign.FILL -> it.sizeFlagsVertical = SizeFlags.FILL
        }
    })

    companion object Instance : RowScope
}

@UIDslMarker
interface ColumnScope {
    fun UIModifier.weight(weight: Float): UIModifier = then(CustomModifier {
        it.sizeFlagsVertical = it.sizeFlagsVertical or SizeFlags.EXPAND_FILL
        it.stretchRatio = weight
    })

    fun UIModifier.align(alignment: HorizontalAlign): UIModifier = then(CustomModifier {
        when (alignment) {
            HorizontalAlign.START -> it.sizeFlagsHorizontal = SizeFlags.SHRINK_BEGIN
            HorizontalAlign.CENTER -> it.sizeFlagsHorizontal = SizeFlags.SHRINK_CENTER
            HorizontalAlign.END -> it.sizeFlagsHorizontal = SizeFlags.SHRINK_END
            HorizontalAlign.FILL -> it.sizeFlagsHorizontal = SizeFlags.FILL
        }
    })

    companion object Instance : ColumnScope
}

@UIDslMarker
interface GridScope {
    companion object Instance : GridScope
}
