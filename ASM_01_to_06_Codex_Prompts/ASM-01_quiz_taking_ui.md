# Prompt cho ASM-01 — Quiz taking UI

## Thông tin backlog
- ID: `ASM-01`
- Module: `Assessment`
- Chức năng: Học viên làm bài trắc nghiệm, lưu tạm và nộp bài
- Actor: Student
- Priority: P0
- Target Week: W6
- Primary Owner: Cường
- Reviewer: Lộc
- Effort: 5 SP
- Dependencies: LRN-01
- Acceptance/Deliverable: Quiz taking UI

# Project Context

Bạn đang làm trong dự án E-learning fullstack dùng Spring Boot và giao diện HTML/CSS/JavaScript thuần.

## Ràng buộc kỹ thuật bắt buộc
- Backend: Spring Boot, Spring MVC, Spring Security/RBAC, JPA/Hibernate, MySQL.
- Frontend: chỉ dùng HTML, CSS, JavaScript thuần. Không dùng React, Vue, Angular, Next.js, Bootstrap nếu project hiện tại không dùng.
- Ưu tiên giữ kiến trúc hiện có: Controller -> Service -> Repository -> Entity/DTO.
- Không refactor hoặc đổi UI các module không liên quan.
- Không xóa code cũ nếu chưa chắc chắn; chỉ sửa đúng phạm vi task.
- Không hard-code userId/courseId/lessonId; lấy từ session/security context và route hiện có.
- Student chỉ truy cập dữ liệu của khóa học đã enroll.
- Teacher chỉ quản lý assessment của khóa học mình phụ trách.
- Admin/System không được bị ảnh hưởng bởi màn hình Student/Teacher.
- Tất cả form phải có validation phía client và server.
- Tất cả API POST/PUT/DELETE phải xử lý lỗi rõ ràng và trả response thống nhất theo convention hiện có.
- Sau khi sửa xong phải chạy test/build phù hợp và ghi rõ file đã thay đổi.

## Quy ước khi Agent thực hiện
1. Đầu tiên đọc source hiện tại để xác định cấu trúc package, entity, route, template, static asset đang có.
2. Tìm các entity sẵn có như Course, Lesson, Enrollment, User, Assignment, Quiz, Submission. Nếu chưa có thì tạo mới tối thiểu, không phá database cũ.
3. Nếu project đang dùng Flyway/Liquibase thì tạo migration mới. Nếu không dùng migration thì cập nhật entity + schema/data seed theo cách project hiện tại đang dùng.
4. Chia nhỏ commit/patch theo từng layer: database/entity, repository, service, controller/API, template, CSS/JS, tests.
5. Không làm task ngoài phạm vi mã ASM đang được yêu cầu.

## Copy-paste prompt cho Codex/Agent

Bạn là Senior Fullstack Engineer. Hãy hoàn thành task `ASM-01`: Student làm bài trắc nghiệm, có thể lưu tạm và nộp bài.

### Phạm vi
Triển khai luồng làm quiz cho học viên đã đăng nhập và đã enroll khóa học:
1. Student mở màn hình làm quiz từ lesson/course.
2. Hệ thống tạo hoặc mở lại attempt đang `DRAFT`.
3. Student chọn đáp án cho từng câu hỏi.
4. Student có thể lưu tạm thủ công và tự động lưu định kỳ.
5. Student nộp bài.
6. Sau khi submit, attempt chuyển trạng thái `SUBMITTED` hoặc `GRADED` nếu chấm tự động được.
7. Không cho sửa answer sau khi đã submit.

### Backend cần làm
- Kiểm tra source hiện có trước. Nếu chưa có entity quiz/attempt/answer thì tạo các entity tối thiểu:
  - `Quiz`, `QuizQuestion`, `QuizOption`, `QuizAttempt`, `QuizAnswer`.
- Tạo repository/service:
  - Kiểm tra quyền: chỉ Student đã enroll course mới được mở quiz.
  - Không cho Student truy cập quiz chưa published.
  - Không tạo quá số lần làm bài nếu có `maxAttempts`.
  - Lưu draft không tính điểm.
  - Submit tính điểm tự động cho SINGLE_CHOICE/MULTIPLE_CHOICE.
  - Tổng điểm = tổng điểm câu hỏi; điểm đạt = điểm câu trả lời đúng.
- API gợi ý:
  - `POST /api/student/quizzes/{quizId}/attempts` tạo hoặc lấy attempt draft.
  - `GET /api/student/quiz-attempts/{attemptId}` lấy attempt + questions/options.
  - `PUT /api/student/quiz-attempts/{attemptId}/draft` lưu đáp án tạm.
  - `POST /api/student/quiz-attempts/{attemptId}/submit` nộp bài.
- View route gợi ý:
  - `GET /student/courses/{courseId}/quizzes/{quizId}/take`.

### Frontend cần làm
- Tạo màn hình quiz taking bằng HTML/CSS/JS thuần:
  - Header: tên quiz, course/lesson, thời lượng, trạng thái lưu.
  - Sidebar hoặc progress bar: số câu đã trả lời / tổng số câu.
  - Question card: câu hỏi, điểm, danh sách option.
  - Nút: Save Draft, Submit, Next/Previous.
  - Timer nếu quiz có thời lượng.
  - Confirm modal trước khi submit.
  - Auto-save mỗi 15-30 giây và khi đổi câu.
  - Khi mất mạng/API lỗi: hiển thị thông báo, không làm mất đáp án đang chọn.
- Không dùng React/Vue/Angular. Chỉ vanilla JS + fetch.
- Nếu project đã có layout Student, reuse layout đó.

### Validation và security
- Student không được gửi answer cho question không thuộc quiz.
- Student không được submit attempt của người khác.
- Không cho submit attempt rỗng nếu quiz yêu cầu trả lời.
- Server phải validate lại toàn bộ selected option, không tin dữ liệu client.
- CSRF/JWT/session phải theo cơ chế hiện có của project.

### Acceptance criteria
- Student vào quiz thành công nếu đã enroll.
- Student chọn đáp án và lưu draft được.
- Reload trang vẫn lấy lại draft.
- Submit quiz thành công và không sửa được sau submit.
- Điểm được tính đúng với câu trắc nghiệm.
- UI responsive và không lỗi console.
- Có test service/controller hoặc checklist test thủ công.

### Sau khi hoàn thành, báo cáo
- File đã tạo/sửa.
- API route mới.
- Cách test bằng browser/Postman.
- Những giả định về entity/database nếu có.
