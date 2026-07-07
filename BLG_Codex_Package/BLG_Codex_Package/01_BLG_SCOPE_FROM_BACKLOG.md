# 01 - BLG Scope From Backlog

Nguồn: sheet `Backlog` trong file tracking dự án. Chỉ lấy các dòng có ID bắt đầu bằng `BLG`.

## Bảng chức năng BLG

| ID | Module | Screen / Function | Actor | Priority | Target Week | Primary Owner | Reviewer | Effort (SP) | Dependencies | Deliverable / Acceptance | Status |
| --- | --- | --- | --- | --- | --- | --- | --- | --- | --- | --- | --- |
| BLG-01 | Blog & Community | Viết và soạn thảo bài Blog mới | Student/Teacher | P1 | W7 | Cường | Lộc | 3 | AUTH-04 | Blog editor | To Do |
| BLG-02 | Blog & Community | Gửi yêu cầu xuất bản bài Blog | Student/Teacher | P1 | W7 | Cường | Lộc | 2 | BLG-01 | Submit for review | To Do |
| BLG-03 | Blog & Community | Chỉnh sửa và gửi lại bài Blog bị từ chối | Student/Teacher | P1 | W7 | Cường | Lộc | 2 | BLG-02 | Revision workflow | To Do |
| BLG-04 | Blog & Community | Xem bài viết công khai, bình luận và thảo luận | All | P1 | W7 | Cường | Lộc | 5 | BLG-01 | Public blog and comments | To Do |
| BLG-05 | Blog Moderation | Duyệt/từ chối bài Blog và ghi lý do | Admin | P1 | W7 | Cường | Cường | 3 | BLG-02 | Moderation queue | To Do |
| BLG-06 | Blog Moderation | Quản lý/ẩn/xóa bài Blog | Admin | P2 | W9 | Cường | Cường | 2 | BLG-05 | Blog administration | To Do |
| BLG-07 | Blog Moderation | Quản lý và xóa bình luận vi phạm | Admin | P2 | W9 | Cường | Cường | 2 | BLG-04 | Comment moderation | To Do |

## Thứ tự triển khai khuyến nghị

### Phase 1 - Blog authoring core, ưu tiên W7

1. **BLG-01**: Tạo editor để Student/Teacher viết blog mới.
2. **BLG-02**: Cho phép gửi blog sang trạng thái chờ duyệt.
3. **BLG-05**: Admin duyệt hoặc từ chối blog, có lý do.

### Phase 2 - Revision + public interaction, ưu tiên W7

4. **BLG-03**: Student/Teacher chỉnh sửa và gửi lại bài bị từ chối.
5. **BLG-04**: Public blog list/detail, bình luận và thảo luận.

### Phase 3 - Moderation nâng cao, ưu tiên W9

6. **BLG-06**: Admin quản lý, ẩn hoặc xóa mềm blog.
7. **BLG-07**: Admin quản lý và xóa bình luận vi phạm.

## Dependency map

```text
AUTH-04
  └── BLG-01 Blog editor
        ├── BLG-02 Submit for review
        │     ├── BLG-05 Moderation queue
        │     │     └── BLG-06 Blog administration
        │     └── BLG-03 Revision workflow
        └── BLG-04 Public blog and comments
              └── BLG-07 Comment moderation
```

## Acceptance tổng cho nhóm BLG

- Student/Teacher có thể tạo draft blog.
- Student/Teacher chỉ sửa được bài của chính mình.
- Blog muốn public phải được Admin duyệt.
- Blog bị từ chối phải có lý do và có thể gửi lại.
- Người dùng xem được blog đã published.
- Người dùng đăng nhập có thể bình luận.
- Admin có hàng chờ duyệt blog.
- Admin có thể ẩn/xóa mềm blog và comment vi phạm.
- UI responsive và có loading/empty/error/success states.
