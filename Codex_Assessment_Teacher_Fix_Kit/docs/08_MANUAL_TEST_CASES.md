# Manual Test Cases

## TC-01 — Teacher Grading có dữ liệu

**Tiền điều kiện:** Teacher có course, assignment và ít nhất một Student đã submit.

1. Đăng nhập Teacher.
2. Mở Grading.
3. Chọn course.
4. Chọn assignment.
5. Kiểm tra submission xuất hiện.
6. Mở chi tiết.

**Kỳ vọng:** UI đúng layout; dữ liệu đúng Student/course/assignment; không có row mẫu.

## TC-02 — Grading empty state

**Tiền điều kiện:** Teacher không có submission cần chấm.

1. Mở Grading.

**Kỳ vọng:** Có thông báo empty state; không phải trang trắng.

## TC-03 — Chấm điểm hợp lệ

1. Mở submission.
2. Nhập score hợp lệ.
3. Nhập feedback.
4. Lưu/publish.

**Kỳ vọng:** Thành công; dữ liệu persist; queue/status cập nhật.

## TC-04 — Score không hợp lệ

1. Nhập score âm.
2. Nhập score lớn hơn max.

**Kỳ vọng:** Bị từ chối; hiển thị validation; database không bị ghi sai.

## TC-05 — Truy cập chéo Teacher

1. Đăng nhập Teacher A.
2. Lấy URL submission thuộc Teacher B.
3. Truy cập trực tiếp.

**Kỳ vọng:** 403/404 theo policy; không lộ dữ liệu.

## TC-06 — Student truy cập Teacher route

1. Đăng nhập Student.
2. Mở URL Grading hoặc POST grade.

**Kỳ vọng:** Bị chặn ở backend.

## TC-07 — Assignments CRUD

1. Teacher tạo assignment.
2. Kiểm tra xuất hiện trong danh sách.
3. Sửa nội dung/deadline.
4. Publish/archive.
5. Reload.

**Kỳ vọng:** Dữ liệu từ DB đúng; status đúng; layout không vỡ.

## TC-08 — Quiz management

1. Teacher tạo quiz.
2. Tạo MCQ và options.
3. Chọn đáp án đúng.
4. Publish.
5. Student làm và submit.
6. Student xem result.

**Kỳ vọng:** Chấm ở server; quyền đúng; lịch sử attempt không mất.

## TC-09 — Code testcase

1. Teacher tạo public và hidden testcase.
2. Student mở bài code.
3. Kiểm tra payload/UI.
4. Student submit.
5. Adapter trả kết quả.

**Kỳ vọng:** Hidden expected output không lộ; score/status lưu đúng.

## TC-10 — Static resources

1. Mở DevTools Network.
2. Reload Grading và Assignments.

**Kỳ vọng:** CSS/JS chính trả 200; không có lỗi JS làm hỏng trang.

## TC-11 — Responsive

Kiểm tra khoảng rộng:

- 1440px
- 1024px
- 768px
- 390px

**Kỳ vọng:** Sidebar/content không che nhau; bảng dùng scroll/card hợp lý; action vẫn sử dụng được.
