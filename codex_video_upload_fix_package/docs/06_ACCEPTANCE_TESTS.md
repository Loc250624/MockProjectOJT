# 06 — Acceptance Tests

## Critical happy paths
- [ ] Teacher add YouTube `watch?v=` URL thành công.
- [ ] Teacher add `youtu.be` URL thành công.
- [ ] Teacher add direct HTTPS MP4 thành công.
- [ ] Teacher upload local MP4 thành công.
- [ ] Duration được lưu và hiển thị.
- [ ] Student phát được từng nguồn video.
- [ ] Refresh trang vẫn còn dữ liệu.
- [ ] Edit/replacement hoạt động.
- [ ] Delete hoạt động theo behavior hiện tại.

## Button regression
- [ ] Nút Add Video enable đúng khi form valid.
- [ ] Không cần click nhiều lần.
- [ ] Không stuck disabled sau lỗi metadata.
- [ ] Cancel rồi mở lại không giữ lỗi cũ.
- [ ] Request fail -> button usable lại.
- [ ] Double-click không tạo duplicate video.

## Validation
- [ ] Empty URL bị chặn.
- [ ] `javascript:` bị chặn.
- [ ] `file://` bị chặn.
- [ ] unsupported provider/page URL bị báo rõ.
- [ ] unsupported file extension/MIME bị chặn.
- [ ] oversized file trả lỗi thân thiện.
- [ ] renamed executable không được lưu như video nếu validation hỗ trợ magic/MIME.

## Security
- [ ] Teacher không sửa lesson course không thuộc quyền.
- [ ] Student không gọi create/edit endpoint.
- [ ] CSRF request thiếu token bị chặn.
- [ ] Remote URL tới `127.0.0.1` bị block.
- [ ] Remote URL tới private IP bị block.
- [ ] Redirect từ public -> private bị block.
- [ ] URL/filename chứa HTML không gây XSS/path traversal.

## Backward compatibility
- [ ] Video URL cũ vẫn phát.
- [ ] Lesson cũ không cần edit lại.
- [ ] Existing course progress không reset.
- [ ] Course estimated duration không giảm/mất.
- [ ] Non-video lesson không bị ảnh hưởng.

## Error handling
- [ ] Network timeout có lỗi rõ ràng.
- [ ] YouTube unavailable không gây 500 không kiểm soát.
- [ ] Storage fail không để DB record hỏng.
- [ ] DB fail không để file orphan nếu có thể cleanup.

## Deployment smoke
- [ ] Upload qua môi trường production/reverse proxy không 413 trong limit cho phép.
- [ ] Media storage tồn tại sau restart theo strategy production.
- [ ] YouTube iframe được CSP cho phép nếu đang dùng CSP.
- [ ] ffprobe/dependency availability được verify nếu implementation cần.
