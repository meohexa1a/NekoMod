---
name: architecture-boundary-reviewer
description: >-
  Kiểm tra tính trong sạch của ranh giới kiến trúc và phân chia package:
  sự gắn kết package (Package Cohesion), hướng phụ thuộc (Dependency Direction),
  phát hiện package "thùng rác" (dumping ground), import vòng, và định vị file sai tầng.
---

# Architecture Boundary Reviewer Skill

Skill này đánh giá **ranh giới phân chia package và cấu trúc tầng kiến trúc** của NekoMod.
Mục tiêu: Đảm bảo mọi class/file đều nằm đúng package ngữ nghĩa, hướng phụ thuộc tuân thủ mô hình 1 chiều (Unidirectional Dependency), và không có package nào trở thành "bãi rác" chứa tạp nham.

---

## I. Mô Hình Phân Tầng Kiến Trúc NekoMod (Layered Architecture)

```
┌─────────────────────────────────────────────────────────────────────────┐
│ 1. Presentation Layer (org.hubdustry.ui.components, org.hubdustry.sample)│
└────────────────────────────────────┬────────────────────────────────────┘
                                     │ phụ thuộc xuống
┌────────────────────────────────────▼────────────────────────────────────┐
│ 2. Compose Primitives & DSL (org.hubdustry.core.compose.primitive, ...)  │
└────────────────────────────────────┬────────────────────────────────────┘
                                     │ phụ thuộc xuống
┌────────────────────────────────────▼────────────────────────────────────┐
│ 3. Compose Framework (modifier, runtime, foundation, unit, view, input)  │
└────────────────────────────────────┬────────────────────────────────────┘
                                     │ phụ thuộc xuống
┌────────────────────────────────────▼────────────────────────────────────┐
│ 4. Core Engine (org.hubdustry.core.layout, org.hubdustry.core.graphics)  │
└─────────────────────────────────────────────────────────────────────────┘
```

**Quy tắc bất biến về hướng phụ thuộc:**
- Tầng dưới **TUYỆT ĐỐI CẤM** import tầng trên.
- `core.layout` và `core.graphics` là lõi độc lập, **CẤM import `core.compose.view`** hay `ui.components`.
- `core.layout.policies` thuần toán học, **CẤM import `ComposeView`** hay Arc Scene2D widgets.
- `ui.components` là tầng trình diễn cao nhất, được phép import primitives và modifiers, nhưng không được can thiệp vào internals của runtime applier.

---

## II. Bộ 5 Tiêu Chí Rà Soát Phân Chia Package

### 1. Tính Gắn Kết Package (Package Cohesion)
Mọi file trong một package phải phục vụ cùng một trách nhiệm chức năng cụ thể:
- `org.hubdustry.core.layout`: Chỉ chứa cấu trúc cây ảo (`LayoutNode`), neo (`Anchor`), cờ co giãn (`SizeFlag`), và policies.
- `org.hubdustry.core.graphics`: Chỉ chứa batching GPU, shader, bo góc SDF.
- `org.hubdustry.core.compose.modifier`: Chỉ chứa định nghĩa modifier và modifier elements.
- `org.hubdustry.core.compose.primitive`: Chỉ chứa các composables nguyên thủy (`Box`, `Row`, `Column`, `Text`, `BasicTextField`).
- `org.hubdustry.core.compose.view`: Cửa khẩu tích hợp Scene2D (`ComposeView`, `NodeRenderer`, `InputDispatcher`, `HitTestManager`).

- 🔴 **CRITICAL**: File nằm sai package hoàn toàn (ví dụ: Composable widget nằm trong `core.layout`, hoặc thuật toán layout policy nằm trong `ui.components`).
- 🟡 **WARNING**: Package có tên quá chung chung hoặc gom các khái niệm không liên quan lại với nhau.

---

### 2. Phát Hiện Package "Thùng Rác" (No Dumping Ground / God Package)
- ❌ **Anti-Pattern**:
  - Tạo một package hoặc file `utils`, `common`, `misc`, `helpers` rồi nhét mọi thứ không biết để đâu vào đó.
  - Một file chứa >5 class/interface không liên quan ngữ nghĩa chặt chẽ với nhau.
- **Invariants**:
  - Mọi helper function phải gắn liền với domain type tương ứng dưới dạng **Extension Function** hoặc **Member Method**.
  - Không có file hay package `util` vô tội vạ.

---

### 3. Phụ Thuộc Vòng Giữa Các Package (Circular Package Dependencies)
- 🔴 **CRITICAL**: Package A import Package B, và Package B import lại Package A.
  - Ví dụ: `core.layout` import `core.compose.runtime` và ngược lại.
  - Phụ thuộc vòng là dấu hiệu rạn nứt kiến trúc, phải cắt đứt bằng interface abstraction hoặc di chuyển file về đúng package chủ quản.

---

### 4. Đóng Gói Tầm Nhìn Package-Private (`internal` vs `public`)
- Các class/hàm chỉ phục vụ nội bộ một package phải được đánh dấu `internal` hoặc `private`.
- Các singleton scope (`RowScopeInstance`, `ColumnScopeInstance`) nếu dùng trong `inline fun` thì dùng `@PublishedApi internal`.
- 🟡 **WARNING**: Phơi bày `public` cho các helper/class trung gian chỉ dùng nội bộ trong engine.

---

### 5. Số Lượng File & Độ Sâu Cây Thư Mục (Package Depth & File Count)
- Một package không nên có >15 files nằm phẳng mà không phân nhóm con nếu các files có nhóm chức năng riêng.
- Không tạo cây package sâu quá 5 tầng vô nghĩa (ví dụ: `a.b.c.d.e.f` chỉ chứa 1 file duy nhất).

---

## III. Mẫu Báo Cáo Đánh Giá

```markdown
## Architecture Boundary Review — [Tên Module / Toàn Dự Án]

### Score: X/10

### Findings:
1. 🔴 [CRITICAL] Phụ thuộc ngược / Phụ thuộc vòng — packageA -> packageB:line — vi phạm gì
2. 🟡 [WARNING] File nằm sai package — file:line — nên chuyển sang package nào
3. 🟡 [WARNING] Package thùng rác / Scope rò rỉ public — file:line
4. 🟢 [GOOD] Hướng phụ thuộc 1 chiều chuẩn mực

### Summary:
1-2 câu tóm tắt độ sạch ranh giới package.

### Recommendation: APPROVE | NEEDS_WORK | REJECT
```
