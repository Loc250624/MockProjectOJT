# ASM Functional Requirements

## ASM-01 — Student làm quiz, lưu tạm và nộp bài

Codex cần xác nhận hoặc hoàn thiện:

- Student chỉ làm quiz thuộc course đã enroll và lesson/assessment được mở.
- Có thể lưu draft mà chưa chấm.
- Submit là thao tác rõ ràng, chống submit trùng do double-click.
- Sau submit, attempt chuyển trạng thái hợp lệ.
- Câu trả lời lưu theo attempt và question.
- Không tin điểm do client gửi lên.
- Trắc nghiệm được chấm ở server.
- Nếu policy cho phép làm lại, tạo attempt mới; không ghi đè lịch sử.
- UI hiển thị lỗi validation và trạng thái draft/submitted.

## ASM-02 — Student nộp bài thực hành/bài tập code

- Student xem đề, deadline, file/resource và quy định.
- Nộp nội dung text, link, file hoặc source code theo loại assignment mà model hỗ trợ.
- File upload phải kiểm tra loại, kích thước và tên file an toàn.
- Cho phép cập nhật trước deadline theo policy.
- Mỗi lần nộp cần lịch sử hoặc version nếu dự án đã có cơ chế đó.
- Sau deadline, áp dụng policy hiện hữu: chặn hoặc đánh dấu late.
- Teacher chỉ thấy submission thuộc course của mình.

## ASM-03 — Student xem kết quả

- Xem điểm quiz và kết quả chấm code.
- Xem feedback Teacher cho bài thực hành/tự luận.
- Chỉ hiển thị đáp án đúng/giải thích theo release policy.
- Không được truy cập result của Student khác bằng cách sửa ID.
- Phân biệt các trạng thái: Draft, Submitted, Pending grading, Graded, Failed execution, Published.

## ASM-04 — Teacher quản lý Quiz/Question/Answer

Teacher có thể:

- Xem danh sách quiz theo course.
- Tạo, cập nhật, archive/xóa theo policy hiện hữu.
- Tạo/sửa/xóa/reorder question.
- Quản lý answer options và đáp án đúng.
- Cấu hình điểm, thời gian, số lần làm, pass score, publish status nếu schema hỗ trợ.
- Validation:
  - quiz phải thuộc course Teacher sở hữu;
  - question phải có nội dung;
  - MCQ phải có đủ option;
  - phải có đáp án đúng phù hợp;
  - tổng điểm hợp lệ.
- Không xóa cứng dữ liệu đã có attempt nếu việc đó phá lịch sử; ưu tiên archive/soft delete hoặc chặn xóa.

## ASM-05 — Code testcase và chấm tự động

Teacher có thể:

- Tạo/sửa/xóa testcase thuộc coding assignment của course mình.
- Cấu hình input, expected output, weight/score, visibility.
- Hidden testcase không được gửi cho Student.
- Khi Student submit code:
  - backend tạo yêu cầu chấm;
  - adapter gửi đến dịch vụ sandbox;
  - lưu trạng thái và kết quả;
  - tính điểm tại server;
  - timeout/error được xử lý rõ ràng.
- Không chạy source code trực tiếp bằng `Runtime.exec`, `ProcessBuilder`, script shell hoặc compiler trong tiến trình web.
- Adapter phải có timeout, error mapping và cấu hình qua property/environment.
- Test tự động dùng fake adapter; production HTTP adapter chỉ hoạt động khi được cấu hình.

## ASM-06 — Teacher chấm và nhận xét

Teacher có thể:

- Xem queue các submission cần chấm.
- Lọc theo course, assignment, status, student.
- Mở chi tiết submission.
- Tải/xem file đính kèm an toàn.
- Nhập score trong khoảng cho phép.
- Nhập feedback.
- Lưu draft grade và publish/finalize nếu domain hiện có phân biệt hai trạng thái.
- Cập nhật điểm có audit metadata tối thiểu:
  - gradedBy;
  - gradedAt;
  - updatedAt;
  - trạng thái.
- Student chỉ thấy kết quả khi đã publish, nếu hệ thống có release policy.
- Ngăn Teacher chấm submission ngoài course sở hữu.

## Yêu cầu chung cho Teacher UI

### Grading page

Phải có tối thiểu:

- Tiêu đề và mô tả ngắn.
- Summary cards hoặc số lượng:
  - pending;
  - graded;
  - late/failed nếu có.
- Bộ lọc course, assignment, status, student/search.
- Bảng submission có:
  - student;
  - course;
  - assignment;
  - submitted time;
  - status;
  - current score;
  - action.
- Empty state đúng khi không có dữ liệu.
- Pagination nếu danh sách lớn.
- Nút xem/chấm mở trang hoặc modal có backend thật.

### Assignments page

Phải có tối thiểu:

- Danh sách assignment thuộc các course Teacher sở hữu.
- Tạo assignment.
- Sửa, publish/archive theo policy.
- Hiển thị submission count và pending grading count.
- Link sang grading queue được filter theo assignment.
- Không hiển thị dòng mẫu hard-code.

## Quyền và ownership

- Mọi truy vấn theo ID phải kèm kiểm tra ownership.
- Không chỉ ẩn nút ở frontend; backend phải chặn.
- Trả về 403 cho truy cập không có quyền, 404 hoặc policy thống nhất cho tài nguyên không tồn tại/không được thấy.
- Không nhận `teacherId` từ form/client để quyết định quyền; lấy principal hiện tại.
