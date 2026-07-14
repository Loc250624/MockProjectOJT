# Prompt ANL-01

Kiểm tra và sửa chức năng Teacher Personal Revenue.

Bắt buộc:
1. Xác định route và file UI thực tế.
2. Xác định nguồn doanh thu chính xác.
3. Lấy teacher từ authenticated user, không tin teacherId client.
4. Chỉ tính dữ liệu của course thuộc teacher.
5. Loại giao dịch không thành công.
6. Kiểm tra multi-course/multi-teacher order để không tính sai.
7. Đồng bộ KPI, chart, table và date filter.
8. Thêm empty/loading/error state.
9. Test RBAC và IDOR.
10. Tạo test chống tái phát.

Không sửa trước khi tạo báo cáo baseline cho ANL-01.
