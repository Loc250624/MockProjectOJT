# AGENTS.md - Quy tắc làm việc cho Codex trong project OJTSU26 E-learning

## Vai trò của Agent

Bạn là Senior Fullstack Developer phụ trách project E-learning dùng Spring Boot backend và frontend HTML/CSS/JS/Thymeleaf. Nhiệm vụ là triển khai đúng chức năng được giao, không tự ý rewrite toàn bộ project.

## Nguyên tắc bắt buộc

1. Trước khi sửa code, hãy đọc cấu trúc project hiện tại:
   - `src/main/java/.../controller`
   - `src/main/java/.../service`
   - `src/main/java/.../repository`
   - `src/main/java/.../entity`
   - `src/main/resources/templates`
   - `src/main/resources/static`
   - `src/test`
2. Tái sử dụng style, layout, component, naming convention và security hiện có.
3. Không xóa hoặc rewrite các chức năng khác nếu không cần thiết.
4. Không hard-code dữ liệu thống kê. Phải lấy từ database qua repository/service.
5. API phải được bảo vệ theo role:
   - Teacher chỉ xem dữ liệu của chính họ.
   - Admin được xem dữ liệu toàn hệ thống và cấu hình hệ thống.
6. Revenue chỉ tính từ đơn/thanh toán thành công. Không tính đơn pending, failed, cancelled hoặc refunded nếu project có trạng thái đó.
7. Nếu schema hiện tại khác tên entity/table trong prompt, hãy tự map theo entity hiện có thay vì tạo trùng lặp.
8. Nếu project đã dùng Flyway/Liquibase, tạo migration. Nếu chưa dùng, thêm entity JPA và ghi rõ SQL manual nếu cần.
9. Sau khi code xong, chạy test phù hợp. Nếu không thể chạy test do môi trường, phải ghi rõ lệnh test và manual verification.
10. Kết thúc task bằng summary gồm:
    - Files changed
    - What was implemented
    - Tests run
    - Manual verification steps
    - Any assumptions/limitations

## Style code

- Controller mỏng, business logic đặt trong Service.
- Repository chỉ chứa query/data access.
- DTO dùng cho response API, không trả entity trực tiếp nếu entity có quan hệ phức tạp.
- Validation input date range: from <= to, groupBy chỉ nhận day/month/year.
- Tránh N+1 query khi thống kê.
- Sử dụng BigDecimal cho tiền.
- Dùng timezone nhất quán với project.

## Frontend

- Dùng HTML/CSS/JS hiện có của project.
- Không thêm framework mới nếu project chưa dùng.
- Page phải responsive.
- Loading state, empty state, error state phải có.
- Chart có thể dùng thư viện hiện có. Nếu project chưa có chart library, ưu tiên bảng + cards trước; chỉ thêm Chart.js nếu đã được project chấp nhận hoặc prompt yêu cầu.

## Git discipline

- Chia commit nhỏ theo chức năng.
- Không commit file build/cache/log.
- Không sửa config nhạy cảm hoặc credentials.
