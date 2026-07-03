# Bộ đặc tả và prompt Codex — E-Learning

Bộ tài liệu này gồm 8 file Markdown, mỗi file tương ứng với một chức năng. Mỗi file chứa:

- Mục tiêu và quy tắc nghiệp vụ.
- Phạm vi backend, frontend, database và security.
- Danh sách test bắt buộc.
- Tiêu chí nghiệm thu.
- Prompt hoàn chỉnh để sao chép vào Codex.

## Thứ tự triển khai đề xuất

1. `LRN-01-enrollment.md`
2. `LRN-02-learning-experience.md`
3. `LRN-03-learning-progress.md`
4. `ASM-01-quiz-attempt.md`
5. `LRN-04-notifications.md`
6. `LRN-05-certificate.md`
7. `LRN-06-teacher-students.md`
8. `LRN-07-teacher-progress-report.md`

## Cách sử dụng

1. Checkout đúng feature branch.
2. Đảm bảo repository có `AGENTS.md` mô tả cấu trúc, lệnh build/test và quy ước dự án.
3. Mở file chức năng cần làm.
4. Sao chép toàn bộ phần **Prompt hoàn chỉnh cho Codex** vào Codex.
5. Kiểm tra kế hoạch Codex đưa ra, diff, migration và test trước khi merge.
6. Mỗi chức năng nên được triển khai trong một branch/PR riêng để giảm conflict.

## Quy tắc chung

- Không chạy đồng thời hai Agent trên cùng working tree.
- Với các chức năng phụ thuộc nhau, merge hoặc rebase nhánh nền trước khi triển khai chức năng tiếp theo.
- Không đưa secret, Client Secret, mật khẩu database hoặc token vào prompt/repository.
- Luôn review các thay đổi về security, authorization, payment, progress và scoring bằng tay.
