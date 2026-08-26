package org.mdt.core.image

import arc.Core
import arc.Events
import arc.graphics.Pixmap
import arc.graphics.Texture
import arc.graphics.g2d.TextureRegion
import arc.util.Log
import mindustry.Vars
import mindustry.game.EventType.Trigger
import org.mdt.core.async.AsyncDispatcher
import org.mdt.core.cache.LRUTextureCache
import org.mdt.core.cache.TextureHandle
import org.mdt.core.net.Net
import org.mdt.core.store.Storage
import java.util.concurrent.ConcurrentLinkedQueue

/**
 * ## ImageLoader
 *
 * Asynchronous, zero-frame-stall image pipeline with Burst Upload Throttling,
 * reference-counted VRAM caching, and single-point fallback to Mindustry's iconic 'ohno' / 'error' region.
 *
 * See: docs/core-subsystems/core_subsystems_en.md
 */
object ImageLoader {

    private const val MAX_UPLOADS_PER_FRAME = 4

    private class UploadTask(
        val cacheKey: String,
        val pixmap: Pixmap,
        val onResult: (TextureRegion, TextureHandle?, Throwable?) -> Unit
    )

    private val uploadQueue = ConcurrentLinkedQueue<UploadTask>()
    private var isHooked = false

    init {
        ensureRenderHook()
    }

    private fun ensureRenderHook() {
        if (!isHooked) {
            isHooked = true
            // Run on both update and uiDrawEnd to ensure execution in menus and paused states
            Events.run(Trigger.uiDrawEnd) { processUploadQueue() }
        }
    }

    /**
     * Resolves Mindustry's iconic 'ohno' error fallback texture region.
     */
    fun fallbackRegion(): TextureRegion {
        val atlas = Core.atlas ?: return TextureRegion()
        return when {
            atlas.has("ohno") -> atlas.find("ohno")
            atlas.has("error") -> atlas.find("error")
            atlas.has("white") -> atlas.find("white")
            else -> atlas.white() ?: TextureRegion()
        }
    }

    fun processUploadQueue() {
        var processed = 0
        while (processed < MAX_UPLOADS_PER_FRAME && !uploadQueue.isEmpty()) {
            val task = uploadQueue.poll() ?: break
            processed++
            try {
                val texture = Texture(task.pixmap).apply {
                    setFilter(Texture.TextureFilter.linear)
                }
                task.pixmap.dispose()
                val handle = LRUTextureCache.shared.put(task.cacheKey, texture)
                task.onResult(handle.region, handle, null)
            } catch (e: Throwable) {
                Log.err("[ImageLoader] Failed to upload texture for ${task.cacheKey}", e)
                task.pixmap.dispose()
                task.onResult(fallbackRegion(), null, e)
            }
        }
    }

    /**
     * Loads an [ImageSource] asynchronously.
     * Invokes [onResult] with resolved [TextureRegion], optional cache [TextureHandle], and any [Throwable].
     */
    fun load(source: ImageSource, onResult: (TextureRegion, TextureHandle?, Throwable?) -> Unit) {
        when (source) {
            is ImageSource.Atlas -> loadFromAtlas(source.name, onResult)
            is ImageSource.Region -> onResult(source.region, null, null)
            is ImageSource.Url -> loadFromUrl(source, onResult)
            is ImageSource.Asset -> loadFromAsset(source.path, onResult)
            is ImageSource.LocalFile -> loadFromFile(source, onResult)
        }
    }

    private fun loadFromAtlas(name: String, onResult: (TextureRegion, TextureHandle?, Throwable?) -> Unit) {
        val atlas = Core.atlas
        if (atlas != null && atlas.has(name)) {
            onResult(atlas.find(name), null, null)
        } else {
            onResult(fallbackRegion(), null, IllegalArgumentException("Atlas region not found: $name"))
        }
    }

    private fun loadFromUrl(source: ImageSource.Url, onResult: (TextureRegion, TextureHandle?, Throwable?) -> Unit) {
        val cacheKey = source.url
        val cached = LRUTextureCache.shared.get(cacheKey)
        if (cached != null) {
            onResult(cached.region, cached, null)
            return
        }

        AsyncDispatcher.launch {
            try {
                val bytes = Net.get(source.url, source.configureRequest ?: {}).awaitBytes()
                val pixmap = Pixmap(bytes, 0, bytes.size)
                uploadQueue.add(UploadTask(cacheKey, pixmap, onResult))
            } catch (e: Throwable) {
                Log.err("[ImageLoader] Failed to download image from ${source.url}: ${e.message}")
                AsyncDispatcher.onMainThread { onResult(fallbackRegion(), null, e) }
            }
        }
    }

    private fun loadFromAsset(path: String, onResult: (TextureRegion, TextureHandle?, Throwable?) -> Unit) {
        val cached = LRUTextureCache.shared.get(path)
        if (cached != null) {
            onResult(cached.region, cached, null)
            return
        }

        AsyncDispatcher.launch {
            try {
                val bytes = run {
                    val treeFi = Vars.tree?.get(path)
                    if (treeFi != null && treeFi.exists()) return@run treeFi.readBytes()
                    val internalFi = Core.files?.internal(path)
                    if (internalFi != null && internalFi.exists()) return@run internalFi.readBytes()
                    val stream = ImageLoader::class.java.classLoader.getResourceAsStream(path)
                    stream?.use { it.readBytes() } ?: throw IllegalArgumentException("Asset not found: $path")
                }

                val pixmap = Pixmap(bytes, 0, bytes.size)
                uploadQueue.add(UploadTask(path, pixmap, onResult))
            } catch (e: Throwable) {
                AsyncDispatcher.onMainThread { onResult(fallbackRegion(), null, e) }
            }
        }
    }

    private fun loadFromFile(source: ImageSource.LocalFile, onResult: (TextureRegion, TextureHandle?, Throwable?) -> Unit) {
        val cacheKey = source.path.toString()
        val cached = LRUTextureCache.shared.get(cacheKey)
        if (cached != null) {
            onResult(cached.region, cached, null)
            return
        }

        AsyncDispatcher.launch {
            try {
                val bytes = Storage.readBytes(source.path)
                    ?: throw IllegalArgumentException("File not found: ${source.path}")
                val pixmap = Pixmap(bytes, 0, bytes.size)
                uploadQueue.add(UploadTask(cacheKey, pixmap, onResult))
            } catch (e: Throwable) {
                AsyncDispatcher.onMainThread { onResult(fallbackRegion(), null, e) }
            }
        }
    }
}
