# BLG Codex Package - Blog & Community / Blog Moderation

Bộ tài liệu này được tạo từ sheet `Backlog` trong file `OJTSU26_Project_Tracking(10).xlsx`, chỉ tập trung vào các chức năng có mã `BLG`.

## Phạm vi

- Tổng số chức năng: **7**
- Tổng effort: **19 SP**
- P1: **15 SP** gồm BLG-01 đến BLG-05, cần ưu tiên trước.
- P2: **4 SP** gồm BLG-06 và BLG-07, chỉ làm sau khi P1 ổn định.
- Target chính: **W7** cho luồng Blog core, **W9** cho phần moderation nâng cao.

## Cách dùng nhanh với Codex

1. Copy file `CODEX_PROMPT_BLG_FULL_IMPLEMENTATION.md` vào Codex.
2. Yêu cầu Codex đọc repo hiện tại trước, không code ngay.
3. Cho Codex thực hiện theo thứ tự:
   - BLG-01
   - BLG-02
   - BLG-05
   - BLG-03
   - BLG-04
   - BLG-06
   - BLG-07
4. Sau mỗi nhóm chức năng, chạy prompt review trong `CODEX_PROMPT_BLG_REVIEW_AND_FIX.md`.
5. Không merge vào `develop` nếu chưa pass checklist trong `05_TESTING_AND_DOD_CHECKLIST.md`.

## Danh sách file

| File | Mục đích |
|---|---|
| `01_BLG_SCOPE_FROM_BACKLOG.md` | Tóm tắt chính xác các task BLG từ Backlog |
| `02_BLG_DOMAIN_DATABASE_DESIGN.md` | Gợi ý entity, bảng DB, status workflow |
| `03_BLG_BACKEND_IMPLEMENTATION_PLAN.md` | Kế hoạch backend Spring Boot: package, controller, service, repository, DTO |
| `04_BLG_FRONTEND_IMPLEMENTATION_PLAN.md` | Kế hoạch frontend HTML/CSS/JS và màn hình cần có |
| `05_TESTING_AND_DOD_CHECKLIST.md` | Checklist test, security, responsive, DoD |
| `06_CODEX_EFFECTIVE_WORKFLOW.md` | Cách dùng Codex hiệu quả trong repo thực tế |
| `CODEX_PROMPT_BLG_FULL_IMPLEMENTATION.md` | Prompt tổng để yêu cầu Codex build chức năng BLG |
| `CODEX_PROMPT_BLG_INCREMENTAL_TASKS.md` | Prompt nhỏ theo từng task BLG |
| `CODEX_PROMPT_BLG_REVIEW_AND_FIX.md` | Prompt yêu cầu Codex review, test và sửa lỗi sau khi code |

## Nguyên tắc quan trọng

- Không tự ý thay đổi các module khác ngoài Blog nếu không cần thiết.
- Không phá vỡ Auth/RBAC hiện có.
- Không hard-code user hiện tại; phải lấy từ SecurityContext/session/JWT tùy kiến trúc repo.
- Không bỏ qua validation, empty state, error state và phân quyền.
- Mọi thay đổi database phải có migration/script rõ ràng.
