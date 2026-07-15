# Problem Statement

## Phạm vi lỗi chính

### Teacher > Grading

Ảnh hiện trạng cho thấy:

- Sidebar Teacher render được.
- Khu vực main content không có nội dung hữu ích.
- Không có danh sách bài cần chấm, bộ lọc, chi tiết submission hoặc form chấm điểm.
- Có thể liên quan đến route/controller không trả đúng template, model rỗng, fragment sai, CSS ẩn nội dung hoặc JavaScript lỗi.

Codex phải xác định nguyên nhân bằng code và log, không được kết luận chỉ dựa trên ảnh.

### Teacher > Assignments

Ảnh hiện trạng cho thấy:

- Trang hiển thị theo style mặc định của trình duyệt.
- Header/sidebar/footer bị render như HTML thô, không theo layout Teacher chuẩn.
- Bảng chỉ có một dòng dữ liệu kiểu mẫu: `Student A`, `Java`, `Assign 1`.
- Nút `Grade` chưa chứng minh có workflow hoạt động.
- Khả năng cao có lỗi layout fragment/static resources và hard-code dữ liệu.

## Mục tiêu

1. Hai trang Grading và Assignments phải render thống nhất với Instructor Portal.
2. Dữ liệu phải lấy từ database theo course Teacher sở hữu.
3. Teacher có thể xem submission, chấm điểm, nhận xét, lưu nháp và công bố kết quả phù hợp với mô hình hiện có.
4. Teacher có thể quản lý quiz/question/answer và code testcase.
5. Student xem được kết quả đúng quyền sau khi Teacher/System chấm.
6. Toàn bộ luồng ASM-01 đến ASM-06 không bị đứt đoạn.
