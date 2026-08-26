package org.mdt.core.engine.image

import arc.graphics.Pixmap
import arc.graphics.Texture
import arc.graphics.g2d.TextureRegion
import arc.util.Log
import okio.ByteString.Companion.encodeUtf8
import org.mdt.core.common.AsyncDispatcher
import org.mdt.core.common.LRUTextureCache
import org.mdt.core.common.TextureHandle
import org.mdt.core.common.TextureKind
import org.mdt.core.engine.EngineContext
import org.mdt.core.common.Net
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.ConcurrentLinkedQueue

/**
 * ## ImageService
 *
 * Modular, multi-tiered image pipeline featuring:
 * 1. **L1 VRAM Memory Caching:** Lock-free [LRUTextureCache] bound to the active context.
 * 2. **In-Flight Request Deduplication (Coalescing):** Merges concurrent requests for the same URL.
 * 3. **L2 Persistent Disk Caching:** Automated MD5-hashed disk cache managed via [EngineContext.storage].
 * 4. **Throttled Burst GPU Upload:** Uploads max 4 textures per frame via [EngineContext.host] frame hooks to prevent stutters.
 * 5. **Graceful Error Fallbacks:** Single-point fallback to iconic 'ohno' / 'error' atlas regions.
 *
 * See: docs/core-subsystems/core_subsystems_en.md
 */
open class ImageService(val context: EngineContext) {

    /** Context-bound L1 VRAM texture cache. */
    open val cache: LRUTextureCache by lazy { createTextureCache() }

    /** Factory method creating the L1 texture cache instance for this context. */
    protected open fun createTextureCache(): LRUTextureCache = LRUTextureCache()

    private class UploadTask(
        val cacheKey: String,
        val pixmap: Pixmap,
        val callbacks: List<(TextureRegion, TextureHandle?, Throwable?) -> Unit>
    )

    private val uploadQueue = ConcurrentLinkedQueue<UploadTask>()
    private val inFlightUrlRequests = ConcurrentHashMap<String, MutableList<(TextureRegion, TextureHandle?, Throwable?) -> Unit>>()
    private var isHooked = false

    init {
        ensureRenderHook()
    }

    private fun ensureRenderHook() {
        if (!isHooked) {
            isHooked = true
            context.host.onFrameEnd { processUploadQueue() }
        }
    }

    /**
     * Resolves the fallback [TextureRegion] (e.g. 'ohno', 'error', or 'white') via [EngineContext.host].
     */
    fun fallbackRegion(): TextureRegion {
        return context.host.resolveAtlasRegion("ohno")
            ?: context.host.resolveAtlasRegion("error")
            ?: context.host.resolveAtlasRegion("white")
            ?: TextureRegion()
    }

    /**
     * Processes pending texture upload tasks onto the GPU main thread (max [MAX_UPLOADS_PER_FRAME] per invocation).
     */
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
                val handle = cache.put(task.cacheKey, texture, kind = TextureKind.MANAGED)
                for (callback in task.callbacks) {
                    callback(handle.region, handle, null)
                }
            } catch (e: Throwable) {
                Log.err("[ImageService] Failed to upload texture for ${task.cacheKey}", e)
                task.pixmap.dispose()
                val fallback = fallbackRegion()
                val handle = cache.put("atlas:ohno", fallback.texture, kind = TextureKind.FALLBACK)
                for (callback in task.callbacks) {
                    callback(fallback, handle, e)
                }
            }
        }
    }

    /**
     * Asynchronously loads an image from an [ImageSource] and invokes [onResult] with the resolved region.
     */
    fun load(source: ImageSource, onResult: (TextureRegion, TextureHandle?, Throwable?) -> Unit) {
        when (source) {
            is ImageSource.Atlas -> loadFromAtlas(source, onResult)
            is ImageSource.Region -> loadFromRegion(source, onResult)
            is ImageSource.Url -> loadFromUrl(source, onResult)
            is ImageSource.Asset -> loadFromAsset(source, onResult)
            is ImageSource.LocalFile -> loadFromFile(source, onResult)
        }
    }

    private fun loadFromAtlas(source: ImageSource.Atlas, onResult: (TextureRegion, TextureHandle?, Throwable?) -> Unit) {
        val reg = context.host.resolveAtlasRegion(source.name)
        if (reg != null) {
            val handle = cache.put("atlas:${source.name}", reg.texture, kind = TextureKind.SHARED)
            onResult(reg, handle, null)
        } else {
            val fallback = fallbackRegion()
            val handle = cache.put("atlas:ohno", fallback.texture, kind = TextureKind.FALLBACK)
            onResult(fallback, handle, IllegalArgumentException("Atlas region not found: ${source.name}"))
        }
    }

    private fun loadFromRegion(source: ImageSource.Region, onResult: (TextureRegion, TextureHandle?, Throwable?) -> Unit) {
        val handle = cache.put(
            key = "region:" + source.region.texture.toString(),
            texture = source.region.texture,
            kind = TextureKind.SHARED
        )
        onResult(source.region, handle, null)
    }

    private fun loadFromUrl(source: ImageSource.Url, onResult: (TextureRegion, TextureHandle?, Throwable?) -> Unit) {
        val cacheKey = source.url

        // 1. L1 Memory Cache hit
        val cached = cache.get(cacheKey)
        if (cached != null) {
            onResult(cached.region, cached, null)
            return
        }

        // 2. In-Flight Request Deduplication (Coalescing)
        val isFirstRequest = synchronized(inFlightUrlRequests) {
            val list = inFlightUrlRequests.getOrPut(cacheKey) { ArrayList() }
            val first = list.isEmpty()
            list.add(onResult)
            first
        }

        if (!isFirstRequest) return

        AsyncDispatcher.launch {
            try {
                // 3. L2 Disk Cache check
                val hash = cacheKey.encodeUtf8().md5().hex()
                val diskCachePath = context.storage.resolveCache("images/$hash.bin")
                var bytes = context.storage.readBytes(diskCachePath)

                if (bytes == null) {
                    // 4. Download from network & persist to disk cache
                    bytes = Net.get(source.url, source.configureRequest ?: {}).awaitBytes()
                    try {
                        context.storage.writeBytes(diskCachePath, bytes)
                    } catch (e: Throwable) {
                        Log.warn("[ImageService] Failed to cache image $cacheKey to disk: ${e.message}")
                    }
                }

                val pixmap = Pixmap(bytes, 0, bytes.size)
                val callbacks = synchronized(inFlightUrlRequests) {
                    inFlightUrlRequests.remove(cacheKey) ?: emptyList()
                }
                uploadQueue.add(UploadTask(cacheKey, pixmap, callbacks))
            } catch (e: Throwable) {
                Log.err("[ImageService] Failed to load image from ${source.url}: ${e.message}")
                val callbacks = synchronized(inFlightUrlRequests) {
                    inFlightUrlRequests.remove(cacheKey) ?: emptyList()
                }
                val fallback = fallbackRegion()
                val handle = cache.put("atlas:ohno", fallback.texture, kind = TextureKind.FALLBACK)
                AsyncDispatcher.onMainThread {
                    for (callback in callbacks) {
                        callback(fallback, handle, e)
                    }
                }
            }
        }
    }

    private fun loadFromAsset(source: ImageSource.Asset, onResult: (TextureRegion, TextureHandle?, Throwable?) -> Unit) {
        val cached = cache.get(source.path)
        if (cached != null) {
            onResult(cached.region, cached, null)
            return
        }

        AsyncDispatcher.launch {
            try {
                val bytes = context.host.resolveAssetBytes(source.path)
                    ?: throw IllegalArgumentException("Asset not found: ${source.path}")

                val pixmap = Pixmap(bytes, 0, bytes.size)
                uploadQueue.add(UploadTask(source.path, pixmap, listOf(onResult)))
            } catch (e: Throwable) {
                val fallback = fallbackRegion()
                val handle = cache.put("atlas:ohno", fallback.texture, kind = TextureKind.FALLBACK)
                AsyncDispatcher.onMainThread { onResult(fallback, handle, e) }
            }
        }
    }

    private fun loadFromFile(source: ImageSource.LocalFile, onResult: (TextureRegion, TextureHandle?, Throwable?) -> Unit) {
        val cacheKey = source.path.toString()
        val cached = cache.get(cacheKey)
        if (cached != null) {
            onResult(cached.region, cached, null)
            return
        }

        AsyncDispatcher.launch {
            try {
                val bytes = context.storage.readBytes(source.path)
                    ?: throw IllegalArgumentException("File not found: ${source.path}")
                val pixmap = Pixmap(bytes, 0, bytes.size)
                uploadQueue.add(UploadTask(cacheKey, pixmap, listOf(onResult)))
            } catch (e: Throwable) {
                val fallback = fallbackRegion()
                val handle = cache.put("atlas:ohno", fallback.texture, kind = TextureKind.FALLBACK)
                AsyncDispatcher.onMainThread { onResult(fallback, handle, e) }
            }
        }
    }

    companion object {
        /** Maximum number of GPU texture uploads permitted per render frame. */
        private const val MAX_UPLOADS_PER_FRAME = 4
    }
}
