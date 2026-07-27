Bạn đang làm việc trong repository LUmiNa/OJTSU26 E-Learning. Hãy sửa tính năng AI Tutor hiện có thành AI Chatbot toàn website.

## Quy trình bắt buộc

### Bước 1 – Khảo sát trước khi sửa

Đọc toàn bộ repository và xác định chính xác:

- Frontend thực tế là Thymeleaf, React hay kết hợp.
- Component/fragment hiện đang render AI Tutor.
- File CSS/JS quản lý drawer, icon, input, quick actions và lịch sử chat.
- Layout dùng chung cho public, Student, Teacher, Admin và trang authentication.
- Controller/service/request DTO/client AI/context adapter/rate limiter hiện có.
- API endpoint hiện tại và cách frontend gọi endpoint.
- Security rules, CSRF, CORS, session/OAuth và cách lấy current user.
- Cơ chế lesson context hiện tại và các kiểm tra quyền truy cập lesson.

Dùng các từ khóa:

```text
AI Tutor
AI TUTOR
ai-tutor
AiTutor
Ask about this lesson
I am ready to help with this lesson
Summarize
Explain simply
Give an example
Quiz me
lessonId
lesson-context
OpenAI
Responses API
```

Không được bắt đầu bằng việc tạo một chatbot mới song song khi chatbot hiện tại đã tồn tại.

### Bước 2 – Lập danh sách file sẽ sửa

Trước khi viết code, ghi rõ:

- File nào sửa.
- Lý do sửa.
- File nào chỉ đọc để tham chiếu.
- Tại sao thay đổi không ảnh hưởng module ngoài chatbot.

### Bước 3 – Thực hiện thay đổi tối thiểu

#### Giao diện

- Đổi mọi chuỗi hiển thị “AI Tutor” thành “AI Chatbot”.
- Xóa subtitle là tên lesson bên dưới tiêu đề drawer.
- Không thay subtitle bằng tên trang khác.
- Trong conversation body, render tin nhắn chào của assistant trước.
- Sau tin nhắn chào mới render bốn quick actions.
- Quick actions chỉ hiển thị khi:
  - greeting đã được tạo;
  - cuộc hội thoại chưa có tương tác của người dùng;
  - quick actions chưa bị dismiss/consume.
- Sau khi người dùng bấm quick action hoặc gửi tin nhắn đầu tiên, ẩn toàn bộ quick actions.
- Khi đóng rồi mở lại cùng cuộc hội thoại, quick actions không được hiện lại.
- Chỉ hiện lại khi có một conversation mới thật sự. Nếu hệ thống chưa có chức năng New chat, dùng trạng thái theo `conversationId`; fallback dùng `sessionStorage` theo user/session.
- Placeholder input đổi từ “Ask about this lesson” thành nội dung toàn cục như “Ask about this website”.
- Icon mở chatbot phải có tại tất cả trang, được mount từ shared root layout/app shell.
- Tránh mount trùng khi layout lồng nhau.
- Drawer phải responsive, hỗ trợ keyboard, focus management, Escape và aria-label.

#### Phạm vi trả lời

Chuyển từ lesson-only sang site-wide, page-aware và role-aware:

- Có thể trả lời về cách dùng toàn website: course, lesson, quiz, coding assignment, certificate, enrollment, payment, blog, profile, dashboard và navigation.
- Trên trang lesson, vẫn có thể dùng lesson context nếu người dùng có quyền.
- Trên trang khác, không bắt buộc có `lessonId`.
- Nhận metadata trang hiện tại ở dạng có kiểm soát: pathname, page key, entity type, entity id.
- Không gửi toàn bộ HTML/DOM hoặc token/cookie từ browser lên model.
- Backend tự xác định user và role; không tin role gửi từ client.
- Chỉ lấy dữ liệu mà user hiện tại được phép đọc.
- Anonymous chỉ nhận public website context và hướng dẫn đăng nhập khi cần dữ liệu riêng.
- Chatbot mặc định read-only. Không tự enroll, đổi điểm, đánh dấu completed, publish course, tạo payment hoặc thay đổi dữ liệu.
- Không tiết lộ dữ liệu user khác, answer key bị ẩn, course chưa publish hoặc nội dung ngoài quyền.

#### API

Ưu tiên mở rộng API hiện tại thay vì phá bỏ:

- `lessonId` trở thành optional.
- Thêm `conversationId` và `pageContext`.
- Giữ tương thích ngược với request cũ nếu hợp lý.
- Có thể thêm route mới `/api/ai-chatbot/chat`, nhưng giữ alias hoặc migration an toàn cho route cũ trong giai đoạn chuyển đổi.
- Không đổi cấu hình secret hiện có:
  - `app.ai-tutor.api-key=${OPENAI_API_KEY:}`
  - `app.ai-tutor.base-url=${OPENAI_BASE_URL:https://api.openai.com/v1}`
- Không hard-code API key.
- Giữ rate limiting, timeout, error fallback và logging không chứa dữ liệu nhạy cảm.

### Bước 4 – Test

Phải test ít nhất:

- Public page.
- Login/Register.
- Student dashboard.
- Student lesson page.
- Student enrolled/completed course page.
- Teacher dashboard/course editor.
- Admin dashboard.
- Mobile width.
- Close/reopen drawer.
- Gửi tin nhắn thường.
- Bấm từng quick action.
- Anonymous request.
- Authenticated request.
- Unauthorized entity id.
- AI timeout/error.
- Route navigation không reload nếu frontend là SPA.

### Bước 5 – Báo cáo

Kết thúc bằng:

- Danh sách file đã thay đổi.
- Tóm tắt diff.
- Lệnh test đã chạy và kết quả.
- Các giả định.
- Những việc chưa làm.
- Xác nhận không có secret bị commit.
- Xác nhận không sửa module ngoài phạm vi.

## Điều cấm

- Không disable CSRF toàn cục.
- Không mở toàn bộ API bằng `permitAll`.
- Không gửi API key ra frontend.
- Không bỏ kiểm tra quyền lesson/course.
- Không query toàn bộ database rồi đưa vào prompt.
- Không để client tự khai role.
- Không làm quick actions xuất hiện sau mỗi response.
- Không mount nhiều widget trên cùng trang.
- Không đổi kiến trúc dự án hoặc dependency lớn nếu không cần.
- Không thay đổi grade/progress/completion chỉ để chatbot trả lời được.
