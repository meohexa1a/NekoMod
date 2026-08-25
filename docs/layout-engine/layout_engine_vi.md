# Thuật toán Dàn trang & Hệ Tọa độ (Layout Engine & Coordinates)

Tài liệu này giải thích chi tiết nguyên lý hoạt động của bộ dàn trang `GodotLayout`, hệ tọa độ trực giao OpenGL trong Mindustry/Arc, và cách phân bổ kích thước giữa các Container (`RowNode`, `ColumnNode`, `GridContainerNode`).

---

## 1. Hệ tọa độ OpenGL Bottom-Left

Mindustry và Arc sử dụng hệ tọa độ chuẩn OpenGL:
* **Gốc tọa độ $(0, 0)$:** Nằm ở **Góc dưới bên trái (Bottom-Left)** của màn hình.
* **Trục $X$:** Tăng dần từ Trái sang Phải ($0 \rightarrow \text{Width}$).
* **Trục $Y$:** Tăng dần từ Dưới lên Trên ($0 \rightarrow \text{Height}$).

### Quy đổi hướng khi dàn trang Dọc (`ColumnNode`):
Vì người dùng luôn mong muốn giao diện xếp từ **Trên xuống Dưới (Top to Bottom)**:
* Tọa độ bắt đầu ở đỉnh slot: `currentTopY = parentY + parentH - padT`.
* Tọa độ đáy của phần tử đầu tiên: `slotY = currentTopY - slotH`.
* Sau khi đặt phần tử, dịch chuyển xuống dưới: `currentTopY -= slotH + gap`.

---

## 2. Thuật toán Dàn trang 2-Pass (Godot Container Algorithm)

Khi một Container (`RowNode` hoặc `ColumnNode`) thực thi `layout()`, nó trải qua 2 bước đo đạc và phân bổ:

### 🔹 Bước 1: Đo đạc kích thước tối thiểu (Measurement Pass)
Container duyệt qua tất cả các con đang hiển thị (`child.visible == true`):
1. Tính kích thước tối thiểu của từng con:
   * Ngang: `getChildMinWidth(child) = child.getPrefWidth() + child.marginL + child.marginR`
   * Dọc: `getChildMinHeight(child) = child.getPrefHeight() + child.marginT + child.marginB`
2. Cộng dồn tổng kích thước tối thiểu: $\text{totalMinMain} = \sum \text{childMinSize}$.
3. Đếm tổng tỷ lệ co giãn của các con có cờ `EXPAND`: $\text{totalStretchRatio} = \sum \text{stretchRatio}$.

### 🔹 Bước 2: Phân bổ không gian & Thụt lề (Slot Allocation Pass)
1. **Tính khoảng trống còn thừa:**
   $$\text{freeSpace} = \max(0, \text{availableSpace} - \text{totalMinMain} - \text{totalGaps})$$
2. **Nếu có con mang cờ `EXPAND`:**
   * Không gian thừa được chia đều theo tỷ lệ:
     $$\text{extraSlot} = \text{freeSpace} \times \left(\frac{\text{child.stretchRatio}}{\text{totalStretchRatio}}\right)$$
   * $\text{slotSize} = \text{childMinSize} + \text{extraSlot}$.
3. **Nếu không có con nào mang cờ `EXPAND`:**
   * Áp dụng chế độ phân bổ [`Arrangement`](#3-mô-hình-arrangement):
     * **`Start`:** Xếp sát mép đầu.
     * **`Center`:** Canh giữa toàn bộ khối con trong vùng trống.
     * **`End`:** Đẩy toàn bộ về mép cuối.
     * **`SpaceBetween`:** Chia đều khoảng trống giữa các con (con đầu và cuối chạm mép).
     * **`SpaceAround`:** Chia đều khoảng trống quanh các con (mỗi bên mép nhận $1/2$ khoảng cách).
     * **`SpaceEvenly`:** Chia đều khoảng trống tuyệt đối cho cả các mép và giữa các con.

---

## 3. Cờ Dàn trang (`SizeFlags`) & Hàm `fitChildInRect`

Mỗi node con có 2 cờ: `sizeFlagsHorizontal` và `sizeFlagsVertical`.
Khi một slot $(rx, ry, rw, rh)$ được cấp cho con:

1. **Thụt lề theo Margin:**
   * $\text{slotInnerX} = rx + \text{marginL}$
   * $\text{slotInnerY} = ry + \text{marginB}$
   * $\text{slotInnerW} = rw - \text{marginL} - \text{marginR}$
   * $\text{slotInnerH} = rh - \text{marginT} - \text{marginB}$

2. **Căn chỉnh vị trí:**
   * **`FILL`:** Node giãn đầy đủ kích thước $\text{slotInnerW} \times \text{slotInnerH}$.
   * **`SHRINK_BEGIN`:** Giữ nguyên kích thước tối thiểu, đặt ở mép Trái (ngang) hoặc Đỉnh (dọc).
   * **`SHRINK_CENTER`:** Giữ nguyên kích thước tối thiểu, đặt ở chính giữa slot.
   * **`SHRINK_END`:** Giữ nguyên kích thước tối thiểu, đặt ở mép Phải (ngang) hoặc Đáy (dọc).
