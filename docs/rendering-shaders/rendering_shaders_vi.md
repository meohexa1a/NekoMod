# GPU Rendering & Shader Pipeline

Tài liệu này giải thích chi tiết tầng dựng hình đồ họa (Rendering Pipeline) của NekoMod, kiến trúc shader SDF (Signed Distance Field) và các quy chuẩn OpenGL trong hệ sinh thái Mindustry / Arc.

---

## 1. Luồng Render Trực giao (Orthographic Render Pass)

Mọi thao tác vẽ UI diễn ra tại hook sự kiện:
```kotlin
Events.run(Trigger.uiDrawEnd) {
    renderer.render(rootCanvas)
}
```
* **Góc chiếu (Projection):** Sử dụng `Draw.proj(0f, 0f, screenW, screenH)` để vẽ trực tiếp theo tọa độ điểm ảnh (pixel space) 1:1 với màn hình.
* **Top-Layer Compositing:** UI được vẽ sau khi Mindustry hoàn thành toàn bộ frame của game và Scene2D, đảm bảo giao diện luôn hiển thị sắc nét trên cùng.
* **Bảo toàn Ma trận Chiếu FBO:** Khi chuyển đổi Render Target sang FrameBuffer tạm (FBO pass), ma trận chiếu được lưu lại qua `scratchMat` và khôi phục ngay sau khi kết thúc pass để không làm sai lệch không gian vẽ của game.

---

## 2. Kiến trúc Shader Bo góc SDF (`box.frag` & `box.vert`)

Thay vì sử dụng các hình ảnh texture cắt 9 phần (9-patch bitmaps) vốn dễ bị vỡ hạt khi phóng to, `BoxRenderer` sử dụng **Toán học Khoảng cách có Dấu (Signed Distance Field - SDF)** trực tiếp trong Fragment Shader:

```
┌──────────────────────────────────────────────────────────┐
│ BOX SHADER FRAGMENT COMPOSITING PIPELINE                 │
│ 1. Base Fill (Màu đơn sắc / Fill Texture)                 │
│ 2. Backdrop Blur Sampling (Hòa trộn texture làm mờ)      │
│ 3. Outer Glow (Bóng phát quang mở rộng)                  │
│ 4. Inner Shadow (Bóng đổ lòng trong)                     │
│ 5. Border Stroke (Viền liền nét / Viền đứt đoạn Dash)    │
│ 6. Color Filter & Grain Noise                            │
│ 7. Opacity Multiplication & Final Alpha Discard          │
└──────────────────────────────────────────────────────────┘
```

1. **Tính khoảng cách từ pixel tới hình chữ nhật bo góc:**
   $$d = \|\max(|p - \text{center}| - \text{halfSize} + r, 0)\| - r$$
2. **Khử răng cưa mịn màng (Anti-Aliasing):**
   $$\text{fillAlpha} = \text{clamp}(1.0 - \text{smoothstep}(-\text{edge}, \text{edge}, d), 0.0, 1.0)$$
3. **Quy tắc Tránh Early Discard:**
   Shader không bao giờ thực hiện lệnh `discard` sớm khi `fillAlpha == 0`, vì các hiệu ứng viền ngoài (`Border`) và bóng phát quang (`Glow`) nằm ngoài biên giới hình chữ nhật gốc ($d > 0$). Việc kiểm tra `if (color.a < 0.001) discard;` chỉ được thực thi duy nhất một lần ở cuối shader sau khi toàn bộ các lớp đã được hòa trộn.

---

## 3. Quy tắc Biên dịch Arc Shader Compiler

* **Arc Shader Headers:** Arc Graphics engine (`arc.graphics.gl.Shader`) tự động tiêm phần khai báo `#ifdef GL_ES` và `precision mediump float;` ngầm vào mã nguồn fragment shader trước khi gửi sang OpenGL driver.
* **Quy chuẩn bất biến:** Tuyệt đối **không khai báo** `#ifdef GL_ES` hoặc `#version` thủ công trong các file shader (`box.frag`, `blur.frag`, `box.vert`, `blur.vert`).

---

## 4. Chuẩn mực Typography & BMFont Rendering

* **Bitmap Font Invariant:** Toàn bộ font chữ của Mindustry (`Fonts.def`, `Fonts.tech`, `Fonts.large`) là BMFont (ảnh chụp lưới pixel atlas).
* **Quy chuẩn tỷ lệ nguyên:** Luôn render ở tỷ lệ tự nhiên **`scale = 1.0f`** (hoặc số nguyên $2.0\times$). Tránh scale số thập phân lẻ (`0.8f`, `0.85f`, `0.9f`, `1.1f`) gây vỡ hạt pixel và méo nét chữ.
* **Phân cấp thị giác:** Thay vì thu nhỏ font scale, phân cấp bằng độ sáng màu sắc (`Color.white` cho tiêu đề, `Color.valueOf("9399b2")` cho phụ đề) và chọn font chuyên biệt (`Fonts.large` cho tiêu đề to, `Fonts.tech` cho số liệu).
