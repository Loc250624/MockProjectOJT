# Guardrails và rollback

## Guardrails

- Tạo commit/branch riêng trước khi sửa.
- Giữ diff nhỏ.
- Không format toàn repository.
- Không update dependency nếu không bắt buộc.
- Không đổi tên config environment hiện có.
- Không thay đổi schema database trừ khi thật sự cần để lưu conversation.
- Không xóa endpoint cũ trước khi xác nhận mọi caller đã chuyển.
- Không sửa behavior grading/progress/completion.

## Rollback points

Tách thay đổi thành các commit logic:

1. Rename + UI ordering.
2. One-time quick action state.
3. Global mount.
4. Site-wide request/context.
5. Security/tests.

Nếu lỗi, có thể revert từng commit mà không ảnh hưởng module khác.

## Feature flag

Nếu dự án đã có feature flag, dùng flag hiện có. Không thêm framework flag mới chỉ cho task này.

Có thể giữ fallback:

```text
AI_CHATBOT_GLOBAL_ENABLED=false
```

chỉ khi kiến trúc hiện tại đã hỗ trợ env-backed flags. Không bắt buộc tạo mới.
