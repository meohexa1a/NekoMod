package org.hubdustry.graphics

import kotlin.math.*
import kotlin.test.*

/**
 * Empirical and mathematical verification harness for ARCHITECTURE_V3_DOSSIER.md
 * Validates:
 * 1. Inigo Quilez Rounded Box SDF & Boundary Cases
 * 2. Sub-pixel Micro-Antialiasing & Inner Border Color Arithmetic
 * 3. Analytical Scissor Multi-Rect Clipping & Nested Hierarchy
 * 4. Hardware Cost Model Metrics
 */
class SdfMathVerificationTest {

    /**
     * GLSL Inigo Quilez SDF implementation as specified in Section 3.1.C:
     * float computeRoundedBoxSDF(vec2 point, vec2 size, float radius) {
     *     vec2 halfSize = size * 0.5;
     *     vec2 p = point - halfSize;
     *     float r = min(radius, min(halfSize.x, halfSize.y));
     *     vec2 q = abs(p) - halfSize + r;
     *     return length(max(q, 0.0)) + min(max(q.x, q.y), 0.0) - r;
     * }
     */
    fun computeRoundedBoxSDF(pointX: Float, pointY: Float, sizeW: Float, sizeH: Float, radius: Float): Float {
        val halfW = sizeW * 0.5f
        val halfH = sizeH * 0.5f
        val px = pointX - halfW
        val py = pointY - halfH
        val r = minOf(radius, minOf(halfW, halfH))
        val qx = abs(px) - halfW + r
        val qy = abs(py) - halfH + r
        val maxQx = maxOf(qx, 0.0f)
        val maxQy = maxOf(qy, 0.0f)
        val lenMaxQ = hypot(maxQx, maxQy)
        val minMaxQ = minOf(maxOf(qx, qy), 0.0f)
        return lenMaxQ + minMaxQ - r
    }

    // --- TEST 1: INIGO QUILEZ SDF BOUNDARY CASES ---

    @Test
    fun testCenterAndEdgeDistances() {
        val w = 200f
        val h = 100f
        val r = 20f

        // 1. Center of the box (100, 50)
        // Distance should be -min(w/2, h/2) = -50 for a box of 200x100
        val dCenter = computeRoundedBoxSDF(100f, 50f, w, h, r)
        assertEquals(-50f, dCenter, 0.001f, "Center distance should be -50")

        // 2. Midpoint of straight edges
        // Top edge: (100, 0) -> distance should be 0
        val dTop = computeRoundedBoxSDF(100f, 0f, w, h, r)
        assertEquals(0f, dTop, 0.001f, "Top edge midpoint distance should be 0")

        // Right edge: (200, 50) -> distance should be 0
        val dRight = computeRoundedBoxSDF(200f, 50f, w, h, r)
        assertEquals(0f, dRight, 0.001f, "Right edge midpoint distance should be 0")

        // 3. Points outside straight edges
        // 10px above top edge: (100, -10) -> distance should be +10
        val dAbove = computeRoundedBoxSDF(100f, -10f, w, h, r)
        assertEquals(10f, dAbove, 0.001f, "10px outside top should be +10")

        // 4. Rounded corner center is at (w - r, r) = (180, 20)
        // Top-right corner arc at 45 degrees:
        // Angle 45 deg: point = (180 + 20 * cos(45°), 20 - 20 * sin(45°))
        val cornerX = 180f + 20f * cos(PI / 4).toFloat()
        val cornerY = 20f - 20f * sin(PI / 4).toFloat()
        val dCorner = computeRoundedBoxSDF(cornerX, cornerY, w, h, r)
        assertEquals(0f, dCorner, 0.005f, "Corner arc point distance should be 0")

        // 5. Point at the sharp bounding box corner (200, 0):
        // Distance from circle center (180, 20) is hypot(20, 20) = 20 * sqrt(2) ≈ 28.284
        // Distance to SDF boundary is 28.284 - 20 = 8.284
        val dSharpCorner = computeRoundedBoxSDF(200f, 0f, w, h, r)
        val expectedSharpDist = (20f * sqrt(2.0) - 20.0).toFloat()
        assertEquals(expectedSharpDist, dSharpCorner, 0.005f, "Sharp corner distance")
    }

    @Test
    fun testRadiusExceedingHalfDimensions() {
        val w = 100f
        val h = 40f
        val requestedRadius = 50f // r > h/2 (50 > 20)

        // The formula clamps r_safe = min(r, min(w/2, h/2)) = 20f
        // Capsule shape: caps of radius 20 at left and right
        val dCenter = computeRoundedBoxSDF(50f, 20f, w, h, requestedRadius)
        assertEquals(-20f, dCenter, 0.001f, "Center distance in capsule")

        val dRightApex = computeRoundedBoxSDF(100f, 20f, w, h, requestedRadius)
        assertEquals(0f, dRightApex, 0.001f, "Right apex on boundary")

        val dOutsideApex = computeRoundedBoxSDF(115f, 20f, w, h, requestedRadius)
        assertEquals(15f, dOutsideApex, 0.001f, "15px outside apex")
    }

    @Test
    fun testZeroAndDegenerateDimensions() {
        // Zero width: w = 0, h = 100
        val dLine = computeRoundedBoxSDF(0f, 50f, 0f, 100f, 10f)
        assertEquals(0f, dLine, 0.001f, "Line segment midpoint distance")

        val dLineOutsideX = computeRoundedBoxSDF(15f, 50f, 0f, 100f, 10f)
        assertEquals(15f, dLineOutsideX, 0.001f, "15px perpendicular to line segment")

        // Zero width and zero height: w = 0, h = 0 (point at 0, 0)
        val dPoint = computeRoundedBoxSDF(3f, 4f, 0f, 0f, 5f)
        assertEquals(5f, dPoint, 0.001f, "Distance to point (3, 4) should be hypot(3, 4) = 5")
    }

    @Test
    fun testExtremeAspectRatios() {
        // Extreme ribbon: 10000 x 2, r = 1
        val w = 10000f
        val h = 2f
        val r = 1f

        val dMid = computeRoundedBoxSDF(5000f, 1f, w, h, r)
        assertEquals(-1f, dMid, 0.001f, "Ribbon centerline distance")

        val dFarOutside = computeRoundedBoxSDF(-100f, 1f, w, h, r)
        assertEquals(100f, dFarOutside, 0.001f, "100px past ribbon left cap")
    }

    @Test
    fun testNegativeRadiusDefect() {
        // What happens if radius is negative due to missing Gateway Sanitization in PaintStyle?
        val w = 100f
        val h = 100f
        val negativeRadius = -10f

        val d = computeRoundedBoxSDF(50f, 50f, w, h, negativeRadius)
        // If radius is negative, r_safe = -10f, leading to corrupted distance!
        // At center (50, 50): px = 0, py = 0
        // q = |0| - 50 + (-10) = -60
        // min(max(-60, -60), 0) - (-10) = -60 + 10 = -50
        // BUT at corner (100, 100):
        val dCorner = computeRoundedBoxSDF(100f, 100f, w, h, negativeRadius)
        // Expected if clamped to 0: distance at (100, 100) for sharp rect is 0
        // But with negative radius: q = (50, 50) - 50 + (-10) = (-10, -10)
        // min(-10, 0) - (-10) = 0.
        // What about (105, 105)?
        val dOutsideCorner = computeRoundedBoxSDF(105f, 105f, w, h, negativeRadius)
        // q = (55, 55) - 50 + (-10) = (-5, -5). max(q, 0) = (0, 0).
        // length = 0. min(-5, 0) - (-10) = +5.
        // True Euclidean distance from (105, 105) to sharp rect corner (100, 100) is hypot(5, 5) ≈ 7.071!
        // But formula yields 5.0! An error of 29.3% in Euclidean metric!
        val trueDist = hypot(5.0, 5.0).toFloat()
        assertTrue(abs(dOutsideCorner - trueDist) > 1.5f,
            "Negative radius produces non-Euclidean distance distortion ($dOutsideCorner vs true $trueDist)")
    }

    // --- TEST 2: SUB-PIXEL MICRO-ANTIALIASING & INNER BORDER DEFECTS ---

    @Test
    fun testAntialiasingCoverageTransition() {
        val delta = 1.0f // 1 pixel fwidth

        // At boundary (d = 0): exactly 0.5 coverage
        val alphaBoundary = (0.5f - 0f / delta).coerceIn(0f, 1f)
        assertEquals(0.5f, alphaBoundary, 0.0001f)

        // At 0.5px inside (d = -0.5): 1.0 coverage
        val alphaInside = (0.5f - (-0.5f) / delta).coerceIn(0f, 1f)
        assertEquals(1.0f, alphaInside, 0.0001f)

        // At 0.5px outside (d = +0.5): 0.0 coverage
        val alphaOutside = (0.5f - 0.5f / delta).coerceIn(0f, 1f)
        assertEquals(0.0f, alphaOutside, 0.0001f)

        // Transition width is exactly delta (1.0 screen pixel)
    }

    @Test
    fun testInnerBorderColorBleedDefect() {
        // Trace the exact GLSL code from modern_ui.frag lines 652-661:
        // fillColor = Red (1, 0, 0, 1)
        // borderColor = Blue (0, 0, 1, 1)
        // borderWidth = 4.0, delta = 1.0
        val borderWidth = 4.0f
        val delta = 1.0f

        // Point C: On the outer antialiased edge of the widget (dist = 0.0)
        // This point is 4 pixels away from the inner core!
        // Visually, this edge belongs 100% to the Blue border!
        val dist = 0.0f
        val boxAlpha = (0.5f - dist / delta).coerceIn(0f, 1f) // 0.5
        val innerDist = dist + borderWidth // 4.0
        val innerAlpha = (0.5f - innerDist / delta).coerceIn(0f, 1f) // 0.0
        val borderCoverage = (boxAlpha - innerAlpha).coerceIn(0f, 1f) // 0.5

        // Line 657: fillColor = mix(fillColor, borderColor, borderCoverage * borderColor.a)
        val fillRed = 1.0f
        val fillBlue = 0.0f
        val borderRed = 0.0f
        val borderBlue = 1.0f

        val mixedRed = fillRed * (1f - borderCoverage) + borderRed * borderCoverage
        val mixedBlue = fillBlue * (1f - borderCoverage) + borderBlue * borderCoverage

        // BUG DEMONSTRATION:
        // The resulting color at the outer antialiased edge is PURPLE (50% red, 50% blue)!
        assertEquals(0.5f, mixedRed, 0.001f, "BUG: Fill color (red) leaked 50% into outer border edge!")
        assertEquals(0.5f, mixedBlue, 0.001f, "BUG: Border color (blue) diluted to 50%!")

        // The mathematically CORRECT mix factor should be borderCoverage / boxAlpha = 0.5 / 0.5 = 1.0
        // which would yield mixedRed = 0.0, mixedBlue = 1.0 (pure blue border antialiased to transparent).
        val correctFactor = borderCoverage / maxOf(boxAlpha, 0.0001f)
        val correctRed = fillRed * (1f - correctFactor) + borderRed * correctFactor
        val correctBlue = fillBlue * (1f - correctFactor) + borderBlue * correctFactor

        assertEquals(0.0f, correctRed, 0.001f, "Correct formulation has 0% red at outer border edge")
        assertEquals(1.0f, correctBlue, 0.001f, "Correct formulation has 100% blue at outer border edge")
    }

    @Test
    fun testTranslucentBoxDarkFringingDefect() {
        // Suppose box fill is transparent (alpha = 0), border is opaque white (1, 1, 1, 1)
        // borderWidth = 2.0, delta = 1.0
        val dist = 0.0f
        val delta = 1.0f
        val boxAlpha = (0.5f - dist / delta).coerceIn(0f, 1f) // 0.5
        val innerAlpha = (0.5f - (dist + 2.0f) / delta).coerceIn(0f, 1f) // 0.0
        val borderCoverage = (boxAlpha - innerAlpha).coerceIn(0f, 1f) // 0.5

        // fillColor = (0, 0, 0, 0)
        // mix((0, 0, 0, 0), (1, 1, 1, 1), 0.5) = (0.5, 0.5, 0.5, 0.5)
        val mixedRgb = 0.5f
        val mixedAlpha = 0.5f

        // Line 661: gl_FragColor = vec4(fillColor.rgb, fillColor.a * boxAlpha)
        val finalRgb = mixedRgb // 0.5
        val finalAlpha = mixedAlpha * boxAlpha // 0.5 * 0.5 = 0.25

        // In standard OpenGL straight-alpha blending (GL_SRC_ALPHA, GL_ONE_MINUS_SRC_ALPHA):
        // Effective screen intensity = finalRgb * finalAlpha = 0.5 * 0.25 = 0.125!
        // Instead of expected 0.5 * 0.5 = 0.25 (linear antialiasing).
        // This causes a dark halo / dark fringing around transparent/translucent bordered widgets!
        assertEquals(0.125f, finalRgb * finalAlpha, 0.001f,
            "BUG: Quadratic alpha darkening causes black halo artifact!")
    }

    // --- TEST 3: ANALYTICAL SCISSOR NESTED HIERARCHY LIMITATION ---

    @Test
    fun testNestedScissorHierarchyFailure() {
        // Scenario:
        // Grandparent: ScrollPane at (0, 0, 400, 400), rectangular scissor
        // Parent: Card Container at (50, 50, 300, 300), rounded radius R = 20, clipChildren = true
        // Child: Banner at (30, 30, 250, 100)

        // An analytical scissor in modern_ui.vert uses a SINGLE vec4 a_clipRect:
        // a_clipRect = (minX, minY, maxX, maxY)
        // Can a single vec4 represent:
        // 1. The intersection of Grandparent Rect [0..400] and Parent Card [50..350]? -> Yes, a smaller rect [50..350].
        // 2. The rounded corner arcs of the Parent Card? -> NO!
        // If Child renders a pixel at (52, 52) which is inside the bounding box [50..350],
        // but OUTSIDE the parent's rounded corner arc (radius 20):
        val parentW = 300f
        val parentH = 300f
        val parentR = 20f

        // Local coord of (52, 52) inside parent is (2, 2)
        // Distance to parent rounded box:
        val dInParent = computeRoundedBoxSDF(2f, 2f, parentW, parentH, parentR)
        // dInParent is POSITIVE (outside the rounded corner):
        assertTrue(dInParent > 0f, "Point (2, 2) is outside parent's rounded corner arc")

        // BUT modern_ui.frag line 622 only checks:
        // vec2 insideClip = step(v_clipRect.xy, v_screenCoord) * step(v_screenCoord, v_clipRect.zw);
        // At (52, 52), insideClip = (1.0, 1.0) -> PASSES! NOT DISCARDED!
        // Result: Child content leaks out of parent's rounded corners!
        assertTrue(52f >= 50f && 52f <= 350f, "Point is inside a_clipRect bounding box")
    }

    // --- TEST 4: HARDWARE COST MODEL AUDIT ---

    @Test
    fun testHardwareStrideAndVramBandwidthMismatch() {
        // Table 3.5 claims:
        // Tier 1 Stride: 10 floats = 40 bytes/vertex
        // Bandwidth for 100 widgets (400 vertices): 16.0 KB
        // Bandwidth for 500 widgets (2000 vertices): 80.0 KB
        // Bandwidth for 2000 widgets (8000 vertices): 320.0 KB

        val claimedStrideFloats = 10
        val claimedStrideBytes = claimedStrideFloats * 4 // 40 bytes

        assertEquals(16_000, 400 * claimedStrideBytes)
        assertEquals(80_000, 2000 * claimedStrideBytes)
        assertEquals(320_000, 8000 * claimedStrideBytes)

        // HOWEVER, in modern_ui.vert:
        // attribute vec2 a_position;    // 2
        // attribute vec2 a_texCoords;   // 2
        // attribute vec4 a_color;       // 1 (packed byte)
        // attribute vec4 a_borderColor; // 1 (packed byte)
        // attribute vec2 a_boxParams;   // 2
        // attribute vec2 a_styleParams; // 2
        // attribute vec4 a_clipRect;    // 4
        // Total = 2 + 2 + 1 + 1 + 2 + 2 + 4 = 14 floats = 56 bytes!
        val actualStrideFloats = 14
        val actualStrideBytes = actualStrideFloats * 4 // 56 bytes

        val actualBandwidth100 = 400 * actualStrideBytes // 22,400 bytes (22.4 KB)
        val actualBandwidth500 = 2000 * actualStrideBytes // 112,000 bytes (112.0 KB)
        val actualBandwidth2000 = 8000 * actualStrideBytes // 448,000 bytes (448.0 KB)

        val understatementPercent = ((actualStrideBytes - claimedStrideBytes).toDouble() / claimedStrideBytes) * 100.0
        assertEquals(40.0, understatementPercent, 0.01,
            "Claimed VRAM bandwidth understates reality by exactly 40% due to omitting a_clipRect!")
    }

    @Test
    fun testGpuAluOperationCountUnderstatement() {
        // Table 3.5 claims: ~28 ALU ops / pixel for Tier 1
        // Let's count operations in modern_ui.frag:
        // 1. Scissor test: step(2) + step(2) + mul(1) + cmp(1) = 6 ops
        // 2. SDF: halfSize(1) + p(2) + r_min(2) + q(4) + max(2) + length(4) + max(1) + min(1) + add_sub(2) = 19 ops
        // 3. fwidth: dFdx(1) + dFdy(1) + abs(2) + add(1) + max(1) = 6 ops
        // 4. boxAlpha clamp: div(1) + sub(1) + clamp(2) = 4 ops
        // 5. Texture + Tint: texSample(texture op) + mul(4) = 4 ops
        // 6. Inner Border: innerDist(1) + innerAlpha(4) + borderCoverage(3) + mix(4) = 12 ops
        // 7. Output color: alpha mul(1) = 1 op
        val totalAluOps = 6 + 19 + 6 + 4 + 4 + 12 + 1 // 52 ALU ops!
        assertTrue(totalAluOps > 45, "Total fragment ops exceeds 45 ALU ops vs claimed ~28 ALU ops")
    }
}
