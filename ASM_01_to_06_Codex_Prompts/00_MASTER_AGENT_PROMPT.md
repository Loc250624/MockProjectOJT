# MASTER PROMPT — Hoàn thành Assessment Module ASM-01 đến ASM-06

Bạn là Senior Fullstack Engineer chuyên Spring Boot + HTML/CSS/JavaScript thuần. Hãy đọc toàn bộ source hiện tại trước khi sửa và triển khai module Assessment theo backlog dưới đây.

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


## Backlog cần hoàn thành

| ID | Module | Function | Actor | Priority | Week | Owner | Reviewer | SP | Dependencies | Deliverable |
|---|---|---|---|---|---|---|---:|---:|---|---|
| ASM-01 | Assessment | Học viên làm bài trắc nghiệm, lưu tạm và nộp bài | Student | P0 | W6 | Cường | Lộc | 5 | LRN-01 | Quiz taking UI |
| ASM-02 | Assessment | Học viên nộp bài thực hành/bài tập code | Student | P1 | W6 | Cường | Lộc | 5 | LRN-01 | Submission flow |
| ASM-03 | Assessment | Học viên xem kết quả trắc nghiệm và kết quả chấm code | Student | P1 | W6 | Cường | Lộc | 3 | ASM-01,ASM-02 | Result screen |
| ASM-04 | Assessment Management | Giảng viên tạo, cập nhật và xóa Quiz/Câu hỏi/Đáp án | Teacher | P0 | W6 | Cường | Cường | 5 | CRS-06 | Quiz builder |
| ASM-05 | Assessment Management | Cấu hình testcase và chấm code tự động qua adapter dịch vụ | Teacher/System | P2 | W8 | Cường | Cường | 8 | ASM-02 | Code judge sandbox |
| ASM-06 | Assessment Management | Chấm điểm và nhận xét bài tập tự luận/thực hành | Teacher | P1 | W6 | Cường | Cường | 5 | ASM-02 | Grading workflow |


## Mục tiêu tổng thể
Hoàn thiện đầy đủ luồng đánh giá học tập cho E-learning:
- Student làm quiz, lưu tạm, nộp bài.
- Student nộp bài thực hành/bài code.
- Student xem kết quả quiz/code/manual grading.
- Teacher tạo và quản lý quiz/câu hỏi/đáp án.
- Teacher cấu hình testcase và gọi adapter chấm code tự động một cách an toàn.
- Teacher chấm điểm và nhận xét bài tự luận/thực hành.

## Data model gợi ý
Hãy kiểm tra source trước. Nếu chưa có model tương ứng, tạo/cập nhật tối thiểu các model sau theo naming convention của project:

- `Quiz`: id, course/lesson, title, description, durationMinutes, maxAttempts, status(DRAFT/PUBLISHED), createdBy, createdAt, updatedAt.
- `QuizQuestion`: id, quiz, content, questionType(SINGLE_CHOICE/MULTIPLE_CHOICE), points, displayOrder.
- `QuizOption`: id, question, content, isCorrect, displayOrder.
- `QuizAttempt`: id, quiz, student, status(DRAFT/SUBMITTED/GRADED), startedAt, submittedAt, score, totalPoints.
- `QuizAnswer`: id, attempt, question, selectedOptionIds hoặc answerText.
- `Assignment`: id, course/lesson, title, description, submissionType(TEXT/CODE/FILE/MIXED), dueDate, maxScore, status.
- `Submission`: id, assignment, student, contentText, codeLanguage, codeContent, filePath/fileUrl, status(DRAFT/SUBMITTED/AUTO_GRADED/GRADED/RETURNED), submittedAt, score, feedback.
- `TestCase`: id, assignment, input, expectedOutput, isHidden, points, displayOrder.
- `CodeJudgeResult`: id, submission, status(PENDING/RUNNING/PASSED/FAILED/ERROR), totalTests, passedTests, outputLog, executionTimeMs.
- `GradeFeedback`: id, submission, teacher, score, feedback, gradedAt.

Không bắt buộc tạo đúng y nguyên nếu project đã có entity tương đương. Hãy map vào cấu trúc hiện có và giải thích ngắn trong output.

## Route/API gợi ý
Hãy điều chỉnh route theo convention hiện có, nhưng cần có đủ các luồng:

### Student
- `GET /student/courses/{courseId}/quizzes/{quizId}/take`
- `POST /api/student/quizzes/{quizId}/attempts`
- `PUT /api/student/quiz-attempts/{attemptId}/draft`
- `POST /api/student/quiz-attempts/{attemptId}/submit`
- `GET /student/assignments/{assignmentId}/submit`
- `POST /api/student/assignments/{assignmentId}/submissions/draft`
- `POST /api/student/assignments/{assignmentId}/submissions/submit`
- `GET /student/results`
- `GET /student/quizzes/{attemptId}/result`
- `GET /student/submissions/{submissionId}/result`

### Teacher
- `GET /teacher/courses/{courseId}/assessments/quizzes`
- `POST /api/teacher/courses/{courseId}/quizzes`
- `PUT /api/teacher/quizzes/{quizId}`
- `DELETE /api/teacher/quizzes/{quizId}`
- `POST /api/teacher/quizzes/{quizId}/questions`
- `PUT /api/teacher/questions/{questionId}`
- `DELETE /api/teacher/questions/{questionId}`
- `GET /teacher/assignments/{assignmentId}/submissions`
- `POST /api/teacher/submissions/{submissionId}/grade`
- `GET /teacher/assignments/{assignmentId}/testcases`
- `POST /api/teacher/assignments/{assignmentId}/testcases`
- `POST /api/teacher/submissions/{submissionId}/judge`

## Quy tắc bảo mật quan trọng cho ASM-05
Không chạy code người dùng trực tiếp trên host application. Nếu chưa có sandbox thật, hãy tạo `CodeJudgeAdapter` dạng interface + implementation mock/local-safe để mô phỏng kết quả, kèm TODO rõ ràng cho tích hợp Judge0/Docker sandbox sau. Không dùng `Runtime.exec`/`ProcessBuilder` để chạy code user trong app nếu chưa có sandbox, timeout, memory limit, network isolation.

## UI yêu cầu
- Light mode, đồng bộ design system hiện có.
- HTML semantic, CSS riêng theo module nếu cần, JS vanilla dùng `fetch`.
- Có loading state, empty state, toast/alert, confirm khi submit/delete.
- Có responsive: màn hình quiz, quiz builder, submission list phải dùng tốt trên mobile.

## Thứ tự thực hiện
1. Đọc source, xác định entity/route/template hiện có.
2. Tạo/cập nhật database/entity/repository.
3. Tạo service với business rule rõ ràng.
4. Tạo controller/API và view controller.
5. Tạo template HTML/CSS/JS.
6. Tạo seed/demo data nếu cần để test.
7. Tạo test hoặc checklist test.
8. Chạy build/test và sửa lỗi phát sinh.
9. Báo cáo lại: file đã sửa, API đã thêm, cách test từng ASM.

## Definition of Done chung
- Không còn lỗi compile.
- Không còn lỗi console JavaScript nghiêm trọng.
- UI responsive trên desktop/tablet/mobile.
- Role guard đúng: Student/Teacher/System theo mô tả task.
- Có trạng thái loading/empty/error/success.
- Có kiểm thử tối thiểu cho service/controller hoặc hướng dẫn test thủ công nếu project chưa có test nền.
- Không làm hỏng các task đã hoàn thành trước đó: AUTH, LRN và CRS liên quan.


## Output mong muốn từ Agent sau khi hoàn thành
- Danh sách file đã tạo/sửa.
- Danh sách route/API mới.
- Cách test từng chức năng ASM-01 đến ASM-06.
- Lưu ý migration/database nếu có.
- Các điểm chưa làm được nếu source hiện tại thiếu nền tảng.
