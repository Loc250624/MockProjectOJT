# AGENTS.md — Codex Instructions for Video Lesson Repair

Bạn đang làm việc trên một dự án E-Learning Java/Spring Boot + web frontend. Nhiệm vụ là sửa luồng Add Video và bổ sung upload local/external URL mà không gây regression.

## 1. Bắt buộc thực hiện theo thứ tự

### Phase A — Repository discovery
Không sửa code ngay.

Tìm toàn bộ code liên quan bằng `rg`/IDE search với các keyword:
- `Add Video`
- `VIDEO URL`
- `videoUrl`
- `video_url`
- `video`
- `duration`
- `lesson`
- `LessonType`
- `multipart`
- `MultipartFile`
- `YouTube`
- `youtube`
- `iframe`
- `player`
- `content-type`
- `csrf`
- `fetch(`
- `axios`
- `FormData`
- `@PostMapping`
- `@PutMapping`
- `@RequestParam`
- `@ModelAttribute`
- `@RequestBody`

Lập dependency map từ:
UI -> JS -> controller/API -> DTO/form -> service -> entity/repository -> DB -> response -> lesson player -> progress/duration aggregation.

### Phase B — Reproduce
Phải reproduce tối thiểu:
1. Add YouTube URL.
2. Add direct HTTPS `.mp4`.
3. Upload local `.mp4`.
4. Invalid URL.
5. Unsupported file.
6. Unauthorized user.
7. Edit existing video lesson.
8. Delete/replace video.
9. Student mở lesson sau khi Teacher thêm video.

Ghi lại:
- request URL/method
- request payload
- response status/body
- browser console error
- server stack trace
- validation state của nút Add Video
- DB state trước/sau

### Phase C — Root cause
Không vá bằng cách bỏ `disabled`, bỏ validation, hoặc nuốt exception.

Phải xác định root cause cho nút Add Video hiện không hoạt động, ví dụ:
- URL validation regex quá chặt.
- Async duration detection fail nhưng UI không recover.
- Promise bị reject làm button luôn disabled.
- Form handler không bind.
- Endpoint/method mismatch.
- CSRF token thiếu.
- DTO/backend validation từ chối YouTube URL.
- Transaction rollback.
- DB constraint.
- lesson type/source type không đồng bộ.
- response contract frontend/backend lệch.

### Phase D — Implement
Triển khai theo tài liệu trong `docs/`.

### Phase E — Test + regression
Chạy unit/integration tests và test tay theo `docs/06_ACCEPTANCE_TESTS.md`.

## 2. Kiến trúc nguồn video bắt buộc

Không dùng một field URL để giả lập upload local.

Tạo/chuẩn hóa source model có ý nghĩa tương đương:

```text
VideoSourceType:
- UPLOAD
- YOUTUBE
- DIRECT_URL
- EMBED_URL   (chỉ nếu project/provider hiện tại cần)
```

Nếu schema hiện tại đã có mô hình tương đương thì reuse, không duplicate.

Mỗi video lesson cần đủ metadata phù hợp:
- sourceType
- sourceUrl hoặc storageKey/path
- durationSeconds
- optional originalFilename
- optional mimeType
- optional providerVideoId
- created/updated metadata nếu dự án đang dùng

Không bắt buộc đổi tên field nếu sẽ gây migration lớn; ưu tiên tương thích ngược.

## 3. UI/UX bắt buộc

Form Add Video phải có lựa chọn rõ:
- `Upload from computer`
- `Video URL`

Không hiển thị input upload và URL như hai field required cùng lúc.

### URL mode
- Input `https://...`
- Help text: hỗ trợ YouTube và direct public video URL; provider embed khác chỉ khi được hỗ trợ.
- Có trạng thái: Validating / Ready / Error.
- Nút Add Video chỉ disabled trong lúc request hoặc khi form invalid.
- Nếu không detect được duration nhưng URL hợp lệ, hiển thị lỗi cụ thể; không để UI treo vô hạn.

### Upload mode
- `<input type="file" accept="video/mp4,video/webm,video/ogg">` hoặc tập MIME tương thích thực tế của app.
- Hiển thị filename + size.
- Client-side metadata dùng để preview UX, backend vẫn phải validate.
- Có loading/progress nếu upload lâu.
- Disable double submit.

### Accessibility/responsive
- Label liên kết với input.
- Error message có `aria-live`.
- Keyboard usable.
- 320px mobile không overflow.
- Không phụ thuộc màu để truyền trạng thái.

## 4. Backend bắt buộc

- Authorization: chỉ role hiện tại được phép quản lý course/lesson mới được add/edit video.
- CSRF: không tắt CSRF toàn app.
- Multipart upload: cấu hình size limit hợp lý và xử lý `MaxUploadSizeExceededException`.
- Validate MIME + extension + magic bytes nếu hạ tầng hiện tại cho phép.
- Sanitize filename; không dùng original filename làm storage path trực tiếp.
- Không cho path traversal.
- Remote URL chỉ `https` mặc định; `http` chỉ nếu project có use case dev rõ ràng.
- SSRF protection: block localhost, loopback, private/link-local IP, metadata endpoints, DNS rebinding risk.
- Limit redirect.
- Limit remote probe size/time.
- Timeout network calls.
- Không log token/credentials.
- Không expose absolute server path cho client.

## 5. Duration

Ưu tiên:
- Upload local: lấy duration bằng media probe đáng tin cậy (ffprobe hoặc media library nếu project đã có).
- YouTube: dùng provider adapter; nếu project đã có API/provider service thì reuse.
- Direct URL: probe metadata với timeout và SSRF guard.
- Không fetch toàn bộ video vào memory chỉ để đọc duration.

Nếu môi trường production không có `ffprobe`, Codex phải:
1. Detect/dependency-check rõ ràng.
2. Có fallback an toàn hoặc cấu hình deployment.
3. Không để app crash khi binary không tồn tại.

## 6. Playback

Student/player phải render đúng theo source type:
- UPLOAD/DIRECT_URL -> HTML5 `<video controls ...>`
- YOUTUBE -> privacy-conscious embed URL theo video id
- EMBED_URL -> whitelist provider

Không render URL người dùng nhập trực tiếp vào `innerHTML`.
Không cho arbitrary iframe origin.

## 7. Dữ liệu và backward compatibility

Codex phải kiểm tra các bản ghi video lesson cũ:
- URL cũ vẫn chạy.
- Không biến lesson cũ thành invalid vì sourceType mới.
- Migration/backfill sourceType nếu cần.
- duration cũ không bị reset.
- Course estimated duration vẫn cộng đúng các lesson có video.

Nếu cần migration, tạo migration theo công nghệ repo đang dùng (Flyway/Liquibase/manual SQL), không tự ý tạo hệ migration mới.

## 8. Không được đụng tới
Trừ khi lỗi video thật sự phụ thuộc:
- Payment/VNPay
- Quiz
- Blog
- Feedback
- Authentication provider linking
- Notification
- AI Chatbot
- Admin unrelated pages

## 9. Definition of Done
Chỉ hoàn thành khi:
- Add YouTube chạy.
- Add direct public video URL chạy.
- Upload local video chạy.
- Edit/replace/delete chạy.
- Student playback chạy.
- Duration lưu đúng.
- Button không bị stuck disabled.
- Error UI rõ ràng.
- Security regression không xuất hiện.
- Existing URL video vẫn tương thích.
- Tests pass.
- Có CHANGE_REPORT với file đã sửa + lý do + test evidence.
