# Core Subsystems Architecture

This document details NekoMod's foundational core subsystems, including asynchronous OkHttp networking, crash-resilient Okio persistence, LRU texture caching, and hierarchical localization.

---

## 1. Asynchronous Networking & Zero Frame Stall (`org.mdt.core.net`)

* **Engine:** Singleton `OkHttpClient` with HTTP/2 Connection Pooling.
* **Thread Safety:** Network I/O executes strictly on `Dispatchers.IO`. Byte streams and image decoding never stall Mindustry's OpenGL render thread (0 frame drop).
* **Fluent DSL:**
  ```kotlin
  val responseBytes = Net.get("https://raw.githubusercontent.com/.../image.png").awaitBytes()
  ```

---

## 2. VRAM Lifecycle & LRU Texture Cache (`org.mdt.core.cache` & `image`)

* **Reference Counting (`TextureHandle`):**
  * Node mount: `handle.acquire()`.
  * Node unmount: `handle.release()`.
* **64MB LRU Texture Cache (`LRUTextureCache`):**
  * Memory ceiling bounded to 64MB.
  * Least recently used textures are disposed safely on Mindustry's render thread (`Core.app.post`).
* **VRAM Upload Throttling:** `ImageLoader` caps GPU texture uploads to a maximum of 4 textures per frame in `processUploadQueue()` to maintain 60/144 FPS smoothness.

---

## 3. Atomic Disk Persistence (`KVStore` & `Storage` via Okio)

* **Disk Location:** Persisted in Mindustry's mod data directory: `%APPDATA%\Mindustry\nekomod\*.kv`.
* **Dual-Tier Caching:** Instant $O(1)$ in-memory lookups via `ConcurrentHashMap` backed by asynchronous Okio disk writes.
* **300ms Debounce & Nano-Staging Atomic Writes:** Mutations debounce for 300ms before writing to a unique nano-timestamped staging file (`default.kv.nanoTime().tmp`) under a `synchronized(lock)` block prior to `atomicMove`, preventing Windows NTFS file locking conflicts.
* **Usage Example:**
  ```kotlin
  KVStore.default.putInt("demo_counter", 10)
  val count = KVStore.default.getInt("demo_counter", 0)
  ```

---

## 4. Hierarchical Localization Engine (`I18nEngine`)

* **Nested Dot Notation:** Resolves localized keys with parameter substitution: `i18n("app.title")`, `i18n("btn.count", "count" to 5)`.
* **Automatic Locale Detection:** Defaults to game/system language settings (`vi`, `en`).
