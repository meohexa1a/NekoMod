# Lộ trình Phát triển Tương lai (Future Development Roadmap)

Tài liệu này vạch ra kế hoạch phát triển theo từng giai đoạn (phased roadmap) cho hệ thống UI Engine và các tính năng mở rộng của NekoMod.

---

## 🎯 Giai đoạn 1: Chuẩn hóa Nền tảng (ĐÃ HOÀN THÀNH ✅)
* [x] Xây dựng Virtual Node DOM (`UINode`, `CanvasNode`, `LayoutNode`).
* [x] Tích hợp Compose Multiplatform Runtime (`CompositionManager`, `NodeApplier`).
* [x] Chuẩn hóa kiến trúc `LayoutNode + MeasurePolicy` (Zero-Overhead DOM).
* [x] Port thuật toán Container 2-Pass của Godot Engine (`GodotLayout`).
* [x] Hệ thống Box Model toàn diện (Margin & Padding thống nhất trên `UINode`).
* [x] Chuỗi `UIModifier` mạch lạc và Scopes có bảo vệ `@UIDslMarker`.
* [x] Bộ Widget cơ bản (`Button`, `Toggle`, `Card`, `Divider`, `Text`, `Row`, `Column`, `Grid`, `Spacer`).
* [x] Shader SDF bo góc và làm mờ hậu cảnh Glassmorphism (`BoxRenderer`, `BoxBlur`).
* [x] Hệ thống Hạ tầng Cốt lõi (`AsyncDispatcher`, `HttpEngine`, `Storage`, `KVStore`, `LRUTextureCache`, `ImageLoader`, `I18nEngine`).

---

## 🚀 Giai đoạn 2: Bộ Widget Nâng cao (ĐÃ HOÀN THÀNH ✅)
* [x] **`ScrollView` & Scissor Clipping:**
  * Cuộn trang mượt mà theo trục dọc/ngang với con lăn chuột và kéo chuột.
  * Cắt xén phần cứng OpenGL Scissor (`ScissorStack`) đa tầng.
  * Thanh cuộn (Scrollbar track & thumb) hiện đại, thanh mảnh.
* [x] **`TextField` / Input Văn bản:**
  * Bộ máy trạng thái nhập liệu độc lập (`TextEditState`).
  * Quản lý con trỏ nhấp nháy, bôi đen (Selection), Copy/Paste (`Ctrl+C`, `Ctrl+V`, `Ctrl+X`, `Ctrl+A`).
  * Tương thích 100% với bộ gõ tiếng Việt Telex (Unikey, EVKey).
* [x] **`Image` & `ImageNode`:**
  * Hỗ trợ đa nguồn ảnh: URL Internet (OkHttp), Sprite Atlas Mindustry, và file cục bộ.
  * Tự động điều tiết băng thông VRAM với hàng đợi Frame Budget.
* [x] **`Slider` (Capsule Pill Style) & `ProgressBar`:**
  * Thanh trượt dạng viên nang tích hợp tiêu đề và phần trăm với kéo thả toàn màn hình (`onPointerDrag`).
  * Thanh tiến độ bo góc mượt mà với nhiều bộ màu trạng thái.
* [x] **`Tooltip` & `Toggle`:**
  * Khung gợi ý tooltip tự động hiển thị khi rê chuột.
  * Nút gạt công tắc dạng viên thuốc với nút trượt 3D và viền sáng phát quang.

---

## 🏗️ Giai đoạn 3: Thay thế Giao diện Game Mindustry (Game UI Replacement - ĐANG TRIỂN KHAI 🔄)
* [ ] **`ModalDialog`:**
  * Hộp thoại popup chặn tương tác màn hình phía sau, có nút đóng và hiệu ứng mờ nền.
* [ ] **Floating World-to-Screen UI:**
  * Menu cấu hình nổi gắn liền với tọa độ của công trình trên bản đồ (như Message Block, Logic Processor, Core status).
  * Tự động chuyển đổi tọa độ thế giới (World Tile $(x, y)$) sang tọa độ màn hình (Screen $(x, y)$) khi người chơi zoom hoặc di chuyển camera.
* [ ] **In-Game Custom HUD:**
  * Thanh trạng thái tài nguyên, minimap tùy biến và bảng điều khiển đơn vị.

---

## 🛠️ Giai đoạn 4: Kiến trúc Schema & Hot-Reload (Data-Driven Declarative UI)
* [ ] **Schema Parser (HJSON / JSON UI):**
  * Nạp và tải lại giao diện động từ các file `.hjson` / `.json` mà không cần biên dịch lại mod (Live Hot-Reload).
* [ ] **2-Way Data-Binding Bridge:**
  * Liên kết tự động giữa Schema JSON và biến trạng thái trong `KVStore`.
