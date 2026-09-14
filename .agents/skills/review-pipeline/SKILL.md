---
name: review-pipeline
description: >-
  Khung tiêu chuẩn cho pipeline review code NekoMod v3. 
  Định nghĩa cách PM agent điều phối nhiều review skills chuyên biệt,
  format output chuẩn hóa, và quy trình mở rộng thêm skill mới.
  Đọc file này TRƯỚC khi tạo hoặc chạy bất kỳ review skill nào.
---

# Review Pipeline — Khung Mở Rộng

## Mục Đích

Khi dự án phình to, 1 agent không thể review hết mọi góc nhìn. Pipeline này chia review thành **nhiều skill chuyên biệt**, mỗi skill chấm 1 khía cạnh, PM agent tổng hợp và quyết định.

---

## Quy Trình

```
Worker Agent hoàn thành task
    ↓
PM Agent kiểm tra chức năng + chạy test (.\gradlew.bat test --rerun-tasks)
    ↓  test fail → trả về worker
    ↓  test pass ↓
PM Agent thu thập danh sách files đã thay đổi
    ↓
PM Agent spawn N review subagents song song (mỗi cái chạy 1 skill)
    ↓
Mỗi subagent trả về Review Report (format chuẩn — xem bên dưới)
    ↓
PM Agent tổng hợp → quyết định:
    - Mọi skill ≥ 7/10, 0 CRITICAL → APPROVE
    - Có CRITICAL hoặc skill < 5/10 → SEND BACK + ghi rõ findings
    - Trung bình → APPROVE WITH NOTES
```

---

## Format Output Chuẩn (Mọi Skill Đều Dùng)

Mọi review skill phải trả về báo cáo theo format sau:

```markdown
## [Tên Skill] Review — [Tên Module/File]

### Score: X/10

### Findings:
1. 🔴 [CRITICAL] Mô tả ngắn — file:line — vi phạm gì
2. 🟡 [WARNING] Mô tả ngắn — file:line — vi phạm gì
3. 🟢 [GOOD] Điểm tốt đáng ghi nhận
4. 💡 [SUGGESTION] Cải thiện không bắt buộc

### Summary:
1-2 câu tóm tắt tình trạng chung.

### Recommendation: APPROVE | NEEDS_WORK | REJECT
```

**Quy tắc chấm điểm:**
- 🔴 CRITICAL: Mỗi finding trừ 2 điểm. Phải sửa trước khi approve.
- 🟡 WARNING: Mỗi finding trừ 1 điểm. Nên sửa nhưng có thể ghi nhận.
- 🟢 GOOD: Không cộng điểm, nhưng ghi nhận để làm gương.
- 💡 SUGGESTION: Không ảnh hưởng điểm.
- Điểm khởi đầu: 10. Tối thiểu: 0.

---

## Danh Sách Skills Hiện Có

| Skill | Folder | Chấm cái gì |
|:---|:---|:---|
| `kotlin-idioms-reviewer` | `.agents/skills/kotlin-idioms-reviewer/` | Java-thinking, Expression vs Statement, Zero-GC, Feature Envy |
| `semantic-clarity-reviewer` | `.agents/skills/semantic-clarity-reviewer/` | Tên nói dối, API phantom, suspend giả, code đọc có hiểu không |
| `code-aesthetics-reviewer` | `.agents/skills/code-aesthetics-reviewer/` | Expression body, blank line phân đoạn, named args, naming, local functions, section banners, ASCII diagrams |
| `coding-conventions-reviewer` | `.agents/skills/coding-conventions-reviewer/` | Quy chuẩn đặt tên Kotlin, khớp package/directory, guard clauses, giới hạn độ dài hàm |
| `architecture-guardian` | `.agents/skills/architecture-guardian/` | Tự động thanh tra, siết chặt đóng gói, phủ KDoc 100%, và đóng băng kiến trúc |
| `architecture-boundary-reviewer` | `.agents/skills/architecture-boundary-reviewer/` | Ranh giới package, hướng phụ thuộc 1 chiều, package cohesion, cấm import vòng |
| `change-reporter` | `.agents/skills/change-reporter/` | Tạo báo cáo thay đổi có cấu trúc (không chấm điểm) |

---

## Khi Nào Chạy Review (Trigger Matrix)

| Sự kiện | kotlin-idioms | semantic-clarity | code-aesthetics | coding-conventions | arch-boundary |
|:---|:---:|:---:|:---:|:---:|:---:|
| **Bất kỳ thay đổi nào (tối thiểu)** | ✅ | ✅ | — | ✅ | — |
| Module 🟢 Stable | ✅ | ✅ | ✅ | ✅ | — |
| File/package mới | ✅ | ✅ | — | ✅ | ✅ |
| Hot-path (layout, render, input) | ✅ | — | — | — | — |
| Task làm đẹp mã | — | — | ✅ | — | — |

---

## Cách Thêm Skill Mới

1. Tạo folder `.agents/skills/<tên-skill>/SKILL.md`
2. YAML frontmatter: `name`, `description`
3. Nội dung SKILL.md gồm:
   - **Tiêu chí đánh giá**: Liệt kê CỤ THỂ từng điều kiện chấm
   - **Ví dụ tốt vs xấu**: Mỗi tiêu chí ít nhất 1 ví dụ code
   - **Hướng dẫn chấm điểm**: Khi nào CRITICAL, khi nào WARNING
   - **Output format**: Dùng format chuẩn ở trên
4. Thêm skill vào bảng "Danh Sách Skills Hiện Có" phía trên
5. Cập nhật GEMINI.md Rule 10 nếu skill là bắt buộc

---

## Skills Mở Rộng Tương Lai (Thêm Khi Cần)

| Skill | Chấm cái gì | Khi nào cần |
|:---|:---|:---|
| `compose-idioms-reviewer` | Composable signature, State hoisting, Modifier.Element contract, side-effects | Khi có >10 composables |
| `performance-reviewer` | Boxing value class, hot-path allocation, scratchpad hygiene | Khi có input/render bugs |
| `test-coverage-reporter` | Liệt kê code paths chưa có test | Khi test suite >30 files |
