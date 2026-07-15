# Prompts theo từng bước cho Codex

Dùng khi muốn kiểm soát Codex theo từng giai đoạn thay vì gửi một prompt lớn.

## Prompt 1 — Audit

```text
Đọc AGENTS.md và toàn bộ docs trong bộ Assessment Teacher Fix Kit. Chưa sửa code.

Hãy audit repository:
- map route Grading/Assignments;
- controller/service/repository/entity/DTO;
- Teacher layout/fragment/CSS/JS;
- SecurityConfig và ownership;
- dữ liệu hard-code;
- test hiện có;
- ASM-01 đến ASM-06.

Xác minh tại sao Grading trống và Assignments mất CSS/dùng dữ liệu mẫu. Trả về root causes có bằng chứng file + đoạn code liên quan, cùng file-level implementation plan. Không đoán.
```

## Prompt 2 — Fix layout/render

```text
Dựa trên audit đã xác minh, sửa riêng lỗi render/layout/static assets của Teacher Grading và Assignments.

Yêu cầu:
- dùng Instructor Portal layout hiện có;
- main content không bị ẩn;
- CSS/JS tải đúng;
- fragment hợp lệ;
- sidebar active đúng;
- empty state rõ ràng;
- responsive;
- không thêm dữ liệu hard-code.

Thêm test MVC/template/static resource phù hợp. Chạy test liên quan và báo file đã sửa.
```

## Prompt 3 — Replace hard-code with real data

```text
Thay toàn bộ dữ liệu mẫu/hard-code tại Teacher Grading và Assignments bằng dữ liệu database.

Yêu cầu:
- query theo authenticated Teacher;
- ownership ở backend;
- filter/pagination;
- không nhận teacherId từ client để cấp quyền;
- tránh N+1 nếu có thể;
- DTO/view model rõ ràng;
- Teacher A không thấy dữ liệu Teacher B;
- Student bị chặn.

Thêm repository/service/controller tests và chạy test.
```

## Prompt 4 — ASM-06 grading

```text
Hoàn thiện ASM-06 end-to-end:
- grading queue;
- filter;
- submission detail;
- score validation;
- feedback;
- draft/final publish theo domain hiện có;
- gradedBy/gradedAt/status;
- Student chỉ thấy kết quả theo release policy;
- audit/ownership;
- error/success UX.

Không bỏ qua backend security. Thêm test tích hợp Teacher submit → grade → Student view.
```

## Prompt 5 — ASM-04 quiz management

```text
Hoàn thiện ASM-04 theo architecture hiện có:
- CRUD/archive quiz;
- CRUD/reorder question;
- answer options/correct answer;
- validation;
- course ownership;
- không xóa làm mất attempt history;
- Teacher UI thống nhất;
- Student ASM-01 và ASM-03 regression tests.

Không tạo entity/bảng trùng nếu dự án đã có.
```

## Prompt 6 — ASM-05 code grading

```text
Hoàn thiện ASM-05 an toàn:
- Teacher CRUD testcase;
- public/hidden visibility;
- weight/score validation;
- code execution adapter;
- timeout/error status;
- server-side score;
- hidden testcase không lộ;
- fake adapter trong test.

Cấm thực thi code Student trực tiếp bằng Runtime.exec, ProcessBuilder hoặc shell trong tiến trình Spring Boot. Nếu chưa có runner production, thêm HTTP adapter cấu hình ngoài code và trạng thái unavailable trung thực.
```

## Prompt 7 — Full regression and report

```text
Chạy full regression theo docs/06_TEST_PLAN.md và docs/08_MANUAL_TEST_CASES.md.

Chạy ./mvnw test hoặc .\mvnw.cmd test.
Kiểm tra ASM-01 đến ASM-06 và AC-01 đến AC-08.
Không sửa test chỉ để che lỗi.

Trả báo cáo theo docs/10_CODEX_OUTPUT_TEMPLATE.md, gồm root causes, file changes, migration, security, test result, PASS/FAIL và limitation.
```
