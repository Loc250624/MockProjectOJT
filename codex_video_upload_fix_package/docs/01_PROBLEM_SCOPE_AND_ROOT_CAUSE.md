# 01 — Problem Scope & Root-Cause Checklist

## Hiện tượng quan sát được
UI Add Video hiện có:
- Label `VIDEO URL *`
- URL YouTube hợp lệ theo hình thức
- Help text cho `http://` hoặc `https://`
- Thông báo duration sẽ được detect trước khi save
- Nút `+ Add Video` đang ở trạng thái không thể thao tác/không hoàn thành action

Điểm đáng nghi nhất cần Codex xác minh:
1. State `disabled` của button phụ thuộc `duration` nhưng duration detector không resolve cho YouTube.
2. URL regex/validator frontend chấp nhận, nhưng backend validator lại từ chối, hoặc ngược lại.
3. JS listener không attach do selector/id thay đổi.
4. `fetch`/XHR bị CSRF 403.
5. Endpoint add video không khớp form action/method.
6. DTO field name khác với JSON/form field.
7. Controller nhận `@RequestBody` trong khi frontend gửi `FormData`, hoặc ngược lại.
8. Transaction fail do constraint/schema.
9. Server không thể lấy metadata YouTube/direct URL.
10. Error bị catch nhưng UI chỉ reset message mà không enable button.

## Scope phải audit

### Frontend
- Template/fragment chứa Add Video modal/form.
- Script mở modal.
- Script validate URL.
- Script detect duration.
- Script submit.
- Script close/cancel/reset.
- Disabled/loading state.
- Error/success toast/modal.
- CSRF token wiring.
- Responsive layout.
- Dark mode CSS, nếu UI dùng theme system.
- Edit Video form nếu dùng chung component.
- Lesson list/card sau khi add.

### Backend
- Controller endpoint add/edit video.
- Request DTO/form object.
- Bean Validation.
- Service orchestration.
- Video metadata resolver.
- YouTube URL parser.
- Remote URL validator.
- Storage service.
- Entity mapping.
- Repository.
- Transaction boundaries.
- Exception mapping.
- Security/role checks.
- CSRF handling.
- Multipart configuration.
- Static/media serving endpoint nếu upload local.

### Database
- video URL/path column length.
- source type.
- duration data type/nullable.
- filename/mime metadata nếu thêm upload.
- migration/backfill.
- FK/cascade khi delete lesson/course.
- unique constraint bất hợp lý.

### Read-side
- Teacher preview.
- Student lesson player.
- Course detail.
- Course progress.
- Course total/estimated duration.
- API/JSON serialization.
- Dashboard/card nếu đang hiển thị duration/video status.

### Deployment
- Max request/body size.
- Reverse proxy upload limit.
- Writable/persistent media storage.
- ffprobe/media metadata dependency.
- Environment variables.
- CSP/frame-src/media-src.
