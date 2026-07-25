# Phạm vi thay đổi an toàn

## Được phép sửa

- Feedback page/component.
- Component rating con chỉ dùng bởi Feedback.
- CSS/SCSS/module/style cục bộ của Feedback.
- State, validation schema và form model của Feedback.
- API request type/service submit Feedback.
- Backend request DTO/controller/service/repository/entity liên quan trực tiếp nếu cần để bỏ yêu cầu category.
- Test của Feedback và migration nhỏ, có kiểm soát.

## Không được tự ý sửa

- Header, sidebar, authentication, role, course enrollment.
- Component radio/card dùng chung cho toàn hệ thống nếu không tách được ảnh hưởng.
- Enum category dùng ở ticket/support/report khác.
- Database schema ngoài bảng Feedback.
- Global spacing/typography làm thay đổi nhiều trang.
- Nội dung rating, thang điểm 1–5 hoặc logic tính điểm nếu không được yêu cầu.

## Từ khóa nên tìm trong repository

```text
Feedback
feedback
category
feedbackCategory
selectedCategory
Choose a topic
Course Content
Instructor Support
Overall Satisfaction
ratingMatrix
submitFeedback
createFeedback
FeedbackRequest
FeedbackDto
```

## Nguyên tắc diff

- Diff nhỏ, có mục tiêu.
- Xóa code chết sau khi bỏ Category.
- Không để import, state, handler, schema hoặc CSS selector không còn dùng.
- Ưu tiên dùng token/mixin/component sẵn có của dự án.
