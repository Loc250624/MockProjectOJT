# MASTER PROMPT CHO CODEX

Bạn đang làm việc trong một dự án E-Learning dùng Spring Boot, Spring MVC, Spring Data JPA, Spring Security, Thymeleaf, HTML, CSS, JavaScript và database quan hệ.

## Phạm vi duy nhất

- ANL-01 — Teacher: Thống kê doanh thu cá nhân của giảng viên
- ANL-02 — Admin: Dashboard tổng quan toàn hệ thống
- ANL-03 — Admin: Thống kê học viên mới và học viên hoạt động
- ANL-04 — Admin: Báo cáo doanh thu theo ngày/tháng/năm
- SYS-01 — Admin: Cấu hình thông số hệ thống chung

Hiện tại chức năng hoạt động sai và UI không đúng mong muốn.

## Yêu cầu làm việc

Đọc toàn bộ:
- `AGENTS.md`
- `README_VI.md`
- `02_REPOSITORY_DISCOVERY.md`
- `03_FEATURE_TEST_MATRIX.md`
- `04_UI_UX_ACCEPTANCE_CRITERIA.md`
- `05_BACKEND_DATA_VALIDATION.md`
- `06_TEST_EXECUTION_RUNBOOK.md`
- prompt từng chức năng trong `prompts/`

Sau đó thực hiện theo đúng 5 giai đoạn.

## Giai đoạn 1 — DISCOVERY

Không sửa code.

1. Kiểm tra Git status, branch hiện tại và thay đổi chưa commit.
2. Tìm tất cả controller, service, repository, entity, DTO, template, CSS, JS, test, SQL/migration liên quan đến analytics, dashboard, revenue, payment, order, enrollment, activity, settings/configuration.
3. Lập bảng route:
   - URL
   - HTTP method
   - role
   - controller method
   - template hoặc response DTO
4. Xác định:
   - trạng thái payment/order nào được tính doanh thu;
   - quan hệ Teacher -> Course -> Order/Payment;
   - định nghĩa new student;
   - dữ liệu có thể dùng cho active student;
   - timezone đang dùng;
   - cách lưu system setting.
5. Chạy build/test ban đầu.
6. Tạo `reports/01-discovery-report.md`.

## Giai đoạn 2 — BASELINE TEST

Không sửa code cho đến khi ghi nhận lỗi tái hiện được.

1. Kiểm thử từng chức năng theo `03_FEATURE_TEST_MATRIX.md`.
2. Ghi:
   - bước tái hiện;
   - dữ liệu test;
   - expected;
   - actual;
   - log/stack trace;
   - screenshot hoặc mô tả UI;
   - root-cause hypothesis.
3. Tạo `reports/02-baseline-test-report.md`.

## Giai đoạn 3 — MINIMAL FIX

1. Chỉ sửa lỗi có bằng chứng.
2. Ưu tiên sửa logic tại service/repository thay vì tính toán trong template/JavaScript.
3. Dùng DTO/view model rõ ràng.
4. Thêm validation và exception handling phù hợp.
5. Giữ nguyên layout/design system hiện có; chỉ điều chỉnh để UI đồng nhất, dễ đọc, responsive.
6. Không hard-code số liệu.
7. Không vô hiệu hóa security.
8. Không xóa dữ liệu.
9. Thêm hoặc cập nhật test.

## Giai đoạn 4 — REGRESSION

1. Chạy test tự động.
2. Kiểm tra RBAC Student/Teacher/Admin.
3. Kiểm tra dữ liệu 0, 1, nhiều bản ghi.
4. Kiểm tra ngày biên, timezone, trạng thái payment.
5. Kiểm tra desktop/tablet/mobile.
6. Kiểm tra console trình duyệt và server log.
7. Tạo `reports/03-regression-report.md`.

## Giai đoạn 5 — FINAL REPORT

Tạo `reports/04-final-report.md` theo `templates/FINAL_REPORT_TEMPLATE.md`.

## Kết quả bắt buộc

- Code sửa hoàn chỉnh.
- Test mới/cập nhật.
- Không còn dữ liệu mock trong luồng thật.
- Không còn lỗi route/RBAC.
- Số liệu giữa KPI, chart và table thống nhất.
- UI đúng role, rõ ràng, responsive.
- Báo cáo đầy đủ.
- Danh sách lệnh manual verification.

Nếu chưa đủ thông tin, hãy tự khảo sát repository trước. Không hỏi người dùng những điều có thể tìm được trong code.
