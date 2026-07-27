# Yêu cầu và phạm vi

## Thay đổi trực tiếp từ thiết kế hiện tại

| Hiện tại | Sau khi sửa |
|---|---|
| Header: AI Tutor | Header: AI Chatbot |
| Có subtitle tên lesson | Không có subtitle |
| Quick actions nằm trước greeting | Greeting nằm trước quick actions |
| Quick actions có thể xuất hiện lại | Chỉ xuất hiện một lần mỗi conversation |
| Input: Ask about this lesson | Input: Ask about this website |
| Chat bắt buộc gắn lesson | Lesson context là optional |
| Widget chỉ có ở khu vực học | Widget có ở mọi layout/page |
| Scope trả lời là lesson | Scope trả lời là toàn website, theo quyền |

## Nội dung greeting đề xuất

```text
I’m ready to help you use this website. You can ask about courses, lessons, quizzes, coding assignments, certificates, enrollment, payments, or the page you are viewing.
```

Có thể dùng ngôn ngữ theo locale của trang. Không được nhắc riêng “this lesson” khi người dùng đang ở trang khác.

## Quick actions đề xuất

- Summarize this page
- Explain this page simply
- Show me how to use this feature
- Quiz me on what I’m learning

Quick action phải dựa vào current page. Ở trang không có nội dung học tập, action “Quiz me” có thể trả lời rằng trang hiện tại không có nội dung phù hợp và gợi ý mở lesson; không được lỗi.

## Ngoài phạm vi

- Không xây agent thực hiện giao dịch.
- Không tự động sửa course/lesson/quiz.
- Không thay đổi payment flow.
- Không thay đổi progress/completion.
- Không thiết kế lại toàn bộ website.
- Không migration database nếu conversation hiện tại không cần lưu lâu dài.
