# Frontend implementation

## Cấu trúc UI bắt buộc

```text
AI Chatbot header
Close button

Conversation area
  Assistant label
  Opening assistant message
  Quick actions – chỉ trong trạng thái pristine
  User/assistant messages tiếp theo

Composer
  Textarea: Ask about this website
  Send button
```

Không render lesson title trong header.

## State model

Các state tối thiểu:

```text
closed
open.pristine
open.active
open.sending
open.error
```

- `open.pristine`: đã có greeting, chưa có user interaction, hiện quick actions.
- `open.active`: đã click quick action hoặc gửi message, không hiện quick actions.
- `open.sending`: giữ quick actions ẩn.
- `open.error`: không làm quick actions hiện lại.

## Conversation persistence

Ưu tiên dùng `conversationId` từ backend.

Fallback:

```text
sessionStorage key:
ai-chatbot:<authenticatedUserId-or-anonymous-session>:<conversationId>:quick-actions-consumed
```

Không dùng một key global làm mất quick actions cho mọi user trên cùng browser.

Nếu ứng dụng chưa có New chat:

- Khởi tạo một conversationId trong session.
- Close/reopen giữ nguyên conversationId.
- Refresh trong cùng session có thể giữ trạng thái.
- Tab/session mới được coi là conversation mới.

## Điều hướng SPA

Khi route thay đổi:

- Widget không remount thành instance thứ hai.
- Giữ conversation.
- Cập nhật page context mới.
- Không tự reset quick actions.
- Không tự gửi toàn bộ nội dung trang lên backend.

## Global mount

Kiểm tra đủ các shell:

| Khu vực | Widget |
|---|---|
| Public/home/course catalog/blog | Có |
| Login/register/OAuth result | Có |
| Student | Có |
| Teacher | Có |
| Admin | Có |
| Lesson/quiz/coding | Có |
| Error pages hợp lý | Có, nếu dùng shared layout |

Tránh mount trực tiếp tại từng page. Mount một lần ở root layout.

## Responsive

- Desktop: drawer cố định cạnh phải.
- Mobile: width tối đa viewport, không tràn ngang.
- Composer không bị keyboard che trên mobile.
- Quick actions có thể wrap thành một cột.
- Icon không che nút quan trọng.
- Dùng safe-area inset khi có.
- Z-index cao hơn page content nhưng có kiểm soát.
