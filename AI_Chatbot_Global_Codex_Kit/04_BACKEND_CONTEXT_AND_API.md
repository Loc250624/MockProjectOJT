# Backend context và API

## Request contract đề xuất

```json
{
  "message": "How do I download my certificate?",
  "conversationId": "uuid-or-session-id",
  "lessonId": null,
  "pageContext": {
    "path": "/student/certificates",
    "pageKey": "student-certificates",
    "entityType": "certificate",
    "entityId": "22",
    "title": "Certificates"
  },
  "recentMessages": []
}
```

### Quy tắc

- `message`: bắt buộc.
- `conversationId`: tạo/validate phía server hoặc dùng id không dự đoán được.
- `lessonId`: optional.
- `pageContext`: metadata có allowlist.
- `recentMessages`: giới hạn số lượng và độ dài.
- Không nhận `role`, `userId`, `isAdmin` từ client làm nguồn tin.

## Response contract đề xuất

```json
{
  "conversationId": "uuid-or-session-id",
  "answer": "Open Certificates, choose a completed course, then select Download.",
  "scope": "SITE",
  "usedPageContext": true
}
```

Không trả quick actions lặp lại trong mọi response.

## Context resolver

Tách context thành các lớp:

1. `PublicSiteContext`
   - route map
   - help/FAQ
   - public course/catalog/blog metadata
2. `AuthorizedUserContext`
   - current user do Spring Security xác định
   - role/permissions
   - dữ liệu riêng tối thiểu cần cho câu hỏi
3. `PageContext`
   - path/page key/entity id đã validate
4. `LearningContext`
   - lesson/course content khi user có quyền
5. `ConversationContext`
   - lịch sử giới hạn, đã sanitize

Không lấy tất cả context ở mọi request. Chỉ resolve theo intent/page.

## Quyền truy cập

- Student: dữ liệu public và dữ liệu course/lesson đã được phép xem.
- Teacher: dữ liệu course do teacher có quyền quản lý; không đọc course private của teacher khác.
- Admin: theo quyền hiện có; không mặc định dump dữ liệu toàn hệ thống.
- Anonymous: public context.
- Entity id không hợp lệ hoặc trái quyền: bỏ context đó và trả lời an toàn; không tiết lộ entity có tồn tại hay không khi điều đó nhạy cảm.

## Tương thích API

Ưu tiên:

- Mở rộng DTO cũ.
- `lessonId` không còn `@NotNull`.
- Service cũ vẫn dùng lesson adapter khi có lessonId.
- Thêm site context adapter.
- Giữ endpoint cũ trong thời gian chuyển đổi hoặc cập nhật toàn bộ caller trong cùng patch.
- Không xóa config `app.ai-tutor.*` chỉ vì đổi tên hiển thị.
