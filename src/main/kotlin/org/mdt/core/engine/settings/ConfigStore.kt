package org.mdt.core.engine.settings

import arc.util.Log
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.serialization.KSerializer
import kotlinx.serialization.json.Json
import kotlinx.serialization.serializer
import okio.FileSystem
import okio.Path
import okio.Path.Companion.toPath
import org.mdt.core.common.AsyncDispatcher
import java.io.IOException
import java.util.concurrent.atomic.AtomicReference
import kotlin.time.Duration.Companion.milliseconds

/**
 * ## ConfigStore
 *
 * Thread-safe, reactive, type-safe configuration store backed by typed Kotlinx Serialization
 * and atomic Okio disk persistence with debounced write throttling.
 *
 * ### Architectural Features:
 * 1. **Lock-Free State Mutation:** Utilizes [AtomicReference] CAS loops in [update] for high concurrency.
 * 2. **Debounced Disk Persistence:** Coalesces rapid sequential mutations into a single atomic write (300ms window).
 * 3. **JVM Shutdown Safety:** Automatically registers a shutdown hook to flush in-flight changes on exit.
 * 4. **Windows NTFS Lock Immunity:** Staged nanosecond temporary file writes prevent NTFS file lock contention (Rule 16).
 *
 * See: docs/core-subsystems/core_subsystems_en.md
 */
class ConfigStore<T : Any>(
    val path: Path,
    val default: T,
    private val serializer: KSerializer<T>,
    private val json: Json = defaultJson,
    private val fileSystem: FileSystem = FileSystem.SYSTEM
) {
    private val stateRef = AtomicReference<T>(loadFromDisk())
    private var saveJob: Job? = null
    private val listeners = ArrayList<(T) -> Unit>()

    /** Current snapshot value of the configuration. Setting a new value triggers an atomic update. */
    var value: T
        get() = stateRef.get()
        set(newValue) {
            update { newValue }
        }

    init {
        Runtime.getRuntime().addShutdownHook(Thread { flushToDisk() })
    }

    private fun loadFromDisk(): T {
        if (!fileSystem.exists(path)) return default
        val content = try {
            fileSystem.read(path) { readUtf8() }
        } catch (e: IOException) {
            Log.warn("[ConfigStore] Failed to read $path: ${e.message}")
            return default
        }

        return try {
            json.decodeFromString(serializer, content)
        } catch (e: Throwable) {
            Log.warn("[ConfigStore] Failed to parse $path, falling back to default: ${e.message}")
            default
        }
    }

    /**
     * Atomically transforms the configuration state using a lock-free CAS loop and schedules a debounced save.
     *
     * @param transform State transition lambda.
     * @return The updated state value.
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
     * Flushes current configuration onto disk immediately using atomic staging.
     */
    fun flushToDisk() {
        try {
            val content = json.encodeToString(serializer, stateRef.get())
            val parentDir = path.parent
            if (parentDir != null && !fileSystem.exists(parentDir)) {
                fileSystem.createDirectories(parentDir)
            }

            val stagingFile = (path.toString() + ".tmp." + System.nanoTime()).toPath()
            try {
                fileSystem.write(stagingFile, mustCreate = false) {
                    writeUtf8(content)
                    flush()
                }
                fileSystem.atomicMove(stagingFile, path)
            } catch (e: Throwable) {
                try {
                    fileSystem.delete(stagingFile, mustExist = false)
                } catch (_: Throwable) {}
                throw e
            }
        } catch (e: Throwable) {
            Log.warn("[ConfigStore] Failed to flush $path: ${e.message}")
        }
    }

    /**
     * Registers a listener callback invoked whenever the state updates.
     */
    fun addListener(listener: (T) -> Unit) {
        synchronized(listeners) { listeners.add(listener) }
    }

    /**
     * Unregisters a state update listener.
     */
    fun removeListener(listener: (T) -> Unit) {
        synchronized(listeners) { listeners.remove(listener) }
    }

    private fun notifyListeners(newValue: T) {
        val copy = synchronized(listeners) { listeners.toList() }
        for (listener in copy) listener(newValue)
    }

    companion object {
        /** Default JSON serializer for configuration stores. */
        val defaultJson: Json = Json {
            ignoreUnknownKeys = true
            encodeDefaults = true
            isLenient = true
            coerceInputValues = true
            prettyPrint = true
        }
    }
}
