# GPU Rendering & Shader Pipeline

Tài liệu này giải thích chi tiết tầng dựng hình đồ họa (Rendering Pipeline) của NekoMod, kỹ thuật vẽ hình chữ nhật bo góc bằng shader SDF (Signed Distance Field) và giải thuật làm mờ hậu cảnh **Shared Screen Capture + Parameterized Per-Box Blur**.

---

## 1. Luồng Render Trực giao (Orthographic Render Pass)

Mọi thao tác vẽ UI diễn ra tại hook sự kiện:
```kotlin
Events.run(Trigger.uiDrawEnd) {
    renderer.render(rootCanvas)
}
```
* **Góc chiếu (Projection):** Sử dụng `Draw.proj(0, 0, screenW, screenH)` để vẽ trực tiếp theo tọa độ điểm ảnh (pixel space) 1:1 với màn hình.
* **Top-Layer Compositing:** UI được vẽ sau khi Mindustry hoàn thành toàn bộ frame của game và Scene2D, đảm bảo giao diện luôn hiển thị sắc nét trên cùng.

---

## 2. Shader Bo góc SDF (`box.frag`)

Thay vì sử dụng các hình ảnh texture cắt 9 phần (9-patch bitmaps) vốn dễ bị vỡ hạt khi phóng to, `BoxRenderer` sử dụng **Toán học Khoằng cách có Dấu (Signed Distance Field - SDF)** trực tiếp trong Fragment Shader:

1. **Tính khoảng cách từ pixel tới hình chữ nhật bo góc:**
   $$d = \|\max(|p - \text{center}| - \text{halfSize} + r, 0)\| - r$$
2. **Khử răng cưa (Anti-Aliasing):**
   $$\text{alpha} = \text{smoothstep}(1.0, 0.0, d)$$
3. **Viền và Bo góc riêng biệt:**
   Shader hỗ trợ 4 bán kính góc độc lập (`topLeftRadius`, `topRightRadius`, `bottomRightRadius`, `bottomLeftRadius`) và độ dày viền `borderWidth` mà không làm giảm tốc độ khung hình.

---

## 3. Kiến trúc Làm mờ Hậu cảnh Tối ưu (Shared Screen Capture + Parameterized Blur)

```
┌───────────────────────────────────────────────────────────┐
│ 1. SCREEN CAPTURE (1 LẦN DUY NHẤT ĐẦU FRAME)              │
│    EngineRenderer chụp màn hình game vào Shared FBO       │
└─────────────────────────────┬─────────────────────────────┘
                              │
            ┌─────────────────┴─────────────────┐
            ▼                                   ▼
┌───────────────────────┐           ┌───────────────────────┐
│ Card A (Blur Nhẹ 4px) │           │ Modal Dialog (Mờ Đục) │
│ • blurRadius = 4f     │           │ • blurRadius = 16f    │
│ • weight = 0.6        │           │ • weight = 0.95       │
│ • Tint = Xanh pastel  │           │ • Tint = Đen khói     │
└───────────────────────┘           └───────────────────────┘
```

1. **Chụp màn hình 1 lần duy nhất ($O(1)$):**
   * Đầu mỗi frame vẽ UI, `EngineRenderer` kiểm tra xem trên màn hình có node nào bật `blur = true` hay không. Nếu có, nó chụp màn hình game **đúng 1 lần** vào `screenCaptureFbo` (downscaled 50%).
   * Xóa bỏ hoàn toàn tình trạng nghẽn GPU khi có nhiều hộp thoại cùng làm mờ.
2. **Cặp FrameBuffer tạm dùng chung (Scratch Ping-Pong FBO):**
   * Các `BoxNode` không lưu trữ FBO riêng trong bộ nhớ.
   * Khi vẽ, node mượn cặp `scratchFboA` $\leftrightarrow$ `scratchFboB` của `BoxBlur` để chạy Gaussian blur rồi hoàn trả ngay lập tức.
3. **Tự do tùy biến tham số cho từng Box riêng biệt:**
   * `blurRadius`: Độ nhòe rộng/hẹp của nhân Gaussian (ví dụ: `2f` cho tooltip, `16f` cho modal).
   * `blurIterations`: Số lượt chạy ping-pong (1 đến 4 lần).
   * `backdropTint`: Màu ám kính mờ (ví dụ: màu trắng trong mờ, ám xanh, đen khói).
   * `backdropWeight`: Độ đục / trong suốt của lớp kính.
