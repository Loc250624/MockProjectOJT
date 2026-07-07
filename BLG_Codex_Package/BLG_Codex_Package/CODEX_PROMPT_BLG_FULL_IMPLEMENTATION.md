# CODEX PROMPT - Full BLG Implementation

Copy toàn bộ prompt này vào Codex khi đang đứng ở root repo Spring Boot của project.

---

You are a senior fullstack engineer working in an existing E-learning project.

Tech stack expected:
- Backend: Spring Boot
- Frontend: HTML, CSS, JavaScript, possibly Thymeleaf depending on the existing repo
- Database: MySQL or the database already configured in the project
- Security: use the existing authentication/RBAC implementation
- Architecture: follow the existing layered architecture and naming conventions

Your task is to implement the Blog features with IDs BLG-01 to BLG-07 from the project backlog.

## Critical instruction

Before writing code, inspect the repository and summarize:
1. Build tool: Maven or Gradle
2. Main package name
3. Existing User entity and role model
4. Existing security/RBAC mechanism
5. Existing controller style: REST controller, view controller, action controller, or mixed
6. Existing frontend structure: Thymeleaf templates or static HTML
7. Existing CSS/JS convention
8. Existing migration/seed data approach
9. Existing test setup

Do not create duplicate User/Auth classes. Reuse the existing project structure.

## Scope

Implement only the Blog & Community / Blog Moderation module:

### BLG-01
Viết và soạn thảo bài Blog mới.
Actor: Student/Teacher.
Acceptance: Blog editor.

### BLG-02
Gửi yêu cầu xuất bản bài Blog.
Actor: Student/Teacher.
Acceptance: Submit for review.

### BLG-03
Chỉnh sửa và gửi lại bài Blog bị từ chối.
Actor: Student/Teacher.
Acceptance: Revision workflow.

### BLG-04
Xem bài viết công khai, bình luận và thảo luận.
Actor: All.
Acceptance: Public blog and comments.

### BLG-05
Duyệt/từ chối bài Blog và ghi lý do.
Actor: Admin.
Acceptance: Moderation queue.

### BLG-06
Quản lý/ẩn/xóa bài Blog.
Actor: Admin.
Acceptance: Blog administration.

### BLG-07
Quản lý và xóa bình luận vi phạm.
Actor: Admin.
Acceptance: Comment moderation.

## Suggested domain

Create or adapt these entities if they do not already exist:

- BlogPost
- BlogComment
- BlogModerationLog

Suggested BlogPost status:
- DRAFT
- PENDING_REVIEW
- REJECTED
- PUBLISHED
- HIDDEN
- ARCHIVED

Suggested BlogComment status:
- VISIBLE
- HIDDEN
- DELETED

## Required business rules

1. Student and Teacher can create blog drafts.
2. Only the blog author can edit their own draft/rejected blog.
3. Blog submit for review changes status to PENDING_REVIEW.
4. Admin can approve a pending blog and publish it.
5. Admin can reject a pending blog and must provide a rejection reason.
6. Rejected blogs can be edited and submitted again by their author.
7. Public blog pages show only PUBLISHED blogs that are not deleted/archived/hidden.
8. Authenticated users can comment on PUBLISHED blogs.
9. Admin can hide/archive blogs.
10. Admin can hide/delete comments.
11. All moderation actions should be traceable with a log or at least stored reason.
12. Backend must enforce permissions. Do not rely only on hiding frontend buttons.

## Suggested endpoints

Adapt URLs to match the existing project style.

Public:
- GET /blogs
- GET /blogs/{slug}
- GET /api/blogs/public
- GET /api/blogs/public/{slug}
- GET /api/blogs/{postId}/comments

Author:
- GET /blog/my
- GET /blog/editor
- GET /blog/editor/{id}
- POST /api/blogs
- PUT /api/blogs/{id}
- POST /api/blogs/{id}/submit
- DELETE /api/blogs/{id}

Comments:
- POST /api/blogs/{postId}/comments
- PUT /api/blogs/comments/{commentId}
- DELETE /api/blogs/comments/{commentId}

Admin:
- GET /admin/blogs/moderation
- GET /api/admin/blogs/pending
- POST /api/admin/blogs/{id}/approve
- POST /api/admin/blogs/{id}/reject
- GET /admin/blogs
- POST /api/admin/blogs/{id}/hide
- POST /api/admin/blogs/{id}/archive
- GET /admin/blog-comments
- POST /api/admin/blog-comments/{id}/hide
- POST /api/admin/blog-comments/{id}/delete

## Frontend requirements

Create or update pages according to the existing UI style:

1. Public blog list
2. Public blog detail with comment section
3. My blogs page for Student/Teacher
4. Blog editor page
5. Admin moderation queue
6. Admin blog management page
7. Admin comment management page

Every page must include:
- loading state
- empty state
- error state
- success feedback/toast
- responsive layout
- role-appropriate actions only

Use existing CSS design system. Do not introduce a heavy frontend framework.

## Validation

Enforce both frontend and backend validation:

- title: required, 5-180 chars
- summary: max 300 chars
- content: required, min 20 chars
- thumbnailUrl: valid URL if present
- rejectionReason: required when rejecting, max 500 chars
- comment content: required, max 1000 chars

## Security

- Student/Teacher: blog authoring only.
- Admin: moderation and administration.
- All users: view public blogs.
- Logged-in users: comment.
- Guest: view only.

Prevent:
- Non-owner editing another user's blog.
- Student/Teacher accessing admin blog endpoints.
- Guest posting comments.
- Public access to draft/pending/rejected/hidden/archived blogs.
- XSS through blog content or comments.

## Testing

After implementation:
1. Run the existing test command.
2. If test command is unknown, inspect the repo and choose the correct Maven/Gradle command.
3. Add or update tests for service/controller logic if the project has test structure.
4. Provide manual verification steps.

## Output expected from you

When finished, report:
1. Files changed
2. New entities/tables
3. New endpoints
4. How to test manually
5. Any assumptions or limitations
6. Whether automated tests passed

Do not rewrite unrelated modules. Do not change package structure unnecessarily. Do not hard-code database credentials, user IDs, roles, or URLs.
