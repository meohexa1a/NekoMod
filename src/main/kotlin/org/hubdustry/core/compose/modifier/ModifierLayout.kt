package org.hubdustry.core.compose.modifier

import org.hubdustry.core.compose.Modifier
import org.hubdustry.core.layout.Alignment
import org.hubdustry.core.layout.AnchorPreset
import org.hubdustry.core.layout.LayoutNode
import org.hubdustry.core.layout.SizeFlag

private data class AlignmentModifier(
    val horizontal: Alignment,
    val vertical: Alignment
) : Modifier.Element {
    override fun applyTo(node: LayoutNode) {
        node.alignHorizontal = horizontal
        node.alignVertical = vertical
    }
}

private data class RowWeightModifier(
    val weight: Float
) : Modifier.Element {
    override fun applyTo(node: LayoutNode) {
        node.stretchRatio = weight
        node.sizeFlagHorizontal = SizeFlag.FILL
    }
}

private data class ColumnWeightModifier(
    val weight: Float
) : Modifier.Element {
    override fun applyTo(node: LayoutNode) {
        node.stretchRatio = weight
        node.sizeFlagVertical = SizeFlag.FILL
    }
}

private data class CrossAlignModifier(
    val alignment: Alignment,
    val isHorizontal: Boolean
) : Modifier.Element {
    override fun applyTo(node: LayoutNode) {
        if (isHorizontal) {
            node.alignHorizontal = alignment
        } else {
            node.alignVertical = alignment
        }
    }
}

private data class OffsetModifier(
    val x: Float,
    val y: Float
) : Modifier.Element {
    override fun applyTo(node: LayoutNode) {
        node.offsetX += x
        node.offsetY += y
    }
}

private data class AnchorModifier(
    val preset: AnchorPreset
) : Modifier.Element {
    override fun applyTo(node: LayoutNode) {
        node.anchor.setPreset(preset)
    }
}

/**
 * Scope cung cấp các modifier chuyên biệt cho các thành phần con bên trong [org.hubdustry.core.compose.primitive.Row].
 */
interface RowScope {
    /**
     * Phân bổ tỷ trọng chiếm dụng không gian còn trống theo trục ngang (main axis).
     * @param weight Tỷ trọng co giãn (tương đương flex-grow).
     */
    fun Modifier.weight(weight: Float): Modifier

    /**
     * Căn chỉnh vị trí theo trục dọc (cross axis) của phần tử trong Row.
     */
    fun Modifier.align(alignment: Alignment): Modifier
}

/**
 * Scope cung cấp các modifier chuyên biệt cho các thành phần con bên trong [org.hubdustry.core.compose.primitive.Column].
 */
interface ColumnScope {
    /**
     * Phân bổ tỷ trọng chiếm dụng không gian còn trống theo trục dọc (main axis).
     * @param weight Tỷ trọng co giãn (tương đương flex-grow).
     */
    fun Modifier.weight(weight: Float): Modifier

    /**
     * Căn chỉnh vị trí theo trục ngang (cross axis) của phần tử trong Column.
     */
    fun Modifier.align(alignment: Alignment): Modifier
}

/**
 * Scope cung cấp các modifier chuyên biệt cho các thành phần con bên trong [org.hubdustry.core.compose.primitive.Box].
 */
interface BoxScope {
    /**
     * Căn chỉnh vị trí cả 2 trục của phần tử trong Box.
     */
    fun Modifier.align(alignment: Alignment): Modifier

    /**
     * Căn chỉnh vị trí độc lập theo trục ngang và trục dọc trong Box.
     */
    fun Modifier.align(horizontal: Alignment, vertical: Alignment): Modifier
}

object RowScopeInstance : RowScope {
    override fun Modifier.weight(weight: Float): Modifier =
        this.then(RowWeightModifier(weight))

    override fun Modifier.align(alignment: Alignment): Modifier =
        this.then(CrossAlignModifier(alignment, isHorizontal = false))
}

object ColumnScopeInstance : ColumnScope {
    override fun Modifier.weight(weight: Float): Modifier =
        this.then(ColumnWeightModifier(weight))

    override fun Modifier.align(alignment: Alignment): Modifier =
        this.then(CrossAlignModifier(alignment, isHorizontal = true))
}

object BoxScopeInstance : BoxScope {
    override fun Modifier.align(alignment: Alignment): Modifier =
        this.then(AlignmentModifier(horizontal = alignment, vertical = alignment))

    override fun Modifier.align(horizontal: Alignment, vertical: Alignment): Modifier =
        this.then(AlignmentModifier(horizontal = horizontal, vertical = vertical))
}

/**
 * Dịch chuyển vị trí của widget thêm một khoảng [x] và [y] pixel so với vị trí được sắp xếp bởi layout cha.
 */
fun Modifier.offset(x: Float = 0f, y: Float = 0f): Modifier =
    this.then(OffsetModifier(x = x, y = y))

/**
 * Đặt chế độ neo định vị tuyệt đối theo mô hình Godot Control ([AnchorPreset]).
 */
fun Modifier.anchor(preset: AnchorPreset): Modifier =
    this.then(AnchorModifier(preset = preset))

