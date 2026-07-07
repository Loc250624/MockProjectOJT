# Backlog Extract — ASM-01 to ASM-06

Nguồn: Sheet `Backlog` trong file Excel người dùng gửi.

| ID | Module | Function | Actor | Priority | Week | Owner | Reviewer | SP | Dependencies | Deliverable |
|---|---|---|---|---|---|---|---:|---:|---|---|
| ASM-01 | Assessment | Học viên làm bài trắc nghiệm, lưu tạm và nộp bài | Student | P0 | W6 | Cường | Lộc | 5 | LRN-01 | Quiz taking UI |
| ASM-02 | Assessment | Học viên nộp bài thực hành/bài tập code | Student | P1 | W6 | Cường | Lộc | 5 | LRN-01 | Submission flow |
| ASM-03 | Assessment | Học viên xem kết quả trắc nghiệm và kết quả chấm code | Student | P1 | W6 | Cường | Lộc | 3 | ASM-01,ASM-02 | Result screen |
| ASM-04 | Assessment Management | Giảng viên tạo, cập nhật và xóa Quiz/Câu hỏi/Đáp án | Teacher | P0 | W6 | Cường | Cường | 5 | CRS-06 | Quiz builder |
| ASM-05 | Assessment Management | Cấu hình testcase và chấm code tự động qua adapter dịch vụ | Teacher/System | P2 | W8 | Cường | Cường | 8 | ASM-02 | Code judge sandbox |
| ASM-06 | Assessment Management | Chấm điểm và nhận xét bài tập tự luận/thực hành | Teacher | P1 | W6 | Cường | Cường | 5 | ASM-02 | Grading workflow |


## Dependency map
- ASM-01 phụ thuộc LRN-01.
- ASM-02 phụ thuộc LRN-01.
- ASM-03 phụ thuộc ASM-01 và ASM-02.
- ASM-04 phụ thuộc CRS-06.
- ASM-05 phụ thuộc ASM-02.
- ASM-06 phụ thuộc ASM-02.
