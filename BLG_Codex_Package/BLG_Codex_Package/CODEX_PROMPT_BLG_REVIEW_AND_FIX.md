# CODEX PROMPT - BLG Review And Fix

Dùng prompt này sau khi Codex đã code xong một phần hoặc toàn bộ BLG.

---

You are reviewing my BLG Blog implementation in an existing Spring Boot + HTML/CSS/JS e-learning project.

Do not add new features. Only review and fix issues related to BLG.

## Review checklist

Backend:
- BlogPost status workflow is correct.
- BlogComment status workflow is correct.
- Student/Teacher can create and submit only their own blogs.
- Admin-only moderation endpoints are protected.
- Public endpoints only expose PUBLISHED and visible content.
- Validation is enforced server-side.
- Error responses are clear and consistent with project convention.
- No duplicate User/Auth classes were created.
- No hard-coded user ID, role, path, or DB credential.

Frontend:
- Public blog list works.
- Public blog detail works.
- Comment section works for logged-in users and blocks guests.
- My Blogs page shows status and rejection reason.
- Blog editor can save draft and submit review.
- Admin moderation queue can approve/reject with reason.
- Admin management pages can hide/archive blog and hide/delete comment.
- UI follows existing design system.
- Responsive on desktop/tablet/mobile.
- Loading, empty, error, success states exist.

Security:
- Check XSS risk from blog content and comments.
- Check non-owner access.
- Check Admin routes.
- Check public data leakage.

Testing:
- Run Maven or Gradle tests.
- If tests cannot run, explain exact blocker.
- Add targeted tests if test structure exists.

## Output

After review/fix, report:
1. Bugs found
2. Bugs fixed
3. Files changed
4. Tests run and result
5. Manual verification steps
6. Remaining risks
