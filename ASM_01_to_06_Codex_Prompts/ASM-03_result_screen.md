# Prompt cho ASM-03 — Result screen

## Thông tin backlog
- ID: `ASM-03`
- Module: `Assessment`
- Chức năng: Học viên xem kết quả trắc nghiệm và kết quả chấm code
- Actor: Student
- Priority: P1
- Target Week: W6
- Primary Owner: Cường
- Reviewer: Lộc
- Effort: 3 SP
- Dependencies: ASM-01,ASM-02
- Acceptance/Deliverable: Result screen

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

Bạn là Senior Fullstack Engineer. Hãy hoàn thành task `ASM-03`: Student xem kết quả trắc nghiệm và kết quả chấm code.

### Dependencies
Task này phụ thuộc `ASM-01` và `ASM-02`. Trước khi làm, hãy kiểm tra dữ liệu/result từ quiz attempt và submission đã tồn tại chưa.

### Phạm vi
Triển khai màn hình kết quả cho Student:
1. Xem danh sách kết quả assessment trong course.
2. Xem chi tiết kết quả quiz.
3. Xem chi tiết kết quả bài code/thực hành.
4. Hiển thị điểm, trạng thái, feedback, testcase result nếu có.
5. Không cho Student xem kết quả của người khác.

### Backend cần làm
- Tạo DTO/read model cho kết quả:
  - Quiz result: attemptId, quizTitle, score, totalPoints, percentage, status, submittedAt, question results.
  - Submission result: submissionId, assignmentTitle, score, maxScore, status, submittedAt, feedback, judge result.
  - Code judge result: passedTests, totalTests, status, outputLog rút gọn, executionTime.
- Service rule:
  - Student chỉ xem result của chính mình.
  - Nếu quiz chưa được phép xem đáp án, chỉ hiển thị score/status. Nếu chưa có config, mặc định cho xem câu đúng/sai sau khi submit.
  - Với hidden testcase, Student chỉ thấy pass/fail tổng quát, không thấy input/expected output nếu `isHidden=true`.
- API gợi ý:
  - `GET /api/student/results`.
  - `GET /api/student/quiz-attempts/{attemptId}/result`.
  - `GET /api/student/submissions/{submissionId}/result`.
- View route:
  - `GET /student/results`.
  - `GET /student/quizzes/{attemptId}/result`.
  - `GET /student/submissions/{submissionId}/result`.

### Frontend cần làm
- Trang danh sách result:
  - Filter theo course, type: Quiz/Assignment/Code.
  - Card/table hiển thị title, type, score, status, submittedAt, action View Detail.
- Trang quiz result:
  - Score summary card.
  - Danh sách câu hỏi, đáp án đã chọn, đúng/sai, điểm từng câu.
  - Badge trạng thái: Submitted/Graded.
- Trang submission/code result:
  - Score, status, feedback teacher.
  - Testcase summary: passed/total, status.
  - Output log rút gọn có nút expand nếu dài.
- UI responsive, đồng bộ Student portal, HTML/CSS/JS thuần.

### Validation và security
- Không expose đáp án đúng nếu policy không cho phép.
- Không expose hidden testcase input/expected output.
- Không expose submission của student khác.
- Escape output log và content để tránh XSS.

### Acceptance criteria
- Student xem được danh sách kết quả của mình.
- Quiz result hiển thị đúng điểm và câu đúng/sai.
- Submission result hiển thị đúng feedback/score/status.
- Code judge result hiển thị tổng testcase pass/fail nếu có.
- Không xem được result của user khác.
- UI responsive, có empty state khi chưa có result.

### Sau khi hoàn thành, báo cáo
- File đã tạo/sửa.
- API route mới.
- Cách test với dữ liệu ASM-01/ASM-02.
- Những policy hiển thị đáp án/testcase đã áp dụng.
