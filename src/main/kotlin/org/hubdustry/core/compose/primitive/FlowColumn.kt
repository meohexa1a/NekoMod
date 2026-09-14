package org.hubdustry.core.compose.primitive

import androidx.compose.runtime.Composable
import androidx.compose.runtime.ComposeNode
import org.hubdustry.core.compose.modifier.FlowColumnScope
import org.hubdustry.core.compose.modifier.FlowColumnScopeInstance
import org.hubdustry.core.compose.modifier.Modifier
import org.hubdustry.core.compose.modifier.applyTo
import org.hubdustry.core.compose.runtime.LayoutNodeApplier
import org.hubdustry.core.compose.unit.Dp
import org.hubdustry.core.compose.unit.dp
import org.hubdustry.core.layout.LayoutNode
import org.hubdustry.core.layout.SizeFlag
import org.hubdustry.core.layout.policies.FlowColumnPolicy

/**
 * [FlowColumn] xếp các phần tử con theo chiều dọc và tự động ngắt sang cột mới khi vượt quá chiều cao khả dụng.
 *
 * @param modifier Modifier tùy biến hình học, visual và gesture của container.
 * @param verticalGap Khoảng cách giữa các phần tử trên cùng một cột.
 * @param horizontalGap Khoảng cách giữa các cột kế tiếp nhau.
 * @param content Nội dung con bên trong phạm vi [FlowColumnScope].
 */
@Composable
inline fun FlowColumn(
    modifier: Modifier = Modifier,
    verticalGap: Float = 0f,
    horizontalGap: Float = 0f,
    crossinline content: @Composable FlowColumnScope.() -> Unit = {}
) {
    ComposeNode<LayoutNode, LayoutNodeApplier>(
        factory = { LayoutNode().apply { policy = FlowColumnPolicy(verticalGap, horizontalGap) } },
        update = {
            set(modifier) {
                this.resetModifierState(SizeFlag.SHRINK, SizeFlag.FILL)
                it.applyTo(this)
            }
            set(verticalGap) {
                this.policy = FlowColumnPolicy(it, horizontalGap)
            }
            set(horizontalGap) {
                this.policy = FlowColumnPolicy(verticalGap, it)
            }
        },
        content = { FlowColumnScopeInstance.content() }
    )
}

/**
 * Overload tiện lợi nhận [Dp] cho khoảng cách giữa các phần tử và các cột của [FlowColumn].
 */
@Composable
inline fun FlowColumn(
    modifier: Modifier = Modifier,
    verticalGap: Dp,
    horizontalGap: Dp = 0.dp,
    crossinline content: @Composable FlowColumnScope.() -> Unit = {}
) = FlowColumn(
    modifier = modifier,
    verticalGap = verticalGap.toPx,
    horizontalGap = horizontalGap.toPx,
    content = content
)
