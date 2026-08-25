# Hướng dẫn Viết Giao diện Compose DSL (Compose DSL Guide)

Tài liệu này cung cấp hướng dẫn toàn diện về cách sử dụng hệ thống DSL khai báo, chuỗi `UIModifier`, và các Component dựng sẵn để tạo giao diện trong NekoMod.

---

## 1. Cấu trúc một Màn hình Chuẩn

```kotlin
EngineRuntime.setContent {
    var count by remember { mutableStateOf(0) }
    var toggled by remember { mutableStateOf(true) }
    var sliderVal by remember { mutableStateOf(0.75f) }

    Card(
        modifier = Modifier.anchor(LayoutPreset.CENTER),
        backgroundColor = Color.valueOf("1e2030").a(0.85f),
        borderColor = Color.valueOf("363a4f").a(0.8f),
        radius = 12f,
        padding = 18f
    ) {
        Column(gap = 10f) {
            Text(text = "NekoMod Dashboard", scale = 1.0f, color = Color.white)
            Text(text = "Declarative Pure KMP Engine", scale = 1.0f, color = Color.valueOf("9399b2"))
            Divider(modifier = Modifier.margin(vertical = 2f))
            
            // Modern Capsule Pill Slider with Integrated Title & Value
            Slider(
                value = sliderVal,
                onValueChange = { sliderVal = it },
                label = "Power Level",
                modifier = Modifier.fillMaxWidth().height(30f)
            )

            Row(arrangement = Arrangement.spacedBy(10f)) {
                Button(
                    text = "Đếm: $count",
                    colors = ButtonColors.Primary,
                    onClick = { count++ }
                )
                Button(
                    text = "Reset",
                    colors = ButtonColors.Danger,
                    onClick = { count = 0 }
                )
                Toggle(
                    checked = toggled,
                    onToggle = { toggled = it }
                )
            }
        }
    }
}
```

---

## 2. Hệ thống `UIModifier`

Chuỗi `Modifier` cho phép định hình kích thước, lề, kiểu dáng đồ họa và sự kiện click:

### A. Box Model & Sizing (Kích thước & Lề)
* `Modifier.pad(all = 16f)`: Thụt lề đệm bên trong 4 cạnh.
* `Modifier.pad(horizontal = 16f, vertical = 8f)`: Thụt lề trong theo trục ngang/dọc.
* `Modifier.margin(all = 8f)`: Khoảng cách lề bên ngoài 4 cạnh.
* `Modifier.size(w = 100f, h = 50f)`: Thiết lập chiều rộng & chiều cao mong muốn.
* `Modifier.width(120f)` / `Modifier.height(32f)`: Thiết lập chiều rộng / chiều cao độc lập.
* `Modifier.fillMaxWidth()` / `Modifier.fillMaxHeight()`: Giãn đầy đủ chiều rộng/cao của container cha.
* `Modifier.weight(1.5f)`: Chiếm không gian thừa theo tỷ lệ trọng số (dùng trong `Row`/`Column`).

### B. Visuals & Styling (Màu sắc, Bo góc, Bóng SDF)
* `Modifier.background(Color.valueOf("1e2030"))`: Màu nền hộp.
* `Modifier.cornerRadius(12f)`: Bo tròn 4 góc với bán kính `Float`.
* `Modifier.border(width = 1f, color = Color.white)`: Viền bao quanh hộp.
* `Modifier.shadow(color = Color.black.a(0.4f), blur = 16f, spread = 2f)`: Đổ bóng mềm SDF.
* `Modifier.glow(color = Color.valueOf("2563eb").a(0.4f), spread = 4f, blur = 8f)`: Tỏa sáng ra ngoài.

### C. Interactivity (Tương tác & Sự kiện)
* `Modifier.clickable { ... }`: Bắt sự kiện click chuột.
* `Modifier.hoverable { isHovered -> ... }`: Lắng nghe trạng thái rê chuột.
* `Modifier.tooltip("Mô tả nút")`: Tự động hiển thị khung gợi ý khi rê chuột.

---

## 3. Thư viện Component Dựng sẵn

| Component | Trách nhiệm | Tham số Nổi bật |
| :--- | :--- | :--- |
| `Card()` | Khung chứa kính mờ bo góc SDF | `backgroundColor`, `borderColor`, `radius`, `padding` |
| `Slider()` | Thanh trượt dạng viên nang (Capsule Pill) | `value`, `label`, `valueText`, `valueRange`, `colors` |
| `Toggle()` | Công tắc viên thuốc với nút trượt 3D | `checked`, `onToggle`, `activeColor`, `inactiveColor` |
| `TextField()` | Ô nhập liệu văn bản với Telex & Focus Glow | `value`, `onValueChange`, `placeholder` |
| `Button()` | Nút bấm tương tác với hiệu ứng Hover/Press | `text`, `onClick`, `colors = ButtonColors.Primary` |
| `ProgressBar()` | Thanh tiến độ bo góc | `progress`, `barHeight`, `colors` |
| `Text()` | Nhãn chữ BMFont chuẩn xác $1.0\times$ | `text`, `color`, `font = Fonts.def`, `scale = 1.0f` |
| `Image()` | Hiển thị ảnh mạng/asset với bộ nhớ đệm LRU | `source = "url / asset / region"`, `scaleType` |
