# Implementation Guide for Codex

## 1. Khảo sát trước khi sửa

Tạo inventory ngắn:

- User entity + account status fields.
- Auth provider field (`LOCAL`, `GOOGLE`, `GITHUB`, ...).
- Admin user controller/service/repository.
- Password reset service/token entity nếu đã có.
- Category entity + Course relation + category controller/service/repository.
- Transaction/payment entity, amount type, status enum, VNPay integration.
- Admin JS/TS và HTML/Thymeleaf/template cho 3 route.
- Shared admin modal/toast utility.
- Spring Security / CSRF strategy.

Sau đó mới sửa.

## 2. Backend patterns nên giữ

Dự án có dấu hiệu là Spring Boot + server-rendered frontend/static JS. Hãy follow structure hiện có:

- Controller chỉ parse request/return response.
- Business rules trong Service.
- Repository chỉ data access.
- Dùng `@Transactional` cho multi-step category reassign-delete và các action cần atomicity.
- Dùng DTO thay vì bind thẳng entity nếu codebase đã theo pattern DTO.
- Không trust `userId/categoryId/role` từ frontend.
- Return status code đúng:
  - 200/204 success
  - 400 malformed/validation
  - 403 forbidden
  - 404 not found
  - 409 business conflict/dependency/last-admin protection

## 3. Suggested API behavior (adapt, do not blindly duplicate routes)

Nếu project đã có endpoint, sửa endpoint cũ. Chỉ thêm endpoint nếu thật sự thiếu.

### Users
- `POST /admin/users/{id}/password-reset-link`
- `PATCH /admin/users/{id}/role`
- `PATCH /admin/users/{id}/block`
- `DELETE /admin/users/{id}` → soft-delete semantics

Possible request:
```json
{ "role": "TEACHER" }
```

Possible block request:
```json
{ "blocked": true }
```

### Categories
- `PATCH /admin/categories/{id}`
- `DELETE /admin/categories/{id}`
- optional safe flow:
  - `POST /admin/categories/{id}/reassign-and-delete`
  - body `{ "replacementCategoryId": 7 }`

Hoặc gộp replacement vào DELETE request nếu architecture hiện có hỗ trợ body/query parameter sạch.

## 4. Frontend interaction

- User profile modal lấy `data-user-id`/actual id từ row/user detail.
- Không parse email/name để làm primary key.
- Các button action bind event một lần, tránh duplicate listeners khi modal mở lại.
- Sau success:
  - update row status/role;
  - update modal state;
  - toast success.
- Sau failure:
  - keep modal;
  - show specific error;
  - re-enable button.

Category:
- Edit button phải mang đúng category id.
- Form edit phải prefill đúng name/description.
- Delete phải gọi đúng id, không dùng index row.
- Nếu dependency conflict, hiển thị modal riêng thay vì alert strip cố định mãi trên page.

Transactions:
- Centralize một `formatVnd(amount)` hoặc server-side equivalent.
- Search toàn feature `/admin/transactions` để loại bỏ các literal `USD`.
- Không global replace `USD` toàn repo nếu project có khu vực thật sự dùng USD.

## 5. Security requirements

- Tất cả write endpoints: `ROLE_ADMIN`.
- CSRF token phải được gửi theo convention hiện có:
  - form `_csrf`, hoặc
  - `X-CSRF-TOKEN` / `X-XSRF-TOKEN`.
- Không disable CSRF globally để “fix” buttons.
- Reset token dùng secure random generator và expiry; nếu token được persist, ưu tiên hash token nếu pattern hiện có cho phép.
- Không log reset token/password/payment secrets ở production logs.
- Change-role/block/delete phải audit nếu project đã có audit logging.

## 6. Database migration rule

Chỉ migration nếu field thật sự thiếu.

Nếu cần soft delete, ưu tiên dạng:
- `deleted_at TIMESTAMP NULL`
hoặc field status hiện có.

Không tự tạo cả một permission system mới.

## 7. Scope guard

Không sửa:
- public homepage
- student dashboard
- teacher dashboard
- quiz
- lesson
- AI chatbot
- course progress
- certificate
- blog UI ngoài category relation cần thiết
- unrelated payment success/failure logic nếu currency display có thể sửa độc lập

Chỉ sửa payment gateway amount conversion nếu khảo sát cho thấy amount đang bị scale sai thực sự, không chỉ sai label.
