# 04 — Backend Contract & Security Specification

## Contract đề xuất
Codex không bắt buộc dùng đúng URL endpoint dưới đây. Hãy map vào convention hiện tại của project.

### A. Create by upload
`POST /teacher/courses/{courseId}/lessons/{lessonId}/video`

Content-Type: `multipart/form-data`

Fields:
- `sourceType=UPLOAD`
- `file=<binary>`
- CSRF theo cơ chế hiện tại

Response ví dụ:
```json
{
  "success": true,
  "video": {
    "sourceType": "UPLOAD",
    "playbackUrl": "/media/...",
    "durationSeconds": 615,
    "originalFilename": "lesson-01.mp4",
    "mimeType": "video/mp4"
  }
}
```

### B. Create by URL
Có thể dùng cùng endpoint, JSON hoặc form theo convention hiện tại.

Payload:
```json
{
  "sourceType": "YOUTUBE",
  "url": "https://www.youtube.com/watch?v=..."
}
```

Hoặc để backend tự detect source type từ URL, nhưng source type lưu trong DB phải rõ ràng.

## Validation matrix

| Source | Required | Forbidden |
|---|---|---|
| UPLOAD | file | url |
| YOUTUBE | url/provider id | file |
| DIRECT_URL | https url | file |
| EMBED_URL | whitelisted url | file |

Không để rule kiểu `@NotBlank videoUrl` làm upload local luôn fail.

## SSRF requirements
Remote URL validation phải diễn ra ở backend:
- Parse bằng URI/URL library, không regex-only.
- Allow `https` (http chỉ khi có lý do rõ ràng).
- Resolve DNS.
- Reject:
  - `localhost`
  - `127.0.0.0/8`
  - `10.0.0.0/8`
  - `172.16.0.0/12`
  - `192.168.0.0/16`
  - `169.254.0.0/16`
  - IPv6 loopback/link-local/private
  - cloud metadata endpoint patterns
- Nếu redirect: validate lại destination.
- Timeout ngắn, có max redirects.
- Không proxy toàn bộ file qua app nếu chỉ cần metadata.

## Upload security
- Server-generated UUID/storage key.
- Normalize/sanitize extension.
- MIME allowlist.
- Không cho `.html`, `.svg`, executable masquerade.
- Không serve upload với MIME nguy hiểm.
- Không để user chọn filesystem path.
- Size limit nên cấu hình bằng property/env thay vì magic number.

## Error contract
UI cần phân biệt:
- `400 INVALID_VIDEO_URL`
- `400 UNSUPPORTED_VIDEO_TYPE`
- `400 VIDEO_METADATA_UNAVAILABLE`
- `413 VIDEO_TOO_LARGE`
- `403 FORBIDDEN`
- `404 LESSON_NOT_FOUND`
- `409 VIDEO_STATE_CONFLICT`
- `500 VIDEO_STORAGE_ERROR`

Tên code có thể khác theo convention hiện tại, nhưng phải machine-readable nếu frontend dùng JS.

## Transaction
DB record và file storage là hai resource khác nhau.
Tránh:
1. lưu file thành công;
2. DB rollback;
3. file orphan.

Cần cleanup khi transaction fail, hoặc temporary -> commit -> promote pattern nếu storage abstraction cho phép.
