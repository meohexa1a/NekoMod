package org.hubdustry.core.compose.primitive

import androidx.compose.runtime.Composable
import androidx.compose.runtime.ComposeNode
import org.hubdustry.core.compose.runtime.LayoutNodeApplier
import org.hubdustry.core.compose.modifier.Modifier
import org.hubdustry.core.compose.modifier.applyTo
import org.hubdustry.core.compose.modifier.FlowRowScope
import org.hubdustry.core.compose.modifier.FlowRowScopeInstance
import org.hubdustry.core.layout.LayoutNode
import org.hubdustry.core.layout.SizeFlag
import org.hubdustry.core.layout.policies.FlowRowPolicy

/**
 * [FlowRow] xếp các phần tử con theo chiều ngang và tự động ngắt xuống dòng mới khi vượt quá chiều rộng khả dụng.
 *
 * @param modifier Modifier tùy biến hình học, visual và gesture của container.
 * @param horizontalGap Khoảng cách giữa các phần tử trên cùng một dòng.
 * @param verticalGap Khoảng cách giữa các dòng kế tiếp nhau.
 * @param content Nội dung con bên trong phạm vi [FlowRowScope].
 */
@Composable
inline fun FlowRow(
    modifier: Modifier = Modifier.Companion,
    horizontalGap: Float = 0f,
    verticalGap: Float = 0f,
    crossinline content: @Composable FlowRowScope.() -> Unit = {}
) {
    ComposeNode<LayoutNode, LayoutNodeApplier>(
        factory = { LayoutNode().apply { policy = FlowRowPolicy(horizontalGap, verticalGap) } },
        update = {
            set(modifier) {
                this.resetModifierState(SizeFlag.FILL, SizeFlag.SHRINK)
                it.applyTo(this)
            }
            set(horizontalGap) {
                this.policy = FlowRowPolicy(it, verticalGap)
            }
            set(verticalGap) {
                this.policy = FlowRowPolicy(horizontalGap, it)
            }
        },
        content = { FlowRowScopeInstance.content() }
    )
}
