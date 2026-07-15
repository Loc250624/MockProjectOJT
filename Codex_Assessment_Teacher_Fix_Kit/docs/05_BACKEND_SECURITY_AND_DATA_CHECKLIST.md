# Backend, Security and Data Checklist

## Authentication/Authorization

- [ ] Route Teacher yêu cầu authenticated user.
- [ ] Role check dùng convention hiện tại (`TEACHER`, `ROLE_TEACHER`, authority...).
- [ ] Ownership kiểm tra trong service/backend.
- [ ] Không dùng teacher ID từ request để cấp quyền.
- [ ] Student gọi Teacher API nhận 403.
- [ ] Teacher A không đọc/sửa dữ liệu Teacher B.
- [ ] CSRF hoạt động theo cấu hình hiện tại.
- [ ] Error response/page không làm lộ stack trace.

## Data

- [ ] Assignments lấy từ DB.
- [ ] Grading queue lấy từ DB.
- [ ] Submission có liên kết đúng Student/Assignment/Course.
- [ ] Quiz attempt/result không bị ghi đè sai.
- [ ] Grade lưu score, feedback, grader, timestamp, status.
- [ ] Code testcase thuộc đúng coding assignment.
- [ ] Hidden testcase không xuất hiện trong Student DTO/template.
- [ ] Archive/soft-delete không làm mất lịch sử.
- [ ] Không có row mẫu `Student A / Java / Assign 1` trong production.

## Validation

- [ ] Score không âm và không vượt max score.
- [ ] Feedback có giới hạn hợp lý.
- [ ] Assignment deadline hợp lệ.
- [ ] File upload có size/type validation.
- [ ] Quiz option/answer hợp lệ.
- [ ] Testcase weight hợp lệ.
- [ ] Không tin dữ liệu tính điểm từ client.

## Code execution safety

- [ ] Không `Runtime.exec`.
- [ ] Không `ProcessBuilder` chạy code Student trong web app.
- [ ] Adapter có timeout.
- [ ] External errors được map sang trạng thái rõ ràng.
- [ ] Secret/API key lấy từ environment/secret config.
- [ ] Fake adapter chỉ nằm trong test profile.
