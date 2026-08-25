package org.mdt.core.image

import arc.Core
import arc.Events
import arc.graphics.Pixmap
import arc.graphics.Texture
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
 * Asynchronous, zero-frame-stall image pipeline with Burst Upload Throttling.
 * Performs network I/O and byte decoding on background coroutines,
 * then uploads to GPU texture on the main render thread with a per-frame budget.
 *
 * See: docs/complex-challenges/complex_challenges_en.md
 */
object ImageLoader {

    private const val MAX_UPLOADS_PER_FRAME = 4

    private class UploadTask(
        val cacheKey: String,
        val pixmap: Pixmap,
        val onResult: (TextureHandle?, Throwable?) -> Unit
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
            Events.run(Trigger.uiDrawEnd) {
                processUploadQueue()
            }
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
                Log.info("[ImageLoader] Uploaded GPU texture for: ${task.cacheKey} (${texture.width}x${texture.height})")
                task.onResult(handle, null)
            } catch (e: Throwable) {
                Log.err("[ImageLoader] Failed to upload texture for ${task.cacheKey}", e)
                task.pixmap.dispose()
                task.onResult(null, e)
            }
        }
    }

    fun load(source: ImageSource, onResult: (TextureHandle?, Throwable?) -> Unit) {
        when (source) {
            is ImageSource.Url -> loadFromUrl(source.url, onResult)
            is ImageSource.Asset -> loadFromAsset(source.path, onResult)
            is ImageSource.LocalFile -> loadFromFile(source, onResult)
            is ImageSource.Region -> {
                val handle = LRUTextureCache.shared.put(source.region.texture.toString(), source.region.texture)
                onResult(handle, null)
            }
        }
    }

    private fun loadFromUrl(url: String, onResult: (TextureHandle?, Throwable?) -> Unit) {
        val cached = LRUTextureCache.shared.get(url)
        if (cached != null) {
            onResult(cached, null)
            return
        }

        AsyncDispatcher.launch {
            try {
                val bytes = Net.get(url).awaitBytes()
                val pixmap = Pixmap(bytes, 0, bytes.size)
                uploadQueue.add(UploadTask(url, pixmap, onResult))
            } catch (e: Throwable) {
                Log.err("[ImageLoader] Failed to download image from $url: ${e.message}")
                AsyncDispatcher.onMainThread { onResult(null, e) }
            }
        }
    }

    private fun loadFromAsset(path: String, onResult: (TextureHandle?, Throwable?) -> Unit) {
        val cached = LRUTextureCache.shared.get(path)
        if (cached != null) {
            onResult(cached, null)
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
                AsyncDispatcher.onMainThread { onResult(null, e) }
            }
        }
    }

    private fun loadFromFile(source: ImageSource.LocalFile, onResult: (TextureHandle?, Throwable?) -> Unit) {
        val cacheKey = source.path.toString()
        val cached = LRUTextureCache.shared.get(cacheKey)
        if (cached != null) {
            onResult(cached, null)
            return
        }

        AsyncDispatcher.launch {
            try {
                val bytes = Storage.readBytes(source.path)
                    ?: throw IllegalArgumentException("File not found: ${source.path}")
                val pixmap = Pixmap(bytes, 0, bytes.size)
                uploadQueue.add(UploadTask(cacheKey, pixmap, onResult))
            } catch (e: Throwable) {
                AsyncDispatcher.onMainThread { onResult(null, e) }
            }
        }
    }
}
