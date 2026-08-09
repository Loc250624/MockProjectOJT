# Functional Requirements

## A. Admin → All Users → User Profile

### A1. Reset Password Link

#### LOCAL account
Khi Admin chọn `Reset Password`:

- Backend tạo **one-time reset token** an toàn, có thời hạn.
- Không sinh/mở/hiển thị mật khẩu mới dạng plaintext.
- Nếu project đã có mail service:
  - gửi reset link đến email user;
  - UI báo gửi thành công.
- Nếu project chưa có mail delivery:
  - backend có thể trả về reset link tạm thời cho Admin;
  - UI hiển thị trong modal với nút `Copy reset link`.
- Token nên hết hạn khoảng 15–30 phút và chỉ dùng một lần.
- Token cũ chưa dùng của user có thể bị revoke khi tạo token mới.

#### OAuth account (Google/GitHub)
Nếu account không có local credential:

- Không cố reset password local.
- Action vẫn phải có phản hồi đúng:
  - disable hoặc chuyển sang trạng thái không khả dụng;
  - giải thích `Password is managed by Google/GitHub`.
- Không báo lỗi 500.

> Nếu hệ thống hiện có cơ chế password reset khác, reuse đúng cơ chế đó thay vì tạo hệ thống song song.

---

### A2. Change Role

Khi click `Change Role`:

- Mở modal.
- Hiển thị current role.
- Cho chọn role hợp lệ theo enum/business rule hiện có, ví dụ `STUDENT`, `TEACHER`, `ADMIN`.
- Submit gọi backend, cập nhật DB và UI ngay sau thành công.
- Validate server-side:
  - không nhận role string ngoài enum;
  - không được tự hạ/xóa quyền của chính tài khoản Admin đang thao tác nếu việc đó có thể khóa quyền quản trị;
  - không được làm mất **Admin cuối cùng** của hệ thống.
- Nếu project không cho promote thành ADMIN theo business rule hiện tại thì UI chỉ hiển thị các role được phép; không tự mở rộng quyền.

---

### A3. Block / Unblock User Account

Nút phải phản ánh trạng thái hiện tại:

- Active user → `Block User Account`
- Blocked user → `Unblock User Account`

Khi block:

- cập nhật trạng thái account bằng field hiện có (`enabled`, `active`, `status`, `blocked`, ...);
- user không thể đăng nhập hoặc tiếp tục dùng session theo cơ chế security của project;
- nếu hệ thống có session registry/token store, revoke/invalidate session hợp lý;
- không xóa dữ liệu học tập, course, payment, blog, feedback.

Safety:

- Admin không được block chính mình.
- Không được block Admin cuối cùng.
- UI có custom confirmation modal.
- Thành công: cập nhật badge `ACTIVE/BLOCKED` mà không cần reload toàn trang nếu frontend hiện hỗ trợ.

---

### A4. Soft-delete User Account

Đây là **soft delete**, không phải hard delete.

- Reuse field hiện có như `deleted`, `deletedAt`, `status=DELETED`, `active=false`...
- Nếu project chưa có soft-delete state, bổ sung tối thiểu cần thiết bằng migration phù hợp.
- User soft-deleted:
  - không được đăng nhập;
  - mặc định không xuất hiện như active account;
  - dữ liệu liên quan như payment/order/enrollment/audit phải được giữ lại.
- Không cascade delete lịch sử thanh toán.
- Không hard delete foreign-key related data.
- Không cho soft-delete chính Admin đang đăng nhập.
- Không cho soft-delete Admin cuối cùng.
- Modal xác nhận phải cảnh báo rõ đây là thao tác vô hiệu hóa tài khoản nhưng vẫn giữ audit/history.

Nếu project đã có màn hình deleted/archived users thì integrate vào đó. Nếu chưa có, không cần tự tạo một module lớn mới chỉ để restore.

---

### A5. Loading/Error/UX chung cho 4 actions

- Button có loading state khi request đang chạy.
- Chống double submit.
- HTTP 4xx/5xx phải được map thành message rõ ràng.
- Không nuốt exception.
- Sau thao tác thành công, user profile modal và row trong table phải đồng bộ.
- Không để text cố định `Not available` nếu tính năng thực tế đã hoạt động.
- OAuth-only password reset là ngoại lệ hợp lệ và phải giải thích đúng lý do.

---

## B. Admin → Categories

### B1. Edit Category

`Edit` phải hoạt động cho mọi category hợp lệ:

- mở modal/form với dữ liệu hiện tại;
- cho sửa ít nhất:
  - Name
  - Description
- trim input.
- Name không rỗng.
- Validate unique name theo rule hiện có, ưu tiên case-insensitive nếu project đang dùng như vậy.
- Không được tạo duplicate chỉ vì khác khoảng trắng đầu/cuối.
- Submit bằng endpoint/backend flow hiện có hoặc endpoint REST phù hợp architecture.
- Sau thành công, cập nhật row trong table và count nếu cần.
- Category đang được course sử dụng **vẫn được phép edit**, vì edit metadata không phá relation.

### B2. Delete Category — safe dependency handling

Delete phải có luồng thực tế, không được chỉ bấm rồi luôn báo lỗi.

#### Trường hợp category chưa được course sử dụng
- custom confirm modal;
- delete/soft-delete theo pattern hiện có;
- update table sau thành công.

#### Trường hợp category đang được course sử dụng
Không được hard-delete làm orphan courses.

Backend trả dependency info hoặc `409 Conflict` có structured error, ví dụ:

- category id/name
- number of dependent courses
- message business-friendly

UI hiển thị modal giải thích rõ.

**Phương án quản trị nên hỗ trợ nếu kiến trúc hiện tại cho phép mà không mở rộng quá mức:**

`Reassign courses & delete category`

- Admin chọn một category thay thế khác.
- Backend chạy trong **một transaction**:
  1. validate source/replacement;
  2. move/reassign toàn bộ courses liên quan sang replacement category;
  3. delete/soft-delete source category;
  4. commit.
- Nếu bất kỳ bước nào fail → rollback toàn bộ.
- Không cho chọn chính category đang xóa làm replacement.
- Nếu không có category thay thế, chỉ cho Cancel và giữ nguyên dữ liệu.

Nếu project hiện có policy cố định là category đang dùng tuyệt đối không được delete, vẫn phải:
- làm nút Delete hoạt động;
- trả và hiển thị lỗi dependency rõ ràng, không generic 500;
- cung cấp link/action `View affected courses` nếu route All Courses có filter phù hợp.
Không tự phá business rule chỉ để “delete được”.

### B3. Backend correctness

- Server-side Admin authorization.
- CSRF hợp lệ.
- DB FK/constraints không bị bypass.
- Không catch exception rồi trả 200 giả.
- Duplicate name nên là 400/409 đúng nghĩa.
- Dependent delete nên là 409 thay vì 500.

---

## C. Admin → Transactions / Payments — VND currency

### C1. Currency display

Hệ thống đang thanh toán qua VNPAY và số tiền là VNĐ. Sửa tất cả hiển thị liên quan trong màn Admin Transactions:

- summary card `SUCCESSFUL AMOUNT`
- amount column trong transaction table
- transaction detail popup/drawer nếu có
- CSV/export nếu màn này có export
- tooltip/empty state/text dùng currency
- bất kỳ formatter shared nào chỉ phục vụ màn transaction/payment này

**Không hiển thị `USD`.**

Recommended display:

- `47.933.750 ₫`
- `3.249.750 ₫`
- hoặc `47.933.750 VND`

Ưu tiên `Intl.NumberFormat('vi-VN', { style: 'currency', currency: 'VND', maximumFractionDigits: 0 })`
hoặc formatter equivalent của stack hiện tại.

Không hiển thị `.00` cho VND nếu không có lý do nghiệp vụ.

### C2. VNPay amount semantics

Kiểm tra dữ liệu trước khi sửa formatter:

- Domain/database amount phải đại diện cho số tiền VND thật.
- `vnp_Amount` thường là đơn vị amount × 100 khi gửi sang gateway.
- Callback/return data từ gateway có thể cần normalize lại đúng 1 lần.
- **Không được** vừa chia ở backend rồi lại chia ở frontend.
- Không được sửa giá trị DB nếu DB hiện đang lưu đúng, chỉ vì UI đang dán label USD.

Ví dụ nếu row đang có amount `3,249,750`, UI phải hiển thị xấp xỉ:
`3.249.750 ₫`,
không phải `32.497,50` và không phải `324.975.000`.

### C3. Totals

`Successful Amount` phải sum đúng các transaction status được coi là thành công theo enum hiện có (`PAID`, `SUCCESS`, `COMPLETED`, ...).

Không nhân/chia currency trong phép sum chỉ để phục vụ presentation.
