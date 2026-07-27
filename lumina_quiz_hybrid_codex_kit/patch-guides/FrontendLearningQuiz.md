# Patch Guide — Learning Quiz Frontend

## Preserve route

Không redirect trang learning sang một quiz page khác nếu không cần.

## Suggested state

```javascript
const quizState = {
  attemptId: null,
  questions: [],
  answers: {},
  submitted: false,
  saving: false,
  submitting: false,
  dirty: false
};
```

## Rules

- Không `sort(() => Math.random() - 0.5)`.
- Không fetch general bank.
- Render backend order.
- Save assigned IDs only.
- Flush debounce before submit.
- Disable submit while saving/submitting.
- Refresh calls start endpoint and resumes draft.
- Use existing CSRF helper.
