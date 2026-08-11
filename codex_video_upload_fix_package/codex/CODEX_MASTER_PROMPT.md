# MASTER PROMPT FOR CODEX

Bạn là Senior Fullstack Developer. Hãy sửa trực tiếp repository E-Learning hiện tại.

## Nhiệm vụ
Hiện form Add Video có URL YouTube nhưng nút `+ Add Video` không hoạt động/không hoàn thành việc thêm video.

Cần:
1. Tìm và sửa root cause khiến Add Video lỗi.
2. Bổ sung 2 mode:
   - Upload video từ máy tính.
   - Nhập Video URL.
3. URL hỗ trợ:
   - YouTube.
   - Direct public video URL.
   - Provider embed khác chỉ khi an toàn/được hỗ trợ bằng adapter; không scrape website.
4. Tự xác định video duration trước hoặc trong lúc lưu bằng cơ chế đáng tin cậy.
5. Sửa mọi UI/backend bug trực tiếp liên quan tới luồng video.
6. Kiểm tra read-side: Teacher preview, Student playback, course duration/progress.
7. Không sửa module không liên quan.

## Quy trình bắt buộc
- Đọc `AGENTS.md`.
- Đọc toàn bộ `docs/`.
- Audit repository trước khi sửa.
- Reproduce lỗi hiện tại.
- Ghi root cause.
- Sửa nhỏ nhất nhưng kiến trúc phải hỗ trợ cả UPLOAD và URL.
- Thêm/điều chỉnh tests.
- Chạy tests.
- Điền `templates/CHANGE_REPORT_TEMPLATE.md`.

## Không được làm
- Không chỉ remove `disabled` của button.
- Không disable CSRF.
- Không `permitAll` endpoint quản lý lesson.
- Không tin MIME/filename do browser gửi.
- Không cho backend gọi arbitrary internal URLs.
- Không hard-code path máy developer.
- Không lưu upload bằng original filename.
- Không thêm dependency lớn nếu không cần.
- Không rewrite toàn bộ lesson module.
- Không sửa Payment, Quiz, Blog, Notification, AI Chatbot hoặc auth flow nếu không có dependency thực tế.

## Đầu ra cuối cùng của Codex
1. Source code đã sửa.
2. Migration/config cần thiết.
3. Tests.
4. `CHANGE_REPORT.md` gồm:
   - root cause
   - file changed
   - API/schema changes
   - security considerations
   - test evidence
   - manual test steps
   - deployment notes
   - remaining risks
