# Prompt cho ASM-06 — Grading workflow

## Thông tin backlog
- ID: `ASM-06`
- Module: `Assessment Management`
- Chức năng: Chấm điểm và nhận xét bài tập tự luận/thực hành
- Actor: Teacher
- Priority: P1
- Target Week: W6
- Primary Owner: Cường
- Reviewer: Cường
- Effort: 5 SP
- Dependencies: ASM-02
- Acceptance/Deliverable: Grading workflow

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

Bạn là Senior Fullstack Engineer. Hãy hoàn thành task `ASM-06`: Teacher chấm điểm và nhận xét bài tập tự luận/thực hành.

### Phạm vi
Triển khai grading workflow cho Teacher:
1. Teacher xem danh sách submissions của assignment.
2. Teacher lọc theo trạng thái: Submitted, Auto Graded, Graded, Returned.
3. Teacher mở chi tiết submission.
4. Teacher nhập điểm và nhận xét.
5. Teacher lưu grade.
6. Student xem được score/feedback ở Result screen.
7. Có thể trả bài về cho Student chỉnh sửa nếu workflow hỗ trợ.

### Backend cần làm
- Kiểm tra entity hiện có. Nếu thiếu/cần bổ sung:
  - `Submission`: score, feedback, status, gradedAt, gradedBy.
  - Hoặc `GradeFeedback`: submission, teacher, score, feedback, gradedAt.
- Service rule:
  - Teacher chỉ chấm submission thuộc assignment/course mình phụ trách.
  - Score phải nằm trong 0..maxScore.
  - Không chấm submission chưa submit, trừ draft bị trả về nếu có rule riêng.
  - Khi grade thành công: status -> `GRADED`.
  - Nếu return: status -> `RETURNED`, feedback required.
  - Nếu đã có auto judge result, manual grade có thể override hoặc combine theo rule rõ ràng. Nếu chưa có rule, manual grade là điểm cuối cùng.
- API gợi ý:
  - `GET /api/teacher/assignments/{assignmentId}/submissions`.
  - `GET /api/teacher/submissions/{submissionId}`.
  - `POST /api/teacher/submissions/{submissionId}/grade`.
  - `POST /api/teacher/submissions/{submissionId}/return`.
- View route:
  - `GET /teacher/assignments/{assignmentId}/submissions`.
  - `GET /teacher/submissions/{submissionId}/grade`.

### Frontend cần làm
- Trang danh sách submissions:
  - Table/card: Student, submittedAt, status, score, action Grade.
  - Filter/search theo tên student/status.
  - Empty state khi chưa có bài nộp.
- Trang grading detail:
  - Hiển thị nội dung bài làm: text/code/file link.
  - Hiển thị judge result nếu có.
  - Form nhập score, feedback.
  - Nút Save Grade, Return for Revision nếu hỗ trợ.
  - Confirm trước khi lưu.
- HTML/CSS/JS thuần, đồng bộ Teacher portal.
- Responsive: table chuyển thành card trên mobile.

### Validation và security
- Teacher không được chấm submission ngoài course của mình.
- Server validate score và feedback.
- Escape nội dung submission/code/output log.
- File download nếu có phải kiểm tra quyền.
- Student không gọi được API grade.

### Acceptance criteria
- Teacher xem submissions của assignment được.
- Teacher chấm điểm và ghi nhận xét được.
- Status submission cập nhật đúng.
- Student xem lại feedback/score ở result screen.
- Không chấm được bài của course không thuộc Teacher.
- UI responsive và không lỗi console.

### Sau khi hoàn thành, báo cáo
- File đã tạo/sửa.
- API route mới.
- Quy tắc manual grade với auto judge.
- Cách test Teacher grading + Student result.
