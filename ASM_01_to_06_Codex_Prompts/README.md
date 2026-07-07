# ASM-01 đến ASM-06 — Prompt Package cho Codex/Agent

Gói này được tạo từ sheet `Backlog` trong file `OJTSU26_Project_Tracking(9).xlsx`.

## Backlog ASM được trích xuất

| ID | Module | Function | Actor | Priority | Week | Owner | Reviewer | SP | Dependencies | Deliverable |
|---|---|---|---|---|---|---|---:|---:|---|---|
| ASM-01 | Assessment | Học viên làm bài trắc nghiệm, lưu tạm và nộp bài | Student | P0 | W6 | Cường | Lộc | 5 | LRN-01 | Quiz taking UI |
| ASM-02 | Assessment | Học viên nộp bài thực hành/bài tập code | Student | P1 | W6 | Cường | Lộc | 5 | LRN-01 | Submission flow |
| ASM-03 | Assessment | Học viên xem kết quả trắc nghiệm và kết quả chấm code | Student | P1 | W6 | Cường | Lộc | 3 | ASM-01,ASM-02 | Result screen |
| ASM-04 | Assessment Management | Giảng viên tạo, cập nhật và xóa Quiz/Câu hỏi/Đáp án | Teacher | P0 | W6 | Cường | Cường | 5 | CRS-06 | Quiz builder |
| ASM-05 | Assessment Management | Cấu hình testcase và chấm code tự động qua adapter dịch vụ | Teacher/System | P2 | W8 | Cường | Cường | 8 | ASM-02 | Code judge sandbox |
| ASM-06 | Assessment Management | Chấm điểm và nhận xét bài tập tự luận/thực hành | Teacher | P1 | W6 | Cường | Cường | 5 | ASM-02 | Grading workflow |


## Cách dùng nhanh
- Nếu muốn Agent làm từng chức năng riêng: mở từng file `ASM-01_...md` đến `ASM-06_...md`, copy toàn bộ prompt và gửi cho Codex/Agent.
- Nếu muốn Agent làm toàn bộ module Assessment theo thứ tự dependency: dùng file `00_MASTER_AGENT_PROMPT.md`.
- Sau khi Agent chạy xong từng task, dùng file `99_ASM_INTEGRATION_CHECKLIST.md` để kiểm tra lại luồng Student/Teacher.

## Thứ tự triển khai khuyến nghị
1. ASM-04 trước hoặc song song với ASM-01 nếu chưa có Quiz/Question data.
2. ASM-01.
3. ASM-02.
4. ASM-06.
5. ASM-05.
6. ASM-03 sau khi đã có dữ liệu kết quả từ quiz/submission/code judge.

Lý do: ASM-03 phụ thuộc ASM-01 và ASM-02; ASM-05 và ASM-06 phụ thuộc ASM-02.
