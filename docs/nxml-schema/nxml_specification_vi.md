# Bản Đặc Tả NXML Schema & Giao Diện Khai Báo (NXML Specification)

---

## 🧭 I. Triết Lý Cốt Lõi & Nền Tảng Kiến Trúc

**NXML (Neko XML/Markup)** là một định dạng lược đồ giao diện khai báo hiệu năng cao, được thiết kế để thay thế khoảng ~95% code giao diện Kotlin trong NekoMod. Định dạng này hỗ trợ Hot-reload trong $<30\text{ms}$, xóa bỏ hoàn toàn thời gian chờ biên dịch và phân tách sạch sẽ giữa tầng bố cục giao diện và logic game.

### 🏛️ 1. Kiến Trúc 1 File Duy Nhất (App Bundle)
* Thay vì phân mảnh thành hàng chục file `.nxml` riêng lẻ gây áp lực đọc đĩa I/O liên tục, toàn bộ giao diện ứng dụng và các Scene được đóng gói vào một file duy nhất (`assets/ui/app.nxml`).
* Phân tích (parse) đúng 1 lần vào RAM khi khởi động, chạy với độ trễ $0\text{ms}$ và đảm bảo độ ổn định $100\%$.

### 🧱 2. Giới Hạn 5 Thẻ Nguyên Bản (Primitive Nodes Invariant)
Theo Quy tắc 27 (Framework Idiomaticity), bộ phân tích cú pháp lõi của Engine **CHỈ nhận diện đúng 5 thẻ nguyên bản**:
1. **`<Box>`**: Thẻ hình chữ nhật GPU SDF đa năng (bo góc, màu nền, kính mờ Frosted Glass, viền nổi, đổ bóng, phát sáng, padding, margin, anchor, kích thước, clipping, bắt click).
2. **`<Text>`**: Thẻ hiển thị văn bản BMFont tỉ lệ chuẩn 1.0x (tự động xuống dòng, căn lề, màu sắc tokens).
3. **`<Row>`**: Thùng chứa xếp ngang Flex Row (khoảng cách gap, phân bổ flex weight, căn trục phụ).
4. **`<Column>`**: Thùng chứa xếp dọc Flex Column (khoảng cách gap, phân bổ flex weight, căn trục phụ).
5. **`<Image>`**: Thẻ vẽ Texture & Sprite Atlas của Mindustry (`ScaleMode.FIT`, `CROP`, `STRETCH`).

Tất cả các component bậc cao (`<Card>`, `<Button>`, `<Toggle>`, `<Slider>`, `<SegmentedControl>`) **KHÔNG** bị đúc chết trong Kotlin; chúng được định nghĩa tái sử dụng dưới dạng **`<template>`** cấu thành từ 5 thẻ nguyên bản này.

---

## 🏷️ II. Quy Chuẩn Danh Mục Thẻ & Cấu Trúc Tổng Thể

### 1. Khung Tài Liệu Gốc & Các Khối Toàn Cục

```xml
<nxml app="NekoMod" version="1.0"
      xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
      xsi:noNamespaceSchemaLocation="nxml.xsd">

    <!-- I. Biến Trạng Thái Phản Ứng (State) -->
    <state>
        <var name="selectedTab" type="string" default="graphics" />
        <var name="musicVol"    type="float"  default="1.0" />
        <var name="sfxVol"      type="float"  default="1.0" />
        <var name="particles"   type="bool"   default="true" />
        <var name="serverList"  type="list"   default="[]" />
    </state>

    <!-- II. Đa Ngôn Ngữ & Nội Suy Chuỗi (i18n) -->
    <i18n>
        <locale id="en">
            <string id="welcome">Welcome, {player}!</string>
            <string id="play">Play Campaign</string>
        </locale>
        <locale id="vi">
            <string id="welcome">Chào mừng, {player}!</string>
            <string id="play">Chơi Chiến Dịch</string>
        </locale>
    </i18n>

    <!-- III. Thẻ Gọi Mạng Cô Lập An Toàn (HTTP Pipeline) -->
    <http id="fetchReleases"
          url="https://api.github.com/repos/Anuken/Mindustry/releases"
          method="GET"
          auto="true"
          cache="10m"
          target="@serverList" />

    <!-- IV. Mẫu Component Tái Sử Dụng (Templates) -->
    <template id="Card">
        <Box radius="16" background="$glassRegular" border="1" borderColor="$borderSubtle" padding="16">
            <slot />
        </Box>
    </template>

    <!-- V. Các Màn Hình Độc Lập (Scenes) -->
    <scene id="main_menu">
        ...
    </scene>

</nxml>
```

---

## ⚛️ III. Quy Chuẩn Văn Bản & Biểu Thức Phong Cách React

Tuân thủ mô hình React / JSX hiện đại:
* **Văn bản thuần ngắn (Literal):** Chỉ dùng cho các hằng số đơn giản (`<Text text="OK" />`).
* **Văn bản dài & Chuỗi động:** Bắt buộc nằm trong `<i18n>` hoặc `@state` và được gọi qua `{...}` hoặc `$t(...)`.

### Cú Pháp & Tiền Tố Chuẩn:
1. **`@variable`**: Ràng buộc trạng thái 2 chiều (ví dụ: `value="@sfxVol"`).
2. **`$colorToken`**: Token màu và giao diện hệ thống (ví dụ: `$accent`, `$textPrimary`, `$surfaceElevated`).
3. **`$t(id, key=value)`**: Bản dịch đa ngôn ngữ có truyền tham số (ví dụ: `$t(welcome, player=@username)`).
4. **`onClick="action"`**: Chuỗi hành động khai báo:
   * `nav:sceneId`: Chuyển sang màn hình khác.
   * `nav:back`: Quay lại màn hình trước trong stack.
   * `set:var=val`: Gán giá trị trực tiếp cho biến State.
   * `http:requestId`: Kích hoạt request mạng khai báo.
   * `sound:soundId`: Phát âm thanh game.

---

## 🚧 IV. Các Quyết Định Kiến Trúc Đang Mở (Chưa Chốt Hoàn Toàn)

Các chủ đề sau đây được ghi nhận để tiếp tục thử nghiệm và đánh giá thực tế:

| # | Chủ Đề | Phương Án A | Phương Án B | Đề Xuất Hiện Tại |
| :--- | :--- | :--- | :--- | :--- |
| **1** | **Bộ Đánh Giá Biểu Thức (Evaluator)** | **Janino kết hợp AST Whitelist**<br>- Tốc độ Bytecode gốc Java<br>- Đầy đủ hàm `Math.*`<br>- Cần bộ lọc an toàn nghiêm ngặt | **Tiny Pure Kotlin Pratt Parser**<br>- ~150 dòng code, 0 dependency<br>- Sandbox an toàn $100\%$<br>- Chỉ xử lý phép tính cơ bản `+ - * / ? :` | Khởi đầu bằng **Tiny Pure Kotlin Parser** để đảm bảo độ an toàn tuyệt đối, nâng cấp lên Sandboxed Janino nếu phát sinh nhu cầu phức tạp. |
| **2** | **Cơ Chế Bắt File Hot-Reload** | **Java NIO WatchService**<br>- Bắt sự kiện trực tiếp từ OS<br>- Phản hồi cực nhanh $<30\text{ms}$ | **Polling Dựa Trên Timestamp**<br>- Quét `file.lastModified()` mỗi 500ms<br>- Tương thích tuyệt đối mọi nền tảng | Dùng **NIO WatchService** trên Desktop và fallback sang timestamp trên Mobile/Android. |
| **3** | **Phân Phối Thư Viện Mẫu (`std_components.nxml`)** | **Nhúng Sẵn Trong Classpath Mod JAR**<br>- Tự động nạp khi khởi động<br>- Cho phép mod khác override template | **Viết Trực Tiếp Trong `app.nxml`**<br>- Gom tất cả trong 1 file<br>- Pipeline phân tích đơn giản | **Nhúng Trong Classpath** và hỗ trợ nạp đè template từ thư mục ngoài. |
| **4** | **Công Cụ Hỗ Trợ IDE** | **File `nxml.xsd` Tĩnh**<br>- Đặt ngay thư mục gốc project<br>- Hoạt động với IntelliJ & VS Code ngay lập tức | **Viết Plugin IntelliJ Riêng**<br>- Tô màu cú pháp chuyên dụng<br>- Nhúng cửa sổ xem trước trực tiếp | Cung cấp **File `nxml.xsd`** trước để có ngay Auto-complete và Hover Docs không tốn công sức. |

---

## 🔗 Tài Liệu Liên Quan
* [Kiến Trúc Tổng Quan (VI)](../architecture/architecture_vi.md)
* [Quy Chuẩn Thiết Kế UI (VI)](../coding-standards/coding_standards_vi.md)
* [Hệ Thống Render Shaders & Blur (VI)](../rendering-shaders/rendering_shaders_vi.md)
