# Test Plan

## 1. Unit tests

Tối thiểu cần test:

- Ownership checker.
- Grade validation.
- Quiz scoring.
- Attempt status transition.
- Assignment submission policy.
- Code result scoring/mapping.
- Hidden testcase filtering.
- Publish grade transition.

## 2. Repository tests

- Teacher chỉ query được course/assignment/submission của họ.
- Filter theo course, assignment, status, student.
- Pagination.
- Archived records được xử lý theo policy.
- Không N+1 nghiêm trọng ở trang danh sách chính nếu có thể kiểm tra.

## 3. MVC/Controller tests

Dùng MockMvc hoặc pattern hiện có:

- Teacher GET Grading trả 200 và view đúng.
- Teacher GET Assignments trả 200 và view đúng.
- Model có collection/summary/filter data cần thiết.
- Student truy cập route Teacher trả 403.
- Anonymous bị redirect/login hoặc 401 theo cấu hình.
- Teacher A truy cập submission Teacher B bị chặn.
- Form validation trả đúng lỗi.
- POST grade hợp lệ persist dữ liệu.
- Publish grade cập nhật trạng thái.
- Static CSS/JS cần thiết không bị security chặn.

## 4. Integration tests

Luồng tối thiểu:

1. Teacher tạo course/assignment.
2. Student enroll.
3. Student submit.
4. Teacher thấy submission trong queue.
5. Teacher lưu draft grade.
6. Student chưa thấy final nếu policy yêu cầu publish.
7. Teacher publish.
8. Student thấy score và feedback.
9. Truy cập chéo bị chặn.

Quiz:

1. Teacher tạo quiz/question/options.
2. Student làm, lưu draft.
3. Student submit.
4. Server chấm.
5. Student xem result theo release policy.

Code:

1. Teacher cấu hình testcase.
2. Student submit code.
3. Fake adapter trong test trả kết quả.
4. Backend lưu status/score.
5. Hidden testcase không lộ input/expected output cho Student.

## 5. Template/UI verification

- HTML không vỡ.
- CSS/JS trả 200.
- Không có row hard-code.
- Empty state render đúng.
- Danh sách có dữ liệu thật khi seed/test data tồn tại.
- Button/link action đúng route.
- Validation/success/error message render.
- Desktop và mobile không bị che content.

## 6. Commands

```bash
./mvnw test
```

Nếu dự án có profile test:

```bash
./mvnw test -Dspring.profiles.active=test
```

Không sửa test để che lỗi production. Không disable test thất bại nếu chưa chứng minh test sai.
