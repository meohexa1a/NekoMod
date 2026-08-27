# Quy Chuẩn Kiến Trúc & Lập Trình UI Component

Tài liệu này thiết lập các tiêu chuẩn bắt buộc, mẫu thiết kế kiến trúc, quy ước tham số, ràng buộc kích thước (Sizing Constraints) và mô hình tương tác cho toàn bộ declarative UI component trong **NekoMod**.

---

## 🏛️ I. Phân Cấp & Danh Mục Component

Các UI component trong NekoMod được tổ chức thành 4 tầng kiến trúc rõ ràng:

```
┌────────────────────────────────────────────────────────────────────────┐
│                        4. Feedback & Dialogs                           │
│              (ModalDialog, Tooltip, Badge, ProgressBar)                │
├────────────────────────────────────────────────────────────────────────┤
│                       3. Surfaces & Containers                         │
│             (Card, GroupedList, ListItem, Divider, ScrollView)         │
├────────────────────────────────────────────────────────────────────────┤
│                         2. Controls & Inputs                           │
│        (Button, TextButton, IconButton, Slider, Toggle, TextField)     │
├────────────────────────────────────────────────────────────────────────┤
│                         1. Layout Primitives                           │
│           (Box, Row, Column, Grid, Spacer, Canvas, Text, Image)        │
└────────────────────────────────────────────────────────────────────────┘
```

1. **Layout Primitives (`org.mdt.ui.components.layout`, `text`, `display`):**
   Các khối xây dựng cơ bản ánh xạ trực tiếp đến cây ảo `UINode` với chi phí styling bằng 0.
2. **Controls & Inputs (`org.mdt.ui.components.input`, `surface`):**
   Các điều khiển form tương tác, nút bấm, thanh trượt, và ô nhập liệu quản lý cử chỉ, tiêu điểm (focus) và phản hồi trạng thái.
3. **Surfaces & Containers (`org.mdt.ui.components.surface`, `list`, `scroll`):**
   Các thẻ card cấu trúc, danh sách nhóm dạng bảng (Grouped List), và khung cuộn áp dụng vật liệu Apple HIG (Kính mờ Frosted Glass, Elevated, Outlined).
4. **Feedback & Dialogs (`org.mdt.ui.components.dialog`, `display`):**
   Cửa sổ modal nổi, tooltip giải thích, thanh tiến trình và chip trạng thái.

---

## 📐 II. Quy Ước Tham Số & Signature Của Component

Mọi hàm composable tích hợp trong NekoMod BẮT BUỘC phải tuân thủ thứ tự tham số chuẩn hóa:

```kotlin
@Composable
fun MyComponent(
    // 1. Dữ liệu / Trạng thái bắt buộc
    value: Float,
    // 2. Callbacks sự kiện
    onValueChange: (Float) -> Unit,
    // 3. Modifier (UIModifier có giá trị mặc định là UIModifier)
    modifier: UIModifier = UIModifier,
    // 4. Tùy chọn tùy biến & Biến thể hình ảnh
    enabled: Boolean = true,
    variant: MyComponentVariant = MyComponentVariant.DEFAULT,
    activeColor: Color = Theme.colors.blue,
    // 5. Slot nội dung con tùy chọn ở cuối
    content: (@Composable () -> Unit)? = null
)
```

### Các bất biến chữ ký bắt buộc:
1. **Modifier Mặc Định:** `modifier: UIModifier = UIModifier` luôn luôn phải có giá trị mặc định.
2. **Chaining Modifier ở Cuối Cùng:** Khung chứa gốc của composable BẮT BUỘC phải nối `.then(modifier)` ở vị trí cuối cùng trong chuỗi modifier:
   ```kotlin
   Box(
       modifier = Modifier
           .radius(shapes.md)
           .background(currentBg)
           .pad(horizontal = 12f, vertical = 6f)
           .then(modifier) // Cho phép bên gọi ghi đè layout bounds, weights, hoặc cử chỉ
   )
   ```
3. **Không tạo Overload dư thừa:** Ưu tiên tham số tùy chọn với giá trị mặc định hợp lý thay vì tạo nhiều hàm trùng tên gây mơ hồ.

---

## 📏 III. Kích Thước, Ràng Buộc & Bất Biến Layout Engine

### 1. Hug Content & Auto-Layout Mặc Định (Quy tắc 6)
* Các điều khiển lá (`Button`, `Badge`, `Toggle`, `SegmentedControl`, `MonoText`) phải tính toán kích thước động dựa trên nội dung bên trong (`getPrefWidth()` / `getPrefHeight()`) cộng với khoảng đệm (`pad`).
* Các container (`Box`, `Row`, `Column`, `Card`, `GroupedList`) tuyệt đối không hardcode kích thước tùy tiện trừ khi thiết kế yêu cầu rõ ràng.

### 2. Ranh Giới Bảo Vệ (`minWidth` / `minHeight`)
* Các điều khiển tương tác bắt buộc phải có ranh giới bảo vệ để tránh bị sụp đổ kích thước (collapse):
  * `Slider`: `minHeight(24f)` trên vùng chạm/kéo, `minHeight(8f)` trên thanh track bên trong.
  * `Button` / `IconButton`: `minHeight(28f)` hoặc `minWidth(28f)`.
  * `TextField`: `minHeight(32f)`.
  * `Toggle`: kích thước cố định `size(44f, 24f)`.

### 3. Mô Hình Đo Đạc 2 Trục Độc Lập (Orthogonal 2-Pass)
Layout engine tính toán trục Ngang (X) và trục Dọc (Y) độc lập hoàn toàn:

$$\text{Dimension} = \begin{cases} 
\text{child.fixedDimension} & \text{nếu } \text{dimension} > 0\text{f} \\
\text{slotInnerDimension} & \text{nếu } (\text{flags} \ \& \ \text{SizeFlags.FILL}) \ne 0 \\
\min(\text{slotInnerDimension}, \text{childPureMinDimension}) & \text{ngược lại (Hug Content)}
\end{cases}$$

* **Trong `Row` (Flex Ngang):**
  * Trục chính (X): Phần tử con nhận độ rộng tự nhiên (`getChildMinWidth`), trừ khi đặt `Modifier.weight(ratio)` hoặc `Modifier.fillMaxWidth()`.
  * Trục phụ (Y): Phần tử con nhận chiều cao tự nhiên và căn giữa theo trục dọc theo `Row.alignment` (mặc định là `Alignment.CenterStart`).
* **Trong `Column` (Flex Dọc):**
  * Trục chính (Y): Phần tử con xếp từ trên xuống dưới theo chiều cao tự nhiên (`getChildMinHeight`), trừ khi đặt `Modifier.weight(ratio)` hoặc `Modifier.fillMaxHeight()`.
  * Trục phụ (X): Phần tử con nhận độ rộng tự nhiên và căn lề theo `Column.alignment` (mặc định là `Alignment.TopStart`).

---

## ⚡ IV. Bộ Nhớ & Bất Biến Zero-GC

Để duy trì hiệu năng mượt mà 60 FPS mà không gặp hiện tượng giật do bộ thu gom rác (GC pause):

1. **Value Class Color:** Luôn luôn sử dụng `org.mdt.core.ui.graphics.Color` (`@JvmInline value class Color(val value: ULong)`). Tuyệt đối không khởi tạo `arc.graphics.Color` trong các luồng render và layout.
2. **Cầu nối Đối tượng Arc:** Khi gọi font Arc hoặc API đồ họa, chuyển đổi thông qua `arc.util.Tmp.c1` (ví dụ: `color.toArcColor(Tmp.c1)`).
3. **Typed Modifier Elements:** Modifier phải được triển khai dưới dạng `data class Element : UIModifier.Element` bất biến với `equals()` và `hashCode()` theo giá trị để hỗ trợ Compose bỏ qua recomposition thừa.
4. **Hiệu Ứng Chuyển Động Trạng Thái:** Tạo hoạt ảnh cho các trạng thái tương tác (hover, press, progress) thông qua `animateFloatAsState` với easing (`tween(durationMillis = 140, easing = FastOutSlowInEasing)`).

---

## 🎮 V. Tương Tác, Cử Chỉ & Phản Hồi Trực Quan

1. **Trạng Thái Hover & Press:**
   Mọi component có thể click phải cung cấp phản hồi trực quan:
   * **Hover:** Phát sáng nhẹ, sáng nền (`colors.glassThin.withAlpha(...)`), và viền nổi bật.
   * **Press:** Làm tối/lắng sâu màu nền (`lerp(pressBg, pressAnim)`).
2. **Điều Hướng Con Trỏ (Pointer Routing):**
   * Các phần tử tương tác phải chỉ định cử chỉ qua `clickable`, `hoverable`, `onPointerDown`, `onPointerDrag`, `onPointerUp`.
   * Cử chỉ kéo (Slider, Resize Handle, Kéo thả Canvas) phải theo dõi delta liên tục qua `onPointerDrag` trong `EngineInputProcessor`, không được thăm dò `Core.input` trong vòng lặp render frame.
3. **Hardware Scissor Clipping:**
   Các khung cuộn và thẻ card có viền giới hạn phải bật `clip = true` (`Modifier.clip(true)`) để ngăn chặn việc nội dung vẽ tràn sang các panel lân cận.
