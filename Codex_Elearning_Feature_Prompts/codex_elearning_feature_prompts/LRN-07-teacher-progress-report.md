# LRN-07 — Teacher xem báo cáo tiến độ học tập của học viên

- **Mã chức năng:** LRN-07
- **Module:** Teacher Portal / Reporting
- **Actor chính:** Teacher
- **Mức ưu tiên:** Trung bình–Cao
- **Phụ thuộc:** LRN-03 Progress, LRN-06, Assessment results

## Mục tiêu

Cho phép Teacher theo dõi tổng quan và chi tiết tiến độ học tập của Student trong course do mình phụ trách, phục vụ phát hiện học viên chậm tiến độ và đánh giá mức độ hoàn thành.

## Quy tắc nghiệp vụ

1. Teacher chỉ xem báo cáo course mình sở hữu/phụ trách.
2. Báo cáo lấy từ progress/assessment thật; không tính từ dữ liệu UI.
3. Chỉ số tối thiểu:
   - Tổng enrollment.
   - Số/chỉ lệ chưa bắt đầu, đang học, hoàn thành.
   - Average course progress.
   - Student progress từng người.
   - Last activity.
   - Quiz/assignment summary nếu dữ liệu có sẵn.
4. Định nghĩa `not started`, `in progress`, `completed`, `inactive` phải được ghi rõ và dùng nhất quán.
5. Student/course không có dữ liệu phải trả 0/empty hợp lệ, không lỗi chia cho 0.
6. Timezone và date range phải theo convention dự án.
7. Không hiển thị dữ liệu ngoài course hoặc dữ liệu cá nhân không cần thiết.
8. Filter/export phải dùng cùng authorization và logic metric.
9. Report không được sửa progress.
10. Query phải có giới hạn, pagination và aggregate hiệu quả.

## Phạm vi triển khai

### Backend
- Service báo cáo dùng query aggregate/projection.
- API tổng quan course.
- API danh sách Student progress có pagination/search/filter.
- API chi tiết một Student trong course nếu UI cần drill-down.
- Filter tùy dữ liệu: progress range, status, inactive days, assessment state.
- Thống nhất rounding và cách tính average.
- Tối ưu index/query và tránh tải toàn bộ lesson progress vào memory.
- Có thể cache ngắn nếu kiến trúc hỗ trợ, nhưng phải invalidation hợp lý.

### Frontend
- Dashboard report gồm KPI cards, progress distribution và bảng Student.
- Có thể dùng chart library hiện có; không thêm thư viện mới nếu không cần.
- Chart phải có text/table tương đương cho accessibility.
- Drill-down hiển thị section/lesson completion, last activity và assessment summary.
- Loading/empty/error/no-data rõ ràng.
- Responsive cho mobile/tablet.

### Export
- Nếu triển khai CSV, escape dữ liệu chống CSV formula injection và giữ đúng filter hiện tại.

## Test bắt buộc

- Owner Teacher xem được report.
- Teacher khác không truy cập được.
- Metric đúng với dataset có Student chưa bắt đầu/đang học/hoàn thành.
- Course không có enrollment trả số 0 hợp lệ.
- Student bị filter đúng theo status/progress/inactivity.
- Pagination và rounding nhất quán.
- Không có cross-course data.
- Query không N+1 và không load toàn bộ dữ liệu không cần thiết.
- CSV nếu có không chứa formula injection.

## Tiêu chí nghiệm thu

- KPI, bảng Student và drill-down dùng cùng một định nghĩa metric.
- Teacher xem được tình trạng học tập mà không có quyền sửa progress.
- Authorization course ownership đầy đủ.
- Report vẫn hoạt động với course lớn nhờ pagination/aggregate query.
- Dữ liệu phù hợp với LRN-03 và ASM-01.

## Nguyên tắc bắt buộc dành cho Codex

1. Đọc `AGENTS.md`, `README.md`, tài liệu kiến trúc, cấu hình build và quy ước trong repository trước khi thay đổi mã nguồn.
2. Khảo sát toàn bộ luồng liên quan từ entity/model → repository → service → controller/API → security → giao diện → test.
3. Tận dụng cấu trúc, naming convention, DTO, mapper, exception handler, response format và design system đang tồn tại; không tạo kiến trúc song song.
4. Không sửa hoặc format hàng loạt các file ngoài phạm vi chức năng.
5. Không xóa hành vi hiện có chỉ để làm test mới chạy.
6. Không hard-code user, role, course, price, trạng thái, URL, secret hoặc dữ liệu môi trường.
7. Mọi quyền truy cập phải được kiểm tra ở backend; việc ẩn nút trên frontend không được xem là biện pháp bảo mật.
8. Với thay đổi database, ưu tiên migration tương thích ngược. Không drop/rename cột đang dùng nếu chưa chứng minh an toàn.
9. Bổ sung unit test và integration test cho luồng thành công, từ chối truy cập và trường hợp biên.
10. Chạy các lệnh build/test phù hợp với repository. Nếu lệnh nào không chạy được, ghi rõ nguyên nhân và phần đã kiểm tra thay thế.
11. Cuối tác vụ, báo cáo:
    - Những file đã thay đổi.
    - Migration hoặc cấu hình mới.
    - Test đã chạy và kết quả.
    - Giả định còn tồn tại.
    - Cách kiểm thử thủ công.

---

## Prompt hoàn chỉnh cho Codex

> Sao chép nguyên khối nội dung bên dưới vào Codex khi đang mở đúng repository và đúng feature branch.

```text
Hãy triển khai **LRN-07 — Báo cáo tiến độ học tập dành cho Teacher**.

### Kết quả cần đạt
- Teacher xem dashboard report cho từng course mình phụ trách.
- Có KPI: tổng học viên, chưa bắt đầu, đang học, hoàn thành, average progress và last activity.
- Có bảng Student progress phân trang, search/filter/sort và drill-down khi phù hợp.
- Kết hợp assessment summary từ dữ liệu hiện có.
- Không cho Teacher sửa progress tại màn báo cáo.

### Hãy thực hiện
- Đọc implementation LRN-03/Progress và định nghĩa trạng thái hiện có.
- Đặt authorization course ownership ở backend cho overview, list, detail và export.
- Dùng aggregate query/projection; không load toàn bộ progress rồi tính trong Java nếu database có thể tính hiệu quả.
- Xử lý course rỗng, divide-by-zero, rounding, timezone/date range nhất quán.
- Tái sử dụng chart library/design system hiện có; có bảng/text thay thế cho accessibility.
- Nếu có CSV export, dùng cùng filter/authorization và chống CSV formula injection.
- Viết test metric correctness, empty course, ownership/IDOR, filter, pagination, cross-course isolation và query efficiency.

### Cách làm việc bắt buộc

- Trước tiên, hãy đọc `AGENTS.md` và khảo sát repository. Không bắt đầu viết code ngay.
- Hãy xác định các thành phần đã tồn tại và lập một kế hoạch ngắn theo đúng kiến trúc hiện tại.
- Chỉ triển khai phạm vi của chức năng này và những thay đổi phụ thuộc trực tiếp.
- Không tạo entity, endpoint, service hoặc trang trùng với thành phần hiện có.
- Không sửa các chức năng không liên quan.
- Bảo đảm authorization ở backend, validation đầu vào, xử lý lỗi nhất quán và không lộ dữ liệu nhạy cảm.
- Thêm hoặc cập nhật test phù hợp.
- Chạy build/test của dự án.
- Sau khi hoàn tất, tự review diff để tìm lỗi logic, N+1 query, IDOR, race condition, lỗi trạng thái và regression.
- Trả về báo cáo gồm: kế hoạch đã thực hiện, file thay đổi, test đã chạy, kết quả, cách kiểm thử thủ công và các giả định.
```
