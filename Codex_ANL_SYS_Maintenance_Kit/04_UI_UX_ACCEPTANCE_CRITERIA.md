# UI/UX Acceptance Criteria

## Layout

- Dùng đúng layout theo role.
- Sidebar/header active đúng trang.
- Tiêu đề trang, breadcrumb và mô tả ngắn rõ ràng.
- Không nhân bản CSS toàn cục.
- Không dùng inline style tràn lan.
- Không có nội dung bị che bởi header/sidebar.

## KPI cards

Mỗi card cần:
- nhãn;
- giá trị;
- đơn vị;
- khoảng thời gian;
- trạng thái loading;
- tooltip hoặc mô tả khi số liệu có định nghĩa đặc biệt.

Không được:
- hiển thị `null`, `undefined`, `NaN`;
- hard-code;
- dùng phần trăm tăng/giảm khi không có dữ liệu so sánh.

## Chart

- Có title, axis/legend phù hợp.
- Tooltip hiển thị số và đơn vị.
- Dữ liệu chart đến từ backend/API thật.
- Không tạo chart lặp khi reload/filter.
- Destroy/reuse chart instance đúng cách.
- Có empty state.
- Không vỡ trên màn hình nhỏ.

## Table

- Header rõ.
- Format tiền tệ/ngày giờ nhất quán.
- Có empty state.
- Có pagination hoặc giới hạn hợp lý nếu dữ liệu lớn.
- Không lộ ID nội bộ không cần thiết.
- Tổng bảng khớp KPI/chart.

## Filter

- Date range có label.
- Validate from/to.
- Giữ filter sau submit/reload.
- Có nút Apply và Reset.
- Loading state khi fetch.
- Error message thân thiện.

## System settings form

- Nhóm setting theo category.
- Có help text.
- Boolean dùng checkbox/switch.
- Number có min/max.
- URL/email có validation phù hợp.
- Secret dùng password field và không trả lại plaintext.
- Nút Save có disabled/loading state.
- Thông báo thành công/thất bại rõ ràng.

## Responsive checkpoints

Kiểm tra tối thiểu:
- 1440 px
- 1024 px
- 768 px
- 390 px

Không được có:
- horizontal overflow toàn trang;
- card chồng;
- chart bị cắt;
- nút ngoài viewport;
- text quá nhỏ;
- table không có phương án scroll.
