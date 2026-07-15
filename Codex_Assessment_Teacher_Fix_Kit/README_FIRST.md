# Codex Assessment Teacher Fix Kit

Bộ tài liệu này dùng để giao cho Codex kiểm tra và sửa toàn bộ luồng Assessment liên quan đến khu vực Teacher, đặc biệt là **Grading** và **Assignments**, trong dự án E-Learning sử dụng:

- Spring Boot 3.x
- Java 17
- Spring MVC, Spring Data JPA, Spring Security
- Thymeleaf
- HTML, CSS, JavaScript thuần
- MySQL
- Maven Wrapper

## Hiện tượng đã quan sát

1. Trang **Teacher > Grading** chỉ hiển thị sidebar/nền, phần nội dung chính trống.
2. Trang **Teacher > Assignments** hiển thị HTML gần như không có CSS, bố cục vỡ và có dấu hiệu dùng dữ liệu mẫu/hard-code.
3. Các thao tác quản lý, chấm điểm và nhận xét Assessment của Teacher chưa hoạt động end-to-end.
4. Cần kiểm tra đồng thời sự liên kết giữa các chức năng ASM-01 đến ASM-06.

Ảnh bằng chứng nằm trong thư mục `evidence/`.

## Cách sử dụng

1. Giải nén file này vào **thư mục gốc của repository**, hoặc đưa toàn bộ nội dung cho Codex làm tài liệu tham chiếu.
2. Mở Codex tại thư mục gốc dự án.
3. Gửi nội dung file `prompts/CODEX_MASTER_PROMPT.txt`.
4. Cho Codex đọc `AGENTS.md`, sau đó thực hiện từng giai đoạn trong `prompts/CODEX_STEP_PROMPTS.md`.
5. Không chấp nhận kết quả chỉ sửa giao diện. Codex phải chứng minh:
   - Route hoạt động.
   - Quyền Teacher được kiểm tra đúng.
   - Dữ liệu lấy từ database, không hard-code.
   - Các nút thao tác gọi đúng backend.
   - Test tự động hoặc hướng dẫn kiểm thử thủ công có kết quả.
   - Không làm hỏng Student flow.

## Kết quả Codex bắt buộc phải trả về

- Danh sách nguyên nhân gốc đã xác minh.
- Danh sách file đã sửa/tạo.
- Mô tả migration hoặc dữ liệu seed nếu có.
- Kết quả `./mvnw test`.
- Kết quả kiểm tra các route Teacher.
- Bảng đối chiếu từng Acceptance Criterion: PASS/FAIL.
- Các giới hạn còn lại, nếu có, phải được nêu rõ; không được tuyên bố hoàn thành khi chưa kiểm chứng.
