# Test checklist

## Functional

- [ ] Mở trang Feedback không thấy Category.
- [ ] Chọn 1–5 sao cho từng tiêu chí.
- [ ] Có thể thay đổi lựa chọn trước khi submit.
- [ ] Submit thành công.
- [ ] Refresh trang không phát sinh lỗi.
- [ ] Validation chỉ yêu cầu những field thực sự còn hiển thị.
- [ ] Không có lỗi console/network liên quan category.

## Responsive

Kiểm tra tối thiểu tại:

- [ ] 320×568
- [ ] 375×812
- [ ] 768×1024
- [ ] 1024×768
- [ ] 1440×900

Tại mỗi kích thước:

- [ ] Không horizontal scroll.
- [ ] Không text overlap.
- [ ] Sao không tràn card.
- [ ] Badge 1–5 không che tiêu đề.
- [ ] Card căn giữa và không có khoảng trắng bất thường.

## Accessibility

- [ ] Tab tới từng nhóm rating.
- [ ] Chọn được bằng bàn phím.
- [ ] Focus ring nhìn thấy rõ.
- [ ] Screen reader đọc được tên tiêu chí và số sao.
- [ ] Trạng thái đã chọn không chỉ được biểu thị bằng màu.

## Regression

- [ ] Trang xem danh sách feedback vẫn hoạt động.
- [ ] Trang admin/teacher xem chi tiết feedback không lỗi với record mới.
- [ ] Feedback cũ có category vẫn đọc được.
- [ ] Build frontend pass.
- [ ] Test backend liên quan pass.
- [ ] Lint/type-check pass.
