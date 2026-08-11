# 08 — Codex Implementation Plan

## Step 1 — Map current implementation
Tạo danh sách file thực tế:
```text
[UI template]
[CSS]
[JS]
[Controller]
[DTO/Form]
[Service]
[Entity]
[Repository]
[Security]
[Config]
[Migration]
[Tests]
```

Không phỏng đoán. Ghi đường dẫn thật vào CHANGE_REPORT.

## Step 2 — Fix current Add Video bug first
Tạo test/reproduction cho bug.
Sửa root cause tối thiểu để URL hiện tại hoạt động.

## Step 3 — Introduce source abstraction
Nếu project đang chỉ có `videoUrl`, mở rộng tương thích:
- sourceType
- storageKey/path hoặc URL
- duration metadata

Backfill legacy rows:
- YouTube-shaped URL -> YOUTUBE
- otherwise URL -> DIRECT_URL
- null URL + existing local path -> UPLOAD nếu có

Không backfill theo rule nguy hiểm nếu dữ liệu không chắc chắn; migration có thể để LEGACY/NULL rồi service infer.

## Step 4 — Local upload
- multipart endpoint/binding
- storage service
- metadata probe
- validation
- public playback mapping
- cleanup

## Step 5 — URL providers
Tách logic provider:
```text
VideoSourceResolver
  - YouTubeResolver
  - DirectVideoResolver
  - Optional whitelisted provider resolver
```

Một resolver không được làm controller phình to.

## Step 6 — Frontend
- source selector
- mode-specific validation
- FormData when upload
- JSON/form when URL, theo backend contract
- async state machine rõ ràng
- reusable Add/Edit component nếu project architecture cho phép

## Step 7 — Read-side rendering
Player dựa trên source type.
Không suy đoán provider ở nhiều nơi bằng duplicated regex.

## Step 8 — Tests
Ưu tiên tests:
- URL parser unit tests
- validation unit tests
- controller integration tests
- authorization tests
- service tests
- repository/migration tests nếu có
- frontend tests nếu repo đã có test framework

Không cài framework test frontend mới chỉ cho task này nếu project chưa dùng.

## Step 9 — Static analysis
- no dead code
- no swallowed exceptions
- no secrets
- no debug prints
- no `alert()`/`confirm()` mới
- no blanket `permitAll`
- no CSRF disable

## Step 10 — Report
Điền CHANGE_REPORT_TEMPLATE.
