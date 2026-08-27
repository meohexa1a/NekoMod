package org.mdt.core.common

import arc.graphics.Texture
import arc.graphics.g2d.TextureRegion
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicInteger

/**
 * ## TextureOwnership
 *
 * Defines whether the underlying OpenGL [Texture] is owned and disposable by this handle.
 *
 * See: docs/core-subsystems/core_subsystems_en.md
 */
enum class TextureOwnership {
    /** Owned by the engine -> Eligible for GPU disposal upon release/eviction. */
    MANAGED,

    /** Externally owned (e.g. Game Atlas, static textures) -> Never disposed. */
    SHARED
}

typealias TextureKind = TextureOwnership

/**
 * ## TextureHandle
 *
 * Lean, thread-safe reference-counted handle wrapping an unmanaged OpenGL [Texture].
 * Manages atomic reference counting and GPU texture disposal without coupling to cache or loader logic.
 *
 * ### Invariants:
 * - Textures marked [TextureOwnership.SHARED] are never destroyed on the GPU.
 * - Negative reference count underflows from redundant [release] calls are strictly prevented.
 * - Disposals are automatically dispatched to the OpenGL main render thread.
 *
 * See: docs/core-subsystems/core_subsystems_en.md
 */
class TextureHandle(
    val texture: Texture,
    val ownership: TextureOwnership = TextureOwnership.MANAGED,
    val key: String = "",
    val byteSize: Long = computeByteSize(texture)
) {
    /** TextureRegion wrapping the underlying OpenGL texture. */
    val region: TextureRegion = TextureRegion(texture)

    /** Width in pixels of the underlying texture. */
    val width: Int get() = texture.width

    /** Height in pixels of the underlying texture. */
    val height: Int get() = texture.height

    /** Backward compatible alias for [ownership]. */
    val kind: TextureOwnership get() = ownership

    /** Whether this texture is managed and eligible for GPU disposal. */
    val isDisposable: Boolean get() = ownership == TextureOwnership.MANAGED

    private val refCount = AtomicInteger(1)
    private val _isDisposed = AtomicBoolean(false)

    /** Whether the underlying OpenGL texture has been disposed from GPU memory. */
    val isDisposed: Boolean get() = _isDisposed.get()

    /** Current number of active references holding this texture. Returns 0 if unreferenced or disposed. */
    val activeRefCount: Int get() = maxOf(0, refCount.get())

    // =========================================================================
    // I. Atomic Reference Counting & Lifecycle Operations
    // =========================================================================

    /**
     * Atomically increments the reference count using a lock-free CAS loop.
     *
     * @return `true` if retained successfully, or `false` if the handle was already disposed.
     */
    fun retain(): Boolean {
        while (true) {
            if (_isDisposed.get()) return false
            val current = refCount.get()
            if (current < 0) return false
            if (refCount.compareAndSet(current, current + 1)) {
                return true
            }
        }
    }

    /**
     * Atomically decrements the reference count using a lock-free CAS loop.
     *
     * @return `true` if released successfully, or `false` if the handle was already disposed or at 0.
     */
    fun release(): Boolean {
        while (true) {
            if (_isDisposed.get()) return false
            val current = refCount.get()
            if (current <= 0) return false
            if (refCount.compareAndSet(current, current - 1)) {
                return true
            }
        }
    }

    /**
     * Atomically marks this handle as disposed and releases the underlying GPU texture if [isDisposable].
     * Dispatches texture disposal safely to the main render thread.
     *
     * @return `true` if this call transitioned the handle to disposed state, or `false` if already disposed.
     */
    fun dispose(): Boolean {
        if (!_isDisposed.compareAndSet(false, true)) return false

        refCount.set(-1)
        if (isDisposable) {
            AsyncDispatcher.onMainThread {
                texture.dispose()
            }
        }
        return true
    }

    // =========================================================================
    // II. Factory & Utility Companion Methods
    // =========================================================================

    companion object {
        /**
         * Calculates the standard 32-bit RGBA VRAM byte size of a texture.
         */
        fun computeByteSize(texture: Texture): Long =
            texture.width.toLong() * texture.height.toLong() * 4L

        /**
         * Factory creating a managed [TextureHandle] from a raw [Texture].
         */
        fun managed(texture: Texture, key: String = ""): TextureHandle =
            TextureHandle(texture, TextureOwnership.MANAGED, key)

        /**
         * Factory creating an unmanaged, shared [TextureHandle] (e.g. from an atlas).
         */
        fun shared(texture: Texture, key: String = ""): TextureHandle =
            TextureHandle(texture, TextureOwnership.SHARED, key)

        /**
         * Factory creating an unmanaged, shared [TextureHandle] from a [TextureRegion].
         */
        fun shared(region: TextureRegion, key: String = ""): TextureHandle =
            TextureHandle(region.texture, TextureOwnership.SHARED, key)

        /**
         * Generic factory constructor helper.
         */
        fun of(
            key: String,
            texture: Texture,
            ownership: TextureOwnership = TextureOwnership.MANAGED
        ): TextureHandle = TextureHandle(texture, ownership, key)
    }
}

/**
 * Executes the given [block] holding a reference to this [TextureHandle], releasing it automatically upon completion.
 */
inline fun <R> TextureHandle.use(block: (TextureHandle) -> R): R {
    return try {
        block(this)
    } finally {
        release()
    }
}
