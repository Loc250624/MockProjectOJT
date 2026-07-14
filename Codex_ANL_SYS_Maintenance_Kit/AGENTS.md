# AGENTS.md — ANL/SYS Maintenance Rules

## Mission

Bảo trì và kiểm thử các chức năng ANL-01, ANL-02, ANL-03, ANL-04, SYS-01 trong dự án Spring Boot hiện có.

## Mandatory working mode

Luôn thực hiện theo thứ tự:

1. DISCOVER
2. REPRODUCE
3. IDENTIFY ROOT CAUSE
4. PROPOSE MINIMAL FIX
5. IMPLEMENT
6. TEST
7. REGRESSION
8. REPORT

Không được bắt đầu bằng việc viết lại trang hoặc thay đổi kiến trúc.

## Repository constraints

- Tôn trọng package, naming convention, layout, component và design system hiện có.
- Ưu tiên tái sử dụng service/repository/DTO hiện có.
- Không tạo module trùng chức năng.
- Không thay Thymeleaf bằng framework frontend khác.
- Không thêm dependency mới nếu giải pháp hiện tại đủ dùng.
- Không thay đổi database schema trừ khi có bằng chứng rõ ràng rằng schema đang thiếu.
- Nếu cần migration, tạo migration mới; không sửa migration đã phát hành.
- Không đưa secrets vào source control.

## Evidence required before change

Trước mỗi thay đổi phải ghi:
- triệu chứng;
- bước tái hiện;
- kết quả thực tế;
- kết quả mong đợi;
- nguyên nhân gốc;
- file liên quan;
- test sẽ dùng để xác nhận.

## Definition of Done

Một chức năng chỉ được coi là hoàn thành khi:
- route đúng;
- role guard đúng;
- dữ liệu đúng;
- UI đúng;
- empty/loading/error state có xử lý;
- không có lỗi JavaScript console;
- không có exception server;
- responsive;
- test pass;
- có báo cáo.

## Feature-specific rules

### ANL-01
- Teacher chỉ xem doanh thu thuộc khóa học của chính mình.
- Không tính payment thất bại, hủy, pending hoặc refunded nếu nghiệp vụ không công nhận.
- Không để teacher truy cập doanh thu teacher khác bằng cách đổi id trên URL/request.
- Tổng doanh thu, số đơn, số học viên trả phí và biểu đồ phải dùng cùng bộ lọc.

### ANL-02
- Dashboard admin phải lấy dữ liệu thật.
- Các KPI phải có cùng snapshot/filter logic.
- Link từ card phải điều hướng tới trang quản trị liên quan.
- Không để dashboard lỗi toàn trang chỉ vì một widget lỗi.

### ANL-03
- “Học viên mới” phải có định nghĩa rõ theo ngày tạo tài khoản hoặc ngày được gán role Student.
- “Học viên hoạt động” phải có định nghĩa rõ theo dữ liệu hệ thống hiện có: login, enrollment activity, lesson progress hoặc event log.
- Không tự bịa định nghĩa nếu dữ liệu không tồn tại; phải báo giới hạn.

### ANL-04
- Phân nhóm theo ngày/tháng/năm phải đúng timezone và không đếm trùng.
- Date range phải validate.
- Khoảng không có doanh thu phải hiển thị 0 hoặc điểm dữ liệu rỗng hợp lý.
- Tổng trên biểu đồ phải khớp tổng trong bảng/KPI.

### SYS-01
- Chỉ Admin được xem/sửa.
- Có validation, audit hoặc ít nhất updatedAt/updatedBy nếu kiến trúc hỗ trợ.
- Cấu hình không được làm lộ secrets.
- Key quan trọng phải có default/fallback an toàn.
- Không cho phép cập nhật key không nằm trong whitelist nếu hệ thống dùng whitelist.

## Stop conditions

Dừng thay đổi và báo cáo nếu:
- database thực tế khác entity/migration nghiêm trọng;
- cần secret bên ngoài;
- không có dữ liệu để định nghĩa “active student”;
- test yêu cầu dịch vụ thanh toán bên ngoài không khả dụng;
- thay đổi có nguy cơ mất dữ liệu.
