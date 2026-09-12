package org.hubdustry.libs.layout

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertSame

class LayoutNodeMutationTest {

    @Test
    fun testAddChildWithIndex() {
        val root = LayoutNode()
        val c1 = LayoutNode()
        val c2 = LayoutNode()
        val c3 = LayoutNode()

        root.addChild(c1, 0)
        root.addChild(c3, 1)
        root.addChild(c2, 1) // Chèn vào giữa c1 và c3

        assertEquals(3, root.children.size)
        assertSame(c1, root.children[0])
        assertSame(c2, root.children[1])
        assertSame(c3, root.children[2])
        assertSame(root, c2.parent)

        // Test kẹp biên an toàn khi index âm hoặc vượt quá size
        val cFirst = LayoutNode()
        root.addChild(cFirst, -99)
        assertSame(cFirst, root.children[0])

        val cLast = LayoutNode()
        root.addChild(cLast, 999)
        assertSame(cLast, root.children[root.children.size - 1])
    }

    @Test
    fun testRemoveChildrenRange() {
        val root = LayoutNode()
        val nodes = (0..4).map { LayoutNode() }
        nodes.forEach { root.addChild(it) }

        assertEquals(5, root.children.size)

        // Xóa 2 phần tử ở giữa (index 1 và 2)
        val removed1 = nodes[1]
        val removed2 = nodes[2]
        root.removeChildren(1, 2)

        assertEquals(3, root.children.size)
        assertSame(nodes[0], root.children[0])
        assertSame(nodes[3], root.children[1])
        assertSame(nodes[4], root.children[2])

        assertNull(removed1.parent)
        assertNull(removed2.parent)

        // Kiểm tra an toàn khi count hoặc index bất thường
        root.removeChildren(0, 0) // count = 0 không ảnh hưởng
        assertEquals(3, root.children.size)

        root.removeChildren(1, 999) // count vượt quá biên -> chỉ xóa phần còn lại
        assertEquals(1, root.children.size)
        assertSame(nodes[0], root.children[0])
    }

    @Test
    fun testMoveChildren() {
        val root = LayoutNode()
        val n0 = LayoutNode()
        val n1 = LayoutNode()
        val n2 = LayoutNode()
        val n3 = LayoutNode()

        listOf(n0, n1, n2, n3).forEach { root.addChild(it) }

        // Di chuyển n0 xuống sau n2 (from=0, to=3, count=1)
        root.moveChildren(from = 0, to = 3, count = 1)
        // Kết quả mong đợi: n1, n2, n0, n3
        assertSame(n1, root.children[0])
        assertSame(n2, root.children[1])
        assertSame(n0, root.children[2])
        assertSame(n3, root.children[3])

        // Di chuyển khối 2 phần tử (n2, n0) lên đầu
        root.moveChildren(from = 1, to = 0, count = 2)
        // Kết quả mong đợi: n2, n0, n1, n3
        assertSame(n2, root.children[0])
        assertSame(n0, root.children[1])
        assertSame(n1, root.children[2])
        assertSame(n3, root.children[3])
    }
}
