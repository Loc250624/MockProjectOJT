# AGENTS.md — Assessment Teacher Repair

## Nhiệm vụ

Sửa và hoàn thiện các chức năng Assessment từ ASM-01 đến ASM-06, ưu tiên lỗi khi đăng nhập bằng role `TEACHER` tại khu vực **Grading** và **Assignments**.

## Nguyên tắc bắt buộc

1. Đọc cấu trúc dự án trước khi sửa. Không đoán tên package, route, entity hoặc bảng.
2. Tái sử dụng kiến trúc, convention, layout và design system hiện có.
3. Không thay toàn bộ trang bằng một implementation độc lập làm mất tích hợp với dự án.
4. Không hard-code:
   - người học;
   - khóa học;
   - bài tập;
   - bài nộp;
   - điểm;
   - trạng thái;
   - nhận xét.
5. Teacher chỉ được xem và thao tác trên course/assessment thuộc quyền quản lý của họ.
6. Student không được gọi API quản trị/chấm điểm của Teacher.
7. Admin không bị mất quyền hiện có.
8. Không dùng dữ liệu giả trong production code. Mock chỉ được phép trong test.
9. Mọi form ghi dữ liệu phải:
   - dùng validation;
   - có CSRF theo cấu hình Spring Security hiện tại;
   - xử lý lỗi và hiển thị thông báo rõ ràng;
   - chống sửa ID để truy cập dữ liệu của Teacher khác.
10. Không thực thi code không tin cậy trực tiếp trong JVM của ứng dụng web.
11. Với chấm code tự động:
   - ưu tiên adapter/service đã tồn tại;
   - nếu chưa có, tạo interface adapter và HTTP implementation cấu hình qua environment/property;
   - tạo fake adapter chỉ trong test;
   - không tuyên bố auto-grading chạy production nếu chưa có dịch vụ sandbox thực.
12. Không sửa schema bằng cách xóa dữ liệu hiện có.
13. Migration phải tương thích với cơ chế migration hiện hữu. Nếu dự án chưa dùng Flyway/Liquibase, cung cấp file SQL riêng và giải thích cách chạy.
14. Không đưa secret, token, mật khẩu hoặc URL nội bộ vào Git.
15. Không kết thúc công việc chỉ vì trang render được. Phải kiểm tra end-to-end.

## Quy trình làm việc

### Giai đoạn 1 — Khảo sát

Tìm và lập bản đồ:

- Controller Teacher, Student, API liên quan Assessment.
- SecurityConfig, method security, ownership checks.
- Entity, DTO, repository, service cho:
  - quiz;
  - question;
  - answer/option;
  - quiz attempt/result;
  - assignment;
  - submission;
  - grading;
  - code testcase;
  - code execution result.
- Template và fragment Teacher.
- CSS/JS của Teacher.
- Route và link trong sidebar.
- Dữ liệu seed/migration.
- Test hiện có.

Trước khi sửa, ghi ra nguyên nhân đã xác minh. Không được chỉ suy đoán từ ảnh.

### Giai đoạn 2 — Sửa nền tảng render và layout

Kiểm tra:

- `th:replace`, `th:insert`, fragment signature.
- Model attribute mà layout/content cần.
- URL CSS/JS dùng Thymeleaf `@{...}` hoặc đường dẫn phù hợp.
- Static resource mapping.
- HTML hợp lệ, không lồng sai `html`, `body`, `main`.
- CSS không làm content bị ẩn, height bằng 0, nằm ngoài viewport hoặc bị overlay.
- Console JS không có lỗi làm ngắt render.
- Active navigation đúng cho Grading/Assignments.
- Responsive trên desktop, tablet và mobile.

### Giai đoạn 3 — Hoàn thiện nghiệp vụ

Thực hiện theo `docs/02_ASM_FUNCTIONAL_REQUIREMENTS.md`.

### Giai đoạn 4 — Kiểm thử

Thực hiện theo:

- `docs/06_TEST_PLAN.md`
- `docs/07_ACCEPTANCE_CRITERIA.md`
- `docs/08_MANUAL_TEST_CASES.md`

## Lệnh kiểm tra ưu tiên

Trên Linux/macOS/Git Bash:

```bash
./mvnw test
./mvnw spring-boot:run
```

Trên Windows PowerShell:

```powershell
.\mvnw.cmd test
.\mvnw.cmd spring-boot:run
```

Nếu test thất bại do môi trường, phải phân biệt rõ:
- lỗi code;
- lỗi database;
- lỗi cấu hình;
- dịch vụ bên ngoài chưa có.

## Báo cáo cuối cùng

Báo cáo phải có:

1. Root causes.
2. File changed.
3. Database changes.
4. Security/ownership enforcement.
5. Tests run và kết quả.
6. Manual verification.
7. Bảng ASM-01 → ASM-06.
8. Remaining limitations.
