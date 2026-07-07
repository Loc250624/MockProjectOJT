# Prompt cho ASM-04 — Quiz builder

## Thông tin backlog
- ID: `ASM-04`
- Module: `Assessment Management`
- Chức năng: Giảng viên tạo, cập nhật và xóa Quiz/Câu hỏi/Đáp án
- Actor: Teacher
- Priority: P0
- Target Week: W6
- Primary Owner: Cường
- Reviewer: Cường
- Effort: 5 SP
- Dependencies: CRS-06
- Acceptance/Deliverable: Quiz builder

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

Bạn là Senior Fullstack Engineer. Hãy hoàn thành task `ASM-04`: Teacher tạo, cập nhật và xóa Quiz/Câu hỏi/Đáp án.

### Phạm vi
Triển khai Quiz Builder cho Teacher:
1. Teacher xem danh sách quiz của course/lesson mình phụ trách.
2. Teacher tạo quiz mới.
3. Teacher cập nhật thông tin quiz.
4. Teacher thêm/sửa/xóa câu hỏi.
5. Teacher thêm/sửa/xóa đáp án.
6. Teacher đánh dấu đáp án đúng.
7. Teacher publish/unpublish hoặc draft quiz.
8. Teacher preview quiz trước khi publish.

### Backend cần làm
- Kiểm tra entity hiện có. Nếu thiếu, tạo/cập nhật:
  - `Quiz`, `QuizQuestion`, `QuizOption`.
- Service rule:
  - Teacher chỉ quản lý quiz trong course mình sở hữu/phụ trách.
  - Quiz phải thuộc course hoặc lesson hợp lệ.
  - Không publish quiz nếu chưa có câu hỏi.
  - Không publish câu hỏi trắc nghiệm nếu chưa có đáp án đúng.
  - Không xóa quiz đã có attempt submitted nếu có thể ảnh hưởng dữ liệu; thay vào đó chuyển `ARCHIVED` hoặc `DRAFT`.
- API gợi ý:
  - `GET /api/teacher/courses/{courseId}/quizzes`.
  - `POST /api/teacher/courses/{courseId}/quizzes`.
  - `PUT /api/teacher/quizzes/{quizId}`.
  - `DELETE /api/teacher/quizzes/{quizId}` hoặc archive.
  - `POST /api/teacher/quizzes/{quizId}/questions`.
  - `PUT /api/teacher/questions/{questionId}`.
  - `DELETE /api/teacher/questions/{questionId}`.
  - `POST /api/teacher/questions/{questionId}/options`.
  - `PUT /api/teacher/options/{optionId}`.
  - `DELETE /api/teacher/options/{optionId}`.
  - `POST /api/teacher/quizzes/{quizId}/publish`.
- View route:
  - `GET /teacher/courses/{courseId}/assessments/quizzes`.
  - `GET /teacher/quizzes/{quizId}/builder`.

### Frontend cần làm
- Quiz Builder bằng HTML/CSS/JS thuần:
  - Left panel: danh sách câu hỏi và trạng thái hợp lệ.
  - Main editor: form quiz + form câu hỏi + form options.
  - Question types: SINGLE_CHOICE và MULTIPLE_CHOICE.
  - Có nút Add Question, Add Option, Save, Delete, Preview, Publish.
  - Cho phép reorder đơn giản bằng nút Up/Down nếu drag-drop phức tạp.
  - Validate realtime: câu hỏi rỗng, điểm <=0, chưa chọn đáp án đúng.
  - Confirm khi xóa câu hỏi/quiz.
- UI:
  - Đồng bộ Teacher portal.
  - Responsive: trên mobile panel danh sách câu hỏi chuyển thành accordion/dropdown.

### Validation và security
- Server validate course ownership.
- Không tin dữ liệu client về `isCorrect`, `points`, `displayOrder`.
- Không cho question/option thuộc quiz khác bị update nhầm.
- Escape nội dung quiz/question/option khi render.

### Acceptance criteria
- Teacher tạo quiz được.
- Teacher thêm/sửa/xóa câu hỏi và đáp án được.
- Teacher chọn đáp án đúng được.
- Quiz publish chỉ khi hợp lệ.
- Student không truy cập được quiz builder.
- Không xóa mất dữ liệu attempt đã submit.
- UI responsive và không lỗi console.

### Sau khi hoàn thành, báo cáo
- File đã tạo/sửa.
- API route mới.
- Quy tắc publish/delete đã chọn.
- Cách test bằng Teacher account.
