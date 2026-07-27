# Quick actions – hành vi chính xác

## Điều kiện hiển thị

```text
showQuickActions =
  panelOpen
  AND greetingExists
  AND conversationMessageCount == 1
  AND firstMessageIsAssistantGreeting
  AND quickActionsConsumed == false
  AND noUserMessageExists
```

## Sự kiện consume

Đặt `quickActionsConsumed = true` ngay khi:

- Người dùng click bất kỳ quick action nào.
- Người dùng gửi text đầu tiên.
- Người dùng gửi bằng Enter.
- Người dùng submit bằng accessibility/keyboard action.

Không chờ API thành công mới consume. Nếu API lỗi, quick actions vẫn không xuất hiện lại.

## Close/reopen

- Close panel: giữ state.
- Reopen panel: không hiện lại quick actions nếu đã consume.
- Route change: giữ state.
- AI error: giữ state.
- Refresh: giữ trong cùng session nếu dùng sessionStorage.
- New conversation: reset đúng một lần.

## Thứ tự render

1. Header.
2. Assistant greeting.
3. Quick actions.
4. Các message tiếp theo.
5. Composer.

Không render quick actions ở vùng header như thiết kế hiện tại.
