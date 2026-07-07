# CODEX PROMPT - Incremental BLG Tasks

Dùng file này khi bạn muốn Codex làm từng phần nhỏ để giảm lỗi và giảm conflict.

---

## Prompt 0 - Repo analysis only

```text
You are working in my existing Spring Boot + HTML/CSS/JS e-learning repository.

First, inspect the repository. Do not modify any files.

Summarize:
1. Build tool and test command
2. Main package name
3. Existing User entity, role enum/model, and repository
4. Authentication/RBAC mechanism
5. Controller naming pattern
6. Service/repository/DTO conventions
7. Frontend template/static folder structure
8. Existing CSS/JS conventions
9. Database migration or ddl-auto approach
10. Any risks before implementing Blog features

Do not code yet.
```

---

## Prompt 1 - BLG-01 + BLG-02

```text
Implement BLG-01 and BLG-02 only.

BLG-01: Student/Teacher can write and save a new Blog draft.
BLG-02: Student/Teacher can submit their Blog for review.

Requirements:
- Reuse existing User/Auth/RBAC.
- Create BlogPost entity/repository/service/controller/DTO if missing.
- BlogPost status must support DRAFT and PENDING_REVIEW.
- Only Student/Teacher can create/submit.
- Only the author can submit their own blog.
- Add a My Blogs page and Blog Editor page using the existing frontend style.
- Include validation and error handling.
- Do not implement Admin moderation yet except what is needed for compilation.
- Run tests or explain why they cannot run.
- Summarize changed files.
```

---

## Prompt 2 - BLG-05 Admin moderation

```text
Implement BLG-05 only on top of the existing BlogPost work.

BLG-05: Admin can review pending blogs, approve or reject them with a reason.

Requirements:
- Admin-only endpoints and pages.
- Show moderation queue for PENDING_REVIEW blogs.
- Approve changes status to PUBLISHED and sets publishedAt.
- Reject changes status to REJECTED and requires rejectionReason.
- Add BlogModerationLog if appropriate.
- Add frontend modal/form for rejection reason.
- Backend must enforce Admin role.
- Run tests or provide manual verification.
- Summarize changed files.
```

---

## Prompt 3 - BLG-03 + BLG-04

```text
Implement BLG-03 and BLG-04.

BLG-03:
- Author can edit a REJECTED blog.
- Author can submit it again for review.
- Show rejection reason in My Blogs/editor.

BLG-04:
- Public users can view published blog list and detail.
- Logged-in users can comment on published blogs.
- Guest users can read but cannot comment.
- Create BlogComment entity/repository/service/controller/DTO if missing.
- Public list/detail must not show DRAFT, PENDING_REVIEW, REJECTED, HIDDEN, ARCHIVED, or deleted blogs.
- Comments should support visible/hidden/deleted status.

Follow existing UI style. Add loading, empty, error, and success states. Run tests or explain blockers.
```

---

## Prompt 4 - BLG-06 + BLG-07

```text
Implement BLG-06 and BLG-07.

BLG-06:
- Admin can manage blogs.
- Admin can hide or archive blogs.
- Hidden/archived blogs must disappear from public pages.
- Use soft delete/archive, not hard delete, unless the project already has a hard-delete policy.

BLG-07:
- Admin can manage comments.
- Admin can hide or delete inappropriate comments.
- Hidden/deleted comments must disappear from public detail pages.
- Store reason/log for moderation action.

Add admin pages and APIs using existing admin layout/style. Run tests or provide manual verification.
```

---

## Prompt 5 - Final review and hardening

```text
Review the entire BLG implementation.

Check:
- Compile errors
- Runtime errors
- Broken routes
- Role permissions
- XSS risks in blog/comment content
- Validation gaps
- Public pages leaking non-published blogs
- Responsive UI issues
- Missing loading/empty/error/success states
- Tests

Fix only issues related to BLG. Do not rewrite unrelated modules.

After fixing, provide:
1. Final changed files
2. Test results
3. Manual test steps
4. Known limitations
```
