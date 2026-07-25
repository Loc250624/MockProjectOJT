# Acceptance Criteria

## Viewport matrix bắt buộc

- 320 × 568 (iPhone SE / màn hình hẹp nhất mục tiêu)
- 360 × 800
- 375 × 667
- 390 × 844
- 412 × 915
- 428 × 926
- 768 × 1024
- 1024 × 768
- 1280 × 800 hoặc lớn hơn

## Tiêu chí toàn cục

- Không có horizontal page scroll ngoài vùng table/tab được thiết kế để cuộn.
- Không có button, menu, avatar, title hoặc action bị cắt ngoài viewport.
- Không có chữ chồng lên nhau.
- Không giảm font/table đến mức không đọc được.
- Tất cả dữ liệu quan trọng vẫn truy cập được.
- Logo LumiNa thống nhất giữa public, student, course player, teacher và admin.
- Header/footer không tạo overflow.
- Touch target chính tối thiểu xấp xỉ 44 × 44px.
- Tab order và focus ring vẫn hoạt động.
- Menu mobile đóng/mở được, Escape/overlay hoạt động nếu framework hỗ trợ.
- Desktop không bị regression rõ ràng.

## Tiêu chí theo khu vực

### Header

- Ở mobile chỉ hiển thị brand + trigger/essential actions.
- Không còn mục navigation bị cắt một phần.
- Search/Login/Register vừa 100% container và không chạm biên.

### Dashboard hero

- Email dài wrap hoặc ellipsis có accessible full value.
- Action buttons nằm dưới intro trên viewport hẹp.

### Course/Roadmap/Admin lists

- 320px vẫn đọc được title, instructor/description, price/status và action.
- Không dùng bảng bị thu nhỏ toàn bộ.
- Search/filter/create controls stack hợp lý.

### Help Guide

- Guide card, tabs và topics vừa màn hình.
- Tab active/focus không bị cắt.

### Course player

- Shared logo đúng.
- Back/My Courses/avatar không chồng hoặc tràn.
