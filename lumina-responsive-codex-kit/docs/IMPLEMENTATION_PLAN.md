# Implementation Plan

## 1. Discovery

- Xác định frontend root và package manager.
- Xác định CSS architecture: Tailwind, CSS Modules, SCSS, styled-components, MUI, Bootstrap hoặc CSS thuần.
- Dò route và layout cho public/student/teacher/admin/course player.
- Dò component logo và mọi bản sao logo khác.
- Chạy `node scripts/scan-responsive-antipatterns.mjs <frontend-root>`.

## 2. Shared foundations

- Chuẩn hóa container width/padding.
- Chuẩn hóa media và form control (`max-width: 100%`).
- Sửa flex/grid child (`min-width: 0`).
- Chuẩn hóa button group responsive.
- Tạo/chuẩn hóa shared `BrandLogo`.
- Xác định breakpoints nhỏ gọn, tránh nhiều media query xung đột.

## 3. Navigation/layout

- Public navbar: desktop navigation + mobile trigger/drawer.
- Student/course player: header actions co giãn.
- Teacher/admin: topbar và content shell không phụ thuộc fixed viewport width.
- Sidebar: desktop sidebar, mobile drawer; content không giữ margin-left khi sidebar ẩn.

## 4. Data-heavy pages

Dùng một trong hai chiến lược có chủ đích:

### Mobile card representation (khuyến nghị với course/roadmap)

- Render field quan trọng thành label/value.
- Action gom vào menu hoặc row button.
- Không lặp markup nghiệp vụ: dùng component/presenter chung.
- Desktop giữ semantic table nếu phù hợp.

### Horizontal table scroll

Chỉ dùng khi so sánh cột là quan trọng. Wrapper có `overflow-x:auto`, table có min-width hợp lý, focus/keyboard scroll và không làm toàn trang tràn.

## 5. Content robustness

Test với:

- Email dài: `very.long.teacher.account+responsive@example-university.edu`.
- Course title 80–120 ký tự.
- Roadmap description dài.
- Giá lớn và `Free`/`Completed`/status badge.
- Breadcrumb 3–5 cấp.

## 6. Verification

- Lint/typecheck/build.
- Existing test suite.
- Playwright overflow checks.
- Visual screenshots theo viewport matrix.
- Kiểm tra keyboard/mobile menu/focus.
