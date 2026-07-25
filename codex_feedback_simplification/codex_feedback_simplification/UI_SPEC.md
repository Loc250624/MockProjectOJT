# UI specification sau khi bỏ Category

## Cấu trúc đề xuất

```text
Feedback page
└── Feedback card (centered)
    ├── Header
    │   ├── RATING MATRIX
    │   ├── Rate your experience
    │   └── 1–5 badge
    ├── Rating grid
    │   ├── Course Content
    │   ├── Instructor Support
    │   ├── Learning Experience
    │   ├── Platform Usability
    │   ├── Assessment Experience
    │   └── Overall Satisfaction (full width)
    ├── Existing optional comment field, nếu trang hiện có
    └── Existing submit action
```

## Desktop

- Card rộng tối đa 880–960px, `width: 100%`.
- Rating grid 2 cột để thao tác nhanh và giảm chiều dài trang.
- Khoảng cách giữa hai cột 20–28px.
- `Overall Satisfaction` chiếm 2 cột.

## Tablet/mobile

- Dưới khoảng 768px: rating grid về 1 cột.
- Padding card 16–20px.
- Header có thể xuống dòng nhưng badge vẫn rõ.
- Mỗi ngôi sao có vùng bấm tối thiểu 40×40px, khuyến nghị 44×44px.
- Không khóa chiều cao card.

## Visual

- Nền card trắng, border nhẹ, radius tương thích hệ thống.
- Tiêu đề đậm, eyebrow màu primary.
- Sao chưa chọn dùng màu neutral; sao đã chọn/hover dùng primary hoặc accent hiện có.
- Focus-visible rõ ràng.
- Divider chỉ dùng khi cần; tránh quá nhiều đường ngang.

## Accessibility

- Mỗi nhóm rating phải có tên tiêu chí được liên kết bằng `fieldset/legend`, `aria-label` hoặc cấu trúc tương đương.
- Mỗi sao có tên “N out of 5 for [criterion]”.
- Trạng thái đã chọn được thể hiện bằng `aria-pressed`, radio semantics hoặc input radio thật.
- Không chỉ dựa vào màu để biểu thị lựa chọn.
- Keyboard có thể tab đến và chọn rating.
