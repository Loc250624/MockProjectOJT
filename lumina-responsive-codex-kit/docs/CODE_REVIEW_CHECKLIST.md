# Code Review Checklist

- [ ] Patch tập trung vào frontend responsive, không thay đổi nghiệp vụ.
- [ ] Shared component được sửa trước khi tạo page override.
- [ ] Không thêm global `overflow-x:hidden` để che lỗi.
- [ ] Không thêm hàng loạt `!important`.
- [ ] Không hard-code theo đúng một thiết bị/screenshot.
- [ ] Không chỉ giảm font-size cho table.
- [ ] Có xử lý content dài và localization.
- [ ] Có mobile/tablet/desktop behavior rõ ràng.
- [ ] Có kiểm tra authenticated routes cho Student/Teacher/Admin.
- [ ] Có lint/typecheck/build/test output.
- [ ] Có screenshot hoặc visual diff sau sửa.
- [ ] Không để console error/hydration warning mới.
