package org.mdt.core.common

import arc.graphics.Texture
import arc.graphics.g2d.TextureRegion
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicInteger
import java.util.concurrent.atomic.AtomicLong

/**
 * ## TextureKind
 *
 * Defines the lifecycle and ownership model of a cached OpenGL texture.
 */
enum class TextureKind {
    /** Dynamic texture allocated by the mod engine -> Safe to dispose on GPU when evicted. */
    MANAGED,

    /** External shared texture (e.g. Mindustry Game Atlas) -> Never disposed. */
    SHARED,

    /** Error placeholder texture (e.g. iconic 'ohno') -> Never disposed, marks loading failure. */
    FALLBACK
}

/**
 * ## TextureHandle
 *
 * Thread-safe, lock-free reference-counted wrapper around an unmanaged OpenGL [Texture].
 * Uses atomic Compare-And-Swap (CAS) state transitions for coroutine compatibility without blocking locks.
 *
 * See: docs/core-subsystems/core_subsystems_en.md
 */
class TextureHandle(
    val key: String,
    val texture: Texture,
    val byteSize: Long,
    val kind: TextureKind = TextureKind.MANAGED,
    private val onZeroRefs: (TextureHandle) -> Unit = {}
) {
    val region = TextureRegion(texture)

    val isDisposable: Boolean get() = kind == TextureKind.MANAGED
    val isFailed: Boolean get() = kind == TextureKind.FALLBACK

    private val refCount = AtomicInteger(1)
    private val _isDisposed = AtomicBoolean(false)
    val lastAccessTimeNano = AtomicLong(System.nanoTime())

    val isDisposed: Boolean get() = _isDisposed.get()

    /** Current number of active references holding this texture. */
    val activeRefCount: Int get() = maxOf(0, refCount.get())

    /**
     * Atomically increments the reference count using a lock-free CAS loop.
     * Guaranteed to succeed only if the handle is not disposed and not dead.
     */
    fun retain(): Boolean {
        while (true) {
            if (_isDisposed.get()) return false
            val current = refCount.get()
            if (current < 0) return false
            if (refCount.compareAndSet(current, current + 1)) {
                lastAccessTimeNano.set(System.nanoTime())
                return true
            }
        }
    }

    /**
     * Atomically decrements the reference count using a lock-free CAS loop.
     * Triggers [onZeroRefs] exactly once upon transitioning from 1 to 0.
     * Protected against negative underflow from duplicate release calls.
     */
    fun release(): Boolean {
        while (true) {
            if (_isDisposed.get()) return false
            val current = refCount.get()
            if (current <= 0) return false

            if (refCount.compareAndSet(current, current - 1)) {
                lastAccessTimeNano.set(System.nanoTime())
                if (current - 1 == 0) onZeroRefs(this)
                return true
            }
        }
    }

    /**
     * Atomically marks this handle as disposed and releases the underlying GPU texture if [isDisposable].
     */
    internal fun disposeDirectly(): Boolean {
        if (!_isDisposed.compareAndSet(false, true)) return false

        refCount.set(-1)
        if (isDisposable) texture.dispose()
        return true
    }
}
