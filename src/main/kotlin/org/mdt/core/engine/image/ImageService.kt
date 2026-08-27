package org.mdt.core.engine.image

import arc.graphics.Pixmap
import arc.graphics.Texture
import arc.graphics.g2d.TextureRegion
import arc.util.Log
import okio.ByteString.Companion.encodeUtf8
import okio.Path
import org.mdt.core.common.AsyncDispatcher
import org.mdt.core.common.LRUTextureCache
import org.mdt.core.common.Net
import org.mdt.core.common.TextureHandle
import org.mdt.core.engine.EngineContext
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.ConcurrentLinkedQueue

/**
 * Functional callback invoked when an image resolution attempt completes.
 *
 * @param region Resolved [TextureRegion] (actual image or fallback placeholder).
 * @param handle Retained [TextureHandle] if cached in memory (null for direct unmanaged regions).
 * @param error [Throwable] error if the resolution failed, or `null` on success.
 */
typealias ImageLoadCallback = (region: TextureRegion, handle: TextureHandle?, error: Throwable?) -> Unit

/**
 * ## ImageService
 *
 * Multi-tiered, high-performance image and texture loading pipeline featuring:
 * 1. **L1 VRAM Memory Cache:** Lock-free [LRUTextureCache] bound to the active context.
 * 2. **In-Flight Request Deduplication (Coalescing):** Merges concurrent requests for the same URL.
 * 3. **L2 Persistent Disk Cache:** MD5-hashed disk storage managed via [EngineContext.storage].
 * 4. **Throttled Burst GPU Upload:** Uploads max 4 textures per render frame to eliminate visual micro-stutters.
 * 5. **Graceful Error Fallbacks:** Platform-abstracted placeholder resolution via [EngineContext.host].
 *
 * See: docs/core-subsystems/core_subsystems_en.md
 */
open class ImageService(val context: EngineContext) {

    /** Context-bound L1 VRAM texture cache. */
    open val cache: LRUTextureCache by lazy { createTextureCache() }

    /** Factory method creating the L1 texture cache instance for this context. */
    protected open fun createTextureCache(): LRUTextureCache = LRUTextureCache()

    /** Internal descriptor for pending asynchronous GPU pixmap texture uploads. */
    private class PendingUploadTask(
        val cacheKey: String,
        val pixmap: Pixmap,
        val pendingCallbacks: List<ImageLoadCallback>
    )

    private val pendingUploadQueue = ConcurrentLinkedQueue<PendingUploadTask>()
    private val inFlightUrlRequests = ConcurrentHashMap<String, MutableList<ImageLoadCallback>>()
    private var isRenderHookInstalled = false

    init {
        installRenderFrameHook()
    }

    private fun installRenderFrameHook() {
        if (!isRenderHookInstalled) {
            isRenderHookInstalled = true
            context.host.onFrameEnd { processUploadQueue() }
        }
    }

    // =========================================================================
    // I. Primary Public Loading & Resolution API
    // =========================================================================

    /**
     * Asynchronously loads an image from an [ImageSource] descriptor and delivers the result via [callback].
     *
     * @param source The origin descriptor for the image asset.
     * @param callback Callback delivering the resolved [TextureRegion], [TextureHandle], and any [Throwable].
     */
    fun load(source: ImageSource, callback: ImageLoadCallback) {
        when (source) {
            is ImageSource.Atlas -> loadAtlasSource(source, callback)
            is ImageSource.Region -> loadRegionSource(source, callback)
            is ImageSource.Url -> loadUrlSource(source, callback)
            is ImageSource.Asset -> loadAssetSource(source, callback)
            is ImageSource.LocalFile -> loadLocalFileSource(source, callback)
        }
    }

    /**
     * Resolves the platform-specific fallback placeholder [TextureRegion].
     */
    fun fallbackRegion(): TextureRegion = context.host.resolveFallbackRegion()

    /**
     * Resolves and caches the platform-specific fallback placeholder [TextureHandle].
     */
    fun fallbackHandle(): TextureHandle = cache.getOrPut("fallback:placeholder") {
        TextureHandle.shared(fallbackRegion(), "fallback:placeholder")
    }

    // =========================================================================
    // II. Pipeline Source Handlers
    // =========================================================================

    private fun loadAtlasSource(source: ImageSource.Atlas, callback: ImageLoadCallback) {
        val resolvedRegion = context.host.resolveAtlasRegion(source.name)
        if (resolvedRegion != null) {
            val handle = cache.getOrPut("atlas:${source.name}") {
                TextureHandle.shared(resolvedRegion, "atlas:${source.name}")
            }
            callback(resolvedRegion, handle, null)
        } else {
            val fallback = fallbackHandle()
            callback(fallback.region, fallback, IllegalArgumentException("Atlas sprite region not found: ${source.name}"))
        }
    }

    private fun loadRegionSource(source: ImageSource.Region, callback: ImageLoadCallback) {
        val key = "region:" + source.region.texture.hashCode()
        val handle = cache.getOrPut(key) {
            TextureHandle.shared(source.region, key)
        }
        callback(source.region, handle, null)
    }

    private fun loadUrlSource(source: ImageSource.Url, callback: ImageLoadCallback) {
        val cacheKey = source.url

        // 1. L1 VRAM Memory Cache lookup
        val cachedHandle = cache.get(cacheKey)
        if (cachedHandle != null) {
            callback(cachedHandle.region, cachedHandle, null)
            return
        }

        // 2. In-Flight Request Deduplication (Coalescing)
        val isLeaderRequest = registerInFlightUrlRequest(cacheKey, callback)
        if (!isLeaderRequest) return

        // 3. Background Download & L2 Cache Pipeline
        AsyncDispatcher.launch {
            try {
                val rawBytes = fetchOrReadCachedUrlBytes(source, cacheKey)
                val decodedPixmap = Pixmap(rawBytes, 0, rawBytes.size)
                val targetCallbacks = drainInFlightUrlRequests(cacheKey)

                enqueuePixmapUpload(cacheKey, decodedPixmap, targetCallbacks)
            } catch (networkError: Throwable) {
                Log.err("[ImageService] Failed to load network image from ${source.url}", networkError)
                val targetCallbacks = drainInFlightUrlRequests(cacheKey)
                dispatchFallbackError(targetCallbacks, networkError)
            }
        }
    }

    private fun loadAssetSource(source: ImageSource.Asset, callback: ImageLoadCallback) {
        val cacheKey = source.path
        val cachedHandle = cache.get(cacheKey)
        if (cachedHandle != null) {
            callback(cachedHandle.region, cachedHandle, null)
            return
        }

        AsyncDispatcher.launch {
            try {
                val rawBytes = context.host.resolveAssetBytes(source.path)
                    ?: throw IllegalArgumentException("Classpath asset not found: ${source.path}")

                val decodedPixmap = Pixmap(rawBytes, 0, rawBytes.size)
                enqueuePixmapUpload(cacheKey, decodedPixmap, listOf(callback))
            } catch (assetError: Throwable) {
                dispatchFallbackError(listOf(callback), assetError)
            }
        }
    }

    private fun loadLocalFileSource(source: ImageSource.LocalFile, callback: ImageLoadCallback) {
        val cacheKey = source.path.toString()
        val cachedHandle = cache.get(cacheKey)
        if (cachedHandle != null) {
            callback(cachedHandle.region, cachedHandle, null)
            return
        }

        AsyncDispatcher.launch {
            try {
                val rawBytes = context.storage.readBytes(source.path)
                    ?: throw IllegalArgumentException("Local filesystem image not found: ${source.path}")

                val decodedPixmap = Pixmap(rawBytes, 0, rawBytes.size)
                enqueuePixmapUpload(cacheKey, decodedPixmap, listOf(callback))
            } catch (fileError: Throwable) {
                dispatchFallbackError(listOf(callback), fileError)
            }
        }
    }

    // =========================================================================
    // III. Asynchronous I/O, Network & Disk Caching Helpers
    // =========================================================================

    /**
     * Atomically registers a pending request callback for a URL.
     *
     * @return `true` if this is the primary leader request responsible for performing the network fetch.
     */
    private fun registerInFlightUrlRequest(urlKey: String, callback: ImageLoadCallback): Boolean =
        synchronized(inFlightUrlRequests) {
            val pendingList = inFlightUrlRequests.getOrPut(urlKey) { ArrayList() }
            val isFirst = pendingList.isEmpty()
            pendingList.add(callback)
            isFirst
        }

    /**
     * Atomically removes and returns all pending callbacks waiting on a URL resolution.
     */
    private fun drainInFlightUrlRequests(urlKey: String): List<ImageLoadCallback> =
        synchronized(inFlightUrlRequests) {
            inFlightUrlRequests.remove(urlKey) ?: emptyList()
        }

    /**
     * Reads image bytes from L2 persistent disk cache, or downloads from network and saves to cache.
     */
    private suspend fun fetchOrReadCachedUrlBytes(source: ImageSource.Url, urlKey: String): ByteArray {
        val diskCacheFilePath = computeDiskCacheFilePath(urlKey)
        val diskCachedBytes = context.storage.readBytes(diskCacheFilePath)
        if (diskCachedBytes != null) return diskCachedBytes

        val downloadedBytes = Net.get(source.url, source.configureRequest ?: {}).awaitBytes()
        try {
            context.storage.writeBytes(diskCacheFilePath, downloadedBytes)
        } catch (diskWriteError: Throwable) {
            Log.warn("[ImageService] Failed to persist image $urlKey to L2 disk cache: ${diskWriteError.message}")
        }
        return downloadedBytes
    }

    /**
     * Computes the MD5-hashed persistent disk cache file path for a URL.
     */
    private fun computeDiskCacheFilePath(urlKey: String): Path {
        val md5HexHash = urlKey.encodeUtf8().md5().hex()
        return context.storage.resolveCache("images/$md5HexHash.bin")
    }

    // =========================================================================
    // IV. GPU Upload Queue & Error Dispatching
    // =========================================================================

    /**
     * Schedules a decoded [Pixmap] for upload to the GPU on the main render thread.
     */
    private fun enqueuePixmapUpload(
        cacheKey: String,
        pixmap: Pixmap,
        pendingCallbacks: List<ImageLoadCallback>
    ) {
        pendingUploadQueue.add(PendingUploadTask(cacheKey, pixmap, pendingCallbacks))
    }

    /**
     * Dispatches an error with the fallback placeholder texture to all [targetCallbacks] on the main thread.
     */
    private fun dispatchFallbackError(
        targetCallbacks: List<ImageLoadCallback>,
        error: Throwable
    ) {
        val fallback = fallbackHandle()
        AsyncDispatcher.onMainThread {
            for (callback in targetCallbacks) {
                callback(fallback.region, fallback, error)
            }
        }
    }

    /**
     * Maximum number of GPU texture uploads permitted per render frame.
     * Defaults to 4, dynamically driven by [AppSettingsService] graphics configuration.
     */
    open val maxUploadsPerFrame: Int
        get() = (context.settings as? org.mdt.core.engine.settings.AppSettingsService)?.current?.graphics?.maxUploadsPerFrame ?: 4

    /**
     * Processes queued texture uploads onto the GPU (throttled to [maxUploadsPerFrame] per frame).
     * Invoked automatically at the end of every render frame.
     */
    fun processUploadQueue() {
        val maxLimit = maxUploadsPerFrame
        var processedCount = 0
        while (processedCount < maxLimit && !pendingUploadQueue.isEmpty()) {
            val task = pendingUploadQueue.poll() ?: break
            processedCount++
            try {
                val uploadedTexture = Texture(task.pixmap).apply {
                    setFilter(Texture.TextureFilter.linear)
                }
                task.pixmap.dispose()
                val handle = TextureHandle.managed(uploadedTexture, task.cacheKey)
                val cachedHandle = cache.put(handle)
                for (callback in task.pendingCallbacks) {
                    callback(cachedHandle.region, cachedHandle, null)
                }
            } catch (uploadError: Throwable) {
                Log.err("[ImageService] Failed to upload GPU texture for key: ${task.cacheKey}", uploadError)
                task.pixmap.dispose()
                val fallback = fallbackHandle()
                for (callback in task.pendingCallbacks) {
                    callback(fallback.region, fallback, uploadError)
                }
            }
        }
    }
}
