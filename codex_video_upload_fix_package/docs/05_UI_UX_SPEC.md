# 05 — UI/UX Spec

## Mục tiêu
Giữ visual language hiện tại của E-Learning nhưng sửa form để dễ hiểu và không bị trạng thái “button chết”.

### Component layout
```text
Add Video
[ Upload from computer ] [ Video URL ]

If Upload:
  Select video file
  [ Choose file ]
  filename.mp4 · 42.8 MB
  Duration: 10:15
  [ Add Video ] [ Cancel ]

If URL:
  Video URL *
  [ https://... ]
  Supports YouTube and direct public video URLs.
  Status: Ready · 10:15
  [ Add Video ] [ Cancel ]
```

## Button state
`Add Video`:
- Enabled khi input hợp lệ và không đang submit.
- Disabled khi:
  - chưa có input;
  - file/url invalid;
  - đang validating bắt buộc;
  - đang submitting.
- Không được disabled vĩnh viễn chỉ vì metadata provider tạm lỗi mà không có message.
- Mọi async path phải có recovery.

## Feedback
Trạng thái cụ thể:
- `Checking video…`
- `Video ready · 10:15`
- `This YouTube URL is not valid.`
- `The remote URL is not a supported public video.`
- `The selected file is too large.`
- `We could not read the video duration. Choose another video or URL.`

Không dùng message mơ hồ như chỉ `Invalid`.

## Upload progress
Nếu project hiện tại có XHR/progress component, dùng lại.
Nếu chưa có:
- ít nhất có spinner + disable double submit;
- không cần thêm thư viện lớn chỉ để có progress bar.

## Visual regression
Screenshot hiện tại cho thấy button Add Video có trạng thái disabled/low-contrast. Sau sửa:
- normal/enabled phải rõ clickability;
- disabled vẫn readable;
- focus ring phải có;
- dark mode contrast đạt mức chấp nhận được.

## Existing lesson UI
Sau add:
- Thumbnail/provider badge là optional.
- Hiển thị duration hiện tại.
- Nút Edit/Delete hiện có phải hoạt động.
- Không thêm UI thừa nếu không cần.
