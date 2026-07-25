# LumiNa Responsive Fix Kit for Codex

Gói này cung cấp ảnh tham chiếu, yêu cầu kỹ thuật, tiêu chí nghiệm thu và bộ kiểm tra để Codex sửa responsive cho dự án LumiNa.

## Cách dùng

1. Giải nén thư mục này vào thư mục gốc của repository, hoặc mở đồng thời repository và thư mục kit trong Codex.
2. Mở `CODEX_PROMPT.md`, sao chép toàn bộ nội dung và giao cho Codex.
3. Cho Codex xem thư mục `references/screenshots/` trước khi sửa.
4. Yêu cầu Codex chạy kiểm tra tĩnh bằng:

```bash
node scripts/scan-responsive-antipatterns.mjs .
```

5. Sau khi Codex xác định stack và route thật, cấu hình/chạy Playwright theo `tests/README.md`.
6. Chỉ chấp nhận thay đổi khi đáp ứng `docs/ACCEPTANCE_CRITERIA.md`.

## Lưu ý quan trọng

- Repository nguồn không được đính kèm trong yêu cầu ban đầu, vì vậy kit này không giả định tên component hoặc đường dẫn file cụ thể.
- Codex phải tự dò đúng layout/component dùng chung rồi sửa tại nguồn, không chèn CSS ngẫu nhiên vào mọi trang.
- Không dùng `overflow-x: hidden` trên `html`, `body` như một cách che lỗi. Phải sửa phần tử gây tràn.
- Không được ẩn cột hoặc nội dung quan trọng trên mobile. Bảng phải chuyển thành card/list hoặc có vùng cuộn ngang có chủ đích.
- Không sửa backend, nghiệp vụ, API, phân quyền hoặc dữ liệu nếu không liên quan trực tiếp đến responsive.

## Cấu trúc

- `CODEX_PROMPT.md`: prompt chính cho Codex.
- `docs/`: ma trận lỗi, kế hoạch, tiêu chí nghiệm thu và checklist.
- `references/screenshots/`: toàn bộ ảnh lỗi đã được đổi tên rõ nghĩa.
- `tests/`: Playwright template kiểm tra overflow và ảnh chụp viewport.
- `scripts/`: công cụ đọc-only tìm anti-pattern responsive.
- `snippets/`: CSS/React patterns tham khảo, không phải patch bắt buộc.
