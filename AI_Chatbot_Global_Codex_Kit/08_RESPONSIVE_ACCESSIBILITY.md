# Responsive và accessibility

## Keyboard

- Tab vào icon.
- Enter/Space mở drawer.
- Focus chuyển vào heading hoặc composer.
- Escape đóng drawer.
- Sau khi đóng, focus trả về icon.
- Tab order hợp lý.
- Không trap focus vĩnh viễn nếu drawer không phải modal thật.

## Screen reader

- Heading là “AI Chatbot”.
- Message list có semantic phù hợp hoặc `aria-live="polite"`.
- Loading được thông báo nhưng không lặp.
- Send button có accessible label.
- Quick action là button thật.

## Mobile

- Không dùng chiều rộng cố định lớn hơn viewport.
- Input và send button không tràn.
- Quick actions stack/wrap.
- Message dài wrap.
- Drawer scroll độc lập; composer vẫn truy cập được.
- Không che navigation hoặc nút submit quan trọng.

## Empty/error/loading

- Greeting không bị render trùng.
- Loading chỉ hiển thị khi có request.
- Error không thay thế toàn bộ lịch sử.
- Retry không reset conversation.
