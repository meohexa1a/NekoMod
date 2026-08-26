package org.mdt.core.store

import arc.util.Log
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.serialization.KSerializer
import kotlinx.serialization.json.Json
import kotlinx.serialization.serializer
import okio.Path
import org.mdt.core.common.AsyncDispatcher
import java.util.concurrent.atomic.AtomicReference
import kotlin.time.Duration.Companion.milliseconds

/**
 * ## ConfigStore
 *
 * Thread-safe, reactive, type-safe configuration store backed by typed Kotlinx Serialization
 * and atomic Okio disk persistence with debounced write throttling.
 *
 * See: docs/core-subsystems/core_subsystems_en.md
 */
class ConfigStore<T : Any>(
    val path: Path,
    val default: T,
    private val serializer: KSerializer<T>,
    private val json: Json = Storage.prettyJson
) {
    private val stateRef = AtomicReference<T>(loadFromDisk())
    private var saveJob: Job? = null
    private val listeners = ArrayList<(T) -> Unit>()

    var value: T
        get() = stateRef.get()
        set(newValue) {
            update { newValue }
        }

    init {
        Runtime.getRuntime().addShutdownHook(Thread { flushToDisk() })
    }

    private fun loadFromDisk(): T {
        val content = Storage.readString(path) ?: return default
        return try {
            json.decodeFromString(serializer, content)
        } catch (e: Throwable) {
            Log.warn("[ConfigStore] Failed to parse $path, falling back to default: ${e.message}")
            default
        }
    }

    /**
     * Atomically transforms the configuration state and schedules a debounced save.
     */
    fun update(transform: (T) -> T): T {
        while (true) {
            val current = stateRef.get()
            val next = transform(current)
            if (stateRef.compareAndSet(current, next)) {
                if (current != next) {
                    save()
                    notifyListeners(next)
                }
                return next
            }
        }
    }

    /**
     * Schedules a debounced disk save pass (300ms after last mutation).
     */
    fun save() {
        saveJob?.cancel()
        saveJob = AsyncDispatcher.launch {
            delay(300L.milliseconds)
            flushToDisk()
        }
    }

    /**
     * Flushes current configuration onto disk immediately.
     */
    fun flushToDisk() {
        try {
            val content = json.encodeToString(serializer, stateRef.get())
            Storage.writeString(path, content)
        } catch (e: Throwable) {
            Log.warn("[ConfigStore] Failed to flush $path: ${e.message}")
        }
    }

    fun addListener(listener: (T) -> Unit) {
        synchronized(listeners) { listeners.add(listener) }
    }

    fun removeListener(listener: (T) -> Unit) {
        synchronized(listeners) { listeners.remove(listener) }
    }

    private fun notifyListeners(newValue: T) {
        val copy = synchronized(listeners) { listeners.toList() }
        for (listener in copy) listener(newValue)
    }

    companion object {
        inline fun <reified T : Any> create(
            name: String,
            default: T,
            json: Json = Storage.prettyJson
        ): ConfigStore<T> = ConfigStore(Storage.resolve("$name.json"), default, serializer<T>(), json)
    }
}
