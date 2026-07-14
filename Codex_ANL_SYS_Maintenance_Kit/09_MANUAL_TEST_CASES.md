# Manual Test Cases

## ANL-01

### Case A — Không có doanh thu
- Login Teacher không có course bán được.
- Mở trang.
- Kỳ vọng: 0, empty chart/table, không 500.

### Case B — Chỉ tính giao dịch thành công
- Tạo 1 success, 1 pending, 1 failed.
- Mở cùng khoảng ngày.
- Kỳ vọng: chỉ success.

### Case C — Cô lập teacher
- Teacher A và B có course riêng.
- Mỗi course có payment.
- Login A.
- Kỳ vọng: không thấy B.

## ANL-02

### Case A — Dashboard dữ liệu rỗng
- DB test gần rỗng.
- Kỳ vọng: tất cả widget render 0.

### Case B — Điều hướng
- Click từng card/link.
- Kỳ vọng: route tồn tại, đúng quyền.

## ANL-03

### Case A — New student
- Tạo student trước, trong, sau khoảng filter.
- Kỳ vọng: chỉ bản ghi trong khoảng.

### Case B — Active distinct
- Một student có nhiều activity.
- Kỳ vọng: count 1.

## ANL-04

### Case A — Ngày cuối tháng
- Payment ở 23:59 cuối tháng và 00:00 đầu tháng.
- Kỳ vọng: nhóm đúng timezone.

### Case B — Khoảng có gap
- Có doanh thu ngày 1 và 3, không có ngày 2.
- Kỳ vọng: chart xử lý ngày 2 rõ ràng.

## SYS-01

### Case A — Validation
- Nhập number ngoài min/max, URL sai, field bắt buộc rỗng.
- Kỳ vọng: không lưu và có message.

### Case B — Security
- Student/Teacher gọi update endpoint.
- Kỳ vọng: 403/redirect.

### Case C — Secret
- Mở page và kiểm tra HTML/network.
- Kỳ vọng: không có secret plaintext.
