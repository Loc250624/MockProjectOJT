# 04 - BLG Frontend Implementation Plan

Mục tiêu: triển khai UI bằng HTML, CSS, JS thuần hoặc Thymeleaf template tùy repo hiện tại. Không dùng React/Vue nếu project đang theo HTML/CSS/JS thuần.

## Màn hình cần có

### 1. Public blog list - BLG-04

URL gợi ý: `/blogs`

Thành phần UI:

- Header/navbar dùng chung.
- Search input.
- Filter theo category/tag nếu project đã có.
- Blog cards:
  - Thumbnail
  - Title
  - Summary
  - Author
  - Published date
  - Comment count
- Pagination.
- Empty state: “Chưa có bài viết nào”.
- Error state khi API lỗi.

### 2. Public blog detail - BLG-04

URL gợi ý: `/blogs/{slug}`

Thành phần UI:

- Title, author, published date.
- Content area đọc dễ, khoảng cách tốt.
- Comment section:
  - List comments.
  - Form comment nếu đã đăng nhập.
  - CTA login nếu chưa đăng nhập.
- Loading state.
- Error state nếu blog không tồn tại hoặc bị ẩn.

### 3. My Blog dashboard - BLG-01/02/03

URL gợi ý: `/blog/my`

Thành phần UI:

- Tabs/filter: All, Draft, Pending Review, Rejected, Published.
- Danh sách bài của tôi.
- Badge status rõ ràng.
- Nút:
  - Create new
  - Edit
  - Submit for review
  - View rejection reason
  - Delete draft
- Empty state theo từng tab.

### 4. Blog editor - BLG-01/03

URL gợi ý:

- `/blog/editor`
- `/blog/editor/{id}`

Form:

- Title
- Summary
- Thumbnail URL hoặc upload nếu project đã có upload service
- Content editor:
  - Nếu không có rich text library, dùng textarea lớn.
  - Có preview mode đơn giản.
- Buttons:
  - Save draft
  - Submit for review
  - Cancel
- Validation trực tiếp trên UI.
- Success toast sau khi lưu.
- Warning khi rời trang mà chưa lưu.

### 5. Admin moderation queue - BLG-05

URL gợi ý: `/admin/blogs/moderation`

Thành phần UI:

- List/table các bài PENDING_REVIEW.
- Preview nội dung.
- Nút Approve.
- Nút Reject mở modal nhập lý do.
- Filter/search.
- Empty state: “Không có bài chờ duyệt”.
- Action feedback sau duyệt/từ chối.

### 6. Admin blog management - BLG-06

URL gợi ý: `/admin/blogs`

Thành phần UI:

- Table blog:
  - Title
  - Author
  - Status
  - Created/Updated/Published date
  - Actions: View, Hide, Archive
- Filter theo status.
- Confirm modal trước action nguy hiểm.
- Ghi reason khi hide/archive nếu backend yêu cầu.

### 7. Admin comment management - BLG-07

URL gợi ý: `/admin/blog-comments`

Thành phần UI:

- Table comment:
  - Content preview
  - Blog title
  - Author
  - Status
  - Created date
  - Actions: Hide, Delete
- Filter/search.
- Modal nhập reason.
- Không hard delete trên UI nếu backend dùng soft delete.

## File frontend gợi ý

Điều chỉnh theo repo hiện tại.

```text
src/main/resources/templates
  ├── public
  │   ├── blogs.html
  │   └── blog-detail.html
  ├── blog
  │   ├── my-blogs.html
  │   └── blog-editor.html
  └── admin
      ├── blog-moderation.html
      ├── blog-management.html
      └── comment-management.html

src/main/resources/static
  ├── css
  │   └── blog.css
  └── js
      ├── blog-public.js
      ├── blog-editor.js
      ├── my-blogs.js
      ├── admin-blog-moderation.js
      └── admin-blog-comments.js
```

## UI state bắt buộc

Mỗi màn hình phải có:

- Loading state.
- Empty state.
- Error state.
- Success state/toast.
- Confirm dialog cho thao tác nguy hiểm.
- Responsive:
  - Desktop: table/card layout đầy đủ.
  - Tablet: card/table hybrid.
  - Mobile: cards stacked, action buttons dễ bấm.

## JS rule

- Không viết toàn bộ JS trong HTML nếu repo đã tách file.
- Không gọi API bằng URL hard-code domain; dùng relative URL.
- Tất cả fetch phải xử lý:
  - `response.ok`
  - JSON parse lỗi
  - loading toggle
  - disabled button khi submit
  - message rõ ràng khi lỗi
- Không expose role bằng biến client-only để bảo mật; role chỉ dùng để ẩn/hiện UI, backend vẫn phải enforce security.

## CSS rule

- Dùng design system sẵn có nếu project đã có.
- Không tạo style phá layout global.
- Đặt class prefix `blog-` hoặc `admin-blog-` để tránh conflict.
- Card/table phải readable, spacing nhất quán.
- Không dùng màu quá gắt; status badge phân biệt rõ.
