# Core Subsystems Architecture

This document details NekoMod's core foundation subsystems, including asynchronous OkHttp networking, atomic Okio storage, LRU texture caching, and next-gen i18n localization.

---

## 1. Asynchronous Networking & Zero Frame Stalls (`org.mdt.core.net`)

* **Engine:** Backed by `OkHttpClient` with connection pooling (reusing HTTP/2 sockets).
* **Game Loop Safety:** All network I/O and byte decoding execute asynchronously on Kotlin Coroutines `Dispatchers.IO`. Results never stall or hitch the game's 60-144 FPS render loop.
* **Fluent DSL:**
  ```kotlin
  val responseText = Net.get("https://api.example.com/data") {
      header("Authorization", "Bearer $token")
      param("type", "latest")
  }.awaitString()
  ```

---

## 2. Safe GPU VRAM Management & Texture Caching (`org.mdt.core.cache` & `image`)

* **Problem Solved:** Unmanaged OpenGL texture creation quickly leads to VRAM bloat or native memory leaks.
* **Reference Counting (`TextureHandle`):**
  * When `ImageNode` mounts to the UI tree: `handle.retain()`.
  * When `ImageNode` is unmounted/destroyed: `handle.release()`.
* **LRU Bounded Cache (`LRUTextureCache`):**
  * Hard VRAM ceiling (default: 64MB).
  * Automatically evicts and disposes oldest unused textures on Mindustry's OpenGL Render Thread (`Core.app.post`).

---

## 3. Asynchronous Image Pipeline (`AsyncImageLoader` & `ImageNode`)

4-step non-blocking image pipeline:
```
1. URL Request ──► 2. Fetch Bytes (OkHttp) ──► 3. Decode Pixmap (Background) ──► 4. Upload Texture (GL Thread)
```
* **Composable DSL:**
  ```kotlin
  Image(
      source = "https://raw.githubusercontent.com/.../icon.png",
      modifier = Modifier.size(48f, 48f),
      scaleMode = ScaleMode.FIT
  )
  ```

---

## 4. Crash-Resilient Atomic Storage (`KVStore` via Okio)

* **Dual Cache Architecture:** In-memory `ConcurrentHashMap` for instant $O(1)$ reads + asynchronous disk persistence via Okio.
* **Atomic File Writes:** Writes to staging `.tmp` file before atomic replacement, completely preventing corrupted settings files if the process is terminated abruptly.
* **Usage:**
  ```kotlin
  KVStore.default.putInt("user_score", 150)
  val score = KVStore.default.getInt("user_score", 0)
  ```

---

## 5. Next-Gen Internationalization (`I18nEngine`)

* Hierarchical nested keys: `i18n("app.title")`.
* Dynamic parameter interpolation: `i18n("btn.count", "count" to 5)`.
* Hot-reloadable locale dictionaries.
