package org.mdt.core.nxml

import arc.graphics.Color
import mindustry.ui.Fonts
import org.dom4j.Element
import org.mdt.core.engine.EngineContext
import org.mdt.core.engine.image.ImageSource
import org.mdt.core.ui.UINode
import org.mdt.core.ui.layout.*
import org.mdt.ui.components.display.image.ScaleMode
import org.mdt.ui.components.layout.BackgroundFill
import org.mdt.ui.components.layout.LayoutNode
import org.mdt.ui.components.scroll.ScrollContainerNode
import org.mdt.ui.components.text.TextNode

/**
 * ## NxmlNodeBuilder
 *
 * Recursive compiler and evaluator translating [org.dom4j.Element] DOM trees into live [UINode] scene graphs.
 * Handles primitive layout containers, typography nodes, images, scroll views, and custom templates.
 *
 * See: docs/nxml-schema/nxml_specification_en.md
 */
object NxmlNodeBuilder {

    /**
     * Recursively builds a [UINode] subtree from an NXML DOM [Element].
     *
     * @param element Current NXML DOM element.
     * @param context Execution context for i18n and state bindings.
     * @return Fully configured [UINode].
     */
    fun build(element: Element, context: NxmlContext = NxmlContext.createDefault()): UINode {
        val tagName = element.name.lowercase()

        return when (tagName) {
            "scene" -> buildScene(element, context)
            "box" -> buildBox(element, context)
            "column" -> buildColumn(element, context)
            "row" -> buildRow(element, context)
            "text" -> buildText(element, context)
            "image" -> buildImage(element, context)
            "scrollview", "scroll" -> buildScrollView(element, context)
            "spacer" -> buildSpacer(element, context)
            "card" -> buildCard(element, context)
            "button" -> buildButton(element, context)
            else -> buildCustomOrFallback(element, context)
        }
    }

    // =========================================================================
    // I. Core Primitive Builders
    // =========================================================================

    private fun buildScene(element: Element, context: NxmlContext): LayoutNode {
        val sceneNode = LayoutNode()
        sceneNode.anchorData.setPreset(LayoutPreset.FULL_RECT)
        applyCommonAttributes(sceneNode, element, context)
        buildChildren(sceneNode, element, context)
        return sceneNode
    }

    private fun buildBox(element: Element, context: NxmlContext): LayoutNode {
        val boxNode = LayoutNode()
        applyCommonAttributes(boxNode, element, context)
        buildChildren(boxNode, element, context)
        return boxNode
    }

    private fun buildColumn(element: Element, context: NxmlContext): LayoutNode {
        val colNode = LayoutNode()
        val gap = element.getFloatAttr("gap", 0f)
        val arrangement = element.getArrangementAttr("arrangement", Arrangement.Start)
        val actualArrangement = if (gap > 0f && arrangement == Arrangement.Start) Arrangement.spacedBy(gap) else arrangement
        val alignment = element.getAlignmentAttr("align", Alignment.TopStart)

        colNode.measurePolicy = ColumnMeasurePolicy(
            gap = actualArrangement.spacing,
            arrangement = actualArrangement,
            alignment = alignment
        )

        applyCommonAttributes(colNode, element, context)
        buildChildren(colNode, element, context)
        return colNode
    }

    private fun buildRow(element: Element, context: NxmlContext): LayoutNode {
        val rowNode = LayoutNode()
        val gap = element.getFloatAttr("gap", 0f)
        val arrangement = element.getArrangementAttr("arrangement", Arrangement.Start)
        val actualArrangement = if (gap > 0f && arrangement == Arrangement.Start) Arrangement.spacedBy(gap) else arrangement
        val alignment = element.getAlignmentAttr("align", Alignment.CenterStart)

        rowNode.measurePolicy = RowMeasurePolicy(
            gap = actualArrangement.spacing,
            arrangement = actualArrangement,
            alignment = alignment
        )

        applyCommonAttributes(rowNode, element, context)
        buildChildren(rowNode, element, context)
        return rowNode
    }

    private fun buildText(element: Element, context: NxmlContext): TextNode {
        val rawText = element.getStringAttr("text", element.textTrim)
        val displayText = context.resolveText(rawText)
        val textNode = TextNode(displayText)

        val color = element.getColorAttr("color", Color.white)
        textNode.textVisuals.color.set(color)
        textNode.textVisuals.wrap = element.getBooleanAttr("wrap", false)

        val fontName = element.getStringAttr("font", "def").lowercase()
        textNode.textVisuals.font = when (fontName) {
            "mono", "monospace" -> Fonts.monospace ?: Fonts.def
            else -> Fonts.def
        }

        val alignStr = element.getStringAttr("align", "left").lowercase()
        textNode.textVisuals.align = when (alignStr) {
            "center" -> arc.util.Align.center
            "right", "end" -> arc.util.Align.right
            else -> arc.util.Align.left
        }

        applyCommonAttributes(textNode, element, context)
        return textNode
    }

    private fun buildImage(element: Element, context: NxmlContext): LayoutNode {
        val imageNode = LayoutNode()
        val src = element.getStringAttr("src", "")
        val tint = element.getColorAttr("tint", Color.white)
        val scaleModeStr = element.getStringAttr("scalemode", "fit").lowercase()

        val scaleMode = when (scaleModeStr) {
            "crop" -> ScaleMode.CROP
            "stretch" -> ScaleMode.STRETCH
            "center" -> ScaleMode.CENTER
            else -> ScaleMode.FIT
        }

        if (src.isNotEmpty()) {
            val imageSource = ImageSource.from(src)
            val imageService = EngineContext.current.image
            imageService.load(imageSource) { loadedRegion, _, _ ->
                imageNode.ensureVisuals().texture(loadedRegion, scaleMode, tint)
            }
        }

        applyCommonAttributes(imageNode, element, context)
        return imageNode
    }

    private fun buildScrollView(element: Element, context: NxmlContext): ScrollContainerNode {
        val scrollNode = ScrollContainerNode()
        scrollNode.enableVertical = element.getBooleanAttr("vertical", true)
        scrollNode.enableHorizontal = element.getBooleanAttr("horizontal", false)

        applyCommonAttributes(scrollNode, element, context)
        buildChildren(scrollNode, element, context)
        return scrollNode
    }

    private fun buildSpacer(element: Element, context: NxmlContext): LayoutNode {
        val spacerNode = LayoutNode()
        applyCommonAttributes(spacerNode, element, context)
        return spacerNode
    }

    // =========================================================================
    // II. Sugar Component Builders (Card, Button)
    // =========================================================================

    private fun buildCard(element: Element, context: NxmlContext): LayoutNode {
        val cardNode = LayoutNode()
        val visuals = cardNode.ensureVisuals()

        // Apple frosted glass styling
        visuals.background.mode = BackgroundFill.Mode.BACKDROP
        visuals.background.color.set(Color.valueOf("1e1e1e"))
        visuals.backdrop.blurRadius = element.getFloatAttr("blur", 20f)
        visuals.backdrop.weight = 0.85f
        visuals.radii.set(element.getFloatAttr("radius", 14f))
        visuals.border.width = 1f
        visuals.border.color.set(Color.valueOf("333333"))

        applyCommonAttributes(cardNode, element, context)
        buildChildren(cardNode, element, context)
        return cardNode
    }

    private fun buildButton(element: Element, context: NxmlContext): LayoutNode {
        val buttonNode = LayoutNode()
        val visuals = buttonNode.ensureVisuals()
        visuals.radii.set(element.getFloatAttr("radius", 8f))
        visuals.background.color.set(element.getColorAttr("background", Color.valueOf("0d99ff")))
        visuals.border.color.set(Color.valueOf("333333"))
        visuals.border.width = 1f

        buttonNode.pad(14f, 8f, 14f, 8f)
        buttonNode.cursor = arc.Graphics.Cursor.SystemCursor.hand

        val rawText = element.getStringAttr("text", "")
        if (rawText.isNotEmpty()) {
            val textNode = TextNode(context.resolveText(rawText))
            textNode.textVisuals.color.set(element.getColorAttr("color", Color.white))
            buttonNode.addChild(textNode)
        }

        applyCommonAttributes(buttonNode, element, context)
        buildChildren(buttonNode, element, context)
        return buttonNode
    }

    private fun buildCustomOrFallback(element: Element, context: NxmlContext): UINode {
        val template = context.findTemplate(element.name)
        if (template != null) {
            // Expand template recursively
            val expandedNode = build(template, context)
            if (expandedNode is LayoutNode) {
                applyCommonAttributes(expandedNode, element, context)
            }
            return expandedNode
        }

        val fallbackNode = LayoutNode()
        applyCommonAttributes(fallbackNode, element, context)
        buildChildren(fallbackNode, element, context)
        return fallbackNode
    }

    // =========================================================================
    // III. Common Attribute Applicator
    // =========================================================================

    private fun applyCommonAttributes(node: LayoutNode, element: Element, context: NxmlContext) {
        // Dimensions
        val width = element.getFloatAttr("width", -1f)
        if (width >= 0f) node.width = width

        val height = element.getFloatAttr("height", -1f)
        if (height >= 0f) node.height = height

        val minWidth = element.getFloatAttr("minwidth", -1f)
        if (minWidth >= 0f) node.minWidth = minWidth

        val minHeight = element.getFloatAttr("minheight", -1f)
        if (minHeight >= 0f) node.minHeight = minHeight

        val weight = element.getFloatAttr("weight", 0f)
        if (weight > 0f) {
            node.sizeFlagsHorizontal = node.sizeFlagsHorizontal or SizeFlags.EXPAND_FILL
            node.sizeFlagsVertical = node.sizeFlagsVertical or SizeFlags.EXPAND_FILL
            node.stretchRatio = weight
        }

        // Insets (Padding & Margin)
        val pad = element.getInsetsAttr("pad")
        if (pad.left > 0f || pad.top > 0f || pad.right > 0f || pad.bottom > 0f) {
            node.pad(pad.left, pad.top, pad.right, pad.bottom)
        }

        val margin = element.getInsetsAttr("margin")
        if (margin.left > 0f || margin.top > 0f || margin.right > 0f || margin.bottom > 0f) {
            node.margin(margin.left, margin.top, margin.right, margin.bottom)
        }

        // Anchor Presets
        val anchorPreset = element.getAnchorPresetAttr("anchor")
        if (anchorPreset != null) {
            node.anchorData.setPreset(anchorPreset)
        } else if (element.getBooleanAttr("fill", false)) {
            node.anchorData.setPreset(LayoutPreset.FULL_RECT)
        }

        // Visuals (Background, Border, Radius, Glow)
        val hasBg = element.attribute("background") != null || element.attribute("bg") != null
        val bgColor = element.getColorAttr("background", element.getColorAttr("bg", Color.clear))
        val radius = element.getFloatAttr("radius", 0f)
        val borderWidth = element.getFloatAttr("borderwidth", 0f)
        val borderColor = element.getColorAttr("bordercolor", Color.clear)
        val glowColor = element.getColorAttr("glow", Color.clear)

        if (hasBg || radius > 0f || borderWidth > 0f || glowColor.a > 0f) {
            val visuals = node.ensureVisuals()
            if (hasBg) {
                visuals.background.color.set(bgColor)
                visuals.background.mode = BackgroundFill.Mode.COLOR
            }
            if (radius > 0f) visuals.radii.set(radius)
            if (borderWidth > 0f) {
                visuals.border.width = borderWidth
                visuals.border.color.set(borderColor)
            }
            if (glowColor.a > 0f) {
                visuals.glow.color.set(glowColor)
                visuals.glow.blur = element.getFloatAttr("glowblur", 8f)
            }
        }
    }

    private fun buildChildren(parent: LayoutNode, element: Element, context: NxmlContext) {
        val childElements = element.elements()
        for (child in childElements) {
            if (child is Element) {
                val childNode = build(child, context)
                parent.addChild(childNode)
            }
        }
    }
}
