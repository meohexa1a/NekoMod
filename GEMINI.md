# NekoMod Universal Coding & Architecture Invariants

Tài liệu này là quy chuẩn lập trình bắt buộc (Always-On Universal Rule) áp dụng cho mọi file Kotlin trong toàn bộ dự án NekoMod. Mọi agent và kỹ sư tham gia vào codebase đều phải tuân thủ nghiêm ngặt các nguyên tắc bất biến sau:

---

## 0. Bộ Lọc Nguồn Gốc & Tính Toàn Vẹn Ngữ Nghĩa (Architectural Origin & Semantic Integrity)

Trước khi viết bất kỳ cấu trúc dữ liệu, enum, modifier, hoặc hàm xử lý mới nào, **BẮT BUỘC** phải vượt qua bài kiểm tra 3 câu hỏi nguồn gốc và các nguyên tắc ngữ nghĩa sau:

1. **Bộ Ba Câu Hỏi Nguồn Gốc (Three Architectural Origin Questions)**:
   - **Khái niệm này đến từ framework nào?** (Jetpack Compose AOSP, Godot Control, Arc Scene2D, hay tự chế?)
   - **Trong framework gốc, nó hoạt động chính xác ra sao?** (Hợp đồng dữ liệu, giả định luồng, thứ tự ưu tiên?)
   - **Khi chuyển sang NekoMod, có phần nào của ngữ nghĩa gốc bị mất mát hoặc xung đột với hệ thống khác không?**
   *(Nếu không thể trả lời rõ ràng 3 câu hỏi này, CẤM TUYỆT ĐỐI việc gộp chắp vá các khái niệm khác nhau vào một class).*

2. **Cấm API Ảo & Ngữ Nghĩa Nói Dối (No Phantom APIs & No Semantic Lies)**:
   - Mọi hàm, mọi tham số được phơi ra public API **bắt buộc phải có tác dụng thật sự 100%**. CẤM tạo các tham số "bán hàng giả" (như `weight(fill = false)` nhưng nhánh `false` không hề làm gì).
   - Hàm mang tên A **tuyệt đối không được phép có side-effect ngầm B**. (Ví dụ: `widthIn()` chỉ được đặt giới hạn biên `minWidth`/`maxWidth`, CẤM TUYỆT ĐỐI việc âm thầm đổi `sizeFlag` thành `SHRINK`).

3. **Độc Lập Tuyệt Đối Giữa Ràng Buộc Khung và Chính Sách Co Giãn (Constraints vs Sizing Independence)**:
   - Ràng buộc hình học (`minWidth`, `maxWidth`, `minHeight`, `maxHeight`) là "hàng rào biên giới".
   - Chính sách co giãn (`sizeFlag`, `stretchRatio`) là "động cơ phân bổ".
   - Hàng rào biên giới không được quyền tắt động cơ. Việc đặt `minWidth` không được phép hủy cờ `FILL` hay `EXPAND` của phần tử.

4. **Sự Trong Sạch Của Thực Thể (Entity Memory Hygiene)**:
   - `LayoutNode` là Virtual DOM Node, chỉ chứa trạng thái hình học và visual của chính nó.
   - CẤM TUYỆT ĐỐI việc nhét các biến nháp, cờ tạm trung gian của thuật toán giải bố cục (như `tempMain`, `isFrozen` của Flex redistribution) vào `LayoutNode`. Mọi Policy phải tự quản lý bộ nhớ giải thuật của riêng mình.

5. **Phân Định Rạch Ròi Vòng Đời Cục Bộ vs Toàn Cục (Lifecycle Isolation)**:
   - Vòng đời instance của một View (`ComposeView.dispose()`) **chỉ được phép dọn dẹp tài nguyên thuộc về chính instance đó**.
   - CẤM TUYỆT ĐỐI việc một instance View khi bị hủy lại gọi `dispose()` lên các tài nguyên Singleton toàn cục của tiến trình (`UIBatch`, `UberShader`, `CompositionManager`).

---

## 1. Thiết Kế Miền & Đóng Gói (Domain Architecture Invariants)

1. **Active Self-Validating Entity thay vì Anemic DTO**:
   - Các thực thể lõi (như `LayoutNode`, `Widget`, `Component`) phải là đối tượng tự trị, tự bảo vệ các bất biến trạng thái của chính nó.
   - **Gateway Sanitization**: Mọi dữ liệu đi vào entity qua setters/constructor phải được chuẩn hóa ngay tại cửa khẩu ($0 \le min \le max$, loại bỏ `NaN`, số âm không hợp lệ). Triệt tiêu tận gốc nguy cơ ném ngoại lệ toán học (như `IllegalArgumentException` trong `coerceIn`) ở các tầng tính toán bên dưới.
   - **Bảo Vệ Cấu Trúc Phân Cấp (Tree Integrity)**:
     - Tuyệt đối không phơi bày mutable collections ra ngoài (`children` phải là read-only `List<T>` phơi ra từ `private val _children = ArrayList<T>()`).
     - Mọi thao tác thêm/xóa node con phải thông qua API của chính Node (`addChild`, `removeChild`, `clearChildren`) để tự động đồng bộ con trỏ hai chiều (`parent`) và chặn chu trình lặp tổ tiên (cycle detection).

2. **Cắt Bỏ Triệt Để Tương Thích Ngược Trong Core (Zero Deprecation Debt)**:
   - Các module cốt lõi (Layout, Virtual DOM, Render Pipeline, Event Dispatch) là nền tảng sống còn, **tuyệt đối KHÔNG giữ lại các lớp bọc tương thích ngược** (`@Deprecated` shims, wrapper giả lập API cũ, adapter rác).
   - Khi tái cấu trúc hoặc nâng cấp kiến trúc, phải thực hiện dứt điểm (clean break), xóa bỏ hoàn toàn cấu trúc cũ và cập nhật trực tiếp test suite cũng như callers. Không để nợ kỹ thuật (legacy cruft) làm biến dạng kiến trúc lõi.

3. **Tự Trị Đệ Quy & Pure Math trong Policies/Processors**:
   - **Self-Arranging Entity**: Node tự chịu trách nhiệm định vị hình học, tự trừ vùng đệm padding nội tại, tự giải neo và cascade đệ quy xuống cây con (`arrangeContent()`, `resolveAnchors()`).
   - **Pure Math Processors**: Các chính sách bố cục (`LayoutPolicy`), trình xử lý vẽ hoặc thuật toán chỉ nhận dữ liệu đã được chuẩn hóa và thực hiện tính toán không gian thuần túy. CẤM TUYỆT ĐỐI việc body function của policy phải bóc tách dữ liệu lặp đi lặp lại, kiểm tra biên rườm rà, hay can thiệp vào cấu trúc đệ quy nội bộ của node.

4. **Bản Chất Virtual DOM Entity & Ranh Giới Policy (Unified Virtual DOM Entity & Pure Math Boundary)**:
   - `LayoutNode` là thực thể tự trị thống nhất đóng vai trò Virtual DOM Node của NekoMod (tương đương `LayoutNode` trong Jetpack Compose AOSP). Node sở hữu phẳng (flat fields) toàn bộ hình học không gian, thuộc tính hiển thị (Visual Tokens: `backgroundColor`, `text`, `textColor`, `alpha`) và chuỗi bộ lọc cử chỉ (`pointerInputFilters`) để triệt tiêu việc cấp phát và ép kiểu (Zero-GC). CẤM TUYỆT ĐỐI việc dùng các con trỏ gián tiếp lỏng lẻo dạng `attachment: Any?` hay monkey-patching làm tổn hại tính an toàn kiểu (type-safety) và hiệu năng.
   - **Ranh giới thuật toán thuần túy (Pure Math Separation)**: Các chính sách bố cục (`LayoutPolicy`: `BoxLayoutPolicy`, `FlexLayoutPolicy`) thuần túy chỉ tính toán giải thuật không gian và tọa độ. CẤM TUYỆT ĐỐI các policy can thiệp hoặc phụ thuộc vào thuộc tính visual/styling hay gesture filters của node.

---

## 2. Kotlin Idiomatic vs Java Thinking

1. **Expression over Statement**:
   - Tận dụng `if`, `when`, `try` như một biểu thức (Expression).
   - Chain các hàm xử lý kẹp biên ngay sau khối `when`:
     ```kotlin
     val childW = when (child.sizeFlagHorizontal) {
         SizeFlag.EXPAND, SizeFlag.FILL -> slotW
         SizeFlag.SHRINK -> child.minWidth
     }.coerceIn(child.minWidth, child.maxWidth)
     ```
2. **Encapsulation over Feature Envy**:
   - Tuyệt đối không bóc tách dữ liệu của đối tượng ra ngoài để truyền vào các hàm utility tĩnh.
   - Sử dụng **Extension Functions** hoặc Member Functions trực tiếp trên receiver type (Ví dụ: `Alignment.computeOffset(allocated, actual)`).
3. **Default Parameters over Overloading**:
   - Sử dụng default arguments (`fun RowPolicy(gap: Float = 0f)`) thay vì tạo các phương thức overload hoặc builder rườm rà.
4. **Trừu Tượng Hóa Theo Trục (Symmetry Abstraction)**:
   - Không nhân đôi logic song song giữa hàng ngang và cột dọc (Row vs Column). Sử dụng `Orientation` (`HORIZONTAL`, `VERTICAL`) để xử lý thống nhất trên `mainAxis` và `crossAxis`.

---

## 3. Kỷ Luật Hiệu Năng & Zero-GC

1. **Hot-Path (Frame Loop: Measure, Layout, Draw, Hit-Test, Tick)**:
   - **CẤM TUYỆT ĐỐI**: `.filter { }`, `.map { }`, `.forEach { }`, `.toList()`, hoặc tạo `Iterator` / lambda closure.
   - **BẮT BUỘC**: Dùng vòng lặp chỉ mục thuần túy (`for (i in 0 until count)`), biến scratchpad trên Node hoặc `value class`.
2. **Đóng Gói Nguyên Thủy (Primitive Packing & Value Classes)**:
   - Các cấu trúc dữ liệu hình học 2D cơ bản (`Offset`, `IntSize`) **BẮT BUỘC phải là `@JvmInline value class`** đóng gói 2 số 32-bit thành một số nguyên 64-bit `Long`. Triệt tiêu hoàn toàn việc cấp phát heap object khi cộng trừ vector hay thay đổi kích thước trong frame loop.
3. **Kỷ Luật Toán Học Cho Cử Chỉ (Distance Squared over Sqrt)**:
   - So sánh khoảng cách cử chỉ (`touchSlop`) bắt buộc dùng khoảng cách bình phương (`dx * dx + dy * dy > slop * slop`). CẤM gọi `sqrt` và cấm trừ `Offset` tạo object trung gian trong frame loop.
4. **Zero-Closure Modifier Traversal**:
   - Duyệt cây modifier phẳng bằng đệ quy pattern-matching (`when (this)`) hoặc vòng lặp tĩnh. CẤM dùng `foldIn` với lambda closure trong giai đoạn update node của Recomposition.
5. **Tái Sử Dụng Bộ Đệm & Vệ Sinh Object Pool (Scratchpad & Pool Hygiene)**:
   - Các bộ đệm sự kiện và hit-test trong `ComposeView` (`hitScratch`, `hitPool`, `hoverPool`, `eventsScratch`, `boundsScratch`) phải được tái sử dụng qua index/pool, tuyệt đối không cấp phát `ArrayList` hay đối tượng mới trên mỗi frame `mouseMoved`, `touchDragged`, hoặc `draw`.
6. **Cold-Path (DSL, Config, Setup, State, Navigation)**:
   - Khuyến khích tối đa functional style: `map`, `filter`, `let`, `apply`, `takeIf`, `data class`.

---

## 4. Ranh Giới Tọa Độ Trục Y (Bức Tường Berlin)

1. **Bên trong Neko Engine (Virtual DOM, Measure, Layout, Anchor)**:
   - Tuân thủ 100% chuẩn **Top-Left ($Y$-down)**. Gốc $(0, 0)$ ở đỉnh, $Y$ tăng dần đi xuống.
   - CẤM TIỆT các phép toán lật Y (`1.0f - anchorTop`, `height - y`) ở các tầng giữa.
2. **Tại Ranh Giới Cửa Khẩu (`ComposeView`)**:
   - Chỉ có đúng 2 điểm được phép lật Y khi giao tiếp với Arc:
     - Input từ Arc vào: `composeY = height - arcY`
     - Lệnh vẽ ra Arc: `arcY = height - (nodeY + nodeHeight)`
3. **Vệ Sinh Trạng Thái Batch Arc (Batch State Hygiene)**:
   - Sau khi hoàn thành pass vẽ của `ComposeView` (`draw()`), bắt buộc phải gọi `Draw.reset()` trong khối `finally` để bảo vệ Arc SpriteBatch, ngăn chặn ô nhiễm màu sắc sang các Actor Scene2D khác.
4. **Không Gian Tọa Độ Cục Bộ 100% Trong Virtual Tree**:
   - Mọi tính toán hình học đệ quy bên trong Virtual Tree (`renderNodeRecursive`, `hitTestChain`) phải hoạt động 100% trên hệ tọa độ cục bộ `(0f, 0f)`. Chỉ cộng tọa độ thế giới của Scene2D `(this.x, this.y)` ở đúng thời điểm phát lệnh vẽ ra Arc.

---

## 5. Kiến Trúc Tương Tác & Pointer Input (Zero-Side-Effect Invariants)

1. **Stateless Node đối với Input (Decoupled Interactive State)**:
   - `LayoutNode` là thực thể Virtual DOM phi trạng thái tương tác. **CẤM TUYỆT ĐỐI** việc đặt các biến trạng thái tương tác mutable (`isPressed`, `isHovered`, `cursor`) trực tiếp trên `LayoutNode`.
   - Node chỉ đóng vai trò host danh sách bộ lọc cử chỉ coroutine qua `pointerInputFilters: List<SuspendingPointerInputFilter>`. Mọi phản hồi tương tác người dùng bắt buộc phải truyền qua luồng `InteractionSource` hoặc Compose Snapshot State.
2. **Cắt Đứt Arc `ClickListener` & Phụ Thuộc Skiko (Zero Bloat & Zero Coupling)**:
   - **CẤM DÙNG `ClickListener` của Arc/Scene2D bên trong cây ảo**: Tránh tuyệt đối việc rò rỉ bộ nhớ và click ma do `touchFocus` trên Stage khi cây Compose re-compose.
   - **CẤM KÉO `compose.ui` / `compose.foundation`**: Tránh phình mod JAR >80MB do Skiko C++ binaries, tránh xung đột OpenGL context với LWJGL3 của Mindustry, và bảo tồn tính tương thích 100% với Android. Chỉ dùng `compose.runtime` + `kotlinx.coroutines` và bóc tách có chọn lọc các data contracts / gesture detectors.
3. **Bất Biến Hóa Luồng Sự Kiện (Immutable Event Stream)**:
   - Toàn bộ sự kiện con trỏ được đóng gói thành value objects / data classes bất biến (`PointerEvent`, `PointerInputChange`, `PointerEventPass`).
4. **Vòng Đời Cử Chỉ bằng Coroutine & Unidirectional Data Flow**:
   - Sử dụng `awaitPointerEventScope` và `detectTapGestures` để bắt cử chỉ (touch-slop, double-tap, long-press).
   - Phản hồi thị giác (visual feedback) chỉ được truyền qua `InteractionSource` (`PressInteraction.Press/Release/Cancel`, `HoverInteraction.Enter/Exit`) hoặc Compose State. CẤM các biến static toàn cục (`lastClickNode`, `lastClickTime`, `pendingSingleClickNode`) của thời v1/v2.

---

## 6. Kiến Trúc Tích Hợp Cửa Khẩu Arc Scene2D

1. **`Table.compose` DSL Tinh Gọn**:
   - Hàm `Table.compose(content: @Composable () -> Unit): Cell<ComposeView>` chỉ đóng vai trò mount point, trả về `Cell<ComposeView>` để caller tự do chain layout Scene2D (`.size()`, `.grow()`, `.pad()`, `.row()`).
   - Không nhồi nhét `width`, `height`, `backgroundColor` vào tham số hàm `Table.compose`.
2. **Triệt Tiêu Nạng Giả Lập Kích Thước**:
   - CẤM dùng các nạng giả lập như `LocalViewDimensions`. `ComposeView` đóng vai trò là Host Window, tự động phân phối bounds xuống `rootLayoutNode`, các composable bên trong tự động co giãn theo `SizeFlag.FILL` / `EXPAND`.

---

## 7. Hợp Đồng Ràng Buộc Khung Bố Cục (Layout Constraints Contract)

1. **Phân Phối Ràng Buộc Một Chiều (Constraints Down, Sizes Up, Parent Positions)**:
   - Khung mẹ (Host Window / Node cha) áp đặt không gian khả dụng (`availableWidth`, `availableHeight`).
   - Node con tự tính toán kích thước nội tại tự thân (`computeMinSize` từ text, padding, con bên trong). Tuy nhiên, kích thước thực thi cuối cùng (`width`, `height`) hoàn toàn do Node cha quyết định.
   - Node con mang `SizeFlag.FILL` hoặc `SizeFlag.EXPAND` **tuyệt đối không được phép xé rách khung cha**:
     ```kotlin
     val childW = when (child.sizeFlagHorizontal) {
         SizeFlag.FILL, SizeFlag.EXPAND -> innerWidth
         SizeFlag.SHRINK -> child.minWidth
     }.coerceAtMost(child.maxWidth)
     ```
2. **Kích Thước Cửa Sổ Mẹ Tuyệt Đối (`exact = true`)**:
   - `ComposeView` phân phối ràng buộc kích thước xuống Node gốc trong 1 pass duy nhất: `rootLayoutNode.layout(currentW, currentH, exact = true)`.
   - Khi `exact = true`, Node gốc nhận trực tiếp kích thước thực tế của Host Window, triệt tiêu hoàn toàn nguy cơ con bên trong có nội dung dài (như văn bản BMFont nhiều ký tự) làm phình to khung cửa sổ mẹ và gây liệt click ở vùng biên.

---

## 8. Vòng Đời Tất Định & Chống Rò Rỉ Bộ Nhớ (Deterministic Lifecycle & Memory Safety)

1. **Quy Tắc "Never Leave a Ghost Behind" (Deterministic Teardown)**:
   - Khi `ComposeView` bị tháo khỏi Scene2D Stage (`setScene(null)`), bắt buộc kích hoạt `dispose()` triệt để:
     - Duyệt đệ quy hủy sạch toàn bộ `pointerInputFilters` trên mọi node con trong cây ảo (`disposeNodeRecursive`) trước khi gọi `composition?.dispose()`.
     - `composition?.dispose()` giải phóng Snapshot state và tự động hủy các Coroutines trong `LaunchedEffect`.
     - Xóa sạch các bộ theo dõi con trỏ (`trackedPointers`), scratchpads, pools.
     - Đảm bảo đối tượng `ComposeView` được GC thu hồi sạch sẽ (`WeakReference == null`) ngay sau khi đóng dialog.
2. **Triết Lý NEVER-THROW Trong Runtime**:
   - Toàn bộ tương tác người dùng, con trỏ click trượt, chuột rung nhẹ qua mép biên (touch-slop), dữ liệu đầu vào `NaN` hoặc số âm đều phải được chuẩn hóa tại cửa khẩu (Gateway Sanitization) hoặc bọc phòng vệ.
   - Tuyệt đối không để văng ngoại lệ toán học hoặc unhandled exception làm sập luồng game loop của Mindustry.

---

## 9. Quy Chuẩn Công Thái Học DSL & Đo Lường Văn Bản (DSL Ergonomics & Invalidation-Driven Pipeline)

1. **Chữ Ký Hàm Composable Chuẩn Mực**:
   - Tham số `modifier: Modifier = Modifier` luôn là tham số đầu tiên (hoặc đứng ngay sau callback bắt buộc như `onClick`).
   - Tham số `content: @Composable () -> Unit` luôn là tham số cuối cùng để tận dụng cú pháp Trailing Lambda của Kotlin.
2. **Đo Lường Văn Bản Theo Sự Kiện (Invalidation-Driven Text Measurement)**:
   - Việc đo kích thước chữ BMFont (`GlyphLayout.setText`) chỉ được chạy ở pha `Measure` khi chuỗi `text` hoặc font thực sự thay đổi.
   - **CẤM TUYỆT ĐỐI**: Đo đạc lại kích thước văn bản hoặc gọi `Color.cpy().mul(...)` trong pha vẽ (`draw()`). Pha `draw()` là Hot-Path chỉ đọc các giá trị hình học đã được đo sẵn để render.
   - Dùng shared singleton `GlyphLayout` tĩnh, cấm cấp phát instance `GlyphLayout()` mới bên trong composables.
3. **Đo Lường Nội Tại Tôn Trọng Ràng Buộc Tường Minh**:
   - Đo đạc kích thước văn bản tự thân (Intrinsic Text Measurement) không bao giờ được phép tự ý mở rộng `maxWidth` / `maxHeight` vượt quá giới hạn tường minh do `Modifier` đặt ra.

---

## 10. Quản Lý Subagents & Review

- Khi muốn rà soát chất lượng code của một PR/Task, hãy kích hoạt skill `kotlin-idioms-reviewer` (`.agents/skills/kotlin-idioms-reviewer/SKILL.md`) để quét vi phạm và chạy kiểm thử tự động.

---

## 11. Đa Bộ Lọc Cử Chỉ Theo Chuỗi (Composite Pointer Input Filter Chain)

1. **Chained Pointer Input Support & Zero Legacy Shim**:
   - Một `LayoutNode` có thể gắn nhiều `SuspendingPointerInputFilter` (ví dụ: vừa `.hoverable()` vừa `.clickable()`).
   - Node chỉ duy trì danh sách read-only `pointerInputFilters: List<SuspendingPointerInputFilter>` và quản lý qua các API tường minh (`addPointerInputFilter`, `removePointerInputFilter`, `clearPointerInputFilters`).
   - **CẤM TUYỆT ĐỐI việc khôi phục getter/setter gán đè đơn lẻ `pointerInputFilter`** (triệt tiêu nợ tương thích ngược theo Rule 1.2). Mọi filter trong chuỗi modifier bắt buộc phải được gắn tuần tự vào danh sách `pointerInputFilters` của Node.
2. **Thứ Tự Phân Phối Sự Kiện Chuẩn Xác**:
   - Bộ điều phối sự kiện 3-pass phải phân phối tuần tự qua tất cả các filter của node:
     - `Initial` pass: Tunneling từ ngoài vào trong (Outer -> Inner).
     - `Main` pass & `Final` pass: Bubbling từ trong ra ngoài (Inner -> Outer).

---

## 12. Phạm Vi Bố Cục Container (Container Scopes: RowScope, ColumnScope, BoxScope)

1. **Ngăn Chặn Triệt Để Ô Nhiễm Scope Toàn Cục**:
   - Các modifier mang tính định vị hoặc phân bổ không gian phụ thuộc vào container cha (như `weight`, `align`) **BẮT BUỘC phải nằm trong Scope tương ứng**:
     - `RowScope.weight`: Chỉ cấp phát theo trục chính ngang (`sizeFlagHorizontal = SizeFlag.EXPAND`).
     - `RowScope.align`: Căn chỉnh theo trục phụ dọc (cross-axis).
     - `ColumnScope.weight`: Chỉ cấp phát theo trục chính dọc (`sizeFlagVertical = SizeFlag.EXPAND`).
     - `ColumnScope.align`: Căn chỉnh theo trục phụ ngang (cross-axis).
     - `BoxScope.align`: Căn chỉnh vị trí 2 chiều trong Box.
   - Tuyệt đối CẤM phơi bày `Modifier.weight` hoặc `Modifier.align` ở top-level global scope gây phá vỡ tính độc lập trục và tạo tác dụng phụ ngoài ý muốn.

---

## 13. Kế Thừa Hiệu Ứng Thị Giác Trên Cây (Visual Tree Cascading & Alpha)

1. **Cascading Đệ Quy Toàn Diện**:
   - Các thuộc tính hiển thị mang tính bao trùm (như độ mờ `alpha`, bộ lọc màu) phải được nhân lũy tiến từ gốc xuống lá:
     `effectiveAlpha = parentEffectiveAlpha * node.alpha`
   - Đảm bảo khi đặt `Modifier.alpha` lên container cha, toàn bộ nội dung con (background, text, button, icon) đều thừa hưởng độ trong suốt đồng nhất.

---

## 14. Hợp Đồng Đẳng Cấu Cho Modifier (Modifier Element Value Equality Contract)

1. **Bắt Buộc Giá Trị Tương Đương (Structural Equality via Data Classes)**:
   - Mọi phần tử `Modifier.Element` **BẮT BUỘC phải là `data class`** (hoặc triển khai `equals` và `hashCode` đầy đủ dựa trên các trường cấu hình).
   - Đảm bảo Compose Runtime có thể so sánh diffing chính xác khi Recomposition, ngăn chặn việc re-apply modifier thừa thãi và kích hoạt layout pass vô cớ.
2. **CẤM Anonymous Objects & Object Trung Gian**:
   - **CẤM TUYỆT ĐỐI** việc trả về anonymous inner class (`object : Modifier.Element`) trong các hàm mở rộng modifier.
   - **CẤM TUYỆT ĐỐI** việc khởi tạo một modifier object đầy đủ chỉ để bóc tách gọi một hàm adapter sinh ra object thứ hai (như `SizeModifier(...).applyToWidthOnly()`). Mỗi extension function modifier phải trực tiếp trả về `Modifier.Element` chuyên biệt của chính nó.



