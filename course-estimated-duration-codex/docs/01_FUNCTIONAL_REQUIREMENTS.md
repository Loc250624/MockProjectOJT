# Functional Requirements

## FR-01 — Tổng thời lượng theo course

Hệ thống tính tổng thời lượng video độc lập cho từng course.

## FR-02 — Chỉ tính video lesson

Một lesson chỉ được cộng khi được xác định là video bằng mô hình dữ liệu chuẩn của dự án.

## FR-03 — Không tính dữ liệu ngoài phạm vi

Quiz, text, resource, document, assignment và lesson của course khác không được cộng.

## FR-04 — Hiển thị nhất quán

Dữ liệu được cung cấp qua DTO/view model dùng chung và hiển thị trên các màn hình course dành cho người học.

## FR-05 — Luôn cập nhật

Kết quả phản ánh thay đổi lesson/video mới nhất, không phụ thuộc dữ liệu tổng bị stale.

## FR-06 — Hỗ trợ dữ liệu thiếu

Course không có duration hợp lệ trả về 0 và chuỗi `Chưa có thời lượng video`.

## FR-07 — Hiệu năng

Trang danh sách course không phát sinh N+1 query.

## FR-08 — An toàn dữ liệu

Không ghi đè duration cũ, không tạo duration giả và không sửa quan hệ course/lesson.
