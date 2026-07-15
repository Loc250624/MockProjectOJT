# Implementation Plan for Codex

## 1. Inventory trước khi thay đổi

Tìm bằng `rg` hoặc IDE:

```text
grading
assignment
submission
quiz
question
answer
testcase
attempt
result
teacher
instructor
th:replace
teacher.css
student.css
SecurityConfig
PreAuthorize
```

Lập bảng:

| Layer | File hiện có | Route/method | Vấn đề | Hành động |
|---|---|---|---|---|
| Controller | | | | |
| Service | | | | |
| Repository | | | | |
| Entity/DTO | | | | |
| Template | | | | |
| CSS/JS | | | | |
| Security | | | | |
| Test | | | | |

## 2. Xác minh root cause của lỗi render

### Grading trống

Kiểm tra lần lượt:

1. Link sidebar trỏ đến route nào.
2. Route có tồn tại và trả đúng view name không.
3. Có redirect loop/403/exception bị custom error page che không.
4. Template có thực sự chèn content fragment vào layout không.
5. Tên fragment và tham số có khớp không.
6. Model attribute được template tham chiếu có null không.
7. CSS có `display:none`, `position`, `z-index`, `overflow`, `height` gây ẩn không.
8. JS có exception trước khi render/khởi tạo không.
9. Network có 404 CSS/JS không.
10. HTML có đóng/mở tag sai không.

### Assignments mất CSS

Kiểm tra:

1. Template có dùng đúng Teacher layout không.
2. CSS link có đường dẫn tương đối sai ở route lồng nhau không.
3. Static resource có bị Spring Security chặn không.
4. Fragment header/sidebar có bị gọi hai lần không.
5. Template có khai báo một trang hoàn chỉnh bên trong fragment content không.
6. Browser/network hoặc test phải xác nhận static assets trả HTTP 200.
7. Xóa toàn bộ row mẫu/hard-code và thay bằng `th:each` từ model.

## 3. Thiết kế route

Không bắt buộc đổi route hiện hữu. Ưu tiên tương thích. Route tham khảo:

```text
GET  /teacher/grading
GET  /teacher/grading/{submissionId}
POST /teacher/grading/{submissionId}/draft
POST /teacher/grading/{submissionId}/publish

GET  /teacher/assignments
GET  /teacher/assignments/new
POST /teacher/assignments
GET  /teacher/assignments/{id}/edit
POST /teacher/assignments/{id}
POST /teacher/assignments/{id}/archive

GET/POST ... teacher quiz management ...
GET/POST ... teacher code testcase management ...
```

Nếu project đang dùng REST API + JS, giữ pattern đó. Không tạo song song MVC và REST trùng nghiệp vụ mà không cần thiết.

## 4. Service boundary

Nghiệp vụ phải nằm ở service, không dồn vào controller:

- `TeacherAssessmentQueryService`
- `AssignmentManagementService`
- `SubmissionGradingService`
- `QuizManagementService`
- `CodeGradingService`
- `CodeExecutionAdapter`

Tên cụ thể phải theo convention dự án; đây chỉ là gợi ý.

## 5. Repository queries

- Query theo authenticated Teacher và ownership.
- Tránh load toàn bộ rồi lọc trong Java.
- Dùng pagination.
- Tránh N+1 bằng fetch/entity graph/projection phù hợp.
- Không dùng native SQL nếu JPA query đáp ứng được và project không có convention bắt buộc.

## 6. DTO và validation

Không bind trực tiếp entity cho form phức tạp. Tạo request DTO có validation:

- score min/max;
- feedback length;
- deadline;
- max score;
- testcase weight;
- quiz configuration.

## 7. Transaction và concurrency

- Grade update chạy trong transaction.
- Cân nhắc optimistic locking nếu entity đã có `@Version`.
- Chống submit/publish lặp.
- Không ghi đè kết quả finalized ngoài policy.

## 8. UI

- Dùng layout/sidebar/header/footer hiện có.
- Không tạo một bộ CSS hoàn toàn khác.
- Có loading, empty, error, success state.
- Các form có label, focus state và validation message.
- Table responsive; trên mobile có horizontal scroll hoặc card layout.
- Không làm mất light/dark mode nếu dự án có.

## 9. Backward compatibility

- Giữ Student assessment routes hoạt động.
- Không đổi enum/database value tùy tiện.
- Nếu cần mapping mới, thêm migration và backward-compatible converter.
- Không xóa API mà template/JS hiện tại đang dùng.
