# Prompt cho ASM-02 — Submission flow

## Thông tin backlog
- ID: `ASM-02`
- Module: `Assessment`
- Chức năng: Học viên nộp bài thực hành/bài tập code
- Actor: Student
- Priority: P1
- Target Week: W6
- Primary Owner: Cường
- Reviewer: Lộc
- Effort: 5 SP
- Dependencies: LRN-01
- Acceptance/Deliverable: Submission flow

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

Bạn là Senior Fullstack Engineer. Hãy hoàn thành task `ASM-02`: Student nộp bài thực hành/bài tập code.

### Phạm vi
Triển khai submission flow cho học viên:
1. Student mở bài tập thuộc course/lesson đã enroll.
2. Student nhập nội dung bài làm: text, code hoặc upload file tùy cấu hình assignment.
3. Student có thể lưu draft.
4. Student submit bài chính thức.
5. Sau submit, hệ thống lưu trạng thái, thời gian nộp và hiển thị thông báo thành công.

### Backend cần làm
- Kiểm tra entity sẵn có. Nếu thiếu, tạo/cập nhật:
  - `Assignment`: title, description, submissionType, dueDate, maxScore, course/lesson, status.
  - `Submission`: assignment, student, contentText, codeLanguage, codeContent, filePath/fileUrl, status, submittedAt.
- Service rule:
  - Chỉ Student đã enroll mới xem/nộp assignment.
  - Không cho submit assignment chưa published/không thuộc course đã enroll.
  - Nếu quá hạn, xử lý theo rule: cho submit late với flag `late` hoặc chặn, tùy convention hiện có. Nếu chưa có rule, mặc định cho submit late nhưng đánh dấu rõ.
  - Mỗi Student có một submission chính cho một assignment, hoặc versioning nếu source hiện có hỗ trợ.
  - Không cho Student sửa submission sau khi đã `SUBMITTED`, trừ khi Teacher trả về `RETURNED`.
- API gợi ý:
  - `GET /api/student/assignments/{assignmentId}`.
  - `POST /api/student/assignments/{assignmentId}/submissions/draft`.
  - `POST /api/student/assignments/{assignmentId}/submissions/submit`.
  - `GET /api/student/submissions/{submissionId}`.
- View route:
  - `GET /student/assignments/{assignmentId}/submit`.

### Frontend cần làm
- Màn hình submission bằng HTML/CSS/JS thuần:
  - Thông tin assignment: title, description, deadline, max score, submission type.
  - Editor text/code đơn giản bằng `<textarea>`.
  - Dropdown chọn language nếu bài code: Java, JavaScript, Python, C++ hoặc theo project.
  - Upload file nếu type cho phép.
  - Hiển thị file đã chọn, size, loại file.
  - Nút Save Draft và Submit.
  - Trạng thái: Draft, Submitted, Late, Returned.
- JS:
  - Validate required fields.
  - Nếu upload file: dùng `FormData`.
  - Nếu text/code: dùng JSON hoặc FormData theo endpoint.
  - Hiển thị loading, success, error rõ ràng.
- CSS:
  - Đồng bộ style Student portal hiện có.
  - Responsive mobile, không vỡ layout khi nội dung bài dài.

### Validation và security
- Giới hạn kích thước file theo cấu hình project hoặc đặt mặc định hợp lý.
- Chỉ cho phép extension an toàn cho bài tập: `.txt`, `.pdf`, `.docx`, `.zip`, `.java`, `.js`, `.py`, `.cpp` nếu project cần.
- Không render HTML raw từ student submission.
- Không cho path traversal khi lưu file.
- Server xác thực student owner của submission.

### Acceptance criteria
- Student xem assignment được.
- Student lưu draft được.
- Student submit bài text/code/file được.
- Reload lại vẫn thấy submission đã lưu.
- Sau submit hiển thị trạng thái và thời gian nộp.
- Không nộp được nếu không enroll.
- UI không lỗi console và responsive.

### Sau khi hoàn thành, báo cáo
- File đã tạo/sửa.
- API route mới.
- Cấu hình upload/migration nếu có.
- Cách test từng loại submission.
