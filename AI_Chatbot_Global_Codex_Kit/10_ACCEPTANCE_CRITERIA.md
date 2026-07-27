# Acceptance criteria

Codex chỉ được coi là hoàn thành khi tất cả mục sau đạt.

## UI

- [ ] Header hiển thị “AI Chatbot”.
- [ ] Không còn subtitle tên lesson.
- [ ] Greeting nằm trước quick actions.
- [ ] Quick actions không nằm phía trên conversation.
- [ ] Quick actions chỉ xuất hiện một lần mỗi conversation.
- [ ] Close/reopen không làm quick actions quay lại.
- [ ] Placeholder không còn “this lesson”.
- [ ] Không còn chuỗi hiển thị “AI Tutor” trong UI.
- [ ] Icon xuất hiện ở tất cả nhóm route.
- [ ] Mỗi trang chỉ có đúng một widget.
- [ ] Mobile không tràn/lộn xộn.

## Scope

- [ ] Chat hoạt động khi không có lessonId.
- [ ] Có thể giải thích navigation và feature toàn website.
- [ ] Trên lesson page vẫn dùng lesson context.
- [ ] Context theo page được cập nhật khi route đổi.
- [ ] Anonymous chỉ dùng public context.
- [ ] Backend tự xác định user/role.
- [ ] Không lộ dữ liệu ngoài quyền.
- [ ] Chatbot read-only.

## Kỹ thuật

- [ ] Không hard-code secret.
- [ ] Không disable CSRF.
- [ ] Không `permitAll` toàn bộ API.
- [ ] Không query/dump toàn database vào prompt.
- [ ] Giữ timeout/rate limit/error handling.
- [ ] Build pass.
- [ ] Test liên quan pass.
- [ ] Không sửa module ngoài phạm vi.
