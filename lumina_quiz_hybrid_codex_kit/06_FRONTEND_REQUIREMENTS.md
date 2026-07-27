# Frontend Requirements

## Student learning page

Codex phải tìm đúng JavaScript/template đang được `student/learning` sử dụng và chỉ thay đổi quiz module.

### State bắt buộc

- loading
- unavailable
- insufficient bank
- active draft
- saving
- save failed
- submitting
- submitted/result

Không render Loading, Unavailable và Submitted cùng lúc.

### Start/resume

- Fetch quiz endpoint khi mở lesson quiz.
- Lưu `attemptId`.
- Render `questions` theo đúng thứ tự backend.
- Không shuffle client-side.
- Refresh nhận cùng attempt và cùng ordered IDs.

### Autosave

- Debounce.
- Dùng CSRF-aware helper hiện có.
- Không để nhiều save request ghi đè sai thứ tự.
- Có indicator saved/unsaved/error.
- Save failure không được giả vờ thành công.

### Submit

- Flush/cancel debounce trước submit.
- Disable button khi request đang chạy.
- Chống double-click.
- Lock input sau success.
- Không reveal correct answer trước khi submitted.

### Student error text

Khi bank thiếu:

> This quiz is temporarily unavailable because its approved question bank does not satisfy the configured blueprint. Please contact your teacher.

Không lộ đáp án hoặc thông tin nội bộ không cần thiết.

## Teacher UI tối thiểu

- status badge
- difficulty badge
- topic
- source
- version
- filters
- approve/reject/archive
- bulk approve
- blueprint table
- readiness status
- required vs available per bucket
- Generate Questions action
- DRAFT warning

Không redesign sidebar/global navigation.
