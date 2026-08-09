# Codex Task Package — Admin Management Fix

## Mục tiêu

Sửa đúng **3 khu vực Admin** sau, dựa trên ảnh trong thư mục `references/`:

1. **All Users**
   - 4 administrative actions trong User Profile hiện không hoạt động:
     - Reset Password Link
     - Change Role
     - Block User Account
     - Soft-delete Account
2. **Categories**
   - Edit Category và Delete Category phải hoạt động đúng, an toàn dữ liệu.
3. **Transactions / Payments**
   - Toàn bộ số tiền đang hiển thị `USD` trong khi hệ thống thanh toán bằng **VNĐ/VND**.
   - Sửa cách format tiền và nhãn currency cho đúng thực tế.

## Nguyên tắc bắt buộc

- Trước khi sửa, **khảo sát code hiện có** và tận dụng controller/service/repository/entity/API hiện hữu.
- **Không hard-code đường dẫn hoặc tên file** theo tài liệu này nếu project đang có route/architecture khác.
- Không sửa những khu vực không liên quan.
- Không redesign toàn bộ Admin Console.
- Không làm thay đổi luồng Student/Teacher, enrollment, course learning, quiz, blog, AI, certificate… trừ khi một thay đổi cực nhỏ là dependency trực tiếp của 3 chức năng trên.
- Ưu tiên reuse design system/modal/toast hiện có.
- Mọi action thay đổi dữ liệu phải có **server-side authorization** cho Admin, CSRF hợp lệ và validation.
- Không dùng `window.confirm()`/`alert()` mặc định nếu project đã có popup/modal tự thiết kế.
- Không xóa payment/order history chỉ vì user bị soft-delete.
- Không thay đổi schema DB nếu field/relationship hiện có đã đáp ứng được. Chỉ migration khi thật sự thiếu dữ liệu cần thiết.

## Search anchors để Codex tìm đúng code

Tìm trong project theo các chuỗi:

- `/admin/users`
- `User Governance`
- `User Profile`
- `Reset Password Link - Not available`
- `Change Role - Not available`
- `Block User Account`
- `Soft-delete Account`
- `/admin/categories`
- `All Categories`
- `Cannot delete category as it is associated with existing courses`
- `/admin/transactions`
- `Transactions`
- `SUCCESSFUL AMOUNT`
- `VNPAY`
- `USD`

Sau khi tìm, lập danh sách file thực sự cần sửa rồi chỉ chạm vào các file đó.
