# Hướng dẫn Viết Giao diện Compose DSL (Compose DSL Guide)

Tài liệu này cung cấp hướng dẫn toàn diện về cách sử dụng hệ thống DSL khai báo, scoped modifiers (`RowScope`, `ColumnScope`, `BoxScope`), chuỗi `UIModifier`, và các Component dựng sẵn để tạo giao diện trong NekoMod.

---

## 1. Cấu trúc một Màn hình Chuẩn

```kotlin
EngineRuntime.setContent {
    var count by remember { mutableStateOf(0) }
    var textInput by remember { mutableStateOf("") }
    var showModal by remember { mutableStateOf(false) }

    Card(
        modifier = Modifier.anchor(LayoutPreset.CENTER),
        color = CardDefaults.color,
        borderColor = CardDefaults.borderColor,
        radius = CardDefaults.radius,
        isGlass = true
    ) {
        Column(gap = 12.0f) {
            Text(
                text = "NekoMod Dashboard",
                color = ThemeTokens.textPrimary
            )
            Text(
                text = "Declarative Pure KMP Compose Engine",
                color = ThemeTokens.textSecondary
            )

            TextField(
                value = textInput,
                onValueChange = { textInput = it },
                placeholder = "Nhập văn bản...",
                modifier = Modifier.fillMaxWidth()
            )

            Row(gap = 8.0f) {
                Button(
                    text = "Đếm: $count",
                    onClick = { count++ },
                    variant = ButtonVariant.FILLED,
                    modifier = Modifier.weight(1.0f)
                )
                Button(
                    text = "Reset",
                    onClick = { count = 0 },
                    variant = ButtonVariant.OUTLINED,
                    modifier = Modifier.weight(1.0f)
                )
            }
        }
    }

    ModalDialog(
        visible = showModal,
        onDismiss = { showModal = false }
    ) {
        Column(gap = 16.0f) {
            Text("Nội dung Hộp thoại Modal")
            Button("Đóng", onClick = { showModal = false })
        }
    }
}
```

---

## 2. Scoped Layout Modifiers (Phạm vi Modifier Phân tầng)

NekoMod tuân thủ nghiêm ngặt quy chuẩn phạm vi modifier của Jetpack/JetBrains Compose nhằm triệt tiêu lỗi biến dạng trục chéo:

### A. `RowScope` (Container Ngang)
* `Modifier.weight(weight: Float)`: Phân bổ không gian thừa theo trục ngang theo tỷ lệ, hoàn toàn không làm biến dạng trục dọc.
* `Modifier.align(alignment: VerticalAlign)`: Căn chỉnh trục dọc nội bộ (`VerticalAlign.Top`, `VerticalAlign.Center`, `VerticalAlign.Bottom`, `VerticalAlign.Fill`).

### B. `ColumnScope` (Container Dọc)
* `Modifier.weight(weight: Float)`: Phân bổ không gian thừa theo trục dọc theo tỷ lệ, hoàn toàn không làm biến dạng trục ngang.
* `Modifier.align(alignment: HorizontalAlign)`: Căn chỉnh trục ngang nội bộ (`HorizontalAlign.Left`, `HorizontalAlign.Center`, `HorizontalAlign.Right`, `HorizontalAlign.Fill`).

### C. `BoxScope` (Tọa độ 2D & Lớp phủ)
* `Modifier.align(alignment: Alignment)`: Căn chỉnh vị trí 2 chiều trong Box (`Alignment.Center`, `Alignment.TopStart`, `Alignment.BottomEnd`, v.v.).

---

## 3. Các Phần tử `UIModifier` Chung

### A. Box Model & Sizing (Kích thước & Lề)
* `Modifier.pad(all = 16.0f)` / `Modifier.pad(horizontal = 16.0f, vertical = 8.0f)`: Thụt lề đệm bên trong.
* `Modifier.margin(all = 8.0f)`: Khoảng cách lề bên ngoài.
* `Modifier.size(width = 100.0f, height = 50.0f)`: Thiết lập kích thước cố định.
* `Modifier.minWidth(100.0f)` / `Modifier.minHeight(40.0f)`: Kích thước tối thiểu bảo vệ.
* `Modifier.fillMaxWidth()` / `Modifier.fillMaxHeight()` / `Modifier.fillMaxSize()`: Giãn nở toàn bộ khung chứa.
* `Modifier.autoWidth()` / `Modifier.autoHeight()` / `Modifier.hugContent()`: Co giãn theo nội dung (Intrinsic Auto-Layout).

### B. Visuals & Styling (1-Draw-Call UIBatch)
* `Modifier.background(color: Color)`: Màu nền đặc hoặc trong suốt.
* `Modifier.radius(radius: Float)`: Bo tròn 4 góc với bán kính SDF pixels.
* `Modifier.border(width: Float, color: Color)`: Viền bao quanh hộp.
* `Modifier.glass(enabled: Boolean = true)`: Hiệu ứng kính mờ Dual-Kawase lấy mẫu nền.
* `Modifier.shadow(radius: Float, color: Color, offsetX: Float, offsetY: Float)`: Đổ bóng mềm SDF.

### C. Interactivity (Tương tác & Sự kiện)
* `Modifier.clickable(onClick = { ... })`: Bắt sự kiện click chuột khi nhả nút.
* `Modifier.hoverable { isHovered -> ... }`: Lắng nghe trạng thái rê chuột vào/ra.
* `Modifier.draggable(onDrag = { dx, dy -> ... })`: Theo dõi cử chỉ kéo rê liên tục.
* `Modifier.consumePointer()`: Chặn và hấp thụ sự kiện pointer để không rò rỉ xuống lớp màn che mờ (scrim).
* `Modifier.cursor(cursor: CursorIcon)`: Thiết lập con trỏ chuột khi hover.

---

## 4. Thư viện Component Dựng sẵn

| Component | Phạm vi / Trách nhiệm | Tham số Nổi bật |
| :--- | :--- | :--- |
| `Card()` | Khung chứa kính mờ bo góc SDF | `modifier`, `color`, `radius`, `borderWidth`, `borderColor`, `isGlass`, `content: BoxScope` |
| `Button()` | Nút bấm tương tác với trạng thái Hover/Press | `onClick`, `modifier`, `variant`, `enabled`, `contentPadding`, `content: BoxScope` |
| `IconButton()` | Nút bấm nhỏ gọn chuyên chứa icon | `region`, `onClick`, `modifier`, `variant`, `tint`, `enabled` |
| `TextField()` | Ô nhập liệu văn bản hỗ trợ IME & trạng thái kiểm soát | `value`, `onValueChange`, `placeholder`, `enabled`, `isMultiline`, `font` |
| `Text()` | Nhãn chữ BMFont chuẩn xác ở natural scale | `text`, `modifier`, `color`, `font`, `align`, `wrap`, `ellipsis` |
| `Image()` | Hiển thị vùng texture đồ họa | `region`, `modifier`, `tint`, `aspectRatio` |
| `ModalDialog()` | Lớp phủ modal toàn màn hình với scrim mờ | `visible`, `onDismiss`, `modifier`, `scrimColor`, `content: BoxScope` |
| `TooltipBox()` | Khung gợi ý xuất hiện khi hover với delay | `tooltip: BoxScope`, `modifier`, `delayMs`, `content: BoxScope` |
