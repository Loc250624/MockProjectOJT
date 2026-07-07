# ASM Integration Checklist

Dùng checklist này sau khi Codex/Agent hoàn thành từng task ASM-01 đến ASM-06.

## Backlog gốc

| ID | Module | Function | Actor | Priority | Week | Owner | Reviewer | SP | Dependencies | Deliverable |
|---|---|---|---|---|---|---|---:|---:|---|---|
| ASM-01 | Assessment | Học viên làm bài trắc nghiệm, lưu tạm và nộp bài | Student | P0 | W6 | Cường | Lộc | 5 | LRN-01 | Quiz taking UI |
| ASM-02 | Assessment | Học viên nộp bài thực hành/bài tập code | Student | P1 | W6 | Cường | Lộc | 5 | LRN-01 | Submission flow |
| ASM-03 | Assessment | Học viên xem kết quả trắc nghiệm và kết quả chấm code | Student | P1 | W6 | Cường | Lộc | 3 | ASM-01,ASM-02 | Result screen |
| ASM-04 | Assessment Management | Giảng viên tạo, cập nhật và xóa Quiz/Câu hỏi/Đáp án | Teacher | P0 | W6 | Cường | Cường | 5 | CRS-06 | Quiz builder |
| ASM-05 | Assessment Management | Cấu hình testcase và chấm code tự động qua adapter dịch vụ | Teacher/System | P2 | W8 | Cường | Cường | 8 | ASM-02 | Code judge sandbox |
| ASM-06 | Assessment Management | Chấm điểm và nhận xét bài tập tự luận/thực hành | Teacher | P1 | W6 | Cường | Cường | 5 | ASM-02 | Grading workflow |


## Kiểm tra database/entity
- [ ] Có entity hoặc bảng tương đương cho Quiz.
- [ ] Có entity hoặc bảng tương đương cho Question.
- [ ] Có entity hoặc bảng tương đương cho Option.
- [ ] Có entity hoặc bảng tương đương cho QuizAttempt.
- [ ] Có entity hoặc bảng tương đương cho QuizAnswer.
- [ ] Có entity hoặc bảng tương đương cho Assignment.
- [ ] Có entity hoặc bảng tương đương cho Submission.
- [ ] Có entity hoặc bảng tương đương cho TestCase.
- [ ] Có entity hoặc bảng tương đương cho CodeJudgeResult.
- [ ] Có field score/status/feedback/gradedAt cho grading.

## Kiểm tra phân quyền
- [ ] Student đã enroll mới làm quiz được.
- [ ] Student không enroll không mở được quiz/assignment.
- [ ] Student không xem được result/submission của người khác.
- [ ] Teacher chỉ quản lý quiz/testcase/submission thuộc course mình phụ trách.
- [ ] Student không truy cập được route Teacher.
- [ ] Teacher không submit bài thay Student.

## Kiểm tra ASM-01
- [ ] Student mở quiz published được.
- [ ] Student tạo/lấy draft attempt được.
- [ ] Chọn đáp án và lưu draft được.
- [ ] Reload trang vẫn còn draft.
- [ ] Submit quiz thành công.
- [ ] Sau submit không sửa được.
- [ ] Điểm quiz tính đúng.

## Kiểm tra ASM-02
- [ ] Student mở assignment được.
- [ ] Nộp text được.
- [ ] Nộp code được.
- [ ] Upload file hợp lệ được nếu hỗ trợ.
- [ ] File không hợp lệ bị chặn.
- [ ] Submission có status và submittedAt.

## Kiểm tra ASM-03
- [ ] Student xem danh sách result được.
- [ ] Quiz result hiển thị score/total/percentage.
- [ ] Submission result hiển thị status/score/feedback.
- [ ] Code result hiển thị passed/total.
- [ ] Hidden testcase không lộ input/expected output.

## Kiểm tra ASM-04
- [ ] Teacher tạo quiz được.
- [ ] Teacher thêm/sửa/xóa question được.
- [ ] Teacher thêm/sửa/xóa option được.
- [ ] Chọn correct answer được.
- [ ] Publish chỉ thành công khi quiz hợp lệ.
- [ ] Xóa/archive không làm mất attempt đã submit.

## Kiểm tra ASM-05
- [ ] Teacher thêm/sửa/xóa testcase được.
- [ ] Có adapter `CodeJudgeAdapter`.
- [ ] Không dùng command execution không an toàn để chạy code user.
- [ ] Run judge tạo `CodeJudgeResult`.
- [ ] Result có status/passedTests/totalTests/score.
- [ ] Adapter lỗi thì UI/API xử lý rõ.

## Kiểm tra ASM-06
- [ ] Teacher xem danh sách submissions được.
- [ ] Filter theo status hoạt động.
- [ ] Teacher nhập điểm trong khoảng hợp lệ.
- [ ] Teacher nhập feedback và lưu được.
- [ ] Student xem được feedback/score.
- [ ] Return for revision hoạt động nếu được triển khai.

## Kiểm tra UI/UX
- [ ] Không lỗi console JS.
- [ ] Có loading state.
- [ ] Có empty state.
- [ ] Có error state.
- [ ] Có confirm khi submit/delete/grade.
- [ ] Responsive desktop/tablet/mobile.
- [ ] Giao diện thống nhất với Student/Teacher portal hiện có.

## Kiểm tra build/test
- [ ] `mvn test` hoặc `./mvnw test` chạy được, nếu project dùng Maven.
- [ ] `gradle test` hoặc `./gradlew test` chạy được, nếu project dùng Gradle.
- [ ] App start không lỗi.
- [ ] Không phát sinh lỗi migration/schema.
- [ ] Manual test pass toàn bộ flow Student -> Teacher -> Student.
