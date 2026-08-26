package org.mdt.core.nxml

import androidx.compose.runtime.Composable
import androidx.compose.runtime.ComposeNode
import androidx.compose.runtime.remember
import org.mdt.core.ui.compose.NodeApplier
import org.mdt.core.ui.compose.UIModifier
import org.mdt.core.ui.layout.LayoutPreset
import org.mdt.ui.components.layout.LayoutNode

/**
 * ## NxmlCanvas
 *
 * Declarative Compose host rendering live NXML scenes directly inside the UI tree.
 * Re-evaluates and mounts the dynamic [LayoutNode] graph reactively whenever [nxmlContent] changes.
 *
 * @param nxmlContent Raw NXML XML string.
 * @param modifier Chainable [UIModifier] applied to the host root container.
 * @param context Evaluation context containing i18n dictionaries and state bindings.
 *
 * See: docs/nxml-schema/nxml_specification_en.md
 */
@Composable
fun NxmlCanvas(
    nxmlContent: String,
    modifier: UIModifier = UIModifier,
    context: NxmlContext = remember { NxmlContext.createDefault() }
) {
    ComposeNode<LayoutNode, NodeApplier>(
        factory = {
            val rootDoc = NxmlDocument.parse(nxmlContent)
            val builtNode = NxmlNodeBuilder.build(rootDoc.rootElement, context)
            val container = LayoutNode()
            container.anchorData.setPreset(LayoutPreset.FULL_RECT)
            container.addChild(builtNode)
            modifier.applyTo(container)
            container
        },
        update = {
            set(nxmlContent) { content ->
                // Clear existing children and re-build from updated NXML string
                val existingChildren = ArrayList(this.children)
                for (child in existingChildren) {
                    this.removeChild(child)
                }

                val rootDoc = NxmlDocument.parse(content)
                val builtNode = NxmlNodeBuilder.build(rootDoc.rootElement, context)
                this.addChild(builtNode)
                this.invalidateLayout()
            }
            set(modifier) {
                it.applyTo(this)
                invalidateLayout()
            }
        }
    )
}
