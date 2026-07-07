# 03 - BLG Backend Implementation Plan

Mục tiêu: triển khai backend Spring Boot cho BLG-01 đến BLG-07 theo kiến trúc layered architecture.

## Package gợi ý

Điều chỉnh package root theo repo hiện tại.

```text
com.ojtsu26.elearning
  ├── controller
  │   ├── BlogViewController.java
  │   ├── BlogActionController.java
  │   └── AdminBlogController.java
  ├── dto
  │   └── blog
  │       ├── BlogPostCreateRequest.java
  │       ├── BlogPostUpdateRequest.java
  │       ├── BlogPostResponse.java
  │       ├── BlogSubmitReviewRequest.java
  │       ├── BlogModerationRequest.java
  │       ├── BlogCommentCreateRequest.java
  │       └── BlogCommentResponse.java
  ├── entity
  │   ├── BlogPost.java
  │   ├── BlogComment.java
  │   └── BlogModerationLog.java
  ├── enums
  │   ├── BlogPostStatus.java
  │   ├── BlogCommentStatus.java
  │   └── BlogModerationAction.java
  ├── repository
  │   ├── BlogPostRepository.java
  │   ├── BlogCommentRepository.java
  │   └── BlogModerationLogRepository.java
  └── service
      ├── BlogPostService.java
      ├── BlogCommentService.java
      ├── BlogModerationService.java
      └── impl
          ├── BlogPostServiceImpl.java
          ├── BlogCommentServiceImpl.java
          └── BlogModerationServiceImpl.java
```

## Endpoint gợi ý

### Public / Blog view

| Method | URL | Role | Mục đích |
|---|---|---|---|
| GET | `/blogs` | All | Danh sách blog đã publish |
| GET | `/blogs/{slug}` | All | Chi tiết blog public |
| GET | `/api/blogs/public` | All | API list public |
| GET | `/api/blogs/public/{slug}` | All | API detail public |

### Student/Teacher author

| Method | URL | Role | Mục đích |
|---|---|---|---|
| GET | `/blog/my` | Student/Teacher | Danh sách blog của tôi |
| GET | `/blog/editor` | Student/Teacher | Form tạo blog |
| GET | `/blog/editor/{id}` | Student/Teacher | Form sửa blog |
| POST | `/api/blogs` | Student/Teacher | BLG-01 tạo draft |
| PUT | `/api/blogs/{id}` | Student/Teacher | BLG-01/03 sửa draft hoặc rejected |
| POST | `/api/blogs/{id}/submit` | Student/Teacher | BLG-02/03 gửi duyệt |
| DELETE | `/api/blogs/{id}` | Student/Teacher | Xóa mềm bài của mình khi chưa published |

### Comment

| Method | URL | Role | Mục đích |
|---|---|---|---|
| GET | `/api/blogs/{postId}/comments` | All | Lấy comment visible |
| POST | `/api/blogs/{postId}/comments` | Authenticated | BLG-04 tạo comment |
| PUT | `/api/blogs/comments/{commentId}` | Owner/Admin | Sửa comment nếu policy cho phép |
| DELETE | `/api/blogs/comments/{commentId}` | Owner/Admin | Xóa mềm comment |

### Admin moderation

| Method | URL | Role | Mục đích |
|---|---|---|---|
| GET | `/admin/blogs/moderation` | Admin | BLG-05 hàng chờ duyệt |
| GET | `/api/admin/blogs/pending` | Admin | API list pending |
| POST | `/api/admin/blogs/{id}/approve` | Admin | Duyệt blog |
| POST | `/api/admin/blogs/{id}/reject` | Admin | Từ chối + lý do |
| GET | `/admin/blogs` | Admin | BLG-06 quản lý blog |
| POST | `/api/admin/blogs/{id}/hide` | Admin | Ẩn blog |
| POST | `/api/admin/blogs/{id}/archive` | Admin | Archive/xóa mềm blog |
| GET | `/admin/blog-comments` | Admin | BLG-07 quản lý comment |
| POST | `/api/admin/blog-comments/{id}/hide` | Admin | Ẩn comment |
| POST | `/api/admin/blog-comments/{id}/delete` | Admin | Xóa mềm comment |

## Phân quyền

| Chức năng | Guest | Student | Teacher | Admin |
|---|---:|---:|---:|---:|
| Xem blog public | Yes | Yes | Yes | Yes |
| Xem comment public | Yes | Yes | Yes | Yes |
| Tạo blog draft | No | Yes | Yes | Optional |
| Sửa blog của mình | No | Yes | Yes | Optional |
| Gửi duyệt | No | Yes | Yes | Optional |
| Bình luận | No | Yes | Yes | Yes |
| Duyệt/từ chối blog | No | No | No | Yes |
| Ẩn/xóa blog | No | No | No | Yes |
| Quản lý comment | No | No | No | Yes |

## Service rules chi tiết

### BLG-01 - Viết và soạn thảo Blog

- Validate title, summary, content.
- Tạo slug unique.
- Mặc định status là `DRAFT`.
- Lấy author từ user đang đăng nhập.
- Không cho tạo blog nếu role không phải Student/Teacher.

### BLG-02 - Gửi yêu cầu xuất bản

- Chỉ author của post được submit.
- Chỉ submit khi status `DRAFT` hoặc `REJECTED`.
- Content không được rỗng.
- Chuyển status sang `PENDING_REVIEW`.
- Xóa hoặc reset `rejectionReason` cũ nếu có.

### BLG-03 - Sửa và gửi lại Blog bị từ chối

- Chỉ author được sửa post bị từ chối.
- Lưu lại nội dung mới.
- Cho phép gửi lại duyệt sau khi sửa.
- Có thể tạo moderation log nếu cần trace.

### BLG-04 - Public Blog và comment

- Danh sách public chỉ lấy `PUBLISHED`, `deleted=false`.
- Chi tiết blog public dùng slug.
- Bình luận chỉ tạo được nếu user đăng nhập và blog đang `PUBLISHED`.
- Comment visible sort theo `createdAt ASC` hoặc thread tùy UI.

### BLG-05 - Duyệt/từ chối

- Admin xem list `PENDING_REVIEW`.
- Approve chuyển `PUBLISHED`, set `publishedAt`.
- Reject chuyển `REJECTED`, bắt buộc `reason`.
- Ghi `BlogModerationLog`.

### BLG-06 - Quản lý blog

- Admin lọc theo status, author, keyword.
- Hide chuyển `HIDDEN`.
- Archive set `ARCHIVED` hoặc `deleted=true` tùy convention.
- Không hard delete khi chưa có yêu cầu.

### BLG-07 - Quản lý comment

- Admin xem comment theo blog, keyword, status.
- Hide hoặc delete mềm comment.
- Bắt buộc ghi reason khi xử lý comment vi phạm.
- Ghi moderation log.

## Validation gợi ý

| Field | Rule |
|---|---|
| title | required, 5-180 chars |
| summary | max 300 chars |
| content | required, min 20 chars |
| thumbnailUrl | valid URL nếu có |
| rejectionReason | required khi reject, max 500 chars |
| comment.content | required, 1-1000 chars |

## Error handling

Dùng GlobalExceptionHandler hiện có nếu repo đã có. Nếu chưa có, tạo các lỗi:

- `BLOG_POST_NOT_FOUND`
- `BLOG_COMMENT_NOT_FOUND`
- `BLOG_FORBIDDEN`
- `BLOG_INVALID_STATUS`
- `BLOG_REJECTION_REASON_REQUIRED`
- `BLOG_COMMENT_NOT_ALLOWED`
- `BLOG_SLUG_DUPLICATED`

## Search/pagination

Repository nên hỗ trợ:

- Public blog list: keyword + page + size.
- Author blog list: authorId + status + keyword.
- Admin moderation queue: status PENDING_REVIEW + keyword.
- Admin blog management: status + author + keyword.
- Admin comment management: status + keyword + postId.
