# Codex package — Simplify Feedback UI

Gói này dùng để giao cho Codex chỉnh khu vực Feedback theo yêu cầu:

- Bỏ hoàn toàn phần **Category / Choose a topic** khỏi giao diện.
- Giữ nguyên chức năng chấm điểm theo sao.
- Tái bố cục thành một khối Feedback gọn, cân đối, dễ thao tác.
- Không làm hỏng API gửi feedback nếu backend hiện vẫn yêu cầu trường `category`.
- Không sửa các khu vực không liên quan.

## Cách dùng

1. Giải nén gói này tại thư mục gốc dự án hoặc mở cùng workspace dự án.
2. Gửi nội dung file `CODEX_TASK.md` cho Codex.
3. Cho Codex đọc thêm các file đặc tả trong gói trước khi thay đổi code.
4. Yêu cầu Codex chạy test/build/lint hiện có của dự án và báo cáo file đã sửa.

## Lưu ý

Do gói hiện chỉ có ảnh giao diện, không có source code thực tế của dự án, các file trong `reference/` là mẫu tham khảo. Codex phải tìm đúng component, stylesheet, DTO/type và service thật trong repository rồi áp dụng thay đổi tối thiểu.
