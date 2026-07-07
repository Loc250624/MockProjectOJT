# 05 - BLG Testing & Definition of Done Checklist

Checklist này dùng để kiểm tra sau khi Codex code xong chức năng BLG.

## Backend test cases

### BLG-01 - Blog editor

- Student tạo draft thành công.
- Teacher tạo draft thành công.
- Guest không tạo được blog.
- Title rỗng trả lỗi validation.
- Content rỗng trả lỗi validation.
- Slug không trùng khi 2 bài có title giống nhau.

### BLG-02 - Submit for review

- Author submit draft thành công.
- Author submit rejected post thành công sau khi sửa.
- Người khác không submit được blog của author.
- Không submit được blog đã published.
- Submit xong status là `PENDING_REVIEW`.

### BLG-03 - Revision workflow

- Author xem được reason khi blog bị reject.
- Author sửa blog rejected.
- Author gửi lại duyệt.
- Non-owner không sửa được.

### BLG-04 - Public blog and comments

- Guest xem được list blog published.
- Guest không thấy blog draft/pending/rejected/hidden/archived.
- Guest xem được detail bằng slug.
- Logged-in user comment thành công trên blog published.
- Guest không comment được.
- Không comment được trên blog hidden/archived.

### BLG-05 - Moderation queue

- Admin xem được pending posts.
- Admin approve post thành công.
- Admin reject post với reason thành công.
- Reject không reason trả lỗi.
- Student/Teacher không truy cập được endpoint admin.

### BLG-06 - Blog administration

- Admin lọc blog theo status.
- Admin hide blog published.
- Hidden blog không xuất hiện ở public.
- Admin archive blog.
- Archive không hard delete dữ liệu nếu policy là soft delete.

### BLG-07 - Comment moderation

- Admin xem danh sách comment.
- Admin hide comment.
- Hidden/deleted comment không hiển thị ở public.
- Action moderation có ghi log/reason.

## Frontend checklist

- `/blogs` responsive desktop/tablet/mobile.
- `/blogs/{slug}` đọc nội dung tốt, comment không bị vỡ layout.
- `/blog/my` hiển thị status badge đúng.
- Editor lưu draft và submit review rõ ràng.
- Admin moderation queue có modal reject reason.
- Admin blog/comment management có confirm modal.
- Loading/empty/error/success states đầy đủ.

## Security checklist

- Backend enforce role, không chỉ ẩn nút ở frontend.
- Author-only rule cho sửa/xóa blog cá nhân.
- Admin-only rule cho approve/reject/hide/archive.
- Validate và sanitize content/comment để tránh XSS.
- Không trả thông tin nhạy cảm của user author.
- CSRF/JWT handling nhất quán với project hiện tại.

## Commands gợi ý

Codex phải tự nhận diện project dùng Maven hay Gradle. Chạy command phù hợp.

### Maven

```bash
./mvnw clean test
./mvnw spring-boot:run
```

### Gradle

```bash
./gradlew clean test
./gradlew bootRun
```

### Manual smoke test

1. Login Student.
2. Tạo blog draft.
3. Submit review.
4. Login Admin.
5. Approve blog.
6. Logout hoặc mở browser khác.
7. Vào public blog list, kiểm tra bài đã hiện.
8. Login user bất kỳ, comment.
9. Admin hide comment.
10. Public detail không còn hiện comment bị hide.

## Definition of Done

Một task BLG chỉ được chuyển Done khi:

- Code đúng layered architecture.
- Có validation và error handling.
- Có phân quyền theo role.
- Có UI responsive.
- Có ít nhất happy path và một error path được test.
- Không phá chức năng Auth, Course, Learning, Assessment hiện có.
- Không còn bug Critical/High.
- README/API docs hoặc ghi chú thay đổi đã cập nhật.
