package org.hubdustry.core.graphics

import arc.graphics.Color
import org.hubdustry.core.compose.modifier.Modifier
import org.hubdustry.core.compose.modifier.applyTo
import org.hubdustry.core.compose.modifier.*
import org.hubdustry.core.layout.LayoutNode
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class UIBatchTest {

    @BeforeEach
    @AfterEach
    fun resetBatch() {
        UIBatch.end()
    }

    @Test
    fun testRoundedCornersConstructors() {
        val none = RoundedCorners.None
        assertEquals(0f, none.topStart)
        assertEquals(0f, none.topEnd)
        assertEquals(0f, none.bottomEnd)
        assertEquals(0f, none.bottomStart)
        assertTrue(none.isZero)

        val uniformRound = RoundedCorners(12f)
        assertEquals(12f, uniformRound.topStart)
        assertEquals(12f, uniformRound.topEnd)
        assertEquals(12f, uniformRound.bottomEnd)
        assertEquals(12f, uniformRound.bottomStart)
        assertFalse(uniformRound.isZero)

        val asymmetricRound = RoundedCorners(
            topStart = 4f,
            topEnd = 8f,
            bottomEnd = 16f,
            bottomStart = 24f
        )
        assertEquals(4f, asymmetricRound.topStart)
        assertEquals(8f, asymmetricRound.topEnd)
        assertEquals(16f, asymmetricRound.bottomEnd)
        assertEquals(24f, asymmetricRound.bottomStart)

        val topBottomRound = RoundedCorners(top = 10f, bottom = 20f)
        assertEquals(10f, topBottomRound.topStart)
        assertEquals(10f, topBottomRound.topEnd)
        assertEquals(20f, topBottomRound.bottomEnd)
        assertEquals(20f, topBottomRound.bottomStart)

        assertTrue(RoundedCorners.Circle.topStart >= 9999f)
    }

    @Test
    fun testLayoutNodeCornerRadiusBorderAndClipGatewaySanitization() {
        val node = LayoutNode()

        // Default state
        assertEquals(0f, node.cornerRadiusTopStart)
        assertEquals(0f, node.cornerRadiusTopEnd)
        assertEquals(0f, node.cornerRadiusBottomEnd)
        assertEquals(0f, node.cornerRadiusBottomStart)
        assertFalse(node.hasRoundedCorners)
        assertEquals(0f, node.borderWidth)
        assertFalse(node.hasBorder)
        assertFalse(node.clip)

        // Gateway sanitization for negative values and NaN
        node.cornerRadiusTopStart = -5f
        node.cornerRadiusTopEnd = Float.NaN
        node.cornerRadiusBottomEnd = 10f
        node.cornerRadiusBottomStart = 15f
        assertEquals(0f, node.cornerRadiusTopStart, "Negative radius must be sanitized to 0f")
        assertEquals(0f, node.cornerRadiusTopEnd, "NaN radius must be sanitized to 0f")
        assertEquals(10f, node.cornerRadiusBottomEnd)
        assertEquals(15f, node.cornerRadiusBottomStart)
        assertTrue(node.hasRoundedCorners)

        // Border sanitization
        node.borderWidth = -2f
        node.borderColor = Color.royal
        assertEquals(0f, node.borderWidth, "Negative borderWidth must be sanitized to 0f")
        assertFalse(node.hasBorder)

        node.borderWidth = 2f
        assertTrue(node.hasBorder)

        // Clip property
        node.clip = true
        assertTrue(node.clip)

        // Reset modifier state cleans all SDF tokens and clip
        node.resetModifierState()
        assertEquals(0f, node.cornerRadiusTopStart)
        assertEquals(0f, node.cornerRadiusTopEnd)
        assertEquals(0f, node.cornerRadiusBottomEnd)
        assertEquals(0f, node.cornerRadiusBottomStart)
        assertFalse(node.hasRoundedCorners)
        assertEquals(0f, node.borderWidth)
        assertFalse(node.hasBorder)
        assertFalse(node.clip)
    }

    @Test
    fun testModifiersApplyToSdfAndClipTokens() {
        val node = LayoutNode()

        // 1. Modifier.background(color, corners)
        val bgMod = Modifier.background(Color.blue, RoundedCorners(8f))
        bgMod.applyTo(node)
        assertEquals(Color.blue, node.backgroundColor)
        assertEquals(8f, node.cornerRadiusTopStart)
        assertEquals(8f, node.cornerRadiusTopEnd)
        assertEquals(8f, node.cornerRadiusBottomEnd)
        assertEquals(8f, node.cornerRadiusBottomStart)

        // 2. Modifier.border(width, color, corners)
        val borderMod = Modifier.border(3f, Color.white, RoundedCorners(top = 4f, bottom = 12f))
        borderMod.applyTo(node)
        assertEquals(3f, node.borderWidth)
        assertEquals(Color.white, node.borderColor)
        assertEquals(4f, node.cornerRadiusTopStart)
        assertEquals(4f, node.cornerRadiusTopEnd)
        assertEquals(12f, node.cornerRadiusBottomEnd)
        assertEquals(12f, node.cornerRadiusBottomStart)

        // 3. Modifier.clip(corners) sets clipCornerRadius, cornerRadius (if unset), AND enables node.clip = true
        val clipMod = Modifier.clip(RoundedCorners(20f))
        val clipNode = LayoutNode()
        clipMod.applyTo(clipNode)
        assertEquals(20f, clipNode.cornerRadiusTopStart)
        assertEquals(20f, clipNode.cornerRadiusBottomEnd)
        assertEquals(20f, clipNode.clipRadiusTopStart)
        assertEquals(20f, clipNode.clipRadiusBottomEnd)
        assertTrue(clipNode.clip, "Modifier.clip should set node.clip = true")

        // 4. Modifier.clipToBounds()
        val node2 = LayoutNode()
        assertFalse(node2.clip)
        Modifier.clipToBounds().applyTo(node2)
        assertTrue(node2.clip, "Modifier.clipToBounds should set node.clip = true")

        // 5. Modifier.textColor
        val textMod = Modifier.textColor(Color.yellow)
        textMod.applyTo(node)
        assertEquals(Color.yellow, node.textColor)
    }

    @Test
    fun testUIBatchQuadLayoutAndAttributes() {
        UIBatch.begin()

        UIBatch.drawBox(
            x = 10f,
            y = 20f,
            width = 100f,
            height = 50f,
            radius = 8f,
            color = Color.red,
            borderWidth = 2f,
            borderColor = Color.blue
        )

        assertEquals(1, UIBatch.queuedQuadCount)
        assertEquals(104, UIBatch.vertexIndex, "1 quad * 4 vertices * 26 floats = 104 floats")

        val buf = UIBatch.vertexBuffer

        // Vertex 0: Bottom-Left (10, 20)
        // a_position (xy = screen pos, zw = uv)
        assertEquals(10f, buf[0])
        assertEquals(20f, buf[1])
        // a_color (packed float)
        assertEquals(Color.red.toFloatBits(), buf[4])
        // a_boxData (xy = local pos, zw = dimensions)
        assertEquals(0f, buf[5])
        assertEquals(0f, buf[6])
        assertEquals(100f, buf[7])
        assertEquals(50f, buf[8])
        // a_style (x = radius, y = borderWidth, z = mode, w = texUnit)
        assertEquals(8f, buf[9])
        assertEquals(2f, buf[10])
        assertEquals(UIBatch.MODE_BOX, buf[11])
        assertEquals(0f, buf[12])
        // a_cornerRadii (x = topStart, y = topEnd, z = bottomEnd, w = bottomStart)
        assertEquals(8f, buf[13])
        assertEquals(8f, buf[14])
        assertEquals(8f, buf[15])
        assertEquals(8f, buf[16])
        // a_borderColor
        assertEquals(Color.blue.toFloatBits(), buf[17])
        // a_clipRect (default unclipped minX, minY, maxX, maxY)
        assertEquals(-UIBatch.UNCLIPPED_BOUND, buf[18])
        assertEquals(-UIBatch.UNCLIPPED_BOUND, buf[19])
        assertEquals(UIBatch.UNCLIPPED_BOUND, buf[20])
        assertEquals(UIBatch.UNCLIPPED_BOUND, buf[21])
        // a_clipRadii (default clip corner radii: 0f)
        assertEquals(0f, buf[22])
        assertEquals(0f, buf[23])
        assertEquals(0f, buf[24])
        assertEquals(0f, buf[25])

        // Vertex 1: Top-Left (10, 70)
        val v1Offset = 26
        assertEquals(10f, buf[v1Offset])
        assertEquals(70f, buf[v1Offset + 1])
        assertEquals(0f, buf[v1Offset + 5])  // localX
        assertEquals(50f, buf[v1Offset + 6]) // localY = height

        // Vertex 2: Top-Right (110, 70)
        val v2Offset = 52
        assertEquals(110f, buf[v2Offset])
        assertEquals(70f, buf[v2Offset + 1])
        assertEquals(100f, buf[v2Offset + 5]) // localX = width
        assertEquals(50f, buf[v2Offset + 6])  // localY = height

        // Vertex 3: Bottom-Right (110, 20)
        val v3Offset = 78
        assertEquals(110f, buf[v3Offset])
        assertEquals(20f, buf[v3Offset + 1])
        assertEquals(100f, buf[v3Offset + 5]) // localX = width
        assertEquals(0f, buf[v3Offset + 6])   // localY

        UIBatch.end()
    }

    @Test
    fun testAsymmetric4CornerRadiiDrawBox() {
        UIBatch.begin()

        UIBatch.drawBox(
            x = 0f,
            y = 0f,
            width = 200f,
            height = 100f,
            radiusTopStart = 4f,
            radiusTopEnd = 8f,
            radiusBottomEnd = 16f,
            radiusBottomStart = 24f,
            color = Color.white
        )

        assertEquals(1, UIBatch.queuedQuadCount)
        val buf = UIBatch.vertexBuffer

        // Verify a_cornerRadii attribute values across all 4 vertices
        for (v in 0 until 4) {
            val offset = v * 26
            assertEquals(4f, buf[offset + 13], "Vertex $v topStart radius")
            assertEquals(8f, buf[offset + 14], "Vertex $v topEnd radius")
            assertEquals(16f, buf[offset + 15], "Vertex $v bottomEnd radius")
            assertEquals(24f, buf[offset + 16], "Vertex $v bottomStart radius")
        }

        UIBatch.end()
    }

    @Test
    fun testAnalyticalScissorClippingStack() {
        UIBatch.begin()

        assertEquals(-UIBatch.UNCLIPPED_BOUND, UIBatch.clipMinX)
        assertEquals(-UIBatch.UNCLIPPED_BOUND, UIBatch.clipMinY)
        assertEquals(UIBatch.UNCLIPPED_BOUND, UIBatch.clipMaxX)
        assertEquals(UIBatch.UNCLIPPED_BOUND, UIBatch.clipMaxY)
        assertEquals(0, UIBatch.clipDepth)

        // 1. Push Container Clip [10, 20, 200, 150] -> Rect [10..210, 20..170]
        UIBatch.pushClip(10f, 20f, 200f, 150f)
        assertEquals(1, UIBatch.clipDepth)
        assertEquals(10f, UIBatch.clipMinX)
        assertEquals(20f, UIBatch.clipMinY)
        assertEquals(210f, UIBatch.clipMaxX)
        assertEquals(170f, UIBatch.clipMaxY)

        // 2. Push Nested Child Clip [50, 50, 300, 200] -> Rect [50..350, 50..250]
        // Intersection should be: minX=max(10, 50)=50, minY=max(20, 50)=50, maxX=min(210, 350)=210, maxY=min(170, 250)=170
        UIBatch.pushClip(50f, 50f, 300f, 200f)
        assertEquals(2, UIBatch.clipDepth)
        assertEquals(50f, UIBatch.clipMinX)
        assertEquals(50f, UIBatch.clipMinY)
        assertEquals(210f, UIBatch.clipMaxX)
        assertEquals(170f, UIBatch.clipMaxY)

        // Draw quad inside nested clip - must carry intersection clip and CLIP_ON
        UIBatch.drawBox(60f, 60f, 50f, 50f)
        val buf = UIBatch.vertexBuffer
        assertEquals(UIBatch.CLIP_ON, buf[12], "Vertex must receive CLIP_ON inside clip container")
        assertEquals(50f, buf[18], "Vertex must receive intersected clipMinX")
        assertEquals(50f, buf[19], "Vertex must receive intersected clipMinY")
        assertEquals(210f, buf[20], "Vertex must receive intersected clipMaxX")
        assertEquals(170f, buf[21], "Vertex must receive intersected clipMaxY")

        // 3. Pop nested clip -> restores parent clip
        UIBatch.popClip()
        assertEquals(1, UIBatch.clipDepth)
        assertEquals(10f, UIBatch.clipMinX)
        assertEquals(20f, UIBatch.clipMinY)
        assertEquals(210f, UIBatch.clipMaxX)
        assertEquals(170f, UIBatch.clipMaxY)

        // 4. Pop container clip -> restores root clip
        UIBatch.popClip()
        assertEquals(0, UIBatch.clipDepth)
        assertEquals(-UIBatch.UNCLIPPED_BOUND, UIBatch.clipMinX)
        assertEquals(-UIBatch.UNCLIPPED_BOUND, UIBatch.clipMinY)
        assertEquals(UIBatch.UNCLIPPED_BOUND, UIBatch.clipMaxX)
        assertEquals(UIBatch.UNCLIPPED_BOUND, UIBatch.clipMaxY)

        // Draw quad outside clip - must carry CLIP_OFF
        UIBatch.drawBox(0f, 0f, 10f, 10f)
        val secondQuadOffset = 104
        assertEquals(UIBatch.CLIP_OFF, buf[secondQuadOffset + 12], "Vertex must receive CLIP_OFF when unclipped")

        UIBatch.end()
    }

    @Test
    fun testInterleavedZIndexStream() {
        UIBatch.begin()

        // 1. Parent Box Background (MODE_BOX)
        UIBatch.drawBox(x = 0f, y = 0f, width = 300f, height = 200f, radius = 10f, color = Color.darkGray)

        // 2. Parent Text Glyph (MODE_TEXT)
        UIBatch.drawGlyph(
            x = 20f, y = 100f, width = 16f, height = 24f,
            uvMinU = 0f, uvMinV = 0f, uvMaxU = 0.1f, uvMaxV = 0.1f,
            color = Color.white
        )

        // 3. Child Card / Badge Background overlapping on top of parent text (MODE_BOX)
        UIBatch.drawBox(x = 10f, y = 90f, width = 50f, height = 40f, radius = 4f, color = Color.scarlet)

        // 4. Child Badge Text Glyph (MODE_TEXT)
        UIBatch.drawGlyph(
            x = 15f, y = 95f, width = 12f, height = 18f,
            uvMinU = 0.2f, uvMinV = 0.2f, uvMaxU = 0.3f, uvMaxV = 0.3f,
            color = Color.yellow
        )

        assertEquals(4, UIBatch.queuedQuadCount, "All 4 quads must be queued in exact preorder sequence")
        assertEquals(104 * 4, UIBatch.vertexIndex)

        val buf = UIBatch.vertexBuffer

        // Quad 0: Parent Box (mode at offset 11)
        assertEquals(UIBatch.MODE_BOX, buf[11], "Quad 0 must be MODE_BOX")
        assertEquals(10f, buf[9], "Quad 0 radius")

        // Quad 1: Parent Text Glyph (mode at offset 104 + 11 = 115)
        assertEquals(UIBatch.MODE_TEXT, buf[104 + 11], "Quad 1 must be MODE_TEXT")
        assertEquals(20f, buf[104], "Quad 1 drawX")

        // Quad 2: Child Card Background (mode at offset 208 + 11 = 219)
        assertEquals(UIBatch.MODE_BOX, buf[208 + 11], "Quad 2 must be MODE_BOX")
        assertEquals(4f, buf[208 + 9], "Quad 2 radius")

        // Quad 3: Child Text Glyph (mode at offset 312 + 11 = 323)
        assertEquals(UIBatch.MODE_TEXT, buf[312 + 11], "Quad 3 must be MODE_TEXT")
        assertEquals(15f, buf[312], "Quad 3 drawX")

        UIBatch.end()
    }

    @Test
    fun testTextGlyphSharpScissorClipping() {
        UIBatch.begin()

        // 1. Đẩy clip container với bo góc SDF 16px
        UIBatch.pushClip(
            x = 10f, y = 10f, width = 200f, height = 100f,
            radiusTopStart = 16f, radiusTopEnd = 16f, radiusBottomEnd = 16f, radiusBottomStart = 16f
        )
        assertEquals(16f, UIBatch.clipRadiusTopStart, "Container clip phải nhận bo góc 16px")

        // 2. Dựng hình ký tự văn bản BMFont bên trong container bo góc
        UIBatch.drawGlyph(
            x = 20f, y = 20f, width = 12f, height = 18f,
            uvMinU = 0f, uvMinV = 0f, uvMaxU = 0.1f, uvMaxV = 0.1f,
            color = Color.white
        )

        val buf = UIBatch.vertexBuffer

        // 3. Kiểm chứng bất biến Text Sharp Scissor: toàn bộ 4 đỉnh của glyph
        // phải có clipActive = CLIP_ON nhưng 4 bán kính bo góc clip (a_clipRadii) BẮT BUỘC bằng 0f
        for (v in 0 until 4) {
            val offset = v * 26
            assertEquals(UIBatch.CLIP_ON, buf[offset + 12], "Vertex $v phải bật cờ CLIP_ON")
            assertEquals(UIBatch.MODE_TEXT, buf[offset + 11], "Vertex $v phải là MODE_TEXT")
            assertEquals(0f, buf[offset + 22], "Vertex $v clipRadiusTopStart phải luôn là 0f (Sharp Scissor)")
            assertEquals(0f, buf[offset + 23], "Vertex $v clipRadiusTopEnd phải luôn là 0f (Sharp Scissor)")
            assertEquals(0f, buf[offset + 24], "Vertex $v clipRadiusBottomEnd phải luôn là 0f (Sharp Scissor)")
            assertEquals(0f, buf[offset + 25], "Vertex $v clipRadiusBottomStart phải luôn là 0f (Sharp Scissor)")
        }

        UIBatch.popClip()
        UIBatch.end()
    }

    @Test
    fun testTexturedBoxWithCornerRadiusAndBorder() {
        UIBatch.begin()

        // Draw a textured avatar with bo góc 16px and border 2px
        val region = arc.graphics.g2d.TextureRegion().apply {
            u = 0.25f
            v2 = 0.25f
            u2 = 0.5f
            v = 0.5f
        }
        UIBatch.drawBox(
            x = 50f,
            y = 50f,
            width = 64f,
            height = 64f,
            region = region,
            radius = 16f,
            color = Color.white,
            borderWidth = 2f,
            borderColor = Color.gold
        )

        assertEquals(1, UIBatch.queuedQuadCount)
        val buf = UIBatch.vertexBuffer

        // Mode is MODE_BOX
        assertEquals(UIBatch.MODE_BOX, buf[11])
        // Radius and borderWidth
        assertEquals(16f, buf[9])
        assertEquals(2f, buf[10])
        // UVs
        assertEquals(0.25f, buf[2]) // BL u
        assertEquals(0.25f, buf[3]) // BL v
        assertEquals(0.25f, buf[26 + 2]) // TL u
        assertEquals(0.5f, buf[26 + 3]) // TL v2
        assertEquals(0.5f, buf[52 + 2]) // TR u2
        assertEquals(0.5f, buf[52 + 3]) // TR v2
        assertEquals(0.5f, buf[78 + 2]) // BR u2
        assertEquals(0.25f, buf[78 + 3]) // BR v

        UIBatch.end()
    }

    @Test
    fun testHeadlessResilience() {
        // In headless testing environment (Core.gl == null), UIBatch must never throw!
        assertFalse(UIBatch.isSupported, "In headless test environment, isSupported should be false")

        UIBatch.begin()
        UIBatch.drawBox(0f, 0f, 100f, 100f)
        UIBatch.flush()
        UIBatch.end()

        assertEquals(0, UIBatch.queuedQuadCount)
        assertEquals(0, UIBatch.vertexIndex)
    }

    @Test
    fun testClipStackOverflowSafetyAndMatchingPops() {
        UIBatch.begin()
        val initialMinX = UIBatch.clipMinX
        val initialMaxX = UIBatch.clipMaxX

        // Push 70 times (exceeds MAX_CLIP_DEPTH = 64)
        for (i in 0 until 70) {
            UIBatch.pushClip(i.toFloat(), i.toFloat(), 500f, 500f)
        }
        assertEquals(70, UIBatch.clipDepth)

        // Pop all 70 times: must restore smoothly without crashing or corrupting
        for (i in 0 until 70) {
            UIBatch.popClip()
        }
        assertEquals(0, UIBatch.clipDepth)
        assertEquals(initialMinX, UIBatch.clipMinX)
        assertEquals(initialMaxX, UIBatch.clipMaxX)

        // Extra pop at depth 0 must be safe
        UIBatch.popClip()
        assertEquals(0, UIBatch.clipDepth)
        assertEquals(-UIBatch.UNCLIPPED_BOUND, UIBatch.clipMinX)
        assertEquals(UIBatch.UNCLIPPED_BOUND, UIBatch.clipMaxX)

        UIBatch.end()
    }
}
