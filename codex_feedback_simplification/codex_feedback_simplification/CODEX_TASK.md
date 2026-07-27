# NHIỆM VỤ CHO CODEX: LƯỢC BỎ CATEGORY TRONG FEEDBACK

Bạn đang làm việc trong một dự án E-Learning có màn hình Feedback giống ảnh `reference/feedback-current.png`.

## Mục tiêu

Tối giản màn hình Feedback để người dùng phản hồi nhanh hơn bằng cách:

1. Xóa hoàn toàn khối **CATEGORY / Choose a topic** và toàn bộ lựa chọn:
   - Course Content
   - Instructor
   - Assessment
   - Platform/UI
   - Technical Issue
   - Other
2. Giữ lại phần **RATING MATRIX / Rate your experience**.
3. Tái bố cục phần rating thành một card duy nhất, nằm giữa trang, nhìn cân đối ở desktop, tablet và mobile.
4. Không làm hỏng luồng submit feedback, validation, lịch sử feedback, trang admin/teacher đọc feedback hoặc API backend.

## Quy tắc bắt buộc

- Trước khi sửa, tìm toàn bộ nơi sử dụng `category`, `feedbackCategory`, `topic`, `selectedCategory` hoặc tên tương đương ở frontend và backend.
- Chỉ sửa code có liên quan trực tiếp đến form Feedback và contract submit của form đó.
- Không xóa enum/cột database/category dùng ở khu vực khác nếu chưa chứng minh chỉ phục vụ riêng form này.
- Không đổi API contract một cách vội vàng.
- Không hard-code một giá trị category mới không tồn tại trong backend.
- Không làm mất dữ liệu rating hiện có.
- Không thay đổi logic phân quyền hoặc authentication.
- Không thay đổi component dùng chung nếu có nguy cơ ảnh hưởng trang khác; ưu tiên style cục bộ hoặc modifier class.

## Quy trình thực hiện

### Bước 1 — Xác định phạm vi

Tìm:

- Route/page chứa Feedback.
- Component render hai card Rating Matrix và Category.
- State/validation của category.
- Type/interface/schema của payload gửi feedback.
- API client/service submit feedback.
- Backend request DTO/entity/validation/database constraint liên quan đến category.
- Test hiện có của Feedback.

Ghi lại đường dẫn file thật trước khi chỉnh.

### Bước 2 — Xóa Category khỏi UI

- Xóa card Category khỏi JSX/template.
- Xóa label, radio button, state hover/focus và validation message dành riêng cho Category.
- Xóa khoảng trống/layout column từng dành cho Category.
- Không để lại hidden radio, DOM rỗng hoặc placeholder trắng.

### Bước 3 — Tái bố cục đẹp và gọn

Thiết kế mục tiêu:

- Một card duy nhất, `max-width` khoảng 880–960px, căn giữa.
- Header gọn: eyebrow “RATING MATRIX”, tiêu đề “Rate your experience”, badge “1–5”.
- Các tiêu chí rating có thể hiển thị 2 cột trên desktop để giảm chiều dài; 1 cột trên màn hình nhỏ.
- `Overall Satisfaction` có thể chiếm toàn chiều rộng để nhấn mạnh.
- Khoảng cách đều, đường phân cách nhẹ, không có vùng trắng lớn bất thường.
- Sao có kích thước chạm tối thiểu 40–44px trên mobile.
- Giữ focus ring rõ ràng, hỗ trợ bàn phím và screen reader.
- Không gây horizontal scroll ở 320px.

Tham khảo mẫu trong:

- `reference/FeedbackRatingMatrix.reference.tsx`
- `reference/feedback-rating-matrix.reference.css`

Không copy mù quáng; phải dùng conventions và design tokens hiện có của dự án.

### Bước 4 — Bảo toàn tương thích API

Áp dụng đúng một trong các nhánh sau sau khi kiểm tra backend:

#### Nhánh A: `category` không bắt buộc

- Bỏ field category khỏi payload frontend.
- Xóa validation liên quan.
- Cập nhật type/schema test tương ứng.
- Không sửa database nếu không cần.

#### Nhánh B: backend vẫn bắt buộc category nhưng category không còn cho người dùng chọn

- Giữ field trong payload nội bộ.
- Gán một **giá trị hợp lệ đã tồn tại** và có ý nghĩa chung, lấy từ enum/constant backend hiện có.
- Giá trị này không xuất hiện trên UI.
- Đặt logic tại một nơi rõ ràng, có comment giải thích đây là default phục vụ backward compatibility.
- Bổ sung test chứng minh submit không bị 400/validation error.

#### Nhánh C: category bắt buộc ở database/API và không có giá trị chung hợp lệ

- Thực hiện migration/DTO change nhỏ nhất để cho phép null hoặc đặt default phù hợp.
- Chỉ làm khi đã kiểm tra toàn bộ nơi đọc dữ liệu category.
- Đảm bảo dữ liệu cũ vẫn đọc được.
- Không xóa cột nếu các trang báo cáo/admin còn dùng.

## Acceptance criteria

- Không còn chữ “CATEGORY” hoặc “Choose a topic” trên màn hình Feedback.
- Không còn radio category trong DOM.
- Card Rating Matrix nằm cân đối, không bị lệch sang trái.
- Submit Feedback thành công khi người dùng chọn rating hợp lệ.
- Không xuất hiện lỗi validation yêu cầu category.
- Không có lỗi console.
- Không có horizontal scroll ở viewport 320, 375, 768, 1024, 1440px.
- Điều khiển sao dùng được bằng chuột, touch và keyboard.
- Build, lint và test liên quan đều pass.
- Codex báo cáo chính xác file đã sửa và lý do sửa từng file.

## Output Codex phải trả về

1. Danh sách file đã sửa.
2. Tóm tắt thay đổi theo frontend/backend/test.
3. Kết quả build/lint/test.
4. Nhánh tương thích API đã chọn (A, B hoặc C) và bằng chứng từ code.
5. Các rủi ro còn lại, nếu có.
