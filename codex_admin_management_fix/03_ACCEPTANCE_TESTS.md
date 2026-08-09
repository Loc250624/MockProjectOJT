# Acceptance Tests

Codex chỉ coi task hoàn thành khi pass tối thiểu các case sau.

## A. All Users

### Reset Password
1. Open LOCAL user → Reset Password.
2. Request thành công.
3. Có success message.
4. Nếu mail enabled: email/reset delivery flow được gọi.
5. Nếu mail disabled: Admin nhận được copyable reset link theo thiết kế đã chọn.
6. Reset token hết hạn và one-time theo service hiện có/new implementation.
7. OAuth user → action không gây 500; UI nói rõ password managed by provider.

### Change Role
1. STUDENT → TEACHER thành công.
2. Reload trang → role vẫn đúng DB.
3. Invalid role → 400.
4. Admin tự cố hạ role của mình → bị chặn nếu có nguy cơ mất quyền quản trị.
5. Last admin protection hoạt động.
6. UI row + profile modal cập nhật.

### Block
1. Active user → Block → status đổi.
2. User đó không login/access authenticated session theo security design.
3. Reload → vẫn blocked.
4. Nút đổi thành Unblock.
5. Unblock → login được lại.
6. Không block self/last admin.

### Soft-delete
1. Soft-delete regular user → thành công.
2. Record lịch sử cần audit/payment vẫn còn.
3. User không login được.
4. Không hard-delete foreign-key data.
5. Self/last-admin protection pass.

## B. Categories

### Edit
1. Edit name only → success.
2. Edit description → success.
3. Edit category đang có courses → success.
4. Empty name → validation.
5. Duplicate name → validation/conflict.
6. Reload → data persistence đúng.

### Delete unused
1. Delete unused category → confirmation.
2. Success → row biến mất/update đúng.
3. Reload → state đúng.

### Delete used
1. Delete category có dependent courses.
2. Không có SQL/FK exception lộ ra UI.
3. Backend trả business conflict/dependency info.
4. Nếu implemented `Reassign & delete`:
   - chọn replacement khác;
   - courses được chuyển đúng;
   - source category bị xóa;
   - all in one transaction;
   - rollback khi cố tình gây lỗi giữa transaction.

## C. Transactions VND

1. `SUCCESSFUL AMOUNT` không còn chữ USD.
2. Table amount không còn chữ USD.
3. Amount `3249750` hiển thị thành khoảng `3.249.750 ₫`/`3.249.750 VND`.
4. Không có `.00` không cần thiết.
5. Sum successful amount khớp dữ liệu.
6. Detail/export (nếu có) cũng là VND.
7. Không có double division/multiplication 100.
8. VNPAY transaction flow không bị hỏng.

## D. Regression

- Admin sidebar/header hoạt động như trước.
- Search/filter/pagination All Users vẫn chạy.
- Search Categories vẫn chạy.
- Transaction filters/date/sort/pagination vẫn chạy.
- Không có console error mới.
- Không có 403 do thiếu CSRF trong các action mới.
- Không disable CSRF toàn cục.
- Build/tests pass.
