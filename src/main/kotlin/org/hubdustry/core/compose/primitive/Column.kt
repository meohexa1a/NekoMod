package org.hubdustry.core.compose.primitive

import androidx.compose.runtime.Composable
import androidx.compose.runtime.ComposeNode
import org.hubdustry.core.compose.runtime.LayoutNodeApplier
import org.hubdustry.core.compose.modifier.Modifier
import org.hubdustry.core.compose.modifier.applyTo
import org.hubdustry.core.compose.modifier.ColumnScope
import org.hubdustry.core.compose.modifier.ColumnScopeInstance
import org.hubdustry.core.layout.LayoutNode
import org.hubdustry.core.layout.SizeFlag
import org.hubdustry.core.layout.policies.ColumnPolicy

/**
 * [Column] xếp các phần tử con theo chiều dọc (trục tung Y) với khoảng cách [gap].
 */
@Composable
inline fun Column(
    modifier: Modifier = Modifier.Companion,
    gap: Float = 0f,
    crossinline content: @Composable ColumnScope.() -> Unit = {}
) {
    ComposeNode<LayoutNode, LayoutNodeApplier>(
        factory = { LayoutNode().apply { policy = ColumnPolicy(gap) } },
        update = {
            set(modifier) {
                this.resetModifierState(SizeFlag.FILL, SizeFlag.FILL)
                it.applyTo(this)
            }
            set(gap) { this.policy = ColumnPolicy(it) }
        },
        content = { ColumnScopeInstance.content() }
    )
}
