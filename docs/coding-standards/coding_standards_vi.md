# Tiêu chuẩn Lập trình & Quy ước Kiến trúc (Coding Standards)

Tài liệu này xác lập toàn bộ các quy chuẩn lập trình, quy ước đặt tên, nguyên tắc quản lý bộ nhớ và cấu trúc phân tầng bắt buộc cho dự án **NekoMod**.

---

## 🎨 I. Đồ họa, Shaders & Typography

### 1. Hệ thống Kiểu số & Đồ họa (Type System: `Float` 100%)
* **Quy tắc bắt buộc:** Toàn bộ kích thước hình học (`width`, `height`), lề (`margin`, `padding`), bán kính bo góc (`radius`), độ dày viền (`borderWidth`), độ mờ (`opacity`), hệ số mờ (`blurRadius`, `backdropWeight`) và tỷ lệ giãn nở (`weight`) **bắt buộc sử dụng `Float`** (hậu tố `f`).
* **Lý do:**
  1. Đồng bộ hoàn hảo với hệ tọa độ OpenGL và Arc Graphics engine.
  2. Triệt tiêu hoàn toàn chi phí ép kiểu `.toFloat()` khi nạp uniform vào GPU Shaders.

### 2. OpenGL Active Texture Enum
* Luôn gọi `Gl.activeTexture(Gl.texture0 + unit)`, tuyệt đối **không** dùng `Gl.texture2d + unit` (gây lỗi OpenGL invalid enum).

### 3. Quy chuẩn Arc Shader Headers & Precisions
* Tuyệt đối không tự khai báo `#ifdef GL_ES` hay `#version` trong các file shader thô (`*.frag`, `*.vert`). Trình biên dịch shader của Arc (`arc.graphics.gl.Shader`) sẽ tự động chèn ngầm; việc khai báo thủ công gây lỗi trùng lặp và crash game khi khởi động.

### 4. Vòng lặp Render Hooks
* Các tác vụ nạp Texture GPU từ hàng đợi, xử lý FrameBuffer và UI tick phải được thực thi trong `Trigger.uiDrawEnd` hoặc `EngineRuntime.draw()`.

### 5. Tiêu chuẩn Typography & BMFont Natural Scaling
* **Quy chuẩn:** Render Bitmap Fonts (`Fonts.def`, `Fonts.tech`, `Fonts.large`) ở tỷ lệ chuẩn **`scale = 1.0f`** (hoặc số nguyên $2.0\times$).
* **Lý do:** BMFont là ảnh lưới pixel cố định. Việc scale số lẻ (`0.8f`, `0.85f`, `0.9f`, `1.1f`) khiến GPU nội suy nét $1\text{px}$ thành $1.5\text{px}$ gây mờ, nhòe và dính chữ.
* **Phân cấp thị giác:** Tạo sự phân cấp bằng **Màu sắc / Độ sáng** (`Color.white` vs `Color.valueOf("9399b2")`) hoặc chuyển đổi bộ font (`Fonts.large` cho tiêu đề, `Fonts.tech` cho số liệu).

---

## 🎮 II. Xử lý Đầu vào & Cử chỉ

### 6. Xử lý Bàn phím & IME Tiếng Việt
* Xử lý phím `Backspace` độc quyền trong `onKeyDown(KeyCode.backspace)`.
* `onKeyTyped` chỉ xử lý các ký tự in được ($\ge 32$) và bỏ qua `\b` để tránh lỗi xoá 2 lần ký tự khi dùng bộ gõ Tiếng Việt (Unikey/EVKey).

### 7. Điều hướng Kéo thả theo Sự kiện (`onPointerDrag`)
* Định tuyến sự kiện kéo chuột liên tục thông qua `onPointerDrag` trong `EngineInputProcessor.touchDragged()`.
* Không thăm dò `Core.input` thủ công trong các hàm vẽ `drawSelf()` để đảm bảo mượt mà và không giật lag.

---

## 🧩 III. Kiến trúc Mã nguồn & Phong cách

### 8. Cấu trúc Package Feature Co-location
* Tổ chức các Widget UI theo gói chuyên biệt (`components.input.textfield`, `components.input.slider`, `components.display.image`,...) gom nhóm Virtual Node, Composable và State Machine cùng một nơi (tối đa 2–4 file/thư mục).

### 9. Triết lý Declarative Post-Design
* Không nhúng inspector overlay nặng vào client game; ưu tiên thiết kế compile-time bằng Kotlin DSL và Reactive Schema (HJSON/JSON) Hot-Reload.

### 10. Thứ tự tham số Composable & UIModifier Chaining
* Thứ tự chuẩn: (1) Dữ liệu/State $\rightarrow$ (2) Callbacks $\rightarrow$ (3) `modifier: UIModifier = UIModifier` $\rightarrow$ (4) Visual Options $\rightarrow$ (5) Content Slot.
* Mọi component áp dụng style mặc định phải kết thúc bằng `.then(modifier)`.
* Xem tài liệu đặc tả đầy đủ: [Quy Chuẩn UI Component](../compose-dsl/ui_component_standards_vi.md).

### 11. Chuẩn hóa Inline Lambda Đơn giản
* Các callback, listener hoặc lambda 1 dòng đơn giản (ví dụ: `AsyncDispatcher.onMainThread { onResult(null, e) }`) phải được viết gọn gàng trên cùng 1 dòng inline.

---

## 💾 IV. Lưu trữ, I/O & Thư viện

### 12. Ghi đĩa Nguyên tử (Atomic Write) & Chống Khoá File Windows
* Debounce ghi đĩa 300ms với `Storage.atomicWrite` sử dụng file tạm thời nano-timestamp (`.nanoTime().tmp`) và khóa đồng bộ `synchronized(lock)` để chống xung đột khóa file NTFS trên Windows.

### 13. Tra cứu Mã nguồn Thư viện Cục bộ (`.lib-source`)
* Luôn đọc mã nguồn trực tiếp trong `.lib-source/` (`mindustry/`, `arc/`, `compose-runtime/`, `MindustryToolMod/`) khi nghiên cứu API hoặc cơ chế OpenGL. Tự động clone vào `.lib-source/<lib-name>` nếu thiếu thư viện mới.

---

## 📖 V. Quy chuẩn Tài liệu & KDoc

### 14. Tài liệu Song ngữ Song hành
* Duy trì song song 2 bản `*_vi.md` và `*_en.md` trong `docs/`. Toàn bộ liên kết liên-tài-liệu bắt buộc dùng đường dẫn tương đối (`./` hoặc `../`).

### 15. KDoc Trong Mã Nguồn Kotlin
* 100% Tiếng Anh chuẩn. Dẫn chiếu tài liệu bằng `See: docs/path/file_en.md` (không dùng `@see` với đường dẫn file).
