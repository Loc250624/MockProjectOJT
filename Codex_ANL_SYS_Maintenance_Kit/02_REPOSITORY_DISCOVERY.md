# Repository Discovery Checklist

## 1. Git và build

Chạy và ghi nhận:

```bash
git status
git branch --show-current
git log -5 --oneline
```

Windows:

```powershell
.\mvnw.cmd -q -DskipTests compile
.\mvnw.cmd test
```

Unix:

```bash
./mvnw -q -DskipTests compile
./mvnw test
```

Nếu không có Maven Wrapper, dùng `mvn`.

## 2. Tìm file liên quan

Từ khóa:
- analytics
- dashboard
- revenue
- income
- earning
- payment
- order
- transaction
- enrollment
- student activity
- progress
- login
- system setting
- configuration
- property
- admin setting

Kiểm tra tối thiểu:
- `src/main/java/**/controller`
- `src/main/java/**/service`
- `src/main/java/**/repository`
- `src/main/java/**/entity`
- `src/main/java/**/dto`
- `src/main/resources/templates`
- `src/main/resources/static/css`
- `src/main/resources/static/js`
- `src/main/resources/db`
- `src/test`

## 3. Route inventory

Tạo bảng:

| Feature | URL | Method | Role | Controller | Template/DTO | Status |
|---|---|---|---|---|---|---|

Kiểm tra cả:
- navigation link;
- form action;
- fetch/AJAX URL;
- redirect;
- API base path;
- CSRF.

## 4. Data model map

Vẽ hoặc mô tả:
- User/Role
- Teacher ownership
- Course
- Enrollment
- Order
- OrderItem
- Payment/Transaction
- Progress/Activity
- SystemSetting

Ghi rõ khóa ngoại và trường trạng thái.

## 5. Revenue source of truth

Trả lời:
- Doanh thu lấy từ Order hay Payment?
- Trạng thái nào là thành công?
- Có refund không?
- Có discount/tax/platform fee không?
- Teacher revenue là gross hay net?
- Thời điểm tính là createdAt, paidAt hay completedAt?
- Có currency không?

## 6. Student metrics source of truth

Trả lời:
- New student dựa trên `createdAt`, role assignment hay enrollment?
- Active student dựa trên last login, progress, enrollment hoặc activity event?
- Có trường `lastLoginAt` không?
- Có audit/event log không?
- Có lịch sử progress timestamp không?

## 7. System settings source of truth

Trả lời:
- settings nằm trong DB, properties, YAML hay environment?
- key/value type là gì?
- có validation không?
- có cache không?
- thay đổi có hiệu lực ngay hay cần restart?
- key nào là secret và không được render?

## 8. UI inventory

Ghi:
- layout đang dùng;
- breadcrumb;
- sidebar/menu;
- card/chart/table component;
- thư viện chart;
- CSS file;
- JS file;
- breakpoint;
- empty/loading/error state.
