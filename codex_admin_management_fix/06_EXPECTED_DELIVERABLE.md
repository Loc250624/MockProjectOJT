# Expected Deliverable from Codex

Codex phải trả về một implementation hoàn chỉnh trong repository, không chỉ đưa ra plan.

## Bắt buộc

- Source code changes.
- Tests tối thiểu cho business rules quan trọng.
- Migration nếu và chỉ nếu cần.
- Không để placeholder kiểu:
  - `TODO`
  - `Not available`
  - fake success
  - mocked frontend-only state
- Không làm button “có vẻ hoạt động” nhưng không persist DB.

## Completion report format

### Files changed
Liệt kê chính xác từng file.

### All Users
Mô tả:
- reset password behavior LOCAL/OAuth
- change role rules
- block/unblock persistence/security
- soft-delete behavior/data preservation

### Categories
Mô tả:
- edit validation
- delete dependency behavior
- reassign flow nếu implemented

### Transactions
Mô tả:
- formatter
- data unit verified
- locations changed from USD → VND

### Tests
- command
- pass/fail count

### Scope confirmation
Xác nhận không sửa module ngoài phạm vi, hoặc giải thích dependency bắt buộc nếu có.
