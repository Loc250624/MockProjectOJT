# 03 — Bug Audit Matrix

Codex phải kiểm tra và đánh dấu PASS/FAIL/FIXED cho từng khu vực.

| ID | Khu vực | Rủi ro/Lỗi cần tìm | Kỳ vọng |
|---|---|---|---|
| UI-01 | Add button | stuck disabled | chỉ disabled khi invalid/loading |
| UI-02 | URL input | regex quá chặt | hỗ trợ YouTube formats + direct URL |
| UI-03 | Source selector | chỉ có URL | có Upload / Video URL |
| UI-04 | Error state | lỗi im lặng | message cụ thể, recover được |
| UI-05 | Loading | request kép | disable trong submit, chống double click |
| UI-06 | Cancel/reset | state cũ còn sót | reset source/file/url/error/duration |
| UI-07 | Responsive | button/input overflow | usable ở mobile |
| UI-08 | Theme | text/disabled contrast kém | readable light/dark |
| UI-09 | Edit modal | logic khác Add | dùng chung validator/service nếu hợp lý |
| UI-10 | Player | render sai source | source-aware renderer |
| FE-01 | CSRF | token thiếu trong fetch/FormData | request hợp lệ |
| FE-02 | FormData | content-type set thủ công sai | browser tạo boundary |
| FE-03 | Async metadata | promise fail làm treo | finally/recovery đúng |
| FE-04 | URL normalize | ID parse lỗi | canonicalize |
| FE-05 | Memory | load toàn bộ file để get duration | chỉ metadata/object URL |
| BE-01 | Endpoint | method/path mismatch | frontend/backend contract khớp |
| BE-02 | Binding | DTO/form mismatch | bind đúng cả URL và multipart |
| BE-03 | Validation | URL-only assumptions | validation theo source type |
| BE-04 | AuthZ | teacher sửa course người khác | ownership/role enforced |
| BE-05 | CSRF | workaround bằng disable CSRF | giữ bảo vệ |
| BE-06 | Upload | filename traversal | generated storage key |
| BE-07 | Upload | file type giả | server validation |
| BE-08 | Upload | size không giới hạn | multipart + app limits |
| BE-09 | URL fetch | SSRF | private hosts/IP bị block |
| BE-10 | URL fetch | không timeout | connect/read timeout |
| BE-11 | Redirect | redirect vô hạn/private | limit + revalidate each hop |
| BE-12 | Duration | client-only trust | backend verify khi khả thi |
| BE-13 | YouTube | chỉ hỗ trợ watch?v | robust parser |
| BE-14 | Error mapping | 500 generic | 4xx có message, 5xx log-safe |
| DB-01 | sourceType | không phân biệt nguồn | source-aware model/backfill |
| DB-02 | URL length | column quá ngắn | đủ cho canonical URL |
| DB-03 | duration | type/rounding sai | integer seconds hoặc chuẩn hiện có |
| DB-04 | nullability | upload không có URL bị fail | schema tương thích source |
| DB-05 | cleanup | orphan file | cleanup policy |
| RD-01 | Student player | URL mới không chơi | playback tests |
| RD-02 | Teacher preview | provider mismatch | preview đúng |
| RD-03 | Progress | video source làm sai progress | không regression |
| RD-04 | Course duration | upload/direct không cộng | tổng duration đúng |
| DP-01 | Reverse proxy | upload bị 413 | documented size limit |
| DP-02 | Storage | local ephemeral | cấu hình durable production storage |
| DP-03 | ffprobe | production thiếu binary | dependency/fallback documented |
| DP-04 | CSP | YouTube iframe bị block | policy cập nhật tối thiểu |
| SEC-01 | XSS | URL inject vào DOM | encode/safe DOM APIs |
| SEC-02 | Open redirect | arbitrary redirect | not applicable or guarded |
| SEC-03 | Content exposure | path tuyệt đối bị trả về | public media URL/id only |
| SEC-04 | Ownership | cross-course overwrite | verify parent entity ownership |
