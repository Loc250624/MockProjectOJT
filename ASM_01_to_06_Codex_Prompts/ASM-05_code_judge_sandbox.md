# Prompt cho ASM-05 — Code judge sandbox

## Thông tin backlog
- ID: `ASM-05`
- Module: `Assessment Management`
- Chức năng: Cấu hình testcase và chấm code tự động qua adapter dịch vụ
- Actor: Teacher/System
- Priority: P2
- Target Week: W8
- Primary Owner: Cường
- Reviewer: Cường
- Effort: 8 SP
- Dependencies: ASM-02
- Acceptance/Deliverable: Code judge sandbox

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

Bạn là Senior Fullstack Engineer. Hãy hoàn thành task `ASM-05`: Teacher cấu hình testcase và hệ thống chấm code tự động qua adapter dịch vụ.

### Cảnh báo kiến trúc bắt buộc
Không chạy code người dùng trực tiếp trong Spring Boot application bằng `Runtime.exec`, `ProcessBuilder` hoặc command line host nếu chưa có sandbox thật. Hãy thiết kế qua adapter:
- `CodeJudgeAdapter` interface.
- `MockCodeJudgeAdapter` hoặc `LocalSafeCodeJudgeAdapter` để mô phỏng/chấm đơn giản trong môi trường dev.
- Có thể chuẩn bị cấu trúc cho Judge0/Docker sandbox về sau, nhưng không hard-code secret/API key.
- Nếu project chưa có sandbox an toàn, hãy triển khai mock adapter + trạng thái/result để demo chức năng.

### Phạm vi
1. Teacher cấu hình testcase cho assignment code.
2. Teacher chọn ngôn ngữ được hỗ trợ nếu cần.
3. System/Teacher trigger auto judge cho submission code.
4. Hệ thống lưu judge result.
5. Student/Teacher xem trạng thái pass/fail tổng quan.
6. Hidden testcase không lộ input/expected output cho Student.

### Backend cần làm
- Kiểm tra entity hiện có. Nếu thiếu, tạo/cập nhật:
  - `TestCase`: assignment, input, expectedOutput, isHidden, points, displayOrder.
  - `CodeJudgeResult`: submission, status, totalTests, passedTests, score, outputLog, executionTimeMs.
  - Có thể thêm `TestCaseResult` nếu cần: testcaseId, passed, actualOutput, errorMessage.
- Service:
  - `TestCaseService`: CRUD testcase, validate assignment ownership.
  - `CodeJudgeService`: nhận submission, gọi adapter, tính điểm theo testcase.
  - `CodeJudgeAdapter`: interface trả result theo input/testcases.
- Adapter:
  - Nếu chưa có external judge an toàn: dùng mock adapter.
  - Mock adapter có thể so sánh output giả lập hoặc trả deterministic result dựa trên content để demo, nhưng phải ghi rõ là mock.
- API gợi ý:
  - `GET /api/teacher/assignments/{assignmentId}/testcases`.
  - `POST /api/teacher/assignments/{assignmentId}/testcases`.
  - `PUT /api/teacher/testcases/{testCaseId}`.
  - `DELETE /api/teacher/testcases/{testCaseId}`.
  - `POST /api/teacher/submissions/{submissionId}/judge`.
  - `GET /api/teacher/submissions/{submissionId}/judge-result`.
- View route:
  - `GET /teacher/assignments/{assignmentId}/testcases`.

### Frontend cần làm
- Màn hình Testcase Builder bằng HTML/CSS/JS thuần:
  - Danh sách testcase: input, expected output, points, hidden/public.
  - Form thêm/sửa testcase.
  - Nút Run Judge cho một submission hoặc link sang submission list nếu đã có.
  - Badge trạng thái judge: Pending, Running, Passed, Failed, Error.
- UI phải có cảnh báo cho Teacher rằng hidden testcase không hiển thị với Student.
- Responsive và đồng bộ Teacher portal.

### Validation và security
- Teacher chỉ cấu hình testcase cho assignment thuộc course mình quản lý.
- Không expose hidden testcase cho Student API.
- Không lưu secret trong code.
- Không chạy arbitrary code trên host app.
- Nếu có upload/code content, escape output log để tránh XSS.
- Có timeout/status nếu adapter lỗi.

### Acceptance criteria
- Teacher thêm/sửa/xóa testcase được.
- Teacher chạy auto judge cho submission code được.
- Hệ thống lưu judge result.
- Result có passedTests/totalTests/status/score.
- Hidden testcase không lộ cho Student.
- Không có command execution không an toàn trong code.
- UI responsive và không lỗi console.

### Sau khi hoàn thành, báo cáo
- File đã tạo/sửa.
- Adapter đang là mock hay external.
- API route mới.
- Cách test testcase + judge result.
- Hạn chế bảo mật còn lại nếu chưa có sandbox thật.
