# Acceptance checklist

## Chức năng

- [ ] Footer được render trên đúng các layout/trang hiện có.
- [ ] Không xuất hiện footer trùng lặp.
- [ ] Link có thật điều hướng đúng.
- [ ] Mục chưa có link được hiển thị plaintext.
- [ ] Không có `href="#"` hoặc URL giả.
- [ ] Social icon có aria-label/tooltip phù hợp.
- [ ] External link có `noopener noreferrer`.

## Giao diện

- [ ] Màu sắc và typography follow theme của dự án.
- [ ] Desktop chia cột cân đối.
- [ ] Tablet không bị dồn chữ.
- [ ] Mobile không tràn ngang.
- [ ] Text dài tự wrap.
- [ ] Icon không bị méo hoặc lệch baseline.
- [ ] Divider/copyright không quá nổi bật.
- [ ] Light/dark mode đúng nếu được hỗ trợ.

## Chất lượng code

- [ ] Không thêm dependency không cần thiết.
- [ ] Dữ liệu footer được gom vào config hoặc cấu trúc dễ cập nhật.
- [ ] Không duplicate logic router/link.
- [ ] Không sửa file ngoài phạm vi footer nếu không bắt buộc.
- [ ] Formatter/linter pass.
- [ ] Build pass.
- [ ] Không có lỗi console.
- [ ] Không có warning accessibility cơ bản.

## Dữ liệu

- [ ] Không sao chép dữ liệu doanh nghiệp của F8.
- [ ] Không bịa hotline/email/địa chỉ/mã số doanh nghiệp.
- [ ] Placeholder hiển thị rõ là “Đang cập nhật”.
