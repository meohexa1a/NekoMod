package org.mdt.ui.theme

import arc.graphics.g2d.Font
import mindustry.ui.Fonts
import org.mdt.core.ui.graphics.Color

/**
 * ## SpacingTokens
 *
 * Mathematical 4pt/8pt modular spacing grid standards (Apple Human Interface Guidelines).
 * All values are strictly [Float] primitives (Rule 1).
 *
 * See: docs/design-system/design_system_en.md
 */
data class SpacingTokens(
    val xxs: Float = 2f,
    val xs: Float = 4f,
    val sm: Float = 8f,
    val md: Float = 12f,
    val lg: Float = 16f,
    val xl: Float = 20f,
    val xxl: Float = 24f,
    val huge: Float = 32f
)

/**
 * ## ShapeTokens
 *
 * Geometric continuous corner curvature standards inspired by Apple macOS / iOS Human Interface Guidelines.
 *
 * See: docs/design-system/design_system_en.md
 */
data class ShapeTokens(
    /** Micro elements, tags, and fine chips (4px). */
    val xs: Float = 4f,

    /** Secondary buttons, small badges, and inner nested items (6px). */
    val sm: Float = 6f,

    /** Interactive buttons, text fields, and segmented control chips (8px). */
    val md: Float = 8f,

    /** Standard cards, surface sheets, and popup inspectors (12px). */
    val lg: Float = 12f,

    /** Prominent window frames, modal dialogs, and large hero panels (16px). */
    val xl: Float = 16f,

    /** Continuous squircle pill curves for docks, status pills, and toggle tracks (999px). */
    val pill: Float = 999f
)

/**
 * ## TypographyTokens
 *
 * Semantic typography tokens backed by Mindustry bitmap atlas fonts (BMFonts).
 * Complies with Rule 5 (rendering BMFonts at natural integer scale 1.0f).
 *
 * See: docs/design-system/design_system_en.md
 */
data class TypographyTokens(
    /** Prominent screen and section headers. */
    val title: Font = Fonts.def,

    /** Monospaced tech font for coordinates, numbers, hex codes, and NXML code. */
    val mono: Font = Fonts.monospace ?: Fonts.def,

    /** Standard body copy, descriptions, and list item text. */
    val body: Font = Fonts.def,

    /** Small badge text and system captions. */
    val caption: Font = Fonts.def
)

/**
 * ## ColorTokens
 *
 * Comprehensive Apple macOS / iOS Human Interface semantic color palette.
 * Provides acrylic materials, glass borders, system accents, and multi-tier surface elevations.
 *
 * See: docs/design-system/design_system_en.md
 */
data class ColorTokens(
    // ==========================================
    // I. Apple System Accents
    // ==========================================
    val blue: Color = Color.valueOf("0a84ff"),
    val green: Color = Color.valueOf("30d158"),
    val indigo: Color = Color.valueOf("5e5ce6"),
    val orange: Color = Color.valueOf("ff9f0a"),
    val pink: Color = Color.valueOf("ff375f"),
    val purple: Color = Color.valueOf("bf5af2"),
    val red: Color = Color.valueOf("ff453a"),
    val teal: Color = Color.valueOf("64d2ff"),
    val yellow: Color = Color.valueOf("ffd60a"),

    // Primary accent alias
    val accent: Color = Color.valueOf("0a84ff"),

    // ==========================================
    // II. Frosted Glass & Acrylic Materials
    // ==========================================
    val glassUltraThin: Color = Color(1f, 1f, 1f, 0.04f),
    val glassThin: Color = Color(1f, 1f, 1f, 0.08f),
    val glassRegular: Color = Color(1f, 1f, 1f, 0.14f),
    val glassThick: Color = Color(1f, 1f, 1f, 0.22f),
    val glassActive: Color = Color(1f, 1f, 1f, 0.28f),

    // Specular highlight borders
    val borderHairline: Color = Color(1f, 1f, 1f, 0.08f),
    val borderRegular: Color = Color(1f, 1f, 1f, 0.15f),
    val borderActive: Color = Color.valueOf("0a84ff"),

    // ==========================================
    // III. Surface & Workspace Elevations
    // ==========================================
    val canvasVoid: Color = Color.valueOf("121214"),
    val canvasGridDark: Color = Color.valueOf("121214"),
    val canvasGridLight: Color = Color.valueOf("1a1a1f"),

    val surfaceBackground: Color = Color.valueOf("16171b"),
    val surfacePrimary: Color = Color.valueOf("1c1d22"),
    val surfaceSecondary: Color = Color.valueOf("22242b"),
    val surfaceTertiary: Color = Color.valueOf("292c36"),
    val surfaceElevated: Color = Color.valueOf("1f2026"),
    val surfaceHighlight: Color = Color.valueOf("2c3e55"),

    // ==========================================
    // IV. Text & Typography Hierarchy
    // ==========================================
    val textPrimary: Color = Color.valueOf("ffffff"),
    val textSecondary: Color = Color(1f, 1f, 1f, 0.70f),
    val textTertiary: Color = Color(1f, 1f, 1f, 0.45f),
    val textQuaternary: Color = Color(1f, 1f, 1f, 0.25f),
    val textOnAccent: Color = Color.valueOf("ffffff"),

    // ==========================================
    // V. Overlays, Dividers & Glows
    // ==========================================
    val divider: Color = Color(1f, 1f, 1f, 0.08f),
    val scrim: Color = Color(0f, 0f, 0f, 0.60f),
    val shadowAmbient: Color = Color(0f, 0f, 0f, 0.35f),
    val shadowKey: Color = Color(0f, 0f, 0f, 0.55f),
    val glowAccent: Color = Color(0.04f, 0.52f, 1f, 0.40f)
)
