package org.mdt.ui.theme

import arc.graphics.Color

/**
 * ## ColorTokens
 *
 * Comprehensive Apple iOS-inspired semantic color design system.
 * Provides translucent glass tints, surface palettes, system accents, and text hierarchies.
 *
 * See: docs/design-system/design_system_en.md
 */
data class ColorTokens(
    // ==========================================
    // I. Apple System Accents
    // ==========================================
    val systemBlue: Color = Color.valueOf("007aff"),
    val systemGreen: Color = Color.valueOf("34c759"),
    val systemIndigo: Color = Color.valueOf("5856d6"),
    val systemOrange: Color = Color.valueOf("ff9500"),
    val systemPink: Color = Color.valueOf("ff2d55"),
    val systemPurple: Color = Color.valueOf("af52de"),
    val systemRed: Color = Color.valueOf("ff3b30"),
    val systemTeal: Color = Color.valueOf("30b0c7"),
    val systemYellow: Color = Color.valueOf("ffcc00"),

    // ==========================================
    // II. Frosted Glass & Material Tints
    // ==========================================
    val glassUltraThin: Color = Color(1f, 1f, 1f, 0.04f),
    val glassThin: Color = Color(1f, 1f, 1f, 0.08f),
    val glassRegular: Color = Color(1f, 1f, 1f, 0.14f),
    val glassThick: Color = Color(1f, 1f, 1f, 0.22f),
    val glassActive: Color = Color(1f, 1f, 1f, 0.28f),

    // Specular highlight borders (hairline glass edge light)
    val glassBorderSubtle: Color = Color(1f, 1f, 1f, 0.08f),
    val glassBorderRegular: Color = Color(1f, 1f, 1f, 0.14f),
    val glassBorderActive: Color = Color(1f, 1f, 1f, 0.30f),

    // ==========================================
    // III. Surface & Container Backgrounds
    // ==========================================
    val background: Color = Color.valueOf("0b0c10"),
    val surfacePrimary: Color = Color.valueOf("14161f"),
    val surfaceSecondary: Color = Color.valueOf("1c1f2b"),
    val surfaceTertiary: Color = Color.valueOf("252938"),
    val surfaceElevated: Color = Color.valueOf("2d3244"),
    val surfaceGrouped: Color = Color(0.08f, 0.09f, 0.13f, 0.24f),

    // ==========================================
    // IV. Text & Typography Hierarchy
    // ==========================================
    val textPrimary: Color = Color.valueOf("ffffff"),
    val textSecondary: Color = Color(1f, 1f, 1f, 0.70f),
    val textTertiary: Color = Color(1f, 1f, 1f, 0.45f),
    val textQuaternary: Color = Color(1f, 1f, 1f, 0.25f),
    val textOnAccent: Color = Color.valueOf("ffffff"),

    // ==========================================
    // V. Overlays, Separators & Shadows
    // ==========================================
    val divider: Color = Color(1f, 1f, 1f, 0.10f),
    val scrim: Color = Color(0f, 0f, 0f, 0.60f),
    val shadowAmbient: Color = Color(0f, 0f, 0f, 0.35f),
    val shadowKey: Color = Color(0f, 0f, 0f, 0.55f),
    val glowAccent: Color = Color(0f, 0.48f, 1f, 0.40f)
)
