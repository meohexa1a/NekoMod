# Các Thách Thức Kỹ Thuật Phức Tạp & Định Hướng Tương Lai (Complex Challenges)

Tài liệu này ghi nhận và phân tích sâu các bài toán kỹ thuật phức tạp trong quá trình xây dựng UI Engine thế hệ mới cho NekoMod. Đây là những hạng mục đòi hỏi nghiên cứu chuyên sâu, không nên triển khai vội vã để tránh phá vỡ tính ổn định của hệ thống.

---

## 🛑 Thách Thức 1: Cắt xén Viewport Lồng nhau (Nested ScissorStack) khi có Xoay / Transform

### 🔍 Vấn đề Kỹ thuật:
* Lệnh cắt xén hình chữ nhật phần cứng `glScissor(x, y, w, h)` của OpenGL chỉ hoạt động với **hình chữ nhật song song với trục tọa độ màn hình (AABB)**.
* Khi một container (ví dụ `ScrollContainer`) bị xoay một góc $30^\circ$ hoặc áp dụng ma trận biến hình 2D affine transform, `glScissor` không thể cắt theo hình bình hành hoặc hình chữ nhật xoay.

### 💡 Hướng giải quyết tương lai:
1. **Sử dụng Stencil Buffer:** Bật `GL_STENCIL_TEST`, vẽ hình dạng cắt xén vào Stencil Mask trước khi vẽ nội dung con.
2. **SDF Shader Clipping:** Truyền trực tiếp ma trận Inverse Transform và góc bo tròn vào SDF Fragment Shader để tự động clip pixel bên ngoài biên giới.

---

## 🛑 Thách Thức 2: Bộ Gõ Tiếng Việt (IME) & Định hình Văn bản Phức tạp trong `TextField`

### 🔍 Vấn đề Kỹ thuật:
* Các bộ gõ tiếng Việt (Telex/VNI trên Unikey/EVKey) và bàn phím tiếng Trung/Nhật/Hàn (CJK) hoạt động dựa trên cơ chế **Composition String (chuỗi ký tự đang gõ dở trước khi kết thúc bằng phím cách hoặc ký tự khác)**.
* Mindustry và Arc Engine xử lý bàn phím ở tầng thấp (`InputProcessor.keyDown` / `keyTyped`) trực tiếp bỏ qua các sự kiện IME Window của hệ điều hành, dẫn đến mất dấu tiếng Việt hoặc gõ bị nhân đôi ký tự.
* BMFont mặc định của game không có thông tin Kerning nâng cao và không tích hợp HarfBuzz/FreeType để render emoji màu hay ký tự Ả Rập/Thái Lan.

### 💡 Hướng giải quyết tương lai:
1. Tạo một cửa sổ/lớp đón sự kiện IME trung gian từ GLFW (`glfwSetCharModsCallback`).
2. Tách biệt chuỗi ký tự đang soạn thảo (Composing text) và chuỗi đã hoàn tất (Committed text) trong `TextFieldNode`.

---

## 🛑 Thách Thức 3: Dynamic Data-Binding 2 chiều giữa HJSON Schema và Live Kotlin Memory

### 🔍 Vấn đề Kỹ thuật:
* UI Editor trong game xuất ra định dạng JSON/HJSON động (Dynamic Tree).
* Trong khi đó, Compose Multiplatform dựa trên cây tĩnh được biên dịch trước (Ahead-of-Time Bytecode Transformation của Compose Compiler Plugin) với các `mutableStateOf` kiểu tĩnh.
* Làm thế nào để kéo thả một slider trong UI Editor và tự động gán giá trị vào một biến Kotlin mà không cần dùng Reflection chậm chạp trên Android?

### 💡 Hướng giải quyết tương lai:
1. **Dynamic Expression Evaluator (Janino / Lightweight AST):** Biên dịch biểu thức `binding: "player.health * 100"` thành bytecode hoặc AST runtime.
2. **Dictionary Key-Path Observable:** Mô hình hóa biến runtime dưới dạng bảng băm quan sát được: `RuntimeState.observe("player.health")`.

---

## 🛑 Thách Thức 4: Ràng buộc Giới hạn Phần cứng Cũ OpenGL 2.0 / GLSL 120 của Mindustry

### 🔍 Vấn đề Kỹ thuật:
* Do Arc Engine và Mindustry ép toàn bộ hệ thống chạy trên nền tảng **OpenGL 2.0 / GL ES 2.0 (GLSL 120 / GLSL ES 100)** để tương thích với mọi dòng máy PC và Android đời cũ:
  1. **Không hỗ trợ Multiple Render Targets (MRT):** Không thể xuất nhiều layer (Albedo, Normals, Blur Mask) ra cùng một FBO trong 1 pass duy nhất.
  2. **Giới hạn số lượng Shader Uniforms:** Các dòng GPU cũ chỉ hỗ trợ tối đa 128 ~ 256 `vec4` uniform registers, hạn chế khả năng truyền mảng dữ liệu lớn vào shader.
  3. **Hạn chế Texture Non-Power-Of-Two (NPOT):** Ở chế độ OpenGL 2.0 cũ, texture có kích thước không phải lũy thừa của 2 không hỗ trợ chế độ lặp `GL_REPEAT` (chỉ hỗ trợ `GL_CLAMP_TO_EDGE`).

### 💡 Hướng giải quyết tương lai:
1. **Kỹ thuật Multi-pass Ping-Pong:** Chia nhỏ các hiệu ứng nặng (như Blur + SDF) thành các pass FBO tuần tự thay vì dồn vào 1 pass MRT.
2. **Tối ưu Uniform Packing:** Đóng gói các cờ boolean và float nhỏ vào các vector gộp (ví dụ `vec4 u_cornerRadii`, `vec4 u_params`) để tiết kiệm register phần cứng.

---

## 🛑 Thách Thức 5: Điều Tiết Băng Thông VRAM khi Tải Đồng Loạt Hàng Trăm Ảnh Mạng (Burst Upload Throttling)

### 🔍 Vấn đề Kỹ thuật:
* Khi cuộn nhanh danh sách Mod / Server Browser, hàng trăm yêu cầu HTTP tải icon cùng lúc.
* Mặc dù việc giải nén `Pixmap` diễn ra trên Background Thread, nhưng lệnh `Texture(pixmap)` và `glTexImage2D` bắt buộc phải chạy trên **OpenGL Render Thread**.
* Nếu nạp quá nhiều texture vào GPU trong một frame (ví dụ 50 ảnh/frame), lệnh upload GPU sẽ làm nghẽn luồng render và gây rớt FPS game nghiêm trọng.

### 💡 Hướng giải quyết tương lai:
1. **Frame Upload Budget Queue:** Giới hạn mỗi frame chỉ được upload tối đa $2 \sim 3$ texture mới lên GPU.
2. **Texture Atlas Tự Động (Texture Packer Runtime):** Ghép các icon nhỏ ($32\times 32$) vào chung 1 Atlas lớn ($1024\times 1024$) thay vì tạo riêng hàng trăm OpenGL Texture riêng lẻ.
