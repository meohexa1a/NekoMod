package org.mdt.ui.screens.editor.canvas

import arc.Core
import arc.graphics.g2d.Draw
import arc.graphics.g2d.Fill
import arc.graphics.g2d.Lines
import arc.util.Tmp
import mindustry.ui.Fonts
import org.mdt.core.ui.graphics.Color
import org.mdt.core.ui.render.EngineRenderer
import org.mdt.ui.components.layout.SceneNode

/**
 * ## CanvasArtboardFrame
 *
 * Dedicated renderer for the Artboard Scene Frame on the Canvas, managing boundary positioning,
 * backdrop shadows, header pill badges, and direct Virtual Node tree rendering.
 *
 * See: docs/design-system/design_system_en.md
 */
object CanvasArtboardFrame {

    /**
     * Lays out and renders the [SceneNode] Artboard frame on the Canvas.
     */
    fun draw(
        rootScene: SceneNode,
        renderer: EngineRenderer,
        panX: Float,
        panY: Float,
        zoomScale: Float
    ) {
        val screenW = Core.graphics?.width?.toFloat() ?: 1920f
        val screenH = Core.graphics?.height?.toFloat() ?: 1080f

        val artW = rootScene.artboardWidth * zoomScale
        val artH = rootScene.artboardHeight * zoomScale

        val sceneX = (screenW - artW) * 0.5f + panX
        val sceneY = (screenH - artH) * 0.5f + panY

        // 1. Layout and Boundary Positioning
        rootScene.setBounds(sceneX, sceneY, artW, artH)
        rootScene.layout()

        // 2. Artboard Frame Drop Shadow
        Draw.color(Color(0f, 0f, 0f, 0.40f).toArcColor(Tmp.c1))
        Fill.rect(sceneX + artW * 0.5f, sceneY + artH * 0.5f - 4f, artW + 8f, artH + 8f)

        // 3. Render Virtual Node Scene Graph
        rootScene.draw(renderer)

        // 4. Artboard Header Pill Badge
        val font = Fonts.def
        val headerText = "${rootScene.name} • ${rootScene.artboardWidth.toInt()} × ${rootScene.artboardHeight.toInt()} px"
        val badgeX = sceneX + 60f
        val badgeY = sceneY + artH + 16f

        Draw.color(Color(0.08f, 0.08f, 0.12f, 0.85f).toArcColor(Tmp.c1))
        Fill.rect(badgeX, badgeY, 140f, 20f)
        Draw.color(Color(0.2f, 0.5f, 1.0f, 0.4f).toArcColor(Tmp.c1))
        Lines.stroke(1f)
        Lines.rect(badgeX - 70f, badgeY - 10f, 140f, 20f)

        Draw.color()
        font.draw(headerText, badgeX - 64f, badgeY + 4f)
        Draw.color()
    }
}
