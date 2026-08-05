# Codex Review Checklist

## Data

- [ ] Đã xác định nguồn duration thật.
- [ ] Không tạo cột mới khi đã có duration.
- [ ] Đơn vị duration thống nhất là giây.
- [ ] Không ghi duration giả cho dữ liệu cũ.

## Calculation

- [ ] Chỉ lesson video được cộng.
- [ ] Đúng course.
- [ ] Bỏ null/0/âm.
- [ ] Bỏ inactive/deleted/archived.
- [ ] Không double count do JOIN.
- [ ] Không N+1.

## API/View model

- [ ] Có `estimatedDurationSeconds`.
- [ ] Có display text hoặc formatter dùng chung.
- [ ] Course list và detail dùng cùng quy tắc.

## UI

- [ ] Course card hiển thị.
- [ ] Course detail hiển thị.
- [ ] Student course card hiển thị khi phù hợp.
- [ ] Dùng icon/design system hiện có.
- [ ] Responsive.

## Safety

- [ ] Không sửa quiz/payment/auth/AI/certificate/blog.
- [ ] Không refactor diện rộng.
- [ ] Build thành công.
- [ ] Test mới và test liên quan đều pass.
