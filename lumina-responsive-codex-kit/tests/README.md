# Responsive Playwright Tests

Các file này là template. Codex phải ghép vào Playwright setup hiện có hoặc cài Playwright trong nhánh làm việc nếu dự án chưa có và việc đó được chấp nhận.

## Biến môi trường

```bash
BASE_URL=http://localhost:8080
RESPONSIVE_PUBLIC_ROUTES=/,/courses,/blog
RESPONSIVE_STUDENT_ROUTES=/student/profile,/student/help,/my-courses
RESPONSIVE_TEACHER_ROUTES=/teacher,/teacher/courses,/teacher/roadmap
RESPONSIVE_ADMIN_ROUTES=/admin,/admin/courses
```

Authenticated route có thể dùng storage state hiện có. Không commit credential/cookie thật.

## Chạy ví dụ

```bash
npx playwright test tests/responsive-overflow.spec.ts
```

Test tự động chỉ phát hiện một phần lỗi. Vẫn phải xem screenshot và kiểm tra tương tác.
