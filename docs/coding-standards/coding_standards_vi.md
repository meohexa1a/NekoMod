# Tiêu chuẩn Lập trình & Quy ước Kiến trúc (Coding Standards)

Tài liệu này xác lập toàn bộ các quy chuẩn lập trình, quy ước đặt tên, nguyên tắc quản lý bộ nhớ và cấu trúc phân tầng bắt buộc cho dự án **NekoMod**.

---

## 1. Hệ thống Kiểu số & Đồ họa (Type System: `Float` 100%)

* **Quy tắc bắt buộc:** Toàn bộ kích thước hình học (`width`, `height`), lề (`margin`, `padding`), bán kính bo góc (`radius`), độ dày viền (`borderWidth`), độ mờ (`opacity`), hệ số mờ (`blurRadius`, `backdropWeight`) và tỷ lệ giãn nở (`weight`) **bắt buộc sử dụng `Float`** (hậu tố `f`).
* **Lý do kỹ thuật:**
  1. Đồng bộ hoàn hảo với hệ tọa độ OpenGL và Arc Graphics engine.
  2. Triệt tiêu hoàn toàn chi phí ép kiểu `.toFloat()` khi nạp uniform vào GPU Shaders.
  3. Code Compose tự nhiên: `Modifier.pad(12f).radius(8f).border(1f, Color.white).opacity(0.9f)`.

---

## 2. Quy ước Tầng Compose Declarative & Modifier

### A. Thứ tự tham số trong `@Composable` function
Mọi component giao diện phải tuân thủ thứ tự tham số chuẩn của Compose:
1. **Dữ liệu chính (Content/State):** `text: String`, `source: Any`, `checked: Boolean`.
2. **Sự kiện callback:** `onClick: () -> Unit`, `onToggle: () -> Unit`.
3. **Tham số Modifier:** `modifier: UIModifier = UIModifier` (hoặc `Modifier`).
4. **Cấu hình tùy chọn (Tùy biến Visual):** `colors: ButtonColors`, `radius: Float`, `scale: Float`.
5. **Slot Lambda con:** `content: @Composable BoxScope.() -> Unit = {}` (luôn ở vị trí cuối cùng).

### B. Chaining `UIModifier`
* Mọi component dựng sẵn khi cấu hình style mặc định **bắt buộc phải kết thúc bằng `.then(modifier)`** để người gọi bên ngoài có thể ghi đè hoặc bổ sung thuộc tính layout/interactivity.

---

## 3. Quản lý Luồng (Thread Dispatching) & An toàn VRAM

```
┌────────────────────────────────────────────────────────┐
│ BACKGROUND THREAD (Dispatchers.IO)                     │
│ • OkHttp Network I/O & Streaming                       │
│ • Okio Disk Reading & Atomic Writing                   │
│ • Background Pixmap Byte Decoding                      │
└──────────────────────────┬─────────────────────────────┘
                           │ AsyncDispatcher.onMainThread / Core.app.post
                           ▼
┌────────────────────────────────────────────────────────┐
│ RENDER THREAD (Mindustry OpenGL Loop - 60 FPS)         │
│ • OpenGL Texture Creation & GPU Uploads                │
│ • Virtual DOM Mutations & Composition Clock            │
│ • 2-Pass Godot Layout & SDF Shader Drawing             │
└────────────────────────────────────────────────────────┘
```

* **Quy tắc Quản lý VRAM:**
  * Tuyệt đối không khởi tạo `Texture(...)` trực tiếp trong hàm `@Composable`.
  * Toàn bộ GPU Texture phải đi qua `TextureHandle` (Reference Counting) hoặc `LRUTextureCache` để tự động thu hồi khi node bị unmount.

---

## 4. Quy ước KDoc & Tài liệu

1. **KDoc trong mã nguồn Kotlin (`*.kt`):**
   * **Bắt buộc 100% Tiếng Anh chuẩn.**
   * Tham chiếu tài liệu bằng text: `See: docs/architecture/architecture_en.md` (không dùng thẻ `@see` với đường dẫn file để tránh cảnh báo IDE).
2. **Tài liệu trong `docs/`:**
   * Luôn duy trì **2 bản song ngữ song song**: `*_vi.md` và `*_en.md`.
   * Mọi đường dẫn liên kết giữa các tài liệu phải là **đường dẫn tương đối (`./` hoặc `../`)**.

---

## 5. Phân tầng Package (Architecture Layering)

* `org.mdt.core.*`: Hạ tầng độc lập (Async, Network, Storage, Cache, Image, I18n).
* `org.mdt.ui.core`: Virtual Node DOM tree (`UINode`, `CanvasNode`, Events, Math).
* `org.mdt.ui.layout`: Thuật toán dàn trang 2-pass Godot (`GodotLayout`, SizeFlags, Anchors).
* `org.mdt.ui.render`: GPU Rendering Pipeline (`EngineRenderer`, `BoxRenderer`, `TextRenderer`, Shaders, BoxBlur).
* `org.mdt.ui.widgets`: Các Node giao diện cơ sở (`BoxNode`, `TextNode`, `ImageNode`, `Containers`).
* `org.mdt.ui.compose`: Tầng DSL Compose Multiplatform (`Components`, `Widgets`, `UIModifier`, Scopes).
