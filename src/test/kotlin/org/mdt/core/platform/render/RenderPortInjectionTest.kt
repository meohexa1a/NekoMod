package org.mdt.core.platform.render

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import org.mdt.core.platform.PlatformHost
import org.mdt.core.platform.assets.AssetPort
import org.mdt.core.platform.window.WindowPort
import org.mdt.core.ui.EngineRuntime
import org.mdt.core.ui.node.LayoutNode
import org.mdt.core.ui.node.UINode

/**
 * ## RenderPortInjectionTest
 *
 * Validates that rendering subsystems ([RenderPort], [ShaderRegistry], [SceneBlur], [UIBatch], [FontRenderer])
 * are correctly unified under [PlatformHost.render] and can be independently instantiated or mocked.
 */
class RenderPortInjectionTest {

    @Test
    fun `ShaderRegistry can be instantiated with custom asset provider`() {
        var requestedPath: String? = null
        val mockAssets = object : AssetPort by AssetPort.NoOp {
            override fun readShaderSource(path: String): String {
                requestedPath = path
                return "// mock shader code"
            }
        }
        val mockHost = object : PlatformHost by PlatformHost.NoOp {
            override val assets: AssetPort = mockAssets
        }

        val shaders = ShaderRegistry { mockHost }
        val content = shaders.readString("test/path.vert")

        assertEquals("test/path.vert", requestedPath)
        assertEquals("// mock shader code", content)
    }

    @Test
    fun `SceneBlur can be instantiated with custom PlatformHost provider`() {
        val mockWindow = object : WindowPort by WindowPort.NoOp {
            override val width: Float = 1920.0f
            override val height: Float = 1080.0f
        }
        val mockHost = object : PlatformHost by PlatformHost.NoOp {
            override val window: WindowPort = mockWindow
        }

        val blur = SceneBlur { mockHost }

        assertNotNull(blur)
        assertEquals(false, blur.isEnabled)
    }

    @Test
    fun `MindustryRenderPort coordinates all render subsystems through PlatformHost`() {
        val renderPort = MindustryRenderPort { PlatformHost.NoOp }

        assertNotNull(renderPort.shaders)
        assertNotNull(renderPort.blur)
        assertNotNull(renderPort.batch)
        assertNotNull(renderPort.fontRenderer)
    }

    @Test
    fun `PlatformHost exposes non-null RenderPort and convenient facade delegates`() {
        assertNotNull(EngineRuntime.host.render)
        assertNotNull(EngineRuntime.host.shaders)
        assertNotNull(EngineRuntime.host.blur)
        assertNotNull(EngineRuntime.host.batch)
        assertNotNull(EngineRuntime.host.fontRenderer)
        assertNotNull(PlatformHost.NoOp.render)
    }

    @Test
    fun `UINode draw traversal executes recursively without throwing`() {
        var parentDrawCount = 0
        var childDrawCount = 0

        val parentNode = object : LayoutNode() {
            override fun drawSelf(batch: UIBatch) {
                parentDrawCount++
            }
        }

        val childNode = object : UINode() {
            override fun drawSelf(batch: UIBatch) {
                childDrawCount++
            }
        }

        parentNode.children.add(childNode)

        // Draw traversal when visible
        parentNode.draw(EngineRuntime.host.batch)

        assertEquals(1, parentDrawCount)
        assertEquals(1, childDrawCount)

        // Child hidden: only parent draws
        childNode.visible = false
        parentNode.draw(EngineRuntime.host.batch)

        assertEquals(2, parentDrawCount)
        assertEquals(1, childDrawCount)
    }
}

