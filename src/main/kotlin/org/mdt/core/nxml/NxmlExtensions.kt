package org.mdt.core.nxml

import arc.graphics.Color
import org.dom4j.Element
import org.mdt.core.ui.layout.Alignment
import org.mdt.core.ui.layout.Arrangement
import org.mdt.core.ui.layout.LayoutPreset

/**
 * ## NxmlExtensions
 *
 * Type-safe attribute parsing and conversion extensions for [org.dom4j.Element].
 * Provides robust fallbacks, color parsing, insets extraction, and layout enums.
 *
 * See: docs/nxml-schema/nxml_specification_en.md
 */

// =============================================================================
// I. Primitive Type Safe Getters
// =============================================================================

fun Element.getStringAttr(name: String, default: String = ""): String {
    return attributeValue(name) ?: default
}

fun Element.getFloatAttr(name: String, default: Float = 0f): Float {
    val raw = attributeValue(name) ?: return default
    return raw.trim().removeSuffix("f").removeSuffix("px").toFloatOrNull() ?: default
}

fun Element.getIntAttr(name: String, default: Int = 0): Int {
    val raw = attributeValue(name) ?: return default
    return raw.trim().toIntOrNull() ?: default
}

fun Element.getBooleanAttr(name: String, default: Boolean = false): Boolean {
    val raw = attributeValue(name) ?: return default
    return raw.trim().lowercase() in listOf("true", "1", "yes", "on")
}

// =============================================================================
// II. Graphics & Color Parsing
// =============================================================================

fun Element.getColorAttr(name: String, default: Color = Color.clear): Color {
    val raw = attributeValue(name)?.trim() ?: return default
    if (raw.isEmpty()) return default

    return try {
        when {
            raw.startsWith("#") -> Color.valueOf(raw.removePrefix("#"))
            raw.startsWith("0x", ignoreCase = true) -> Color.valueOf(raw.removePrefix("0x").removePrefix("0X"))
            raw.equals("white", ignoreCase = true) -> Color.white
            raw.equals("black", ignoreCase = true) -> Color.black
            raw.equals("clear", ignoreCase = true) || raw.equals("transparent", ignoreCase = true) -> Color.clear
            raw.equals("gray", ignoreCase = true) -> Color.gray
            raw.equals("red", ignoreCase = true) -> Color.red
            raw.equals("green", ignoreCase = true) -> Color.green
            raw.equals("blue", ignoreCase = true) -> Color.blue
            raw.equals("yellow", ignoreCase = true) -> Color.yellow
            else -> Color.valueOf(raw)
        }
    } catch (_: Throwable) {
        default
    }
}

// =============================================================================
// III. Layout Insets & Padding Parsers
// =============================================================================

data class InsetsData(val left: Float, val top: Float, val right: Float, val bottom: Float)

fun Element.getInsetsAttr(name: String, default: InsetsData = InsetsData(0f, 0f, 0f, 0f)): InsetsData {
    val raw = attributeValue(name)?.trim() ?: return default
    if (raw.isEmpty()) return default

    val parts = raw.split("\\s+".toRegex()).mapNotNull { it.removeSuffix("f").removeSuffix("px").toFloatOrNull() }
    return when (parts.size) {
        1 -> InsetsData(parts[0], parts[0], parts[0], parts[0])
        2 -> InsetsData(parts[1], parts[0], parts[1], parts[0]) // vertical horizontal
        4 -> InsetsData(parts[3], parts[0], parts[1], parts[2]) // top right bottom left (CSS order)
        else -> default
    }
}

// =============================================================================
// IV. Layout Enums (Alignment, Arrangement, Anchors)
// =============================================================================

fun Element.getAlignmentAttr(name: String, default: Alignment = Alignment.TopStart): Alignment {
    val raw = attributeValue(name)?.trim()?.lowercase() ?: return default
    return when (raw) {
        "topstart", "top_start", "topleft", "top_left" -> Alignment.TopStart
        "topcenter", "top_center" -> Alignment.TopCenter
        "topend", "top_end", "topright", "top_right" -> Alignment.TopEnd
        "centerstart", "center_start", "centerleft", "center_left" -> Alignment.CenterStart
        "center" -> Alignment.Center
        "centerend", "center_end", "centerright", "center_right" -> Alignment.CenterEnd
        "bottomstart", "bottom_start", "bottomleft", "bottom_left" -> Alignment.BottomStart
        "bottomcenter", "bottom_center" -> Alignment.BottomCenter
        "bottomend", "bottom_end", "bottomright", "bottom_right" -> Alignment.BottomEnd
        else -> default
    }
}

fun Element.getArrangementAttr(name: String, default: Arrangement = Arrangement.Start): Arrangement {
    val raw = attributeValue(name)?.trim()?.lowercase() ?: return default
    return when (raw) {
        "start" -> Arrangement.Start
        "center" -> Arrangement.Center
        "end" -> Arrangement.End
        "space_between", "spacebetween" -> Arrangement.SpaceBetween
        "space_evenly", "spaceevenly" -> Arrangement.SpaceEvenly
        "space_around", "spacearound" -> Arrangement.SpaceAround
        else -> default
    }
}

fun Element.getAnchorPresetAttr(name: String): LayoutPreset? {
    val raw = attributeValue(name)?.trim()?.lowercase() ?: return null
    return when (raw) {
        "full_rect", "fullrect", "fill" -> LayoutPreset.FULL_RECT
        "top_left", "topleft" -> LayoutPreset.TOP_LEFT
        "top_right", "topright" -> LayoutPreset.TOP_RIGHT
        "bottom_left", "bottomleft" -> LayoutPreset.BOTTOM_LEFT
        "bottom_right", "bottomright" -> LayoutPreset.BOTTOM_RIGHT
        "center" -> LayoutPreset.CENTER
        "center_left", "centerleft" -> LayoutPreset.CENTER_LEFT
        "center_right", "centerright" -> LayoutPreset.CENTER_RIGHT
        "center_top", "centertop" -> LayoutPreset.CENTER_TOP
        "center_bottom", "centerbottom" -> LayoutPreset.CENTER_BOTTOM
        "top_wide", "topwide" -> LayoutPreset.TOP_WIDE
        "bottom_wide", "bottomwide" -> LayoutPreset.BOTTOM_WIDE
        "left_wide", "leftwide" -> LayoutPreset.LEFT_WIDE
        "right_wide", "rightwide" -> LayoutPreset.RIGHT_WIDE
        else -> null
    }
}
