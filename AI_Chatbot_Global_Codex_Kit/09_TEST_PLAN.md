# Test plan

## Frontend unit/state tests

1. Greeting được render trước quick actions.
2. Không có lesson subtitle.
3. Title là AI Chatbot.
4. Placeholder là Ask about this website.
5. Click quick action → action biến mất ngay.
6. Gửi text → action biến mất ngay.
7. API fail → action không xuất hiện lại.
8. Close/reopen → action không xuất hiện lại.
9. New conversation → action xuất hiện đúng một lần.
10. Route change → một widget, conversation giữ nguyên.
11. Header không còn chuỗi AI Tutor.

## E2E route matrix

Mở chatbot và gửi “What can I do on this page?” tại:

- Home.
- Course catalog.
- Login.
- Student dashboard.
- Student lesson.
- Certificates.
- Teacher dashboard.
- Teacher course editor.
- Admin dashboard.

Kỳ vọng: icon hiện, drawer mở, không duplicate, response phù hợp scope.

## Backend tests

- Request chỉ có message + pageContext, không có lessonId → 200.
- Request có lessonId hợp lệ → lesson context được dùng.
- lessonId trái quyền → không lộ nội dung.
- Client gửi role giả → server bỏ qua.
- Anonymous public page → public answer.
- Anonymous hỏi dữ liệu riêng → hướng dẫn login, không lộ dữ liệu.
- Invalid page/entity → safe fallback.
- Rate limit → status/response theo chuẩn hiện tại.
- AI timeout → fallback; không crash page.
- CSRF/security behavior giữ đúng theo kiến trúc hiện tại.

## Regression

- Existing lesson chatbot call vẫn hoạt động.
- OAuth/login không hỏng.
- Enroll/payment/progress không bị thay đổi.
- Student/Teacher/Admin route guard không bị nới lỏng.
- Build frontend/backend thành công.
