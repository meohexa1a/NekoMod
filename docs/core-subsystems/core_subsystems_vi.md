# Kiến trúc Hạ tầng Lõi (Core Subsystems Architecture)

Tài liệu này mô tả chi tiết các hệ thống con cốt lõi (Core Subsystems) của NekoMod, bao gồm mạng bất đồng bộ OkHttp, lưu trữ nguyên tử Okio, bộ nhớ đệm Texture LRU và hệ thống đa ngôn ngữ hiện đại.

---

## 1. Mạng Bất đồng bộ & Zero Frame Stall (`org.mdt.core.net`)

* **Engine:** Sử dụng `OkHttpClient` với Connection Pool (tái sử dụng kết nối HTTP/2).
* **Luồng chạy an toàn:** Toàn bộ I/O mạng được thực thi trên `Dispatchers.IO` của Kotlin Coroutines. Kết quả tải và decode ảnh không bao giờ làm đứng khung hình game (0 frame drop).
* **Fluent DSL:**
  ```kotlin
  val responseText = Net.get("https://api.example.com/data") {
      header("Authorization", "Bearer $token")
      param("type", "latest")
  }.awaitString()
  ```

---

## 2. Quản lý Bộ nhớ VRAM & Cache Texture (`org.mdt.core.cache` & `image`)

* **Vấn đề giải quyết:** Nạp texture tùy tiện vào GPU dễ làm tràn VRAM hoặc rò rỉ bộ nhớ khi tắt giao diện.
* **Mô hình Đếm Tham chiếu (`TextureHandle`):**
  * Khi `ImageNode` hiển thị ảnh: `handle.retain()`.
  * Khi `ImageNode` bị hủy khỏi cây UI: `handle.release()`.
* **Bộ đệm LRU (`LRUTextureCache`):**
  * Giới hạn trần VRAM (mặc định 64MB).
  * Khi vượt ngưỡng, tự động giải phóng (dispose) các texture ít dùng nhất trên Render Thread của Mindustry (`Core.app.post`).

---

## 3. Tải ảnh Bất đồng bộ (`AsyncImageLoader` & `ImageNode`)

Pipeline tải ảnh 4 bước khép kín:
```
1. URL Request ──► 2. Tải bytes (OkHttp) ──► 3. Decode Pixmap (Background) ──► 4. Upload Texture (GL Thread)
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

## 4. Lưu trữ Dữ liệu Nguyên tử (`KVStore` via Okio)

* **Bộ nhớ đệm kép:** Lưu trên RAM qua `ConcurrentHashMap` để đọc tức thì ($O(1)$) + Tự động lưu xuống đĩa bằng Okio.
* **Ghi file nguyên tử (Atomic Write):** Ghi vào file `.tmp` trước khi thay thế file chính, đảm bảo file cấu hình không bao giờ bị hỏng dù game bị tắt đột ngột.
* **Cách sử dụng:**
  ```kotlin
  KVStore.default.putInt("user_score", 150)
  val score = KVStore.default.getInt("user_score", 0)
  ```

---

## 5. Đa ngôn ngữ Hiện đại (`I18nEngine`)

* Hỗ trợ key phân cấp nhiều tầng: `i18n("app.title")`.
* Hỗ trợ nội suy tham số động: `i18n("btn.count", "count" to 5)`.
* Đổi ngôn ngữ tức thì trong game mà không cần khởi động lại.
