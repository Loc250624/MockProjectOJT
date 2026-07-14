# Test Execution Runbook

## A. Chuẩn bị branch

```bash
git checkout develop
git pull
git checkout -b fix/anl-sys-maintenance
git status
```

Nếu có thay đổi local, không được tự ý xóa. Ghi nhận và hỏi người quản lý repository khi cần.

## B. Chạy script hỗ trợ

Windows:

```powershell
powershell -ExecutionPolicy Bypass -File .\scripts\windows\run-verification.ps1
```

Unix:

```bash
chmod +x scripts/unix/run-verification.sh
./scripts/unix/run-verification.sh
```

## C. Chạy ứng dụng

Windows:

```powershell
.\mvnw.cmd spring-boot:run
```

Unix:

```bash
./mvnw spring-boot:run
```

Nếu port 8080 bị chiếm:

Windows:

```powershell
netstat -ano | findstr :8080
tasklist /FI "PID eq <PID>"
```

Chỉ dừng process sau khi xác định đúng.

## D. Tài khoản test

Chuẩn bị:
- 1 Admin;
- 2 Teacher;
- 3 Student;
- payment success/pending/failed;
- course thuộc nhiều teacher;
- dữ liệu ở các ngày/tháng/năm khác nhau;
- một khoảng không có dữ liệu;
- setting hợp lệ/không hợp lệ.

Không dùng tài khoản production.

## E. Test sequence

1. Anonymous access.
2. Student access.
3. Teacher A.
4. Teacher B.
5. Admin.
6. Empty data.
7. Normal data.
8. Boundary data.
9. Invalid input.
10. Responsive UI.
11. Regression.

## F. Browser checks

- Network status.
- Console error.
- Request payload.
- Response payload.
- CSRF.
- Duplicate API request.
- Chart re-render.
- Layout overflow.

## G. Server checks

- Stack trace.
- SQL exception.
- N+1 query.
- Unauthorized access.
- Null pointer.
- conversion/date error.
- slow query.

## H. Kết thúc

```bash
git status
git diff --stat
git diff
```

Tạo báo cáo và chỉ commit file thuộc phạm vi.
