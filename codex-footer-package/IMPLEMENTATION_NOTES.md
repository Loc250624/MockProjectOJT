# Implementation notes

## Cách xử lý dữ liệu chưa có

Không nên dùng chuỗi rỗng vì sẽ làm bố cục khó hiểu. Dùng label rõ ràng:

```text
Hotline: Đang cập nhật
Email: Đang cập nhật
Địa chỉ: Đang cập nhật
Mã số doanh nghiệp: Đang cập nhật
```

Các giá trị này là plaintext. Khi có dữ liệu thật, chỉ cần cập nhật config mà không phải sửa layout.

## Hàm kiểm tra link

Một footer item nên có dạng:

```ts
type FooterItem = {
  label: string;
  href?: string;
};
```

- Có `href`: render link.
- Không có `href`: render `span` với trạng thái muted.
- Không bao giờ tự điền `#`.

## Router

Codex phải dùng router component đang có:

- React Router: `Link`
- Next.js: `next/link`
- Vue Router: `RouterLink`
- Angular: `routerLink`
- Server-side template: URL helper hiện có

Component mẫu trong gói dùng thẻ `a` để giữ tính độc lập. Khi áp dụng vào dự án, cần thay bằng router component tương ứng.

## Icon mạng xã hội

- Ưu tiên icon package đã được cài sẵn.
- SVG trong gói có `fill="currentColor"` nên tự nhận màu từ CSS.
- Icon không có URL phải render bằng `span`, không phải `button`, vì không có hành động.
- Icon có URL phải có tên truy cập được và focus ring.

## Design tokens

Nên map footer vào token hiện có, ví dụ:

```css
background: var(--surface-inverse);
color: var(--text-on-inverse);
border-color: var(--border-subtle);
```

Chỉ dùng fallback hard-coded nếu dự án chưa có token.

## Tránh phạm vi ngoài yêu cầu

Không:

- đổi logo toàn website;
- thêm API footer;
- tạo bảng database;
- thêm CMS;
- sửa route không liên quan;
- thay đổi global typography;
- sao chép nội dung pháp lý của website khác.
