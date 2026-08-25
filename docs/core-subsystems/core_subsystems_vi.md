# Kiến trúc Hạ tầng Lõi (Core Subsystems Architecture)

Tài liệu này mô tả chi tiết các hệ thống con cốt lõi (Core Subsystems) của NekoMod, bao gồm mạng bất đồng bộ OkHttp, lưu trữ nguyên tử Okio, bộ nhớ đệm Texture LRU và hệ thống đa ngôn ngữ hiện đại.

---

## 1. Mạng Bất đồng bộ & Zero Frame Stall (`org.mdt.core.net`)

* **Engine:** Sử dụng `OkHttpClient` với Connection Pool (tái sử dụng kết nối HTTP/2).
* **Luồng chạy an toàn:** Toàn bộ I/O mạng được thực thi trên `Dispatchers.IO` của Kotlin Coroutines. Kết quả tải và decode ảnh không bao giờ làm đứng khung hình game (0 frame drop).
* **Fluent DSL:**
  ```kotlin
  val responseBytes = Net.get("https://raw.githubusercontent.com/.../image.png").awaitBytes()
  ```

---

## 2. Quản lý Bộ nhớ VRAM & Cache Texture (`org.mdt.core.cache` & `image`)

* **Mô hình Đếm Tham chiếu (`TextureHandle`):**
  * Khi `ImageNode` hiển thị ảnh: `handle.retain()`.
  * Khi `ImageNode` bị hủy khỏi cây UI: `handle.release()`.
* **Bộ đệm LRU (`LRUTextureCache`):**
  * Giới hạn trần VRAM (mặc định 64MB).
  * Khi vượt ngưỡng, tự động giải phóng (dispose) các texture ít dùng nhất trên Render Thread của Mindustry (`Core.app.post`).
* **Hàng đợi Upload VRAM Throttling:** `ImageLoader` giới hạn tối đa nạp 4 GPU Texture mỗi frame trong `processUploadQueue()`, tránh nghẽn luồng render khi tải đồng loạt nhiều ảnh.

---

## 3. Lưu trữ Dữ liệu Nguyên tử (`KVStore` & `Storage` via Okio)

* **Vị trí lưu trữ:** Lưu tại thư mục dữ liệu của Mindustry: `%APPDATA%\Mindustry\nekomod\*.kv`.
* **Bộ nhớ đệm kép:** Lưu trên RAM qua `ConcurrentHashMap` để đọc tức thì ($O(1)$) + Tự động lưu xuống đĩa bằng Okio.
* **Debounce 300ms & Nano-Staging File:** Trì hoãn ghi đĩa 300ms sau lần thao tác cuối cùng; ghi vào file tạm thời có nano-timestamp (`default.kv.nanoTime().tmp`) và đồng bộ qua `synchronized(lock)` trước khi `atomicMove` để chống lỗi khóa file NTFS trên Windows.
* **Cách sử dụng:**
  ```kotlin
  KVStore.default.putInt("demo_counter", 10)
  val count = KVStore.default.getInt("demo_counter", 0)
  ```

---

## 4. Hệ thống Đa ngôn ngữ Phân cấp (`I18nEngine`)

* **Nested Dot Notation:** Hỗ trợ khóa lồng nhau `i18n("app.title")`, `i18n("btn.count", "count" to 5)`.
* **Đa ngôn ngữ mặc định:** Tự động phát hiện ngôn ngữ của game (`vi`, `en`) từ hệ thống hoặc cấu hình mod.
