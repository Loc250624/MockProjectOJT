# Kiểm tra tương thích API khi bỏ Category

Bỏ Category khỏi UI không đồng nghĩa được phép xóa field ở mọi tầng.

## Checklist điều tra

1. Frontend có khởi tạo `category` trong default form values không?
2. Schema validation có `.required()` / `.min(1)` cho category không?
3. Payload submit có category không?
4. Backend DTO có `@NotNull`, `@NotBlank`, `required=true` hay validation tương đương không?
5. Entity/database column có `nullable = false`, `NOT NULL` hoặc enum bắt buộc không?
6. Admin/report/filter có hiển thị hoặc lọc theo category không?
7. Dữ liệu seed/test fixture có category không?

## Cách xử lý ưu tiên

1. Nếu backend chấp nhận thiếu category: bỏ khỏi payload.
2. Nếu backend bắt buộc nhưng có enum chung hợp lệ: gửi default nội bộ.
3. Chỉ thay đổi backend/database khi hai cách trên không khả thi.

## Không làm

- Không tự phát minh giá trị như `GENERAL` nếu enum không có.
- Không gửi chuỗi rỗng chỉ để qua TypeScript.
- Không đặt `null` nếu backend/database chưa chấp nhận.
- Không xóa cột database khi trang quản trị còn dùng.

## Test tối thiểu

- Request không có category vẫn thành công, hoặc request có default hợp lệ.
- Validation frontend không chặn submit vì category.
- Backend không trả 400/422/500.
- Feedback cũ có category vẫn hiển thị bình thường ở trang quản trị.
