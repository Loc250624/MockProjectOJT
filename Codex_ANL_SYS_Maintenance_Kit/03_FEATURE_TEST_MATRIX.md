# Feature Test Matrix

## ANL-01 — Teacher Personal Revenue

| ID | Test | Expected |
|---|---|---|
| ANL01-01 | Teacher mở trang doanh thu | HTTP 200, đúng layout Teacher |
| ANL01-02 | Student truy cập URL | Bị chặn 403 hoặc redirect đúng |
| ANL01-03 | Admin truy cập URL Teacher | Theo policy hiện có; không giả mạo teacher |
| ANL01-04 | Teacher có 0 giao dịch | KPI = 0, chart/table empty state, không lỗi |
| ANL01-05 | Có payment success | Được tính đúng |
| ANL01-06 | Có pending/failed/cancelled | Không được tính |
| ANL01-07 | Có refund | Xử lý theo nghiệp vụ, ghi rõ |
| ANL01-08 | Đơn có nhiều course/teacher | Chỉ tính phần thuộc teacher hiện tại |
| ANL01-09 | Đổi teacherId trên request | Không xem được dữ liệu người khác |
| ANL01-10 | Filter date | KPI/chart/table cùng kết quả |
| ANL01-11 | Mobile | Không tràn, chart/table dùng được |

## ANL-02 — Admin Overview Dashboard

| ID | Test | Expected |
|---|---|---|
| ANL02-01 | Admin mở dashboard | HTTP 200, đúng layout Admin |
| ANL02-02 | Non-admin truy cập | Bị chặn |
| ANL02-03 | KPI total users/courses/orders/revenue | Khớp DB và định nghĩa |
| ANL02-04 | Không có dữ liệu | Dashboard vẫn render |
| ANL02-05 | Widget lỗi riêng | Không làm sập toàn trang nếu có thể xử lý |
| ANL02-06 | Card navigation | Link tới trang đúng |
| ANL02-07 | Dữ liệu thật | Không hard-code/mock |
| ANL02-08 | Responsive | Card/chart không chồng lấn |

## ANL-03 — New & Active Students

| ID | Test | Expected |
|---|---|---|
| ANL03-01 | New student theo khoảng ngày | Đếm đúng theo định nghĩa đã ghi |
| ANL03-02 | User không có role Student | Không bị tính |
| ANL03-03 | Active student | Đếm theo nguồn dữ liệu thật |
| ANL03-04 | Một student hoạt động nhiều lần | Chỉ count distinct |
| ANL03-05 | Khoảng ngày rỗng | Trả 0 |
| ANL03-06 | Từ ngày > đến ngày | Validation, không 500 |
| ANL03-07 | Timezone boundary | Không lệch ngày |
| ANL03-08 | Chart/table | Cùng số liệu |

## ANL-04 — Revenue by Day/Month/Year

| ID | Test | Expected |
|---|---|---|
| ANL04-01 | Group by day | Nhóm đúng ngày |
| ANL04-02 | Group by month | Nhóm đúng tháng và năm |
| ANL04-03 | Group by year | Nhóm đúng năm |
| ANL04-04 | Date range | Inclusive/exclusive rõ ràng |
| ANL04-05 | Pending/failed | Không tính |
| ANL04-06 | Refund | Xử lý đúng policy |
| ANL04-07 | Gap periods | Hiển thị 0 hoặc gap hợp lý |
| ANL04-08 | KPI vs chart vs table | Tổng khớp nhau |
| ANL04-09 | Invalid granularity | Validation |
| ANL04-10 | Export nếu có | File và số liệu đúng |

## SYS-01 — General System Settings

| ID | Test | Expected |
|---|---|---|
| SYS01-01 | Admin mở settings | HTTP 200 |
| SYS01-02 | Non-admin | Bị chặn |
| SYS01-03 | Load setting | Giá trị đúng DB/config |
| SYS01-04 | Update hợp lệ | Lưu thành công |
| SYS01-05 | Update không hợp lệ | Hiển thị validation, không lưu |
| SYS01-06 | Key không cho phép | Không cập nhật |
| SYS01-07 | Secret key | Không render plaintext |
| SYS01-08 | Concurrent update | Không silent overwrite nếu có versioning |
| SYS01-09 | Audit | updatedAt/updatedBy được cập nhật nếu hỗ trợ |
| SYS01-10 | Restart/cache | Hành vi được ghi rõ |
| SYS01-11 | CSRF | POST/PUT có bảo vệ |
| SYS01-12 | Responsive | Form dễ dùng trên mobile |
