# Current Code Audit

## Luồng A — Quiz trong trang lesson

### `StudentActionController`

Trang học dùng các endpoint dạng:

- `GET /student/courses/{courseId}/lessons/{lessonId}/quiz`
- `POST /student/courses/{courseId}/lessons/{lessonId}/quiz/save`
- `POST /student/courses/{courseId}/lessons/{lessonId}/quiz/submit`

Controller gọi `StudentAssessmentService`.

### `StudentAssessmentServiceImpl`

Hiện tại luồng này:

- lấy toàn bộ câu hỏi bằng `findByQuizIdOrderByDisplayOrderAscIdAsc`;
- tạo `Submission` cho quiz;
- lưu answer map trong JSON `submittedContent`;
- trả cùng danh sách câu hỏi cho mọi Student;
- grade từ live question list.

Rủi ro:

- mọi Student nhận cùng đề;
- refresh và thay đổi database có thể làm đề không tái hiện được;
- backend có thể grade câu không thực sự được cấp;
- không có audit set;
- `Submission` đang gánh cả quiz và coding.

## Luồng B — Assessment API

### `StudentAssessmentRestController`

Có endpoint dạng:

- `POST /api/student/quizzes/{quizId}/attempts`
- `PUT /api/student/quiz-attempts/{attemptId}/draft`
- `POST /api/student/quiz-attempts/{attemptId}/submit`

Controller gọi `AssessmentService`.

### `AssessmentServiceImpl`

Luồng này đã sử dụng:

- `QuizAttempt`
- `QuizAnswer`
- `QuizAttemptRepository`

Nhưng:

- chưa persist question assignment;
- start mới tạo attempt;
- total/grade vẫn có thể phụ thuộc live `Question`;
- chưa stratified sample.

## Entity hiện có

### `QuizAttempt`

Đã có quiz, student, status, startedAt, submittedAt, score, totalPoints và answers.

Đây là entity phù hợp làm canonical quiz attempt.

### `QuizAnswer`

Đã liên kết với attempt/question. Chuẩn hóa quiz answer về `(attempt, question)`.

### `Submission`

Giữ cho Coding/Assignment. Không tạo quiz Submission mới.

## Kết luận

Không phát triển hai cơ chế độc lập.

Tạo một application service chung:

- `QuizAttemptApplicationService`
- `QuizQuestionAssignmentService`
- `QuizGradingService`

Sau đó:

- `StudentAssessmentServiceImpl` delegate phần quiz;
- `AssessmentServiceImpl` delegate cùng service;
- Coding methods giữ nguyên.

Mục tiêu cuối: một implementation duy nhất cho start/save/submit/grade.
