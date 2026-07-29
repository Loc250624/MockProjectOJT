# NHIỆM VỤ CHO CODEX

Bạn là Senior Fullstack Developer đang làm việc trực tiếp trong repository E-Learning hiện tại.

Hãy kiểm tra codebase trước khi sửa để xác định chính xác:

- Framework frontend và hệ thống styling đang sử dụng.
- Shared layout, landing page hoặc component đang render khu vực `Ready to begin?`.
- Footer hiện tại có phải component dùng chung hay chỉ nằm trong một page.
- Design tokens, CSS variables, Tailwind theme, typography, spacing, button style và breakpoints hiện có.
- Logo/tên thương hiệu chính xác đang được Header sử dụng.

## Vấn đề hiện tại

Khu vực cuối trang hiện chỉ là CTA lớn gồm tiêu đề `Ready to begin?`, mô tả và hai nút. Nội dung này bị trùng mục đích với các nút phía trên trang, không cung cấp thông tin hữu ích và tạo cảm giác footer còn thiếu.

Ảnh hiện trạng:

- `references/01-current-footer.png`

Ảnh tham khảo bố cục:

- `references/02-fpt-shop-footer-reference.png`

Chỉ tham khảo cách FPT Shop chia footer thành nhiều nhóm thông tin. Tuyệt đối không sao chép màu sắc, logo, thương hiệu, QR, mạng xã hội, chứng nhận, phương thức thanh toán hoặc nội dung của FPT Shop.

## Yêu cầu bắt buộc

### 1. Thay CTA hiện tại bằng footer thực sự

- Loại bỏ khu vực CTA `Ready to begin?` ở cuối trang.
- Không giữ lại hai nút `Browse Courses` và `Create Account` trong footer mới vì phía trên trang đã có CTA tương ứng.
- Tạo footer có bố cục đầy đủ, cân đối và lấp đầy toàn bộ chiều ngang.
- Footer phải có giá trị thông tin thực tế cho nền tảng E-Learning.

### 2. Giữ nguyên footprint của khu vực cuối trang

- Trên desktop, không làm footer mới cao hơn đáng kể so với khu vực footer/CTA hiện tại.
- Tận dụng chiều ngang bằng grid nhiều cột thay vì tăng chiều cao.
- Giữ padding trên/dưới và tổng visual footprint gần với hiện trạng; sai lệch mục tiêu tối đa khoảng 8–16 px nếu cần để tránh cắt chữ.
- Không dùng `overflow: hidden` để che nội dung.
- Trên tablet/mobile, được phép chuyển thành 2 cột hoặc 1 cột và để chiều cao tự nhiên nhằm đảm bảo không mất nội dung.

### 3. Bố cục đề xuất

Ưu tiên bố cục desktop 4 cột, có thể điều chỉnh theo hệ thống grid hiện tại:

1. **Brand / About**
   - Dùng đúng logo hoặc tên thương hiệu đang xuất hiện ở Header.
   - Một mô tả ngắn về nền tảng học trực tuyến.
   - Hiển thị email hỗ trợ hoặc thông tin liên hệ chỉ khi codebase đã có dữ liệu thật; không tự bịa thông tin.

2. **Explore**
   - Course Catalog
   - Learning Paths
   - Blog
   - Certificates
   - AI Chatbot

3. **Learning & Teaching**
   - My Courses
   - Progress Tracking
   - Quizzes
   - Teacher Workspace
   - Feedback & Support

4. **Policies & Help**
   - Help Center
   - Privacy Policy
   - Terms of Use
   - Refund Policy
   - Accessibility

Có thể đổi tên hoặc bỏ những mục không tồn tại trong sản phẩm, nhưng phải giữ mật độ nội dung tương đương và chỉ dùng các tính năng có thật trong codebase.

Thêm một hàng nhỏ ở đáy footer:

- Copyright dùng năm hiện tại tự động.
- Tên thương hiệu lấy từ codebase.
- Một thông điệp ngắn, ví dụ `Learn. Practice. Grow.` nếu phù hợp với tone hiện tại.

### 4. Các mục dạng liên kết phải hoàn toàn không điều hướng

Người dùng yêu cầu các đường link chỉ để hiển thị.

- Không dùng `href="#"` vì sẽ làm trang cuộn lên đầu.
- Không dùng route nội bộ, `Link`, `NavLink`, `router.push`, `window.location`, hoặc handler điều hướng.
- Không để click làm đổi URL, reload trang, mở tab mới hoặc cuộn trang.
- Cách ưu tiên: render bằng `span` hoặc phần tử text có style giống link.
- Không thêm `onClick` giả nếu không cần thiết.
- Con trỏ nên là `default`, không phải `pointer`, để không gây hiểu nhầm.
- Có thể dùng `aria-disabled="true"` nếu phần tử được mô phỏng như liên kết, nhưng không đưa vào tab order.

### 5. Design phải follow toàn bộ website

- Tái sử dụng chính xác font, container width, border radius, spacing, màu chủ đạo, màu text và gradient hiện có.
- Không dùng nền đen và màu đỏ theo FPT Shop.
- Footer nên cùng họ màu với hero/branding hiện tại: deep navy, blue hoặc gradient đang dùng trong project.
- Dùng độ tương phản tốt cho text chính/phụ.
- Dùng divider mảnh, opacity thấp nếu cần.
- Hover chỉ được thay đổi nhẹ về màu/opacity; không được tạo cảm giác có thể click.
- Không thêm icon rời rạc nếu project không có hệ icon sẵn.
- Không thêm package icon mới.

### 6. Responsive

- Desktop: 4 cột, khoảng cách đều, không có vùng trống lớn bất thường.
- Tablet: 2 cột.
- Mobile: 1 cột, text không bị cắt, không tràn ngang.
- Không để footer che chatbot/floating button nếu dự án có các thành phần fixed.
- Kiểm tra ở tối thiểu các viewport: 1440 px, 1024 px, 768 px, 390 px.

### 7. Phạm vi thay đổi

- Chỉ sửa component/page/style liên quan trực tiếp đến footer cuối trang.
- Có thể tách dữ liệu footer thành một constant/config nhỏ nếu giúp code dễ bảo trì.
- Không thay đổi Header, Hero, routing, authentication, course logic, backend API hoặc database.
- Không sửa toàn cục các class chung theo cách có thể làm thay đổi các trang khác.
- Không thêm dependency mới.
- Không xóa code ngoài phạm vi footer.

## Cách triển khai mong muốn

1. Tìm component render CTA/footer hiện tại.
2. Ghi lại ngắn gọn các file sẽ sửa và lý do.
3. Tạo footer mới theo data-driven structure để tránh lặp JSX/template.
4. Dùng token/style có sẵn của project.
5. Đảm bảo tất cả mục liên kết là static display-only text.
6. Chạy formatter, linter và build/test phù hợp với repository.
7. Tự kiểm tra responsive và regression.
8. Cuối cùng báo cáo:
   - File nào đã sửa.
   - CTA cũ đã được thay ở đâu.
   - Footer mới gồm những nhóm nào.
   - Cách đảm bảo các mục không điều hướng.
   - Lệnh test/build đã chạy và kết quả.

## Điều kiện hoàn thành

Chỉ coi nhiệm vụ hoàn tất khi:

- CTA `Ready to begin?` không còn xuất hiện ở cuối trang.
- Footer mới lấp đầy khu vực, không tạo khoảng trống vô nghĩa.
- Desktop không cao hơn đáng kể so với footer cũ.
- Không có mục nào làm đổi route hoặc URL khi click.
- Không có horizontal scroll hoặc text bị cắt.
- Giao diện đồng bộ với website hiện tại.
- Build frontend thành công.
- Không có thay đổi ngoài phạm vi footer.
