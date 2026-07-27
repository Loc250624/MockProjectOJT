# LumiNa Hybrid Quiz — Codex Implementation Kit

## Mục tiêu

Bộ tài liệu và mã tham chiếu này hướng dẫn Codex hoàn thiện chức năng quiz theo mô hình:

1. AI hoặc Teacher tạo ngân hàng câu hỏi.
2. Câu hỏi AI được lưu ở trạng thái `DRAFT`.
3. Teacher review và chuyển câu hợp lệ sang `APPROVED`.
4. Quiz có blueprint theo `topic + difficulty`.
5. Student được cấp đề khi bắt đầu attempt, không phải khi enroll.
6. Danh sách câu hỏi đã cấp được lưu cố định trong database.
7. Refresh, đăng nhập lại hoặc Teacher sửa ngân hàng câu hỏi không làm đổi đề đang làm.
8. Save và submit chỉ xử lý những câu đã cấp.
9. Chấm điểm bằng snapshot của attempt.
10. OpenAI không nằm trong critical path của Student quiz.

## Phát hiện quan trọng trong code hiện tại

Repository đang tồn tại hai luồng quiz:

- `StudentAssessmentServiceImpl` dùng `Submission` và JSON `submittedContent`.
- `AssessmentServiceImpl` dùng `QuizAttempt` và `QuizAnswer`.

Codex phải hợp nhất nghiệp vụ quiz về `QuizAttempt`, đồng thời giữ nguyên các endpoint lesson hiện tại để không làm hỏng trang học.

`Submission` tiếp tục phục vụ Coding/Assignment.

## Cách sử dụng

1. Giải nén ZIP này.
2. Mở repository LumiNa bằng Codex.
3. Đưa `00_CODEX_MASTER_PROMPT.md` cho Codex.
4. Yêu cầu Codex đọc toàn bộ thư mục trước khi sửa.
5. Yêu cầu Codex audit repository mới nhất, không copy mù mã tham chiếu.
6. Chạy migration trên database test trước.
7. Chạy `./mvnw test` và `./mvnw clean package`.

## Mức ưu tiên

- **P0:** một cơ chế attempt duy nhất, đề cố định, chấm đúng snapshot.
- **P1:** blueprint và stratified sampler.
- **P2:** Teacher quản lý question bank/review.
- **P3:** AI batch generation ở workflow riêng.
