# Lộ trình Phát triển Tương lai (Future Development Roadmap)

Tài liệu này vạch ra kế hoạch phát triển theo từng giai đoạn (phased roadmap) cho hệ thống UI Engine và các tính năng mở rộng của NekoMod.

---

## 🎯 Giai đoạn 1: Chuẩn hóa Nền tảng (ĐÃ HOÀN THÀNH ✅)
* [x] Xây dựng Virtual Node DOM (`UINode`, `CanvasNode`).
* [x] Tích hợp Compose Multiplatform Runtime (`CompositionManager`, `NodeApplier`).
* [x] Port thuật toán Container 2-Pass của Godot Engine (`GodotLayout`).
* [x] Hệ thống Box Model toàn diện (Margin & Padding thống nhất trên `UINode`).
* [x] Chuỗi `UIModifier` mạch lạc và Scopes có bảo vệ `@UIDslMarker`.
* [x] Bộ Widget cơ bản (`Button`, `Toggle`, `Card`, `Divider`, `Text`, `Row`, `Column`, `Grid`, `Spacer`).
* [x] Shader SDF bo góc và làm mờ hậu cảnh Glassmorphism (`BoxRenderer`, `BoxBlur`).

---

## 🚀 Giai đoạn 2: Bộ Widget Nâng cao (Widget Toolkit Expansion)
* [ ] **`ScrollView` & Scissor Clipping:**
  * Cuộn trang mượt mà theo trục dọc/ngang.
  * Cắt xén vùng hiển thị bằng OpenGL Scissor (`ScissorStack`) để nội dung dài không tràn ra ngoài.
  * Thanh cuộn (Scrollbar) tùy biến màu sắc và độ dày.
* [ ] **`TextField` / Input Văn bản:**
  * Nhập liệu văn bản bằng bàn phím thực tế.
  * Quản lý con trỏ nhấp nháy (Caret), bôi đen (Selection), Copy/Paste (`Ctrl+C`, `Ctrl+V`).
* [ ] **`Slider` & `ProgressBar`:**
  * Thanh kéo giá trị âm lượng, tỷ lệ, thanh máu / tiến độ xây dựng.
* [ ] **`ImageNode`:**
  * Vẽ icon và texture Mindustry (`TextureRegion`, `AtlasRegion`) với các chế độ Scale (`Fit`, `Crop`, `Stretch`).
* [ ] **`Dropdown` & `Tooltip`:**
  * Menu thả xuống và hộp thoại gợi ý thông tin bám theo vị trí con trỏ chuột.

---

## 🏗️ Giai đoạn 3: Thay thế Giao diện Game Mindustry (Game UI Replacement)
* [ ] **`ModalDialog`:**
  * Hộp thoại popup chặn tương tác màn hình phía sau, có nút đóng và hiệu ứng mờ nền.
* [ ] **Floating World-to-Screen UI:**
  * Menu cấu hình nổi gắn liền với tọa độ của công trình trên bản đồ (như Message Block, Logic Processor, Core status).
  * Tự động chuyển đổi tọa độ thế giới (World Tile $(x, y)$) sang tọa độ màn hình (Screen $(x, y)$) khi người chơi zoom hoặc di chuyển camera.
* [ ] **In-Game Custom HUD:**
  * Thanh trạng thái tài nguyên, minimap tùy biến và bảng điều khiển đơn vị.

---

## 🛠️ Giai đoạn 4: Trình Soạn thảo UI trong Game (In-game UI Inspector & Editor)
* [ ] **UI Inspector (`F12`):**
  * Soi trực tiếp bounding box, margin, padding và cây phân cấp node khi hover chuột trên màn hình game.
* [ ] **Schema Parser (HJSON / JSON UI):**
  * Nạp và tải lại giao diện động từ các file `.hjson` / `.json` mà không cần biên dịch lại mod (Live Hot-Reload).
