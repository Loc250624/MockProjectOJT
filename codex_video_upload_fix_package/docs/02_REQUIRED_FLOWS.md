# 02 — Required User Flows

## Flow A — Upload from computer
1. Teacher mở Add Video.
2. Chọn `Upload from computer`.
3. Chọn file video.
4. UI validate sơ bộ:
   - file tồn tại
   - MIME thuộc allowlist
   - size trong giới hạn
5. UI đọc metadata để hiển thị duration preview nếu browser đọc được.
6. Bấm Add Video.
7. Gửi `multipart/form-data` kèm CSRF.
8. Backend:
   - authorization
   - validate file
   - lưu bằng storage service
   - probe duration
   - persist metadata
9. Trả response chuẩn.
10. UI cập nhật lesson ngay hoặc reload có kiểm soát.
11. Student phát được video.

## Flow B — YouTube URL
1. Chọn `Video URL`.
2. Paste một trong các dạng phổ biến:
   - `https://www.youtube.com/watch?v=...`
   - `https://youtu.be/...`
   - `https://www.youtube.com/shorts/...` nếu app quyết định support
3. Normalize -> extract video ID.
4. Validate.
5. Resolve metadata/duration.
6. Persist canonical provider id + canonical URL/source type.
7. Student render bằng embed an toàn.

Không dùng regex chỉ hỗ trợ đúng một format `watch?v=`.

## Flow C — Direct public video URL
1. Nhập URL HTTPS trực tiếp tới media.
2. Backend kiểm tra:
   - scheme
   - host/IP safety
   - redirect limit
   - content type
   - timeout
3. Probe metadata.
4. Lưu source type DIRECT_URL.
5. Student render bằng HTML5 video.

## Flow D — URL provider khác
Chỉ support nếu:
- provider cho phép embed/public playback;
- có adapter rõ ràng;
- domain nằm trong allowlist;
- không cần scrape/download nội dung từ page HTML.

Ưu tiên kiến trúc `VideoProviderResolver`/adapter để sau này thêm Vimeo/Dailymotion mà không sửa core lesson logic.

## Flow E — Edit/replace
- URL -> upload.
- Upload -> URL.
- URL cũ -> URL mới.
- Khi replace upload, cleanup file cũ chỉ sau khi DB update thành công hoặc qua safe cleanup strategy.
- Không orphan file vô hạn.
- Không xóa file đang được lesson khác tham chiếu nếu storage có dedup/reference.

## Flow F — Delete
- Xóa video metadata đúng.
- Nếu chỉ xóa video khỏi lesson, không xóa lesson ngoài ý muốn.
- Nếu local upload, cleanup storage theo transaction-safe policy.
