# Ma trận mount toàn website

Codex phải lập danh sách route/layout thật trong repository và đánh dấu.

| Nhóm route | Shared layout được dùng | Widget mount một lần | Anonymous/Auth |
|---|---|---:|---|
| `/`, catalog, public course, blog | điền sau khảo sát | Có | Anonymous |
| `/login`, `/register`, OAuth pages | điền sau khảo sát | Có | Anonymous |
| `/student/**` | điền sau khảo sát | Có | Auth |
| `/teacher/**` | điền sau khảo sát | Có | Auth |
| `/admin/**` | điền sau khảo sát | Có | Auth |
| lesson/quiz/coding routes | điền sau khảo sát | Có | Auth |
| error/403/404 | điền sau khảo sát | Có nếu layout hỗ trợ | Mixed |

## Kiểm tra duplicate

Trong browser console hoặc test:

```js
document.querySelectorAll('[data-ai-chatbot-root]').length === 1
```

Nếu framework không dùng attribute này, thêm một root marker tương đương.

## Icon

- Accessible name: `Open AI Chatbot`.
- Khi drawer mở: `aria-expanded="true"`.
- Nút close: `Close AI Chatbot`.
- Không dùng label “AI Tutor” còn sót.
