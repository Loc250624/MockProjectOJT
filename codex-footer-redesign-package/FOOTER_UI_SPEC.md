# Footer UI specification

## 1. Visual hierarchy

Footer mới nên có ba tầng rõ ràng nhưng gọn:

1. **Main information grid** — phần lớn diện tích, 4 cột trên desktop.
2. **Subtle divider** — đường phân cách mảnh, độ tương phản thấp.
3. **Bottom meta row** — copyright và tagline.

Không cần CTA, nút lớn, card nổi hoặc khối hình minh họa.

## 2. Suggested content model

Codex phải đối chiếu với tính năng thật trong codebase trước khi giữ các mục này.

```ts
const footerSections = [
  {
    title: 'Explore',
    items: ['Course Catalog', 'Learning Paths', 'Blog', 'Certificates', 'AI Chatbot'],
  },
  {
    title: 'Learning & Teaching',
    items: ['My Courses', 'Progress Tracking', 'Quizzes', 'Teacher Workspace', 'Feedback & Support'],
  },
  {
    title: 'Policies & Help',
    items: ['Help Center', 'Privacy Policy', 'Terms of Use', 'Refund Policy', 'Accessibility'],
  },
];
```

Đây chỉ là content guide, không bắt buộc tên biến hoặc TypeScript.

## 3. Non-clickable item pattern

Ưu tiên:

```jsx
<span className="footerItem" aria-disabled="true">
  Course Catalog
</span>
```

Không dùng:

```jsx
<a href="#">Course Catalog</a>
<Link to="/courses">Course Catalog</Link>
<button onClick={() => navigate('/courses')}>Course Catalog</button>
```

## 4. Desktop density target

- Container dùng cùng max-width với Header hoặc main content.
- Bố cục gợi ý: brand column rộng 1.25–1.5 lần mỗi link column.
- Column gap vừa phải để không tạo khoảng trống lớn.
- Heading cột: 14–16 px, semibold/bold theo hệ thống typography hiện tại.
- Item: 13–15 px, line-height đủ đọc nhưng compact.
- Mỗi cột khoảng 4–6 dòng thông tin.
- Bottom row nhỏ hơn nội dung chính.

Không hard-code các con số trên nếu project đã có design tokens tương đương.

## 5. Styling direction

- Background: dùng navy/blue/gradient sẵn có trong dự án.
- Main text: white hoặc token foreground on-primary.
- Secondary text: white với opacity thấp hơn hoặc token muted foreground.
- Divider: white/blue với opacity thấp.
- Không dùng FPT red.
- Không dùng logo, QR, payment badges hoặc certification badges của bên thứ ba.

## 6. Accessibility

- Contrast text đạt mức dễ đọc.
- Không làm các static labels có focus style như link.
- Không đưa labels vào keyboard tab order.
- Logo hoặc brand image phải có alt text phù hợp.
- Không dùng heading level làm sai cấu trúc trang; footer section title có thể dùng heading nhỏ hoặc text semibold tùy layout hiện tại.
