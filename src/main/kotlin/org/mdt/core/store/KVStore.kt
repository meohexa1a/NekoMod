package org.mdt.core.store

import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import okio.Path
import org.mdt.core.async.AsyncDispatcher
import java.util.concurrent.ConcurrentHashMap

/**
 * ## KVStore
 *
 * Blazing fast, thread-safe Key-Value store backed by an in-memory cache
 * and Okio atomic disk persistence with debounced write throttling.
 */
class KVStore(name: String = "config") {

    private val storePath: Path = Storage.resolve("$name.kv")
    private val memoryMap = ConcurrentHashMap<String, String>()
    private var saveJob: Job? = null

    init {
        loadFromDisk()
    }

    private fun loadFromDisk() {
        val content = Storage.readString(storePath) ?: return
        for (line in content.lines()) {
            val trimmed = line.trim()
            if (trimmed.isEmpty() || trimmed.startsWith("#")) continue
            val separatorIdx = trimmed.indexOf('=')
            if (separatorIdx > 0) {
                val key = trimmed.substring(0, separatorIdx).trim()
                val value = trimmed.substring(separatorIdx + 1).trim()
                memoryMap[key] = value
            }
        }
    }

    /**
     * Schedules a debounced disk persistence pass (300ms after last mutation).
     */
    fun save() {
        saveJob?.cancel()
        saveJob = AsyncDispatcher.launch {
            delay(300L)
            flushToDisk()
        }
    }

    /**
     * Flushes current in-memory entries onto disk immediately.
     */
    fun flushToDisk() {
        val snapshot = HashMap(memoryMap)
        try {
            Storage.atomicWrite(storePath) { sink ->
                for ((k, v) in snapshot) {
                    sink.writeUtf8("$k=$v\n")
                }
            }
        } catch (_: Exception) {
            // Ignore collision retries
        }
    }

    // --- GETTERS ---

    fun getString(key: String, default: String = ""): String = memoryMap[key] ?: default
    fun getInt(key: String, default: Int = 0): Int = memoryMap[key]?.toIntOrNull() ?: default
    fun getFloat(key: String, default: Float = 0f): Float = memoryMap[key]?.toFloatOrNull() ?: default
    fun getBoolean(key: String, default: Boolean = false): Boolean = memoryMap[key]?.toBooleanStrictOrNull() ?: default

    // --- SETTERS ---

    fun putString(key: String, value: String): KVStore {
        memoryMap[key] = value
        save()
        return this
    }

    fun putInt(key: String, value: Int): KVStore = putString(key, value.toString())
    fun putFloat(key: String, value: Float): KVStore = putString(key, value.toString())
    fun putBoolean(key: String, value: Boolean): KVStore = putString(key, value.toString())

    fun remove(key: String): KVStore {
        if (memoryMap.remove(key) != null) {
            save()
        }
        return this
    }

    fun clear(): KVStore {
        memoryMap.clear()
        save()
        return this
    }

    companion object {
        /** Default shared application KV store instance. */
        val default by lazy { KVStore("default") }
    }
}
