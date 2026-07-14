# Prompt SYS-01

Kiểm tra và sửa trang cấu hình thông số hệ thống chung.

Bắt buộc:
1. Xác định settings lưu ở đâu.
2. Chỉ Admin truy cập và cập nhật.
3. Inventory tất cả key đang hỗ trợ.
4. Whitelist key editable.
5. Validate theo type/range/format.
6. Không render hoặc log secret plaintext.
7. Xử lý missing key bằng default an toàn.
8. Cập nhật updatedAt/updatedBy nếu kiến trúc có.
9. Xử lý cache/effective time và ghi rõ cần restart hay không.
10. Sửa UI thành form nhóm theo category, responsive, có help text và success/error state.
11. Test CSRF, invalid input, unauthorized update.
