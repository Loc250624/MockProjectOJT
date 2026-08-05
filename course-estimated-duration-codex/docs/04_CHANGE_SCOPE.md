# Change Scope

## Có thể sửa

- Course/Lesson entity nếu duration chưa tồn tại.
- Migration liên quan duy nhất đến duration video.
- Course/Lesson repository.
- Course service.
- Course DTO/response/view model.
- Mapper.
- Controller chỉ để truyền dữ liệu cần thiết.
- Course card/detail/student course templates hoặc component dùng chung.
- Unit/repository/integration tests liên quan.

## Không được sửa

- Payment.
- Quiz attempts/question bank.
- OAuth/login.
- Profile.
- AI chatbot.
- Blog.
- Feedback.
- Certificate.
- Header/footer ngoài phần metadata course.
- Theme system.
- Cấu trúc database không liên quan.

## Nguyên tắc diff

- Diff nhỏ nhất có thể.
- Không đổi format toàn file.
- Không rename hàng loạt.
- Không thêm dependency khi Java/JPA hiện tại xử lý được.
- Không xoá code chỉ vì không dùng trong tính năng này.
