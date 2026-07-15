# Acceptance Criteria

## AC-01 — Grading render

- Teacher mở Grading nhận HTTP 200.
- Trang dùng đúng Instructor Portal layout.
- Main content hiển thị, không trống/ẩn.
- CSS và JS cần thiết tải thành công.
- Sidebar active tại Grading.

## AC-02 — Assignments render

- Teacher mở Assignments nhận HTTP 200.
- Trang không còn style mặc định.
- Không còn dữ liệu hard-code.
- Dữ liệu lấy theo Teacher hiện tại.
- Empty state hiển thị khi không có assignment.

## AC-03 — Ownership

- Teacher chỉ thấy course/assignment/submission của họ.
- Sửa ID để truy cập dữ liệu Teacher khác bị chặn ở backend.
- Student không truy cập được Teacher assessment routes.

## AC-04 — Assignment management

- Teacher tạo/sửa/publish hoặc archive assignment theo policy.
- Validation hoạt động.
- Danh sách cập nhật sau thao tác.
- Không phá submission history.

## AC-05 — Submission grading

- Teacher xem được nội dung submission.
- Chấm score hợp lệ và feedback.
- Score vượt max bị từ chối.
- Lưu draft/publish hoạt động theo domain hiện có.
- Grader và thời gian được lưu.
- Student xem đúng kết quả sau publish.

## AC-06 — Quiz management

- Teacher CRUD quiz/question/answer trong course của mình.
- Validation đáp án đúng.
- Quiz đã có attempt không bị xóa làm mất lịch sử.
- Student quiz flow ASM-01 và result ASM-03 vẫn hoạt động.

## AC-07 — Code grading

- Teacher CRUD testcase.
- Hidden testcase không lộ cho Student.
- Adapter chấm code có interface rõ ràng.
- Test dùng fake adapter.
- Production không chạy code Student trong JVM/web process.
- Khi external service không cấu hình hoặc lỗi, UI/API báo trạng thái thật, không giả điểm.

## AC-08 — Regression

- Student enrollment/learning pages không hỏng.
- Teacher sidebar links khác vẫn dùng được.
- Security tests pass.
- `./mvnw test` pass, hoặc mọi failure môi trường được ghi rõ và không bị che giấu.

## Definition of Done

Chỉ đánh dấu hoàn thành khi:

- Tất cả AC liên quan đạt PASS.
- Có test hoặc bằng chứng thủ công cho từng luồng chính.
- Không hard-code.
- Không bypass security.
- Không có TODO giả vờ hoàn thành ở đường chạy chính.
