# Codex Work Package — Fix Add Video + Local Upload + External Video URL

## Mục tiêu
Gói này dùng để giao cho Codex xử lý toàn bộ luồng **Add Video** của dự án E-Learning theo hướng an toàn, ít ảnh hưởng nhất đến các chức năng khác.

Yêu cầu chính:
1. Sửa lỗi hiện tại khiến nút **+ Add Video** không thể thêm video.
2. Cho phép Teacher thêm video theo **2 nguồn**:
   - Upload video từ máy tính.
   - Nhập URL video bên ngoài.
3. URL phải hỗ trợ tối thiểu:
   - YouTube.
   - Direct public video URL (`https://.../video.mp4`, `webm`, v.v.).
   - Provider có thể embed an toàn như Vimeo nếu kiến trúc hiện tại cho phép.
4. Tự lấy **duration** của video trước khi lưu nếu có thể xác định đáng tin cậy.
5. Audit và sửa toàn bộ lỗi UI/backend có liên quan tới video lesson.
6. Không sửa các module không liên quan.

## Nguyên tắc bắt buộc
- Không hard-code chỉ một video/provider.
- Không scrape/downloader video từ website bên thứ ba.
- Không cho backend fetch URL tùy ý mà không có SSRF protection.
- Không tin duration do client gửi lên nếu backend có thể xác minh.
- Không làm hỏng flow lesson hiện tại, course progress, estimated duration, enrollment, quiz, payment.
- Giữ CSRF và authorization.
- Không vô hiệu hóa validation để “cho chạy”.
- Tất cả thay đổi schema phải backward-compatible hoặc có migration rõ ràng.

## Cách sử dụng
1. Giải nén gói vào **root repository**.
2. Cho Codex đọc `AGENTS.md`.
3. Yêu cầu Codex chạy audit trước, sau đó mới sửa code.
4. Codex phải điền `templates/CHANGE_REPORT_TEMPLATE.md` thành báo cáo cuối.
5. Chỉ commit các file thực sự liên quan.

> Gói này không chứa source code của dự án vì source repository chưa được cung cấp trong cuộc trò chuyện. Codex phải xác định đúng file thật trong repository thay vì đoán tên/path.
