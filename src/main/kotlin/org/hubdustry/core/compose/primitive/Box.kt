package org.hubdustry.core.compose.primitive

import androidx.compose.runtime.Composable
import androidx.compose.runtime.ComposeNode
import org.hubdustry.core.compose.modifier.BoxScope
import org.hubdustry.core.compose.modifier.BoxScopeInstance
import org.hubdustry.core.compose.modifier.Modifier
import org.hubdustry.core.compose.modifier.applyTo
import org.hubdustry.core.compose.runtime.LayoutNodeApplier
import org.hubdustry.core.layout.LayoutNode
import org.hubdustry.core.layout.SizeFlag
import org.hubdustry.core.layout.policies.BoxLayoutPolicy

/**
 * [Box] là container cơ bản đa năng nhất của NekoMod Compose.
 * Áp dụng [BoxLayoutPolicy] để xếp chồng các phần tử con theo lớp z-index,
 * hỗ trợ đầy đủ bo góc, viền ngoài và đổ bóng thông qua SDF Shader.
 */
@Composable
inline fun Box(
    modifier: Modifier = Modifier.Companion,
    crossinline content: @Composable BoxScope.() -> Unit = {}
) {
    ComposeNode<LayoutNode, LayoutNodeApplier>(
        factory = { LayoutNode().apply { policy = BoxLayoutPolicy } },
        update = {
            set(modifier) {
                this.resetModifierState(SizeFlag.FILL, SizeFlag.FILL)
                it.applyTo(this)
            }
        },
        content = { BoxScopeInstance.content() }
    )
}
