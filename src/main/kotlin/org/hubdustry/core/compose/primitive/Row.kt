package org.hubdustry.core.compose.primitive

import androidx.compose.runtime.Composable
import androidx.compose.runtime.ComposeNode
import org.hubdustry.core.compose.LayoutNodeApplier
import org.hubdustry.core.compose.Modifier
import org.hubdustry.core.compose.applyTo
import org.hubdustry.core.compose.modifier.RowScope
import org.hubdustry.core.compose.modifier.RowScopeInstance
import org.hubdustry.core.layout.LayoutNode
import org.hubdustry.core.layout.SizeFlag
import org.hubdustry.core.layout.policies.RowPolicy

/**
 * [Row] xếp các phần tử con theo chiều ngang (trục hoành X) với khoảng cách [gap].
 */
@Composable
inline fun Row(
    modifier: Modifier = Modifier.Companion,
    gap: Float = 0f,
    crossinline content: @Composable RowScope.() -> Unit = {}
) {
    ComposeNode<LayoutNode, LayoutNodeApplier>(
        factory = { LayoutNode().apply { policy = RowPolicy(gap) } },
        update = {
            set(modifier) {
                this.resetModifierState(SizeFlag.FILL, SizeFlag.FILL)
                it.applyTo(this)
            }
            set(gap) { this.policy = RowPolicy(it) }
        },
        content = { RowScopeInstance.content() }
    )
}
