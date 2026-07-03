# LRN-04 — Thông báo tự động trong hệ thống

- **Mã chức năng:** LRN-04
- **Module:** Learning Experience / Communication
- **Actor:** All
- **Mức ưu tiên:** Trung bình–Cao
- **Phụ thuộc:** Authentication/RBAC và các domain event liên quan

## Mục tiêu

Cung cấp hệ thống thông báo trong ứng dụng cho Student, Teacher và Admin, được tạo tự động từ các sự kiện nghiệp vụ và hiển thị theo đúng người nhận.

## Quy tắc nghiệp vụ

1. Notification phải có người nhận cụ thể hoặc tập người nhận được xác định ở backend.
2. User chỉ đọc, đánh dấu đã đọc hoặc xóa/ẩn notification của chính mình.
3. Không cho client tự tạo notification hệ thống với recipient tùy ý.
4. Loại notification phải dùng enum/constant thay vì chuỗi rời rạc.
5. Mỗi notification có tối thiểu: recipient, type, title/message, read state, createdAt và target/deep link an toàn nếu có.
6. Không lưu HTML/script không tin cậy vào message.
7. Sự kiện lặp phải có cơ chế chống notification trùng nếu nghiệp vụ yêu cầu idempotency.
8. Ví dụ sự kiện:
   - Enrollment thành công.
   - Payment thành công/thất bại.
   - Course/lesson mới được xuất bản.
   - Assignment/quiz sắp đến hạn hoặc đã chấm.
   - Chứng chỉ được cấp.
   - User/course bị Admin thay đổi trạng thái.
9. Deep link phải được allowlist theo route nội bộ; không tạo open redirect.
10. Realtime là phần bổ sung; database là nguồn sự thật để notification không mất khi offline.

## Phạm vi triển khai

### Backend
- Rà soát entity/repository/service notification hiện có.
- Tạo notification từ domain service/event sau khi transaction nghiệp vụ thành công.
- API:
  - Danh sách notification của user hiện tại có phân trang.
  - Đếm unread.
  - Mark one as read.
  - Mark all as read.
  - Xóa/ẩn nếu policy dự án cho phép.
- Nếu dùng WebSocket/SSE, chỉ push tới đúng authenticated user; không broadcast dữ liệu cá nhân.
- Chuẩn hóa pagination, sorting mới nhất trước và giới hạn page size.
- Cân nhắc transaction-after-commit để tránh gửi thông báo cho giao dịch rollback.

### Frontend
- Notification bell với unread badge.
- Dropdown/notification center có phân trang hoặc load more.
- Trạng thái unread/read rõ ràng.
- Deep link tới đúng màn hình và fallback khi target không còn.
- Loading, empty, error và reconnect state.
- Responsive trên mobile.

### Security và privacy
- Không lộ email, payment detail hoặc nội dung nhạy cảm trong thông báo.
- Kiểm tra ownership cho từng thao tác.

## Test bắt buộc

- Sự kiện nghiệp vụ tạo đúng notification cho đúng recipient.
- Transaction rollback không để lại notification sai.
- User A không đọc/mark/delete notification của User B.
- Mark one/mark all cập nhật unread count đúng.
- Pagination/sorting hoạt động.
- Duplicate event không tạo notification trùng ngoài mong muốn.
- WebSocket/SSE không gửi notification sang user khác.
- Deep link nguy hiểm hoặc external URL bị chặn.

## Tiêu chí nghiệm thu

- Mỗi role nhận đúng thông báo liên quan.
- Unread count nhất quán sau refresh và giữa các màn hình.
- Notification vẫn tồn tại khi user offline.
- Không có cross-user data leak.
- Các event tích hợp được liệt kê rõ trong báo cáo hoàn thành.

## Nguyên tắc bắt buộc dành cho Codex

1. Đọc `AGENTS.md`, `README.md`, tài liệu kiến trúc, cấu hình build và quy ước trong repository trước khi thay đổi mã nguồn.
2. Khảo sát toàn bộ luồng liên quan từ entity/model → repository → service → controller/API → security → giao diện → test.
3. Tận dụng cấu trúc, naming convention, DTO, mapper, exception handler, response format và design system đang tồn tại; không tạo kiến trúc song song.
4. Không sửa hoặc format hàng loạt các file ngoài phạm vi chức năng.
5. Không xóa hành vi hiện có chỉ để làm test mới chạy.
6. Không hard-code user, role, course, price, trạng thái, URL, secret hoặc dữ liệu môi trường.
7. Mọi quyền truy cập phải được kiểm tra ở backend; việc ẩn nút trên frontend không được xem là biện pháp bảo mật.
8. Với thay đổi database, ưu tiên migration tương thích ngược. Không drop/rename cột đang dùng nếu chưa chứng minh an toàn.
9. Bổ sung unit test và integration test cho luồng thành công, từ chối truy cập và trường hợp biên.
10. Chạy các lệnh build/test phù hợp với repository. Nếu lệnh nào không chạy được, ghi rõ nguyên nhân và phần đã kiểm tra thay thế.
11. Cuối tác vụ, báo cáo:
    - Những file đã thay đổi.
    - Migration hoặc cấu hình mới.
    - Test đã chạy và kết quả.
    - Giả định còn tồn tại.
    - Cách kiểm thử thủ công.

---

## Prompt hoàn chỉnh cho Codex

> Sao chép nguyên khối nội dung bên dưới vào Codex khi đang mở đúng repository và đúng feature branch.

```text
Hãy triển khai **LRN-04 — Hệ thống thông báo tự động trong ứng dụng cho tất cả role**.

### Kết quả cần đạt
- Notification được lưu trong database và sinh từ các sự kiện nghiệp vụ hiện có.
- Có danh sách phân trang, unread count, mark read, mark all read và hành vi xóa/ẩn theo policy dự án.
- Notification bell/center hiển thị đúng trên giao diện hiện tại.
- Nếu codebase đã có WebSocket/SSE, cập nhật realtime đến đúng authenticated user; database vẫn là nguồn sự thật.

### Hãy thực hiện
- Khảo sát notification model, event mechanism và security hiện có.
- Dùng enum cho notification type và payload/deep-link có cấu trúc, an toàn.
- Tích hợp trước với các event thực sự tồn tại như enrollment, payment, assessment result, certificate hoặc admin status change; không tạo mock event.
- Chỉ tạo notification sau khi transaction nghiệp vụ thành công.
- Không cho client gửi recipient tùy ý hoặc tạo notification hệ thống.
- Bảo vệ ownership cho list/read/delete.
- Chống duplicate khi event được xử lý lại.
- Viết test cross-user access, rollback, duplicate event, unread count, pagination, realtime isolation và deep-link validation.

### Cách làm việc bắt buộc

- Trước tiên, hãy đọc `AGENTS.md` và khảo sát repository. Không bắt đầu viết code ngay.
- Hãy xác định các thành phần đã tồn tại và lập một kế hoạch ngắn theo đúng kiến trúc hiện tại.
- Chỉ triển khai phạm vi của chức năng này và những thay đổi phụ thuộc trực tiếp.
- Không tạo entity, endpoint, service hoặc trang trùng với thành phần hiện có.
- Không sửa các chức năng không liên quan.
- Bảo đảm authorization ở backend, validation đầu vào, xử lý lỗi nhất quán và không lộ dữ liệu nhạy cảm.
- Thêm hoặc cập nhật test phù hợp.
- Chạy build/test của dự án.
- Sau khi hoàn tất, tự review diff để tìm lỗi logic, N+1 query, IDOR, race condition, lỗi trạng thái và regression.
- Trả về báo cáo gồm: kế hoạch đã thực hiện, file thay đổi, test đã chạy, kết quả, cách kiểm thử thủ công và các giả định.
```
