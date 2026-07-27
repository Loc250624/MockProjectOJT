Bạn đang làm việc trên repository LumiNa E-Learning.

Stack đã quan sát:

- Spring Boot 3
- Java 17
- Spring Data JPA
- MySQL
- Thymeleaf
- Vanilla JavaScript
- package gốc: `com.ojtsu26.elearning`

## Nhiệm vụ

Sửa code thật để hoàn thiện quiz cho Student theo kiến trúc:

```text
Lesson content
    -> AI generation job
    -> DRAFT question bank
    -> validation + teacher review
    -> APPROVED question bank
    -> stratified sampler
    -> persisted question set for attempt
    -> student answers
    -> snapshot grading
```

Không dừng ở viết tài liệu hoặc pseudo-code.

---

## 1. Audit trước khi sửa

Tìm toàn bộ references của:

- `StudentAssessmentService`
- `StudentAssessmentServiceImpl`
- `AssessmentService`
- `AssessmentServiceImpl`
- `QuizAttempt`
- `QuizAnswer`
- `Submission`
- `Question`
- `Quiz`
- endpoint `/quiz`, `/quiz/save`, `/quiz/submit`
- JavaScript/template của trang `student/learning`
- teacher quiz/question editor

Viết plan file cụ thể trước khi sửa.

---

## 2. Hợp nhất cơ chế quiz attempt

Hiện có hai cơ chế quiz:

1. Lesson flow dùng `Submission`.
2. Assessment flow dùng `QuizAttempt` và `QuizAnswer`.

Phải chọn `QuizAttempt` làm canonical model.

Yêu cầu:

- Không tạo `Submission` mới cho quiz.
- `Submission` tiếp tục dùng cho Coding/Assignment.
- Các endpoint lesson hiện tại phải vẫn hoạt động.
- Các endpoint `/api/student/...` phải delegate vào cùng một application service.
- Không để hai thuật toán start/save/submit/grade độc lập.
- Giữ backward compatibility response JSON tối đa có thể.

Nên tạo:

- `QuizAttemptApplicationService`
- `QuizQuestionAssignmentService`
- `QuizGradingService`
- `StratifiedQuestionSampler`

---

## 3. Persist question assignment

Tạo entity/table `QuizAttemptQuestion` với tối thiểu:

- `id`
- `attempt`
- `question`
- `displayOrder`
- `pointsSnapshot`
- `questionVersion`
- `questionTextSnapshot`
- `optionsJsonSnapshot`
- `correctAnswerSnapshot`
- `questionTypeSnapshot`
- `topicSnapshot`
- `difficultySnapshot`
- `assignedAt`

Ràng buộc:

- unique `(attempt_id, question_id)`
- unique `(attempt_id, display_order)`
- index `(attempt_id, display_order)`
- không trả `correctAnswerSnapshot` trước submit
- refresh chỉ đọc bảng này, không sample lại
- grade từ snapshot
- attempt cũ vẫn hoạt động nếu question bị sửa/archive

---

## 4. Metadata cho question bank

Mở rộng `Question` với:

- `topicCode`
- `difficulty`: `EASY`, `MEDIUM`, `HARD`
- `reviewStatus`: `DRAFT`, `APPROVED`, `REJECTED`, `ARCHIVED`
- `version`
- `active`
- `generationSource`: `MANUAL`, `AI`

Backfill câu hỏi hiện có:

- topic: `GENERAL`
- difficulty: `MEDIUM`
- review status: `APPROVED`
- active: `true`
- version: `1`
- source: `MANUAL`

Mục tiêu là quiz hiện có vẫn chạy sau migration.

---

## 5. Quiz blueprint

Tạo `QuizBlueprintItem`:

- `quiz`
- `topicCode`
- `difficulty`
- `questionCount`
- `displayOrder`

Ví dụ 10 câu:

- Topic A + EASY: 2
- Topic A + MEDIUM: 2
- Topic B + MEDIUM: 3
- Topic B + HARD: 2
- Topic C + EASY: 1

Tổng `questionCount` là số câu xuất hiện trong một attempt.

Không dùng:

- `ORDER BY RAND() LIMIT`
- shuffle toàn bộ bank rồi lấy N
- client-side random

---

## 6. Start/resume attempt

Khi Student mở quiz:

1. Verify authentication.
2. Verify enrollment.
3. Verify lesson/quiz availability.
4. Lock đủ để hai request đồng thời không tạo hai DRAFT attempt.
5. Nếu đã có DRAFT attempt, resume và trả đúng assigned set.
6. Respect `maxAttempts`.
7. Load blueprint.
8. Load chỉ `APPROVED + active` questions.
9. Sample theo từng bucket.
10. Persist `QuizAttempt` và toàn bộ `QuizAttemptQuestion` trong cùng transaction.
11. Set `totalPoints` bằng tổng `pointsSnapshot`.
12. Return DTO theo `displayOrder`.

Không cấp đề lúc enroll.

Nếu bank thiếu một bucket:

- không tự ý lấy câu sai topic/difficulty;
- không tạo partial attempt;
- trả business error rõ ràng;
- Teacher readiness API phải cho biết bucket bị thiếu.

---

## 7. Selection algorithm

Trong mỗi bucket:

1. Loại câu không approved/inactive.
2. Ưu tiên câu Student chưa gặp ở attempt trước.
3. Sau đó ưu tiên câu có global usage thấp.
4. Randomize trong nhóm tương đương bằng `SecureRandom`.
5. Nếu unseen không đủ, được reuse câu đã gặp trong cùng bucket.
6. Không chọn trùng ID trong cùng đề.
7. Cố gắng giữ overlap dưới `maxOverlapPercent`.
8. Tránh toàn bộ đề trùng hệt attempt trước khi còn phương án khác.
9. Log metric/reason khi buộc phải overlap cao, nhưng không log đáp án đúng.

Sampler phải là pure component để unit test được.

---

## 8. Save, submit và grading

### Save

- Chỉ accept `questionId` thuộc `QuizAttemptQuestion` của attempt.
- Reject ID lạ dù ID đó thuộc cùng quiz.
- Upsert `QuizAnswer` theo `(attempt, question)`.
- Autosave idempotent.
- Không cho sửa attempt đã submit.

### Submit

- Idempotent.
- Request lặp không grade hai lần.
- Chỉ grade assigned set.
- Missing answer = 0 điểm.
- Dùng `pointsSnapshot`.
- Dùng `correctAnswerSnapshot`.
- Không dùng live `Question.correctAnswer`.
- Tổng điểm là tổng snapshot.
- Lưu submitted/graded time một lần.
- Cập nhật lesson progress theo passing policy hiện tại.
- Không leak correct answer trước submit.

---

## 9. Teacher question-bank workflow

Bổ sung service/API/UI tối thiểu:

- list/filter theo status/topic/difficulty/source
- create manual question
- approve
- reject
- archive
- bulk approve
- configure blueprint
- readiness check
- hiển thị required/available của từng bucket

Không cho publish/activate quiz khi bank không đáp ứng blueprint.

Nếu một question đã từng được assigned:

- không làm attempt cũ thay đổi;
- ưu tiên tạo version mới hoặc dựa vào snapshot;
- không hard-delete nếu còn reference.

---

## 10. AI generation workflow

Không tái sử dụng trực tiếp `OpenAiResponsesClient` của chatbot.

Tạo riêng:

- `QuestionGenerationProvider`
- `OpenAiQuestionGenerationClient`
- `QuestionBankGenerationService`
- `QuestionValidationService`

Yêu cầu:

- AI chạy trong Teacher/Admin workflow.
- Batch generation.
- Structured JSON output.
- Validate schema.
- Validate số option.
- Validate correct answer có trong options.
- Detect duplicate/similar question.
- Validate độ dài.
- Lưu generated question ở `DRAFT`.
- Teacher review trước `APPROVED`.
- Retry/timeout/rate limit riêng.
- Không gọi AI trong Student start/save/submit.
- AI lỗi/quota hết không làm Student quiz ngừng hoạt động.

Nếu background infrastructure chưa có, triển khai job entity/status hoặc service boundary rõ ràng; tuyệt đối không đưa synchronous OpenAI vào critical path.

---

## 11. Frontend Student

Trang learning phải:

- start hoặc resume;
- render đúng assigned questions;
- refresh không đổi câu;
- không client shuffle;
- autosave có trạng thái saving/saved/error;
- chống double submit;
- giữ CSRF helper hiện tại;
- không hiển thị Loading/Unavailable/Submitted cùng lúc;
- hiển thị lỗi dễ hiểu nếu approved bank chưa đủ;
- không hiển thị đáp án đúng trước submit.

Không redesign global layout/sidebar.

---

## 12. Migration và compatibility

- Dùng Flyway migration mới.
- Không sửa migration cũ đã chạy.
- Đối chiếu naming strategy/table names thật.
- Migration additive.
- Backfill dữ liệu cũ an toàn.
- Không drop table/column legacy trong release đầu.
- Không xóa quiz `Submission` lịch sử.
- Có compatibility reader cho lịch sử cũ nếu cần.
- Không dùng `ddl-auto=create` hoặc `create-drop`.

---

## 13. Tests bắt buộc

Viết unit/integration/controller tests cho:

- exact blueprint distribution
- only approved/active questions
- refresh/resume stable
- no duplicate draft under concurrent start
- avoid previous questions when sufficient
- same-bucket fallback when insufficient unseen
- no wrong-bucket fallback
- reject unassigned answer
- grading uses snapshots
- edit question after start does not change score
- archive question after start does not break attempt
- submit idempotency
- max attempts
- insufficient bank creates no partial attempt
- OpenAI provider never called in Student runtime
- CSRF behavior
- authorization
- coding flow unaffected
- old result/history readable

Chạy:

```bash
./mvnw test
./mvnw clean package
```

---

## 14. Guardrails

- Không sửa payment, certificate, chatbot, authentication hoặc coding ngoài phần cần để compile.
- Không hard-code ID.
- Không đưa correct answers vào log.
- Không log API key.
- Không bỏ CSRF.
- Không tạo 60–100 dữ liệu giả trong production migration.
- Không thay response contract một cách phá vỡ nếu có thể dùng additive fields.
- Không để Teacher chỉnh sửa làm thay đổi attempt đã bắt đầu.
- Không tạo thêm một service quiz thứ ba song song.

---

## 15. Báo cáo hoàn thành

Sau khi sửa, báo cáo:

- files added/modified/removed
- migration impact
- compatibility behavior
- test results
- manual test evidence
- remaining risks
- chỗ nào chưa hoàn thành và lý do

Không tuyên bố hoàn thành nếu compile/test chưa chạy.
