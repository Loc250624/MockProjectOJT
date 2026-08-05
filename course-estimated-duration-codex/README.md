# Codex Work Package — Estimated Course Completion Time

Gói này dùng để giao việc cho Codex trong dự án **OJTSU26 E-Learning**.

## Mục tiêu

Mỗi course phải hiển thị **thời gian hoàn thành dự tính**, được tính bằng:

> Tổng thời lượng của tất cả lesson có video thuộc đúng course đó.

Không cộng thời gian quiz, text lesson, tài liệu, assignment, course khác hoặc bất kỳ nội dung không phải video.

## Cách sử dụng

1. Giải nén gói vào thư mục tạm, không chép đè trực tiếp lên source code.
2. Mở repository dự án bằng Codex.
3. Gửi toàn bộ nội dung của `CODEX_PROMPT_VI.md` cho Codex.
4. Cho phép Codex đọc thêm các file trong `docs/`, `reference/` và `checklists/`.
5. Yêu cầu Codex tự khảo sát tên entity, table, DTO, service, controller và template thực tế trước khi sửa.

## Lưu ý

Các file trong `reference/` là mẫu triển khai và quy tắc tính toán. Codex phải điều chỉnh theo cấu trúc thật của repository, không được tạo class trùng chức năng chỉ vì tên file khác nhau.
