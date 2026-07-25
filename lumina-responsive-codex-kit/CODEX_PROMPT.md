# Nhiệm vụ: Sửa toàn diện responsive cho LumiNa, giới hạn đúng phạm vi

Bạn là Senior Fullstack Developer. Hãy sửa responsive dự án LumiNa dựa trên ảnh trong `references/screenshots/`.

## Mục tiêu

Sửa những khu vực đang bị vỡ layout, tràn ngang, cắt mất nội dung, logo không đồng nhất, button nằm ngoài viewport, table bị ép quá nhỏ và menu hiển thị lộn xộn. Đồng thời kiểm tra các trang dùng chung layout/component với các khu vực này để phát hiện lỗi tương tự.

## Nguyên tắc bắt buộc

1. Trước khi sửa, hãy xác định framework frontend, hệ thống CSS, router, layout dùng chung và breakpoint hiện có.
2. Dùng `git status` và đọc code trước; không đoán tên file.
3. Ưu tiên sửa component/layout dùng chung thay vì vá từng route.
4. Không sửa nghiệp vụ, API contract, database, authentication, authorization, payment hoặc backend nếu không cần thiết.
5. Không xóa tính năng hay ẩn dữ liệu để làm giao diện vừa màn hình.
6. Không dùng global `overflow-x: hidden` để che lỗi. Có thể dùng overflow có chủ đích ở table/tab carousel cụ thể.
7. Không tạo breakpoint chồng chéo vô tổ chức. Tận dụng design tokens/breakpoint hiện có; nếu chưa có thì tạo một hệ thống nhỏ, rõ ràng.
8. Giữ desktop hiện tại ổn định. Mọi thay đổi phải được kiểm tra cả mobile, tablet và desktop.
9. Nội dung động dài phải an toàn: email, tên khóa học, mô tả, giá, breadcrumb, tiêu đề roadmap, tên người dùng.
10. Giữ khả năng truy cập: touch target khoảng 44px, focus state, menu có aria, không dùng font quá nhỏ để ép dữ liệu.

## Khu vực phải kiểm tra và sửa

### A. Header/navigation công khai

Ảnh: `01`, `04`, `05`.

- Logo và tên LumiNa không được đẩy menu ra ngoài viewport.
- Menu desktop phải tự chuyển sang mobile menu đúng breakpoint, không để mục `Blog` hoặc mục khác bị cắt.
- Nút ba chấm/mobile trigger phải có kích thước ổn định, không chồng logo.
- Search bar, Login và Register phải vừa chiều rộng, có khoảng cách đồng nhất.
- Mobile menu không tạo khoảng trắng bất thường hoặc làm phần nội dung phía dưới nhảy layout.
- Dùng cùng một component logo/brand ở public, student, teacher, admin và course player.

### B. Student Help and Guide

Ảnh: `02`.

- Container không vượt viewport.
- Card hướng dẫn, tab Overview/Workflow/Support và danh sách quick topics phải co giãn tốt.
- Tab không được ép chữ hoặc tràn. Có thể dùng grid ba cột khi đủ rộng; ở màn hình rất hẹp dùng vùng cuộn ngang có chỉ báo rõ hoặc stack hợp lý.
- Text dài phải wrap tự nhiên; không bị cắt phần cuối trang.

### C. Course player header

Ảnh: `03`.

- Sửa logo chữ `L` thành brand/logo dùng chung của LumiNa.
- Hai nút `Back to course` và `My Courses` không được chiếm quá rộng hoặc làm avatar rơi vào vị trí bất thường.
- Ở mobile: ưu tiên stack nút hoặc grid 2 cột khi đủ chỗ; avatar và action phải nằm trong viewport.
- Không dùng absolute positioning phụ thuộc chiều rộng cố định.

### D. Teacher dashboard/top bar

Ảnh: `06`, `09`, `11`.

- Email dài trong câu chào phải wrap/ellipsis hợp lý mà không đẩy action ra ngoài.
- Các nút như `View ...`, `New ...` phải xuống hàng hoặc thành full-width trên mobile.
- Breadcrumb, notification và Help phải không chồng nhau.
- Hero section dùng grid/flex có `min-width: 0`, `flex-wrap` hoặc chuyển một cột ở mobile.

### E. Teacher Courses

Ảnh: `07`.

- Bảng hiện tại bị thu nhỏ đến mức khó đọc. Không được chỉ giảm font-size.
- Ở mobile, chuyển mỗi course thành card/list row có tối thiểu: ID (nếu cần), icon, title, instructor/category và price/status.
- Các action vẫn truy cập được bằng button/menu rõ ràng.
- Ở tablet/desktop có thể giữ table.
- Search/filter/create course không được tràn, button nên stack trên mobile.
- Footer phải có kích thước chữ đọc được và không bị ép ngang.

### F. Teacher Roadmap

Ảnh: `08`.

- Header action `Add Roadmap` đang lệch sang phải và table vượt viewport.
- Chuyển header thành một cột trên mobile; button full-width hoặc vừa nội dung nhưng không vượt màn hình.
- Table roadmap chuyển thành card list trên mobile hoặc đặt trong scroll container có chủ đích. Ưu tiên card nếu cần đọc nhiều mô tả.
- Không để description/action nằm ngoài vùng nhìn thấy.
- Footer không được tạo overflow.

### G. Admin Courses

Ảnh: `10`.

- Áp dụng cùng pattern responsive table/card như Teacher Courses.
- Badge system health, breadcrumb/topbar, moderation button, search/filter và table phải vừa viewport.
- Dùng component danh sách/table dùng chung nếu dữ liệu tương đồng; tránh copy CSS.

### H. Kiểm tra lan truyền

Tìm tất cả trang dùng chung các component sau (tên thật tùy repo):

- Public Header/Navbar/Mobile Menu/Logo.
- Student Header/Sidebar/Help Guide/Course Player.
- Teacher Layout/Topbar/Dashboard/Table/List/Footer.
- Admin Layout/Topbar/Table/List/Footer.
- Shared Button, Card, Table, Breadcrumb, SearchInput, Modal/Drawer.

Kiểm tra các route tương tự: danh sách users, enrollments, quizzes, blogs, certificates, payments, assignments, notifications, roadmaps và course management. Chỉ sửa khi cùng nguyên nhân responsive; không mở rộng sang redesign không liên quan.

## Quy trình thực hiện

1. Chạy script scan trong kit và ghi lại các file đáng ngờ.
2. Lập bảng mapping `ảnh -> route -> component -> nguyên nhân -> file sẽ sửa`.
3. Sửa shared primitives/layout trước.
4. Sửa page-specific table/card sau.
5. Thêm/điều chỉnh test responsive.
6. Chạy lint, typecheck, unit/integration test hiện có và build frontend.
7. Chạy kiểm tra viewport theo `docs/ACCEPTANCE_CRITERIA.md`.
8. Chụp ảnh sau sửa ở 320x568, 375x667, 390x844, 768x1024 và desktop.
9. Báo cáo chính xác file đã sửa, lý do, test đã chạy và phần chưa thể kiểm tra.

## Yêu cầu triển khai kỹ thuật

- Thêm `min-width: 0` cho flex/grid child chứa text dài.
- Dùng `max-width: 100%` cho media/form/control.
- Dùng `overflow-wrap: anywhere` có chọn lọc cho email/tên rất dài.
- Tránh fixed width lớn; ưu tiên `clamp()`, `minmax()`, `%`, `fr`.
- Grid card nên dùng `repeat(auto-fit, minmax(min(100%, ...), 1fr))` khi phù hợp.
- Button group mobile nên `flex-wrap: wrap` hoặc chuyển column.
- Table wrapper desktop/tablet có `overflow-x: auto`; mobile nên dùng card nếu table nhiều cột.
- Không đặt font dưới mức đọc được chỉ để vừa màn hình.
- Fixed/sticky element phải tính safe area và không che content.
- Drawer mobile phải khóa scroll đúng lúc và trả focus khi đóng.

## Đầu ra bắt buộc

- Patch code thực tế trong repository.
- Danh sách file thay đổi, không bao gồm file không liên quan.
- Báo cáo responsive theo từng route/viewport.
- Kết quả lint/typecheck/build/test.
- Ảnh before/after hoặc visual snapshots.
- Không được nói “đã sửa” nếu chưa chạy kiểm tra tương ứng.
