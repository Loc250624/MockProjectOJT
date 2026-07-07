# 02 - BLG Domain & Database Design

Tài liệu này là thiết kế đề xuất để Codex triển khai chức năng Blog trong Spring Boot.

## Entity chính

### BlogPost

Mục đích: lưu bài blog của Student/Teacher và trạng thái kiểm duyệt.

Trường đề xuất:

| Field | Type gợi ý | Ghi chú |
|---|---|---|
| `id` | Long | Primary key |
| `author` | User | Many-to-One tới User |
| `title` | String | Bắt buộc, 5-180 ký tự |
| `slug` | String | Unique, tạo từ title, dùng cho public URL |
| `summary` | String | 0-300 ký tự |
| `content` | TEXT/LONGTEXT | Bắt buộc, có thể chứa HTML đã sanitize hoặc plain text/markdown |
| `thumbnailUrl` | String | Optional |
| `status` | Enum | DRAFT, PENDING_REVIEW, REJECTED, PUBLISHED, HIDDEN, ARCHIVED |
| `rejectionReason` | String | Bắt buộc khi reject |
| `publishedAt` | LocalDateTime | Có khi status PUBLISHED |
| `createdAt` | LocalDateTime | Audit |
| `updatedAt` | LocalDateTime | Audit |
| `deleted` | boolean | Soft delete |
| `viewCount` | long | Optional, có thể để sau |

### BlogComment

Mục đích: bình luận/thảo luận ở bài blog public.

| Field | Type gợi ý | Ghi chú |
|---|---|---|
| `id` | Long | Primary key |
| `post` | BlogPost | Many-to-One |
| `author` | User | Many-to-One |
| `parent` | BlogComment | Optional nếu muốn reply/comment thread |
| `content` | TEXT | Bắt buộc, giới hạn độ dài |
| `status` | Enum | VISIBLE, HIDDEN, DELETED |
| `moderationReason` | String | Lý do admin ẩn/xóa |
| `createdAt` | LocalDateTime | Audit |
| `updatedAt` | LocalDateTime | Audit |
| `deleted` | boolean | Soft delete |

### BlogModerationLog

Mục đích: lưu lịch sử admin duyệt/từ chối/ẩn/xóa.

| Field | Type gợi ý | Ghi chú |
|---|---|---|
| `id` | Long | Primary key |
| `post` | BlogPost | Nullable nếu log cho comment |
| `comment` | BlogComment | Nullable nếu log cho post |
| `moderator` | User | Admin thao tác |
| `action` | Enum | APPROVE, REJECT, HIDE_POST, ARCHIVE_POST, HIDE_COMMENT, DELETE_COMMENT |
| `reason` | String | Bắt buộc với reject/hide/delete |
| `createdAt` | LocalDateTime | Thời điểm moderation |

## Status workflow

```text
DRAFT
  └── PENDING_REVIEW
        ├── PUBLISHED
        │     ├── HIDDEN
        │     └── ARCHIVED
        └── REJECTED
              └── DRAFT hoặc PENDING_REVIEW sau khi chỉnh sửa/gửi lại
```

## Quy tắc nghiệp vụ

- Chỉ Student/Teacher mới được tạo blog.
- Tác giả chỉ được sửa bài của mình khi status là DRAFT hoặc REJECTED.
- Khi gửi duyệt: status chuyển sang PENDING_REVIEW.
- Admin duyệt: status chuyển sang PUBLISHED, set `publishedAt`.
- Admin từ chối: status chuyển sang REJECTED, bắt buộc nhập `rejectionReason`.
- Trang public chỉ hiển thị `PUBLISHED` và `deleted = false`.
- Blog HIDDEN không xuất hiện ở public list nhưng Admin vẫn xem được.
- ARCHIVED là xóa mềm, không hard delete trừ khi dự án đã có quy định riêng.
- Bình luận chỉ cho phép trên blog PUBLISHED.
- User đăng nhập mới được bình luận.
- Admin có thể ẩn/xóa mềm comment vi phạm.

## Migration SQL tham khảo

Codex phải điều chỉnh tên bảng/cột theo project hiện tại. Không copy mù quáng nếu repo đã có convention khác.

```sql
CREATE TABLE blog_posts (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    author_id BIGINT NOT NULL,
    title VARCHAR(180) NOT NULL,
    slug VARCHAR(220) NOT NULL UNIQUE,
    summary VARCHAR(300),
    content LONGTEXT NOT NULL,
    thumbnail_url VARCHAR(500),
    status VARCHAR(30) NOT NULL,
    rejection_reason VARCHAR(500),
    published_at DATETIME NULL,
    view_count BIGINT NOT NULL DEFAULT 0,
    deleted BOOLEAN NOT NULL DEFAULT FALSE,
    created_at DATETIME NOT NULL,
    updated_at DATETIME NOT NULL,
    CONSTRAINT fk_blog_posts_author FOREIGN KEY (author_id) REFERENCES users(id)
);

CREATE TABLE blog_comments (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    post_id BIGINT NOT NULL,
    author_id BIGINT NOT NULL,
    parent_id BIGINT NULL,
    content TEXT NOT NULL,
    status VARCHAR(30) NOT NULL,
    moderation_reason VARCHAR(500),
    deleted BOOLEAN NOT NULL DEFAULT FALSE,
    created_at DATETIME NOT NULL,
    updated_at DATETIME NOT NULL,
    CONSTRAINT fk_blog_comments_post FOREIGN KEY (post_id) REFERENCES blog_posts(id),
    CONSTRAINT fk_blog_comments_author FOREIGN KEY (author_id) REFERENCES users(id),
    CONSTRAINT fk_blog_comments_parent FOREIGN KEY (parent_id) REFERENCES blog_comments(id)
);

CREATE TABLE blog_moderation_logs (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    post_id BIGINT NULL,
    comment_id BIGINT NULL,
    moderator_id BIGINT NOT NULL,
    action VARCHAR(40) NOT NULL,
    reason VARCHAR(500),
    created_at DATETIME NOT NULL,
    CONSTRAINT fk_blog_logs_post FOREIGN KEY (post_id) REFERENCES blog_posts(id),
    CONSTRAINT fk_blog_logs_comment FOREIGN KEY (comment_id) REFERENCES blog_comments(id),
    CONSTRAINT fk_blog_logs_moderator FOREIGN KEY (moderator_id) REFERENCES users(id)
);
```
