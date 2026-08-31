// [AGENT INVARIANT] Synchronously update @property, @param, and @see KDocs when modifying this file.

package org.mdt.core.ui.layout

import kotlin.test.Test
import kotlin.test.assertEquals
import org.mdt.core.ui.node.LayoutNode

/**
 * ## FlowRowMeasurePolicyTest
 *
 * Validates stateless line wrapping, gap calculation, and item distribution in [FlowRowMeasurePolicy].
 */
class FlowRowMeasurePolicyTest {

    @Test
    fun `FlowRow wraps children onto multiple lines when available width is exceeded`() {
        val container = LayoutNode().apply {
            measurePolicy = FlowRowMeasurePolicy(
                horizontalGap = 10.0f,
                verticalGap = 10.0f
            )
        }

        // Add 4 boxes of width 60px and height 30px
        for (i in 1..4) {
            val child = LayoutNode().apply {
                minWidth = 60.0f
                minHeight = 30.0f
            }
            container.children.add(child)
        }

        // Available width = 150px
        // Line 1: child 1 (60px) + gap (10px) + child 2 (60px) = 130px <= 150px
        // Line 2: child 3 (60px) + gap (10px) + child 4 (60px) = 130px <= 150px
        val measuredHeight = container.measurePolicy.measureHeight(container, 150.0f)
        assertEquals(70.0f, measuredHeight, "2 lines (30px + 30px) + 1 gap (10px) = 70px")

        container.setBounds(0.0f, 0.0f, 150.0f, measuredHeight)
        container.measurePolicy.layout(container, 0.0f, 0.0f, 150.0f, measuredHeight)

        // Line 1 children
        assertEquals(0.0f, container.children[0].bounds.x)
        assertEquals(40.0f, container.children[0].bounds.y)
        assertEquals(70.0f, container.children[1].bounds.x)
        assertEquals(40.0f, container.children[1].bounds.y)

        // Line 2 children
        assertEquals(0.0f, container.children[2].bounds.x)
        assertEquals(0.0f, container.children[2].bounds.y)
        assertEquals(70.0f, container.children[3].bounds.x)
        assertEquals(0.0f, container.children[3].bounds.y)
    }

    @Test
    fun `FlowRow measure policy is completely stateless across multiple passes`() {
        val policy = FlowRowMeasurePolicy(horizontalGap = 5.0f, verticalGap = 5.0f)

        val container1 = LayoutNode().apply { measurePolicy = policy }
        val child1 = LayoutNode().apply { minWidth = 100.0f; minHeight = 20.0f }
        container1.children.add(child1)

        val container2 = LayoutNode().apply { measurePolicy = policy }
        val child2 = LayoutNode().apply { minWidth = 50.0f; minHeight = 40.0f }
        container2.children.add(child2)

        val h1 = policy.measureHeight(container1, 200.0f)
        val h2 = policy.measureHeight(container2, 200.0f)

        assertEquals(20.0f, h1)
        assertEquals(40.0f, h2)
    }
}
