# Prompt ANL-04

Kiểm tra và sửa báo cáo doanh thu ngày/tháng/năm.

Bắt buộc:
1. Xác định source of truth và timestamp.
2. Validate granularity DAY/MONTH/YEAR.
3. Group month theo cả year+month.
4. Xử lý timezone.
5. Loại payment không hợp lệ.
6. Không double count do join.
7. Tổng KPI = tổng bảng = tổng chart.
8. Xử lý periods không có dữ liệu.
9. Test ngày cuối tháng, cuối năm và khoảng rỗng.
10. Tối ưu query hợp lý, không load toàn bộ transaction về Java để group nếu DB có thể aggregate.
