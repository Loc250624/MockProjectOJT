# Codex ANL/SYS Maintenance & Test Kit

Bộ tài liệu này giúp Codex kiểm tra, tìm nguyên nhân, sửa lỗi và kiểm thử hồi quy cho các chức năng:

| Mã | Chức năng | Vai trò |
|---|---|---|
| ANL-01 | Thống kê doanh thu cá nhân của giảng viên | Teacher |
| ANL-02 | Dashboard tổng quan toàn hệ thống | Admin |
| ANL-03 | Thống kê học viên mới và học viên hoạt động | Admin |
| ANL-04 | Báo cáo doanh thu theo ngày/tháng/năm | Admin |
| SYS-01 | Cấu hình thông số hệ thống chung | Admin |

## Mục tiêu

Codex phải xác minh cả 5 lớp sau:

1. **Routing & Security**: URL, controller mapping, role guard, điều hướng.
2. **Backend**: controller, service, repository, DTO, validation, exception handling.
3. **Dữ liệu**: truy vấn doanh thu, phạm vi dữ liệu, timezone, trạng thái thanh toán, người dùng mới/hoạt động.
4. **Frontend**: Thymeleaf, HTML, CSS, JavaScript, chart, responsive, trạng thái loading/empty/error.
5. **Regression**: không làm hỏng login, dashboard, payment, enrollment, course và các module khác.

## Quy trình bắt buộc

### Giai đoạn 1 — Discovery

Codex phải:
- đọc `AGENTS.md`;
- lập inventory file liên quan;
- tìm route thực tế;
- xác định entity và trạng thái nghiệp vụ;
- xác định nguồn dữ liệu dashboard;
- ghi nhận lỗi bằng bằng chứng trước khi sửa.

Đầu ra bắt buộc: `reports/01-discovery-report.md`.

### Giai đoạn 2 — Test trước khi sửa

Codex phải chạy:
- Maven test;
- kiểm tra compile;
- test endpoint/controller/service/repository hiện có;
- test thủ công UI nếu repository không có browser automation;
- ghi lỗi tái hiện được.

Đầu ra bắt buộc: `reports/02-baseline-test-report.md`.

### Giai đoạn 3 — Fix

Chỉ sửa sau khi có:
- nguyên nhân gốc;
- file bị ảnh hưởng;
- phạm vi thay đổi;
- tiêu chí chấp nhận;
- kế hoạch test lại.

### Giai đoạn 4 — Regression

Chạy lại:
- test tự động;
- test từng chức năng;
- test RBAC;
- test dữ liệu biên;
- test responsive;
- test không có lỗi console/server.

Đầu ra bắt buộc: `reports/03-regression-report.md`.

### Giai đoạn 5 — Final Report

Codex phải tạo:
- danh sách file sửa;
- lỗi gốc;
- cách sửa;
- test đã chạy;
- test chưa thể chạy;
- rủi ro còn lại;
- hướng dẫn kiểm tra thủ công.

Đầu ra bắt buộc: `reports/04-final-report.md`.

## Cách gọi Codex

Dùng prompt tổng trong `01_MASTER_PROMPT.md`. Sau đó có thể giao từng chức năng bằng prompt trong thư mục `prompts/`.

Ví dụ:

```text
Đọc AGENTS.md, README_VI.md và prompts/ANL-01.md.
Thực hiện đầy đủ DISCOVERY và BASELINE TEST trước.
Không sửa code cho đến khi đã tạo báo cáo nguyên nhân gốc.
Sau đó sửa ANL-01, chạy regression và tạo final report.
```

## Nguyên tắc an toàn

- Không hard-code doanh thu hoặc số liệu dashboard.
- Không dùng dữ liệu mock trong production path.
- Không đổi endpoint công khai nếu không cần thiết.
- Không hard delete dữ liệu.
- Không vô hiệu hóa Spring Security để “làm cho chạy”.
- Không sửa migration cũ đã dùng ở môi trường khác.
- Không chạy `DROP`, `TRUNCATE`, `DELETE` diện rộng.
- Không commit secrets.
- Không sửa ngoài phạm vi nếu không có dependency trực tiếp.
