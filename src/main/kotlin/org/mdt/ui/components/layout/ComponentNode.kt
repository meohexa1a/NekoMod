package org.mdt.ui.components.layout

import org.mdt.core.ui.graphics.Color

/**
 * ## ComponentNode
 *
 * Virtual DOM container representing either a Master Component definition (Sub-Artboard on Canvas)
 * or a dynamic Component Instance with reactive Props binding.
 *
 * See: docs/layout-engine/layout_engine_en.md
 */
class ComponentNode(
    var componentName: String = "PrimaryButton",
    var isMaster: Boolean = false
) : LayoutNode() {

    /** Map of declared parameters and their types/defaults for Master Components. */
    val declaredParams = mutableMapOf<String, String>()

    /** Map of active property values for Component Instances. */
    val boundProps = mutableMapOf<String, String>()

    init {
        this.name = componentName
        if (isMaster) {
            val vis = ensureVisuals()
            vis.background.mode = BackgroundFill.Mode.COLOR
            vis.background.color = Color.valueOf("1e1828")
            vis.border.width = 1f
            vis.border.color = Color.valueOf("bf5af2") // Purple for Master Component
        }
    }

    /**
     * Sets a property value and requests layout update.
     */
    fun setProp(name: String, value: String) {
        boundProps[name] = value
        invalidateLayout()
    }

    /**
     * Reads a property value falling back to default.
     */
    fun getProp(name: String, default: String = ""): String = boundProps[name] ?: default
}
