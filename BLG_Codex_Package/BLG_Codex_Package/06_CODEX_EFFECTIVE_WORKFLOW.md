# 06 - Codex Effective Workflow

Tài liệu này hướng dẫn cách dùng Codex hiệu quả với repo Spring Boot + HTML/CSS/JS.

## Nguyên tắc chính

Codex hoạt động tốt nhất khi bạn giao việc theo module nhỏ, có context rõ, có acceptance criteria và có quyền kiểm tra bằng test/command. Không nên đưa một prompt quá chung kiểu “làm chức năng blog” rồi để Codex tự đoán toàn bộ.

## Workflow nên dùng

### Bước 1 - Tạo branch riêng

```bash
git checkout develop
git pull origin develop
git checkout -b feature/blog-community-blg
```

### Bước 2 - Cho Codex đọc repo trước

Dùng prompt ngắn:

```text
Read this repository and summarize the current architecture, authentication/RBAC mechanism, frontend template structure, database migration approach, and test setup. Do not modify files yet.
```

Sau khi Codex trả lời, kiểm tra xem nó có nhận diện đúng:

- Maven hay Gradle.
- Spring Security dùng session/JWT/OAuth.
- Entity User nằm ở đâu.
- Controller hiện có chia ViewController/ActionController không.
- Frontend dùng Thymeleaf hay static HTML.
- Static assets đang nằm ở path nào.

### Bước 3 - Giao BLG theo cụm nhỏ

Không giao cả 7 task cùng lúc nếu repo đang nhiều conflict. Nên dùng:

- Prompt 1: BLG-01 + BLG-02.
- Prompt 2: BLG-05.
- Prompt 3: BLG-03 + BLG-04.
- Prompt 4: BLG-06 + BLG-07.
- Prompt 5: Review + fix + test.

### Bước 4 - Yêu cầu Codex tự test

Luôn thêm vào prompt:

```text
After implementation, run the existing test command if available. If tests cannot run, explain exactly why and provide manual verification steps.
```

### Bước 5 - Review diff trước khi commit

```bash
git status
git diff
```

Chỉ commit khi bạn hiểu các file đã đổi.

```bash
git add .
git commit -m "Implement BLG blog community workflow"
```

### Bước 6 - Merge thường xuyên, tránh conflict

```bash
git checkout develop
git pull origin develop
git checkout feature/blog-community-blg
git merge develop
```

Resolve conflict ngay khi còn ít thay đổi.

## Prompt pattern hiệu quả

Một prompt tốt nên có 7 phần:

1. Vai trò của Codex.
2. Bối cảnh project.
3. Phạm vi được phép sửa.
4. Chức năng cần làm.
5. Quy tắc kỹ thuật.
6. Acceptance criteria.
7. Test command và yêu cầu báo cáo diff.

Mẫu:

```text
You are working in an existing Spring Boot + HTML/CSS/JS e-learning project.

Before coding, inspect the repository and identify the existing conventions for:
- entities
- repositories
- services
- controllers
- DTOs
- validation
- security/RBAC
- Thymeleaf/static assets
- migration/seed data
- tests

Implement only BLG-01 and BLG-02.
Do not change unrelated modules unless required for compilation.
Follow the existing naming/package/style conventions.
After coding, run tests and summarize changed files.
```

## Khi Codex làm sai thì sửa thế nào?

Nếu Codex code lan sang module khác:

```text
Revert unrelated changes outside the Blog module. Keep only changes required for BLG. Explain each remaining file change.
```

Nếu Codex tạo entity không khớp User hiện có:

```text
Refactor the Blog entities to use the existing User entity and repository. Do not create a duplicate User model.
```

Nếu Codex không chạy test:

```text
Find the correct build tool and test command in this repo, then run the smallest relevant test set. If the environment prevents tests from running, explain the blocker and provide manual test steps.
```

Nếu UI xấu hoặc lệch layout:

```text
Keep the existing design system and layout. Refactor only the Blog pages so they match the current public/student/admin UI style and remain responsive.
```

## Không nên làm

- Không yêu cầu Codex làm BLG + PAY + ASM cùng lúc.
- Không cho Codex rewrite toàn bộ project.
- Không bỏ qua bước review diff.
- Không merge khi test chưa chạy hoặc chưa có manual verification.
- Không để Codex hard-code userId, role hoặc database credentials.
