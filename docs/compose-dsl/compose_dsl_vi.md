# Hướng dẫn Viết Giao diện Compose DSL (Compose DSL Guide)

Tài liệu này cung cấp hướng dẫn toàn diện về cách sử dụng hệ thống DSL khai báo, chuỗi `UIModifier`, và các Component dựng sẵn để tạo giao diện trong NekoMod.

---

## 1. Cấu trúc một màn hình chuẩn

```kotlin
EngineRuntime.setContent {
    var count by remember { mutableStateOf(0) }

    Card(
        modifier = Modifier.anchor(LayoutPreset.CENTER),
        backgroundColor = Color.valueOf("1e1e2e"),
        borderColor = Color.valueOf("89b4fa"),
        borderWidth = 1.8,
        radius = 12.0,
        padding = 20f
    ) {
        Column(gap = 12f) {
            Text("Tiêu đề", scale = 1.3, color = Color.white)
            Divider(modifier = Modifier.margin(vertical = 4f))
            
            Row(arrangement = Arrangement.spacedBy(8f)) {
                Button("Tăng đếm: $count", colors = ButtonColors.Primary, onClick = { count++ })
                Button("Reset", colors = ButtonColors.Danger, onClick = { count = 0 })
            }
        }
    }
}
```

---

## 2. Hệ thống `UIModifier`

Chuỗi `Modifier` cho phép định hình kích thước, lề, kiểu dáng đồ họa và sự kiện click một cách mạch lạc:

### A. Box Model & Sizing (Kích thước & Lề)
* `Modifier.pad(all = 16f)`: Thụt lề đệm bên trong 4 cạnh.
* `Modifier.pad(horizontal = 16f, vertical = 8f)`: Thụt lề trong theo trục ngang/dọc.
* `Modifier.margin(all = 8f)`: Khoảng cách lề bên ngoài 4 cạnh.
* `Modifier.margin(horizontal = 12f, vertical = 6f)`: Khoảng cách lề ngoài theo trục.
* `Modifier.size(w = 100f, h = 50f)`: Thiết lập chiều rộng & chiều cao mong muốn.
* `Modifier.fixed(w = 100f, h = 100f)`: Khóa cố định kích thước (không co giãn).
* `Modifier.fillMaxWidth()` / `Modifier.fillMaxHeight()`: Giãn đầy đủ chiều rộng/cao.
* `Modifier.weight(1.5f)`: Chiếm không gian thừa theo tỷ lệ trọng số (dùng trong `Row`/`Column`).

### B. Visuals & Styling (Màu sắc, Bo góc, Bóng)
* `Modifier.background(Color.valueOf("1e1e2e"))`: Màu nền hộp.
* `Modifier.radius(12.0)`: Bo tròn 4 góc với bán kính 12px.
* `Modifier.border(width = 2.0, color = Color.white)`: Viền bao quanh hộp.
* `Modifier.shadow(color = Pal.shadow, spread = 4.0, blur = 8.0)`: Đổ bóng mờ bên trong.
* `Modifier.glow(color = Color.royal, spread = 6.0, blur = 12.0)`: Tỏa sáng ra ngoài.
* `Modifier.backdrop(blur = true, weight = 0.8)`: Làm mờ hậu cảnh đằng sau hộp.

### C. Interactivity (Tương tác chuột)
* `Modifier.onClick { ... }`: Xử lý click chuột trái.
* `Modifier.onDoubleClick { ... }`: Xử lý click đúp.
* `Modifier.onHover { isHovered -> ... }`: Lắng nghe trạng thái rê chuột.

---

## 3. Các Component Giao diện Sẵn có

| Component | Mô tả |
| :--- | :--- |
| **`Box`** | Khối container cơ sở với nền, viền và hỗ trợ lồng các node con. |
| **`Row`** | Sắp xếp các phần tử con theo hàng ngang từ Trái qua Phải. |
| **`Column`** | Sắp xếp các phần tử con theo hàng dọc từ Trên xuống Dưới. |
| **`Grid`** | Bảng lưới đa cột đa dòng với khoảng cách ô tùy chỉnh. |
| **`Spacer`** | Khoảng trống cố định hoặc co giãn linh hoạt (`weight`). |
| **`Text`** | Hiển thị văn bản BMFont, tự đổi ngôn ngữ bundle (`$key`), tự ngắt dòng. |
| **`Button`** | Nút bấm có hiệu ứng màu hover/pressed tự động và màu chủ đề (`Primary`, `Danger`, `Success`). |
| **`Toggle`** | Nút gạt bật/tắt nhị phân (Switch). |
| **`Card`** | Thẻ container bo góc, có viền nét và khoảng đệm định sẵn. |
| **`Divider`** | Đường kẻ phân cách mỏng. |
