package org.hubdustry.core.graphics

import arc.graphics.Color
import org.hubdustry.core.compose.Modifier
import org.hubdustry.core.compose.applyTo
import org.hubdustry.core.compose.modifier.*
import org.hubdustry.core.layout.LayoutNode
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class UIBatchTest {

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

        // 3. Modifier.clip(corners) sets cornerRadius AND enables node.clip = true
        val clipMod = Modifier.clip(RoundedCorners(20f))
        clipMod.applyTo(node)
        assertEquals(20f, node.cornerRadiusTopStart)
        assertEquals(20f, node.cornerRadiusBottomEnd)
        assertTrue(node.clip, "Modifier.clip should set node.clip = true")

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
        assertEquals(88, UIBatch.vertexIndex, "1 quad * 4 vertices * 22 floats = 88 floats")

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
        assertEquals(-100000f, buf[18])
        assertEquals(-100000f, buf[19])
        assertEquals(100000f, buf[20])
        assertEquals(100000f, buf[21])

        // Vertex 1: Top-Left (10, 70)
        val v1Offset = 22
        assertEquals(10f, buf[v1Offset])
        assertEquals(70f, buf[v1Offset + 1])
        assertEquals(0f, buf[v1Offset + 5])  // localX
        assertEquals(50f, buf[v1Offset + 6]) // localY = height

        // Vertex 2: Top-Right (110, 70)
        val v2Offset = 44
        assertEquals(110f, buf[v2Offset])
        assertEquals(70f, buf[v2Offset + 1])
        assertEquals(100f, buf[v2Offset + 5]) // localX = width
        assertEquals(50f, buf[v2Offset + 6])  // localY = height

        // Vertex 3: Bottom-Right (110, 20)
        val v3Offset = 66
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
            val offset = v * 22
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

        assertEquals(-100000f, UIBatch.clipMinX)
        assertEquals(-100000f, UIBatch.clipMinY)
        assertEquals(100000f, UIBatch.clipMaxX)
        assertEquals(100000f, UIBatch.clipMaxY)
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

        // Draw quad inside nested clip - must carry intersection clip
        UIBatch.drawBox(60f, 60f, 50f, 50f)
        val buf = UIBatch.vertexBuffer
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
        assertEquals(-100000f, UIBatch.clipMinX)
        assertEquals(-100000f, UIBatch.clipMinY)
        assertEquals(100000f, UIBatch.clipMaxX)
        assertEquals(100000f, UIBatch.clipMaxY)

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
        assertEquals(88 * 4, UIBatch.vertexIndex)

        val buf = UIBatch.vertexBuffer

        // Quad 0: Parent Box (mode at offset 11)
        assertEquals(UIBatch.MODE_BOX, buf[11], "Quad 0 must be MODE_BOX")
        assertEquals(10f, buf[9], "Quad 0 radius")

        // Quad 1: Parent Text Glyph (mode at offset 88 + 11 = 99)
        assertEquals(UIBatch.MODE_TEXT, buf[88 + 11], "Quad 1 must be MODE_TEXT")
        assertEquals(20f, buf[88], "Quad 1 drawX")

        // Quad 2: Child Card Background (mode at offset 176 + 11 = 187)
        assertEquals(UIBatch.MODE_BOX, buf[176 + 11], "Quad 2 must be MODE_BOX")
        assertEquals(4f, buf[176 + 9], "Quad 2 radius")

        // Quad 3: Child Text Glyph (mode at offset 264 + 11 = 275)
        assertEquals(UIBatch.MODE_TEXT, buf[264 + 11], "Quad 3 must be MODE_TEXT")
        assertEquals(15f, buf[264], "Quad 3 drawX")

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
        assertEquals(0.25f, buf[22 + 2]) // TL u
        assertEquals(0.5f, buf[22 + 3]) // TL v2
        assertEquals(0.5f, buf[44 + 2]) // TR u2
        assertEquals(0.5f, buf[44 + 3]) // TR v2
        assertEquals(0.5f, buf[66 + 2]) // BR u2
        assertEquals(0.25f, buf[66 + 3]) // BR v

        UIBatch.end()
    }

    @Test
    fun testHeadlessResilienceAndDispose() {
        // In headless testing environment (Core.gl == null), UIBatch must never throw!
        assertFalse(UIBatch.isSupported, "In headless test environment, isSupported should be false")

        UIBatch.begin()
        UIBatch.drawBox(0f, 0f, 100f, 100f)
        UIBatch.flush()
        UIBatch.end()
        UIBatch.dispose()

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
        assertEquals(-100000.0f, UIBatch.clipMinX)
        assertEquals(100000.0f, UIBatch.clipMaxX)

        UIBatch.end()
    }
}
