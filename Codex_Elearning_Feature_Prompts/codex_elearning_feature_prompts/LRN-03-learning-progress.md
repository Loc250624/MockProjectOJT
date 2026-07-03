# LRN-03 — Theo dõi tiến độ theo bài, chương và khóa học

- **Mã chức năng:** LRN-03
- **Module:** Learning Experience
- **Actor chính:** Student
- **Mức ưu tiên:** Cao
- **Phụ thuộc:** LRN-01, LRN-02, Curriculum

## Mục tiêu

Theo dõi tiến độ học đáng tin cậy ở ba cấp:
- Lesson progress.
- Section/chapter progress.
- Course progress.

Student nhìn thấy phần trăm hoàn thành, bài gần nhất và trạng thái hoàn thành; dữ liệu này là nguồn cho báo cáo Teacher và điều kiện cấp chứng chỉ.

## Quy tắc nghiệp vụ

1. Chỉ ghi progress cho Student có enrollment hoạt động.
2. Mỗi Student + Lesson có tối đa một bản ghi progress.
3. Với video, không tin tuyệt đối phần trăm/thời lượng do client gửi.
4. Progress không được giảm do request đến sai thứ tự; dùng giá trị lớn nhất hợp lệ hoặc version/timestamp phù hợp.
5. Lesson chỉ hoàn thành khi đạt quy tắc của loại lesson:
   - Text/resource: theo hành động hoàn thành hợp lệ của hệ thống.
   - Video: đạt ngưỡng xem do dự án quy định.
   - Quiz/assignment: theo kết quả assessment nếu curriculum coi đó là điều kiện.
6. Section progress được tính từ các lesson bắt buộc đang hoạt động.
7. Course progress được tính từ toàn bộ nội dung bắt buộc; tránh chia cho 0.
8. Lesson draft/deleted/optional phải được xử lý theo policy nhất quán.
9. Khi curriculum thay đổi, không xóa lịch sử progress; phần trăm được tính lại dựa trên curriculum hiện tại.
10. Cập nhật progress phải idempotent và chịu được request lặp/out-of-order.

## Phạm vi triển khai

### Data model
- Rà soát `LessonProgress`, `CourseProgress`, `Enrollment` hoặc model tương đương.
- Ưu tiên lưu progress nguồn ở lesson và tính aggregate, trừ khi kiến trúc hiện tại đã có snapshot.
- Bổ sung unique constraint cho Student/Enrollment + Lesson nếu thiếu.
- Lưu các trường cần thiết như trạng thái, max position, completedAt, lastAccessedAt theo model thực tế.

### Backend
- API ghi heartbeat/progress với validation phạm vi.
- API đánh dấu hoàn thành khi policy cho phép.
- API lấy progress của Student hiện tại cho course.
- Tính aggregate hiệu quả, tránh query cho từng lesson.
- Bảo vệ khỏi IDOR và spoofing.
- Dùng transaction/atomic update khi cần.

### Frontend
- Thanh phần trăm ở course card, course player và section.
- Icon/trạng thái lesson: chưa học, đang học, hoàn thành, khóa.
- Resume đúng lesson gần nhất.
- Gửi heartbeat có throttle/debounce; không gửi mỗi frame.
- Flush hợp lý khi pause, seek hoặc rời trang nhưng không gây spam.

## Test bắt buộc

- Tạo progress lần đầu và cập nhật lần sau không sinh duplicate.
- Request cũ không làm progress giảm.
- Giá trị âm, vượt duration hoặc payload giả bị từ chối/chuẩn hóa.
- Student chưa enrolled hoặc lesson course khác bị từ chối.
- Hoàn thành lesson cập nhật section/course progress đúng.
- Course không có lesson bắt buộc không gây divide-by-zero.
- Curriculum thêm/xóa/ẩn lesson cho kết quả theo policy.
- Request đồng thời không làm mất max progress.
- UI resume và hiển thị phần trăm chính xác.

## Tiêu chí nghiệm thu

- Progress nhất quán giữa lesson, section, course và dashboard.
- Refresh hoặc đăng nhập lại vẫn giữ đúng tiến độ.
- Không thể sửa progress của người khác.
- Không thể dùng payload tùy ý để hoàn thành course ngay lập tức.
- Dữ liệu đủ để LRN-07 và LRN-05 sử dụng.

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
Hãy triển khai **LRN-03 — Theo dõi tiến độ học theo lesson, section và course** trong codebase hiện tại.

### Kết quả cần đạt
- Ghi nhận progress cho Student enrolled theo từng lesson.
- Tính section progress và course progress từ các nội dung bắt buộc.
- Hỗ trợ resume lesson gần nhất.
- Request lặp, đồng thời hoặc đến sai thứ tự không làm giảm/mất progress.
- UI hiển thị đúng trạng thái và phần trăm trên dashboard/course player.

### Yêu cầu kỹ thuật
- Khảo sát model progress hiện có trước khi tạo mới.
- Không tin `studentId`, `completed`, `duration`, `percentage` từ client nếu backend có thể tự suy ra hoặc xác minh.
- Dùng unique constraint và cập nhật atomic/transaction phù hợp.
- Heartbeat phải được throttle; lưu max progress hợp lệ.
- Xác định quy tắc hoàn thành cho video, text, resource, quiz/assignment dựa trên cấu hình hiện có.
- Aggregate query phải tránh N+1 và divide-by-zero.
- Bảo vệ toàn bộ API khỏi IDOR.
- Viết test cho duplicate, out-of-order, concurrent update, invalid values, unauthorized access, curriculum change và aggregate calculation.

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
