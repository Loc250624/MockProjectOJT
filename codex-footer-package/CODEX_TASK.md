# CODEX TASK — Xây dựng footer mới cho E-Learning

## Vai trò

Bạn là Senior Fullstack Developer. Hãy kiểm tra repository hiện tại và triển khai footer mới an toàn, nhất quán với design system của dự án.

## Mục tiêu

Tạo footer có bố cục đầy đủ như ảnh tham khảo:

1. Khối nhận diện nền tảng và thông tin liên hệ.
2. Nhóm liên kết “Về chúng tôi”.
3. Nhóm liên kết “Hỗ trợ”.
4. Nhóm liên kết “Tài nguyên/Công cụ”.
5. Khối thông tin đơn vị vận hành.
6. Dòng copyright.
7. Danh sách icon mạng xã hội.

Footer phải follow template hiện tại của website, không sao chép màu sắc hoặc branding F8.

## Yêu cầu bắt buộc

### 1. Kiểm tra dự án trước khi sửa

- Xác định frontend framework, cấu trúc layout, router, component dùng chung, design tokens và cách import icon hiện tại.
- Tìm footer cũ và mọi nơi đang render footer.
- Nếu đã có component footer, chỉnh component đó thay vì tạo footer trùng lặp.
- Nếu footer nằm trong layout dùng chung, chỉ thay đổi khu vực footer.
- Không sửa header, nội dung trang, API, authentication, quiz, lesson hoặc các khu vực không liên quan.

### 2. Dữ liệu tạm thời

Dự án hiện chưa có đầy đủ dữ liệu doanh nghiệp và liên kết. Hãy dùng nội dung tạm thời trung tính:

- Tên nền tảng: lấy từ cấu hình/branding hiện có. Nếu chưa có, dùng `E-Learning Platform`.
- Slogan: `Học tập linh hoạt, phát triển bền vững`.
- Hotline: `Đang cập nhật`.
- Email: `Đang cập nhật`.
- Địa chỉ: `Đang cập nhật`.
- Tên đơn vị vận hành: `Thông tin đơn vị vận hành đang được cập nhật`.
- Mã số doanh nghiệp: `Đang cập nhật`.
- Thông tin pháp lý/chứng nhận: `Đang cập nhật`.

Không tạo số điện thoại, email, địa chỉ, mã số doanh nghiệp hoặc URL giả.

### 3. Quy tắc link và plaintext

- Chỉ dùng thẻ liên kết khi route hoặc URL thật sự tồn tại.
- Route nội bộ phải dùng router component hiện có của dự án.
- Nếu chưa có route hoặc URL, hiển thị label bằng plaintext (`span`/`p`), không dùng `href="#"`, `javascript:void(0)` hoặc link chết.
- Không tạo trang mới chỉ để làm cho link có vẻ hoạt động.
- Link ngoài phải có `target="_blank"` và `rel="noopener noreferrer"`.

### 4. Mạng xã hội

Hiển thị icon cho các nền tảng phù hợp, ưu tiên:

- Facebook
- YouTube
- TikTok
- GitHub
- LinkedIn
- Discord

Quy tắc:

- Nếu dự án đã có thư viện icon, ưu tiên tái sử dụng.
- Nếu chưa có, có thể dùng SVG trong `assets/social/`.
- Nếu chưa có URL mạng xã hội, vẫn có thể hiển thị icon ở trạng thái plaintext/non-interactive kèm tooltip `Đang cập nhật`.
- Không dùng link giả.
- Mỗi icon phải có `aria-label` hoặc text hỗ trợ screen reader.
- Trạng thái hover/focus phải phù hợp theme hiện tại.

### 5. Giao diện

- Dùng màu, font, spacing, border, shadow và breakpoint của dự án.
- Footer phải có độ tương phản tốt ở light mode và dark mode nếu dự án hỗ trợ cả hai.
- Bố cục desktop có thể chia 4–5 cột tương tự ảnh tham khảo.
- Tablet giảm số cột hợp lý.
- Mobile xếp một cột hoặc accordion nếu dự án đã có pattern accordion.
- Không để text tràn, cột quá hẹp hoặc icon đè lên nội dung.
- Không hard-code chiều cao footer.
- Dòng copyright và social icons có thể nằm ở hàng dưới, có divider nhẹ.
- Không thêm badge/chứng nhận giả. Chỉ render badge khi asset và dữ liệu thật tồn tại.

### 6. Cấu trúc code

- Tách dữ liệu footer khỏi JSX/template nếu kiến trúc dự án cho phép.
- Tái sử dụng component/link hiện có.
- Tránh dependency mới chỉ để hiển thị icon.
- Không đưa thông tin nhạy cảm hoặc environment secret vào frontend.
- Không duplicate CSS global.
- Tên class/component phải follow convention hiện có.
- Starter trong `starter-react/` chỉ là tài liệu tham khảo, không được chép máy móc nếu dự án không dùng React/TypeScript.

### 7. Kiểm thử

Sau khi sửa:

- Chạy formatter/linter hiện có.
- Chạy unit test/build frontend hiện có.
- Kiểm tra route chính, trang đăng nhập, trang course, lesson và dashboard không bị ảnh hưởng.
- Kiểm tra footer ở 320px, 375px, 768px, 1024px và desktop lớn.
- Kiểm tra keyboard navigation, focus ring, screen reader labels và contrast.
- Không để lỗi console, warning key, hydration error hoặc broken import.

## Danh sách nội dung footer đề xuất

### Cột thương hiệu

- Logo/brand hiện có
- Slogan
- Hotline: Đang cập nhật
- Email: Đang cập nhật
- Địa chỉ: Đang cập nhật

### Về chúng tôi

- Giới thiệu
- Liên hệ
- Điều khoản sử dụng
- Chính sách bảo mật

Chỉ tạo link cho route có thật.

### Hỗ trợ

- Trung tâm trợ giúp
- Hướng dẫn học tập
- Câu hỏi thường gặp
- Chính sách thanh toán

Chỉ tạo link cho route có thật.

### Tài nguyên

- Khóa học
- Blog
- Chứng chỉ
- AI Chatbot

Chỉ tạo link cho tính năng/route có thật.

### Đơn vị vận hành

- Thông tin đơn vị vận hành đang được cập nhật
- Mã số doanh nghiệp: Đang cập nhật
- Thông tin pháp lý: Đang cập nhật

## Output Codex cần cung cấp

1. Danh sách file đã sửa/thêm.
2. Tóm tắt quyết định kỹ thuật.
3. Kết quả build/test.
4. Các trường dữ liệu vẫn đang là placeholder.
5. Screenshot hoặc mô tả kiểm tra responsive nếu môi trường cho phép.
