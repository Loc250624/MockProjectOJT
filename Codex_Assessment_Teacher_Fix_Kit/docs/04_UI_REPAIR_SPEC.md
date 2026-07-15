# UI Repair Specification

## Mục tiêu hình ảnh

Teacher Grading và Assignments phải nằm trong cùng Instructor Portal đã có:

- Nền, typography, spacing, sidebar và header nhất quán.
- Main content bắt đầu bên phải sidebar trên desktop.
- Không để content bị phủ bởi sidebar hoặc header.
- Sidebar có active state đúng.
- Footer không nằm ngay giữa nội dung khi trang còn dữ liệu.
- Không dùng style mặc định của trình duyệt.

## Grading UI đề xuất

```text
Page header
  Grading
  Review and publish student assessment results

Summary
  Pending review | Graded | Late | Execution errors

Filters
  Course | Assignment | Status | Search student | Reset

Submission table
  Student | Course | Assessment | Submitted | Status | Score | Action

Pagination / empty state
```

## Assignment UI đề xuất

```text
Page header
  Assignments
  Create and manage assignments for your courses
  [Create Assignment]

Filters
  Course | Type | Publish status | Search

Assignment cards/table
  Title | Course | Type | Due date | Status
  Submissions | Pending grading | Actions
```

## Chi tiết chấm điểm

- Hiển thị thông tin Student và submission.
- Nội dung/file/code.
- Rubric nếu model hiện có.
- Score/max score.
- Feedback.
- Save draft.
- Publish grade.
- Back to queue.
- Xác nhận trước khi publish nếu thao tác khó hoàn tác.

## Thymeleaf/static resource rules

- Dùng `th:href="@{/...}"`, `th:src="@{/...}"` nếu project dùng Thymeleaf.
- Fragment phải có contract rõ ràng.
- Không đặt nguyên thẻ `<html>` trong content fragment được nhúng vào layout.
- Không dùng URL tương đối kiểu `../../css/...`.
- Không render dữ liệu mẫu khi collection rỗng; dùng empty state.
- Escape output mặc định; chỉ dùng `th:utext` khi có sanitizer và lý do rõ ràng.

## Accessibility cơ bản

- Heading hierarchy hợp lý.
- Input có label.
- Button không chỉ dựa vào icon.
- Bảng có header.
- Keyboard focus nhìn thấy.
- Modal có focus management nếu dùng modal.
- Status không chỉ phân biệt bằng màu.
