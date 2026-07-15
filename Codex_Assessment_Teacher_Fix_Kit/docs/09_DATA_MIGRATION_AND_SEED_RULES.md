# Data Migration and Seed Rules

## Trước khi thay đổi schema

1. Kiểm tra dự án có Flyway/Liquibase hay chỉ JPA DDL.
2. Kiểm tra entity/table hiện có.
3. Không tạo bảng trùng chức năng.
4. Không đổi tên enum/column mà không migration dữ liệu.
5. Không bật `create-drop` hoặc xóa database.

## Các trường có thể cần, chỉ thêm khi model hiện tại thiếu

### Assignment/Submission

- assignment type
- max score
- publish/archive status
- submission status
- submittedAt
- gradedAt
- gradedBy
- score
- feedback
- late flag
- version/audit fields

### Quiz

- status
- pass score
- max attempts
- time limit
- question order
- answer correctness
- attempt status
- submittedAt
- calculated score

### Code grading

- testcase visibility
- input
- expected output
- weight
- execution status
- execution message
- runtime/memory metadata nếu service cung cấp
- adapter request/reference id

Đây không phải lệnh bắt buộc thêm tất cả field. Codex phải map với domain hiện hữu và chỉ bổ sung phần thiếu.

## Seed data

Nếu cần seed cho manual verification:

- Dùng profile dev/test.
- Không chèn tự động vào production.
- Tạo tối thiểu:
  - Teacher A và Teacher B;
  - Student;
  - course của từng Teacher;
  - assignment và submission;
  - quiz;
  - coding assignment/testcase.
- Không dùng mật khẩu thật hoặc secret.
