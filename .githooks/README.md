# NekoMod Git Hooks Infrastructure

Thư mục này chứa các script Git Hooks tự động nhằm bảo vệ chất lượng mã nguồn, quy chuẩn lập trình và tính toàn vẹn kiến trúc của dự án NekoMod v3.

---

## 📌 Các Cơ Chế Kiểm Tra Trong `pre-commit`

Mỗi khi thực hiện `git commit`, hook `pre-commit` sẽ tự động kích hoạt 4 lớp bảo vệ:

1. **Khóa Kiến Trúc (Architectural Lockdown)**:
   - Ngăn chặn việc sửa đổi hoặc commit vào các module cốt lõi đã đạt trạng thái **🟢 Stable** (`core/layout/`, `core/graphics/`, `core/compose/`).
   - Có thể mở khóa có chủ đích bằng cờ môi trường: `ALLOW_LOCKED_MODIFICATIONS=1 git commit` hoặc `--no-verify`.

2. **Quy Chuẩn Đặt Tên PascalCase (Coding Conventions)**:
   - Quét toàn bộ các tệp `.kt` trong staging để phát hiện các khai báo `class`, `object`, `interface` bắt đầu bằng chữ thường.

3. **Khớp Nối Package & Thư Mục (Package Path Integrity)**:
   - Đảm bảo khai báo `package org.hubdustry...` khớp 100% với cấu trúc thư mục vật lý chứa tệp tin.

4. **Kiểm Tra Biên Dịch Tự Động (Zero Compile Errors Gate)**:
   - Chạy `compileKotlin` và `compileTestKotlin` trong chế độ quiet để ngăn chặn tuyệt đối việc commit mã nguồn lỗi cú pháp hoặc lỗi kiểu.

---

## 🚀 Kích Hoạt Hooks Trên Môi Trường Local

Để kích hoạt Git sử dụng các hooks trong thư mục này, chỉ cần chạy một lần duy nhất:

### Cách 1: Chạy script PowerShell
```powershell
.\setup-hooks.ps1
```

### Cách 2: Chạy lệnh Git trực tiếp
```bash
git config core.hooksPath .githooks
```
