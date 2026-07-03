# LRN-01 — Đăng ký khóa học miễn phí hoặc kích hoạt sau thanh toán

- **Mã chức năng:** LRN-01
- **Module:** Enrollment
- **Actor chính:** Student
- **Mức ưu tiên:** Cao
- **Phụ thuộc:** Authentication/RBAC, Course, Order/Payment (đối với khóa trả phí)

## Mục tiêu

Cho phép Student:
- Đăng ký ngay một khóa học miễn phí.
- Được kích hoạt quyền học khóa trả phí chỉ sau khi giao dịch đã được hệ thống xác nhận thành công.
- Không thể tạo nhiều enrollment hoạt động cho cùng một khóa học.
- Nhìn thấy trạng thái đăng ký rõ ràng trên trang chi tiết khóa học và dashboard.

## Quy tắc nghiệp vụ

1. Chỉ tài khoản Student đang hoạt động mới được tự đăng ký.
2. Student không được đăng ký khóa học chưa xuất bản, bị khóa, bị xóa mềm hoặc không còn cho phép enrollment.
3. Khóa miễn phí:
   - Tạo enrollment trực tiếp.
   - Trạng thái cuối cùng phải cho phép truy cập nội dung học.
4. Khóa trả phí:
   - Không kích hoạt enrollment chỉ vì client báo thanh toán thành công.
   - Chỉ kích hoạt sau khi backend xác minh order/payment thuộc đúng Student, đúng khóa học, đúng số tiền và có trạng thái thanh toán hợp lệ.
5. Một Student + một Course chỉ có tối đa một enrollment hiệu lực.
6. Các request lặp lại phải idempotent: không tạo enrollment/order item trùng.
7. Teacher hoặc Admin không được tự động trở thành Student enrollment thông qua endpoint này.
8. Không tin `userId`, `price`, `paymentStatus` gửi từ frontend; lấy user từ authentication context và dữ liệu giá từ database.
9. Nếu enrollment đã tồn tại, API trả về trạng thái hiện tại thay vì tạo bản ghi mới.
10. Ghi nhận thời điểm đăng ký/kích hoạt và nguồn kích hoạt nếu kiến trúc hiện tại hỗ trợ audit.

## Phạm vi triển khai

### Backend
- Rà soát entity `Course`, `Enrollment`, `Order`, `OrderItem`, `Payment` và enum trạng thái hiện có.
- Hoàn thiện service đăng ký miễn phí và service kích hoạt sau thanh toán.
- Bổ sung kiểm tra unique ở cả service và database khi phù hợp.
- Dùng transaction cho luồng tạo/kích hoạt enrollment.
- Tích hợp với payment callback/webhook/service nội bộ đang tồn tại; không tạo endpoint giả cho phép client tự chuyển payment thành `PAID`.
- Chuẩn hóa exception cho: course không tồn tại, course không khả dụng, sai role, đã đăng ký, chưa thanh toán, payment không khớp.
- Tránh race condition khi hai request enrollment chạy đồng thời.

### API/Controller
Tận dụng route convention hiện tại. Các capability cần có:
- Đăng ký khóa miễn phí.
- Lấy trạng thái enrollment của Student hiện tại đối với một course.
- Kích hoạt enrollment từ luồng thanh toán đã được xác minh.
- Không nhận `studentId` từ client cho thao tác tự đăng ký.

### Frontend
- Nút theo trạng thái: `Enroll for free`, `Buy/Checkout`, `Continue learning`, `Payment pending`, `Unavailable`.
- Chặn double-click và hiển thị loading/error rõ ràng.
- Sau thành công, cập nhật CTA và dashboard mà không tạo enrollment lần hai.
- Không hiển thị nút đăng ký với course không khả dụng.

### Database
- Kiểm tra constraint/index bảo đảm không trùng `(student_id, course_id)`.
- Nếu cần migration, tạo migration theo công cụ dự án đang dùng.

## Test bắt buộc

- Student đăng ký khóa miễn phí thành công.
- Request lặp lại không tạo enrollment thứ hai.
- Hai request đồng thời không tạo bản ghi trùng.
- Không đăng ký được course draft/blocked/deleted.
- Anonymous, Teacher và Admin bị từ chối đúng chuẩn.
- Khóa trả phí không được kích hoạt khi payment chưa thành công.
- Payment của người khác hoặc khóa khác không thể kích hoạt enrollment.
- Payment hợp lệ kích hoạt enrollment đúng một lần.
- Frontend hiển thị đúng CTA theo trạng thái.

## Tiêu chí nghiệm thu

- Student học được khóa miễn phí ngay sau khi đăng ký thành công.
- Student chỉ học được khóa trả phí sau xác nhận thanh toán ở backend.
- Không có duplicate enrollment.
- Không có IDOR bằng cách đổi `studentId`, `courseId`, `orderId` hoặc `paymentId`.
- Build và test hiện có không bị regression.

## Nguyên tắc bắt buộc dành cho Codex

1. Đọc `AGENTS.md`, `README.md`, tài liệu kiến trúc, cấu hình build và quy ước trong repository trước khi thay đổi mã nguồn.
2. Khảo sát toàn bộ luồng liên quan từ entity/model → repository → service → controller/API → security → giao diện → test.
3. Tận dụng cấu trúc, naming convention, DTO, mapper, exception handler, response format và design system đang tồn tại; không tạo kiến trúc song song.
4. Không sửa hoặc format hàng loạt các file ngoài phạm vi chức năng.
5. Không xóa hành vi hiện có chỉ để làm test mới chạy.
6. Không hard-code user, role, course, price, trạng thái, URL, secret hoặc dữ liệu môi trường.
7. Mọi quyền truy cập phải được kiểm tra ở backend; việc ẩn nút trên frontend không được xem là biện pháp bảo mật.
8. Với thay đổi database, ưu tiên migration tương thích ngược. Không drop/rename cột đang dùng nếu chưa chứng minh an toàn.
9. Bổ sung unit test và integration test cho luồng thành công, từ chối truy cập và trường hợp biên.
10. Chạy các lệnh build/test phù hợp với repository. Nếu lệnh nào không chạy được, ghi rõ nguyên nhân và phần đã kiểm tra thay thế.
11. Cuối tác vụ, báo cáo:
    - Những file đã thay đổi.
    - Migration hoặc cấu hình mới.
    - Test đã chạy và kết quả.
    - Giả định còn tồn tại.
    - Cách kiểm thử thủ công.

---

## Prompt hoàn chỉnh cho Codex

> Sao chép nguyên khối nội dung bên dưới vào Codex khi đang mở đúng repository và đúng feature branch.

```text
Bạn đang làm việc trong repository E-Learning hiện tại. Hãy triển khai **LRN-01 — Student đăng ký khóa học miễn phí hoặc được kích hoạt sau thanh toán**.

### Kết quả cần đạt
1. Student đang đăng nhập có thể đăng ký trực tiếp khóa học miễn phí đang được xuất bản và cho phép enrollment.
2. Với khóa trả phí, quyền học chỉ được kích hoạt sau khi backend xác minh payment/order thành công.
3. Không tạo enrollment trùng cho cùng Student và Course; request phải idempotent và an toàn khi chạy đồng thời.
4. Frontend hiển thị đúng CTA theo trạng thái: enroll miễn phí, mua khóa, chờ thanh toán hoặc tiếp tục học.
5. Không tin dữ liệu `studentId`, `price`, `paymentStatus` từ client.

### Hãy thực hiện
- Tìm và tái sử dụng entity, repository, service, controller, DTO, exception, security và UI hiện có.
- Phân tích enum trạng thái enrollment/order/payment trước khi chỉnh sửa.
- Thêm transaction, constraint/index hoặc locking phù hợp để tránh duplicate.
- Tích hợp kích hoạt enrollment vào luồng thanh toán được xác minh đang tồn tại; tuyệt đối không thêm API công khai cho client tự đánh dấu thanh toán thành công.
- Lấy Student hiện tại từ security context.
- Bổ sung validation, error response và thông báo UI.
- Viết test cho happy path, duplicate, concurrent request, course không khả dụng, sai role, payment chưa thành công, payment không thuộc user/course và payment hợp lệ.

### Cách làm việc bắt buộc

- Trước tiên, hãy đọc `AGENTS.md` và khảo sát repository. Không bắt đầu viết code ngay.
- Hãy xác định các thành phần đã tồn tại và lập một kế hoạch ngắn theo đúng kiến trúc hiện tại.
- Chỉ triển khai phạm vi của chức năng này và những thay đổi phụ thuộc trực tiếp.
- Không tạo entity, endpoint, service hoặc trang trùng với thành phần hiện có.
- Không sửa các chức năng không liên quan.
- Bảo đảm authorization ở backend, validation đầu vào, xử lý lỗi nhất quán và không lộ dữ liệu nhạy cảm.
- Thêm hoặc cập nhật test phù hợp.
- Chạy build/test của dự án.
- Sau khi hoàn tất, tự review diff để tìm lỗi logic, N+1 query, IDOR, race condition, lỗi trạng thái và regression.
- Trả về báo cáo gồm: kế hoạch đã thực hiện, file thay đổi, test đã chạy, kết quả, cách kiểm thử thủ công và các giả định.
```
