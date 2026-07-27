# Patch Guide — Teacher Question Bank

## Lifecycle

```text
MANUAL or AI -> DRAFT -> APPROVED
                    \-> REJECTED
APPROVED -> ARCHIVED
```

## Editing policy

Nếu question đã được assigned:

- không để attempt cũ thay đổi;
- không hard-delete;
- dùng snapshot và/hoặc tạo version mới.

## Publish readiness

Trước publish:

1. blueprint tồn tại;
2. count dương;
3. total > 0;
4. mọi bucket có đủ APPROVED + active questions;
5. points/options/correct answer hợp lệ.

Teacher phải thấy đúng bucket thiếu.

## AI generation action

Input nên gồm:

- quizId
- desired count
- topic targets
- difficulty distribution
- lesson content
- learning objectives

Output luôn là DRAFT.
