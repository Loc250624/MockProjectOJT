# Repository discovery

Codex phải chạy bước khám phá tương đương trước khi sửa.

## Tìm frontend hiện tại

```bash
rg -n -i "AI Tutor|AI TUTOR|ai-tutor|Ask about this lesson|I am ready to help with this lesson|Summarize|Explain simply|Give an example|Quiz me" .
```

### Nếu là Thymeleaf

Tìm:

```bash
find src/main/resources/templates -type f
find src/main/resources/static -type f
rg -n "th:replace|th:insert|layout|fragment" src/main/resources/templates
```

Ưu tiên mount widget tại base layout hoặc fragment chung. Kiểm tra riêng layout public, auth, student, teacher và admin.

### Nếu là React

Tìm:

```bash
find . -maxdepth 4 -type f \( -name "App.*" -o -name "*Layout*" -o -name "*Shell*" -o -name "*Router*" \)
rg -n "createBrowserRouter|Routes|Route|Outlet|Layout|Provider" src
```

Ưu tiên mount một `AiChatbotProvider` và một `AiChatbotWidget` tại root shell nằm ngoài route content.

## Tìm backend

```bash
rg -n "class .*Ai.*Tutor|AiTutor|ai-tutor|lessonId|OpenAI|Responses API|rate.?limit" src/main/java src/main/resources
rg -n "SecurityFilterChain|authorizeHttpRequests|csrf|CorsConfiguration|oauth2Login" src/main/java
```

## Kiểm tra request hiện tại

Xác định:

- `lessonId` có bắt buộc bằng validation annotation không.
- Context lesson lấy bằng repository/service nào.
- Quyền Student/Teacher/Admin được kiểm tra ở đâu.
- Lịch sử chat lưu ở client, server session hay database.
- API có yêu cầu CSRF token không.
- Anonymous hiện bị 401/403 hay được xử lý.

## Quy tắc chọn file sửa

- Sửa component/widget hiện có.
- Sửa shared layout hiện có.
- Mở rộng DTO/service/context resolver hiện có.
- Không tạo duplicate controller/service nếu có thể tái sử dụng.
- Không rename package hàng loạt.
