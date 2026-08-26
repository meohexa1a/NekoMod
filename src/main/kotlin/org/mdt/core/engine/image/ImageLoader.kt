package org.mdt.core.engine.image

import arc.graphics.g2d.TextureRegion
import org.mdt.core.common.TextureHandle
import org.mdt.core.engine.EngineContext

/**
 * ## ImageLoader
 *
 * Facade for [EngineContext.current.image], providing asynchronous image loading,
 * in-flight request deduplication, L1 VRAM & L2 disk caching, and burst upload throttling.
 *
 * See: docs/core-subsystems/core_subsystems_en.md
 */
object ImageLoader {

    val service: ImageService
        inline get() = EngineContext.current.image

    fun fallbackRegion(): TextureRegion = service.fallbackRegion()

    fun processUploadQueue() = service.processUploadQueue()

    fun load(source: ImageSource, onResult: (TextureRegion, TextureHandle?, Throwable?) -> Unit) =
        service.load(source, onResult)
}
