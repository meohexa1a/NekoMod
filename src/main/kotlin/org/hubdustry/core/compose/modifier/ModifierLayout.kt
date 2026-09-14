package org.hubdustry.core.compose.modifier

import org.hubdustry.core.compose.unit.Dp
import org.hubdustry.core.compose.unit.dp
import org.hubdustry.core.layout.Alignment
import org.hubdustry.core.layout.AnchorPreset
import org.hubdustry.core.layout.LayoutNode
import org.hubdustry.core.layout.Orientation
import org.hubdustry.core.layout.SizeFlag

// ─────────────────────────────────────────────────────────────────────────────
// 1. STANDALONE GEOMETRY MODIFIERS (Offset & Anchor)
// ─────────────────────────────────────────────────────────────────────────────

/**
 * Dịch chuyển vị trí của widget thêm một khoảng [x] và [y] pixel so với vị trí được sắp xếp bởi layout cha.
 */
fun Modifier.offset(x: Float = 0f, y: Float = 0f): Modifier =
    this.then(OffsetModifier(x = x, y = y))

fun Modifier.offset(x: Dp = 0.dp, y: Dp = 0.dp): Modifier =
    this.offset(x.toPx, y.toPx)

/**
 * Đặt chế độ neo định vị tuyệt đối theo mô hình Godot Control ([AnchorPreset]).
 */
fun Modifier.anchor(preset: AnchorPreset): Modifier =
    this.then(AnchorModifier(preset = preset))

// ─────────────────────────────────────────────────────────────────────────────
// 2. CONTAINER SCOPES & PUBLISHED SINGLETONS
// ─────────────────────────────────────────────────────────────────────────────

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

/**
 * Scope cung cấp các modifier chuyên biệt cho các thành phần con bên trong [org.hubdustry.core.compose.primitive.FlowRow].
 */
interface FlowRowScope {
    /**
     * Căn chỉnh vị trí theo trục dọc (cross axis) của phần tử trong từng dòng của FlowRow.
     */
    fun Modifier.align(alignment: Alignment): Modifier
}

/**
 * Scope cung cấp các modifier chuyên biệt cho các thành phần con bên trong [org.hubdustry.core.compose.primitive.FlowColumn].
 */
interface FlowColumnScope {
    /**
     * Căn chỉnh vị trí theo trục ngang (cross axis) của phần tử trong từng cột của FlowColumn.
     */
    fun Modifier.align(alignment: Alignment): Modifier
}

@PublishedApi
internal object RowScopeInstance : RowScope {
    override fun Modifier.weight(weight: Float): Modifier =
        this.then(RowWeightModifier(weight))

    override fun Modifier.align(alignment: Alignment): Modifier =
        this.then(CrossAlignModifier(alignment, orientation = Orientation.VERTICAL))
}

@PublishedApi
internal object ColumnScopeInstance : ColumnScope {
    override fun Modifier.weight(weight: Float): Modifier =
        this.then(ColumnWeightModifier(weight))

    override fun Modifier.align(alignment: Alignment): Modifier =
        this.then(CrossAlignModifier(alignment, orientation = Orientation.HORIZONTAL))
}

@PublishedApi
internal object BoxScopeInstance : BoxScope {
    override fun Modifier.align(alignment: Alignment): Modifier =
        this.then(AlignmentModifier(horizontal = alignment, vertical = alignment))

    override fun Modifier.align(horizontal: Alignment, vertical: Alignment): Modifier =
        this.then(AlignmentModifier(horizontal = horizontal, vertical = vertical))
}

@PublishedApi
internal object FlowRowScopeInstance : FlowRowScope {
    override fun Modifier.align(alignment: Alignment): Modifier =
        this.then(CrossAlignModifier(alignment, orientation = Orientation.VERTICAL))
}

@PublishedApi
internal object FlowColumnScopeInstance : FlowColumnScope {
    override fun Modifier.align(alignment: Alignment): Modifier =
        this.then(CrossAlignModifier(alignment, orientation = Orientation.HORIZONTAL))
}

// ─────────────────────────────────────────────────────────────────────────────
// 3. INTERNAL LAYOUT MODIFIER ELEMENTS
// ─────────────────────────────────────────────────────────────────────────────

internal data class AlignmentModifier(
    val horizontal: Alignment,
    val vertical: Alignment
) : Modifier.Element {
    override fun applyTo(node: LayoutNode) {
        node.alignHorizontal = horizontal
        node.alignVertical = vertical
    }
}

internal data class RowWeightModifier(
    val weight: Float
) : Modifier.Element {
    override fun applyTo(node: LayoutNode) {
        node.stretchRatio = weight
        node.sizeFlagHorizontal = SizeFlag.FILL
    }
}

internal data class ColumnWeightModifier(
    val weight: Float
) : Modifier.Element {
    override fun applyTo(node: LayoutNode) {
        node.stretchRatio = weight
        node.sizeFlagVertical = SizeFlag.FILL
    }
}

internal data class CrossAlignModifier(
    val alignment: Alignment,
    val orientation: Orientation
) : Modifier.Element {
    override fun applyTo(node: LayoutNode) {
        when (orientation) {
            Orientation.HORIZONTAL -> node.alignHorizontal = alignment
            Orientation.VERTICAL -> node.alignVertical = alignment
        }
    }
}

internal data class OffsetModifier(
    val x: Float,
    val y: Float
) : Modifier.Element {
    override fun applyTo(node: LayoutNode) {
        node.offsetX += x
        node.offsetY += y
    }
}

internal data class AnchorModifier(
    val preset: AnchorPreset
) : Modifier.Element {
    override fun applyTo(node: LayoutNode) {
        node.anchor.setPreset(preset)
    }
}

