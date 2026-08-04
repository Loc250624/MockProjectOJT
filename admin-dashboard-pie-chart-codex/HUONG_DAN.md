# Hướng dẫn sử dụng gói Codex

Gói này được chuẩn bị để Codex sửa đúng ba file liên quan tới biểu đồ của Admin Dashboard, không đụng tới backend và các trang khác.

## Cách giao cho Codex

1. Giải nén gói vào thư mục tạm.
2. Đưa `CODEX_TASK.md` cho Codex làm yêu cầu chính.
3. Cho Codex đọc ảnh trong `reference/`.
4. Codex có thể:
   - áp dụng `patches/0001-admin-dashboard-pie-charts.patch`; hoặc
   - chạy `tools/apply_admin_dashboard_pie.py`.

## Phạm vi thay đổi

- Chỉ hai biểu đồ ở `/admin/dashboard`.
- Student Breakdown: Pie/Donut gồm New students và Active students.
- Revenue Breakdown: tối đa 5 kỳ có doanh thu cao nhất và `Other periods`.
- Các trang Analytics chi tiết vẫn dùng Bar Chart như hiện tại.
- Không thêm thư viện chart mới và không đổi API/database.

## Lệnh kiểm tra đề xuất

```bash
python tools/apply_admin_dashboard_pie.py --root . --check
python tools/apply_admin_dashboard_pie.py --root . --apply
git diff --check
./mvnw test
```
