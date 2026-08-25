# Các Thách Thức Kỹ Thuật Phức Tạp & Định Hướng Giải Quyết (Complex Challenges)

Tài liệu này ghi nhận và phân tích sâu các bài toán kỹ thuật phức tạp trong quá trình xây dựng UI Engine thế hệ mới cho NekoMod.

---

## 🛑 Thách Thức 1: Cắt xén Viewport Lồng nhau (Nested ScissorStack) khi có Xoay / Transform

### 🔍 Vấn đề Kỹ thuật:
* Lệnh cắt xén hình chữ nhật phần cứng `glScissor(x, y, w, h)` của OpenGL chỉ hoạt động với **hình chữ nhật song song với trục tọa độ màn hình (AABB)**.
* Khi một container (ví dụ `ScrollContainer`) bị xoay một góc $30^\circ$ hoặc áp dụng ma trận biến hình 2D affine transform, `glScissor` không thể cắt theo hình bình hành hoặc hình chữ nhật xoay.

### 💡 Hướng giải quyết:
1. **Sử dụng Stencil Buffer:** Bật `GL_STENCIL_TEST`, vẽ hình dạng cắt xén vào Stencil Mask trước khi vẽ nội dung con.
2. **SDF Shader Clipping:** Truyền trực tiếp ma trận Inverse Transform và góc bo tròn vào SDF Fragment Shader để tự động clip pixel bên ngoài biên giới.

---

## 🛑 Thách Thức 2: Bộ Gõ Tiếng Việt (IME) & Định hình Văn bản trong `TextField`

### 🔍 Vấn đề Kỹ thuật:
* Các bộ gõ tiếng Việt (Telex/VNI trên Unikey/EVKey) hoạt động dựa trên cơ chế gửi ký tự điều khiển xóa (`\b`) kèm ký tự mới đã bỏ dấu.
* Nếu xử lý `\b` trong `onKeyTyped`, nó sẽ xung đột với `onKeyDown(KeyCode.backspace)` gây xóa 2 lần ký tự liên tiếp.

### 💡 Giải pháp đã triển khai:
* Xử lý `Backspace` độc quyền trong `onKeyDown(KeyCode.backspace)`.
* `onKeyTyped` chỉ lọc và nhận các ký tự in được ($\ge 32$) và bỏ qua hoàn toàn `\b`.

---

## 🛑 Thách Thức 3: Điều hướng Kéo chuột Toàn cục & Vòng lặp Layout (Layout Feedback Loop)

### 🔍 Vấn đề Kỹ thuật:
1. **Mất cảm ứng kéo khi chuột rời khỏi Node:** Nếu chỉ lắng nghe chuột bên trong node, khi người dùng kéo nhanh thanh trượt và con trỏ chuột bay ra ngoài biên giới, cử chỉ kéo sẽ bị đứt đoạn.
2. **Vòng lặp dao động bố cục (Layout Feedback Loop):** Khi độ rộng của chữ hiển thị (ví dụ `Power: 99%` sang `100%`) làm thay đổi kích thước container, container bị đẩy sang vị trí mới dưới con trỏ chuột, khiến tỷ lệ chuột bị tính lùi về `99%` $\rightarrow$ sinh ra hiện tượng giật rung qua lại 2 giá trị liên tục.

### 💡 Giải pháp đã triển khai:
1. **Event-Driven Pointer Drag (`onPointerDrag`):** Định tuyến sự kiện `touchDragged` từ `EngineInputProcessor` trực tiếp đến `pressedNode` trên toàn màn hình.
2. **Cố định kích thước nhãn Text:** Quy định `Modifier.width(...)` cố định cho các nhãn giá trị động để triệt tiêu việc co giãn chiều ngang làm xê dịch thanh trượt.

---

## 🛑 Thách Thức 4: Khóa File NTFS trên Hệ điều hành Windows & Ghi đĩa Bất đồng bộ

### 🔍 Vấn đề Kỹ thuật:
* Trên hệ điều hành Windows, khi một tiến trình hoặc luồng đang mở file (Stream/Sink), NTFS sẽ áp dụng cấm truy cập (`process cannot access the file`).
* Khi người dùng kéo slider liên tục (sinh ra 60 mutations/giây), nếu ghi đĩa ngay lập tức sẽ gây nghẽn đĩa và crash ngoại lệ `IOException`.

### 💡 Giải pháp đã triển khai:
1. **Debounce 300ms:** Trì hoãn ghi đĩa 300ms sau lần thao tác cuối cùng trên luồng nền `Dispatchers.IO`.
2. **Atomic Write với Nano-Staging File:** Tạo file tạm riêng biệt có nano-timestamp (`target.nanoTime().tmp`), ghi xong flush hoàn chỉnh rồi mới dùng `atomicMove` đè lên file đích dưới khối đồng bộ `synchronized(lock)`.

---

## 🛑 Thách Thức 5: Ràng buộc Giới hạn Phần cứng Cũ OpenGL 2.0 / GLSL 120 của Mindustry

### 🔍 Vấn đề Kỹ thuật:
* Arc Engine và Mindustry chạy trên nền tảng **OpenGL 2.0 / GL ES 2.0 (GLSL 120 / GLSL ES 100)**:
  1. Không hỗ trợ Multiple Render Targets (MRT).
  2. Giới hạn số lượng Shader Uniform registers.

### 💡 Giải pháp đã triển khai:
1. **Multi-pass Ping-Pong:** Chia nhỏ các hiệu ứng nặng (như Gaussian Blur) thành các pass FBO tuần tự.
2. **Uniform Packing:** Gộp các tham số bo góc thành `vec4 u_cornerRadii` để tiết kiệm tài nguyên GPU.
