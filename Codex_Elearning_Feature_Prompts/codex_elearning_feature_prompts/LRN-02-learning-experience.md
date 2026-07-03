# LRN-02 — Học trực tuyến bằng video, nội dung văn bản và tài nguyên

- **Mã chức năng:** LRN-02
- **Module:** Learning Experience
- **Actor chính:** Student
- **Mức ưu tiên:** Cao
- **Phụ thuộc:** LRN-01 Enrollment, Course Curriculum, Lesson/Resource, Authentication/RBAC

## Mục tiêu

Xây dựng luồng học trực tuyến thống nhất để Student đã có enrollment hợp lệ có thể:
- Mở chương/bài học theo thứ tự khóa học.
- Xem video.
- Đọc nội dung văn bản an toàn.
- Xem hoặc tải tài nguyên được giáo viên cho phép.
- Tiếp tục từ bài học gần nhất.

## Quy tắc nghiệp vụ

1. Chỉ Student có enrollment hoạt động mới truy cập nội dung học của course.
2. Course, section, lesson và resource phải ở trạng thái khả dụng.
3. Không cho phép truy cập chéo khóa học bằng cách thay ID trên URL/API.
4. Thứ tự chương và bài lấy từ dữ liệu backend.
5. Lesson có thể gồm video, text hoặc kết hợp tùy model hiện có.
6. URL/private resource không được lộ cho người không có quyền.
7. Tài nguyên chỉ được tải khi resource cho phép download.
8. Nội dung HTML do Teacher nhập phải được sanitize theo cơ chế an toàn của dự án; không render script tùy ý.
9. Video progress không được đánh dấu hoàn thành chỉ vì mở trang.
10. Nếu lesson bị khóa theo prerequisite, backend phải từ chối truy cập, không chỉ disable trên UI.
11. Ghi nhận `lastAccessedLesson` hoặc cơ chế tương đương nếu model hiện tại hỗ trợ.

## Phạm vi triển khai

### Backend
- Xây dựng/hoàn thiện API trả curriculum chỉ gồm nội dung Student được phép xem.
- Xây dựng API lấy chi tiết lesson với authorization dựa trên enrollment và course ownership.
- Tách metadata video/resource khỏi các trường nội bộ không cần thiết.
- Nếu dùng signed URL/object storage, tạo URL ngắn hạn theo cơ chế hiện có.
- Cập nhật last accessed an toàn, không làm hỏng request đọc lesson nếu tracking phụ thất bại.
- Tránh N+1 khi tải curriculum.

### Frontend
- Learning player có sidebar curriculum, vùng nội dung chính và điều hướng Previous/Next.
- Hỗ trợ:
  - Video player.
  - Rich text/text lesson.
  - Danh sách resource với tên, loại, kích thước nếu có.
- Responsive: mobile chuyển sidebar thành drawer/collapsible.
- Hiển thị trạng thái loading, empty, forbidden, lesson unavailable và playback error.
- Nút quay lại course/dashboard và continue learning.
- Không dùng mock data khi backend đã có dữ liệu thật.

### Accessibility
- Điều khiển video dùng được bằng bàn phím.
- Có label, heading hierarchy và focus state hợp lý.
- Không phụ thuộc chỉ vào màu sắc để biểu thị lesson đã học/đang học.

## Test bắt buộc

- Student enrolled xem được lesson video/text/resource.
- Student chưa enrolled bị từ chối.
- Student của course A không mở lesson thuộc course B bằng đổi ID.
- Lesson/course không khả dụng bị từ chối.
- Resource không cho download không thể tải qua API trực tiếp.
- HTML nguy hiểm không được thực thi.
- Curriculum giữ đúng thứ tự chương/bài.
- UI xử lý lesson video, text, mixed, không có resource và lỗi tải.
- Teacher/Admin behavior tuân thủ policy đang tồn tại, không vô tình mở endpoint Student.

## Tiêu chí nghiệm thu

- Student enrolled có thể học liên tục từ curriculum đến từng lesson.
- Authorization được áp dụng ở backend cho lesson và resource.
- Giao diện hoạt động trên desktop/mobile và giữ design system hiện có.
- Không lộ private resource hoặc field nội bộ.
- Không phát sinh N+1 nghiêm trọng khi tải course player.

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
Hãy triển khai **LRN-02 — Learning Experience cho Student**, cho phép Student đã enrollment hợp lệ xem video, nội dung văn bản và tài nguyên của khóa học.

### Kết quả cần đạt
- Có trang/course player sử dụng curriculum thật từ backend.
- Student có thể chọn section/lesson, xem video, đọc text, mở hoặc tải resource được phép.
- Có Previous/Next và Continue learning từ lesson gần nhất.
- Backend kiểm tra enrollment, course, lesson và resource; đổi ID không thể truy cập dữ liệu khóa khác.
- Giao diện responsive và bám design system hiện tại.

### Hãy thực hiện
- Khảo sát model Course/Section/Lesson/Resource/Enrollment và route hiện có.
- Tái sử dụng component/video player/layout hiện có.
- Hoàn thiện API curriculum và lesson detail với DTO tối thiểu, tránh lộ field nội bộ.
- Bảo vệ resource/private URL; tuân theo cơ chế storage hiện tại.
- Sanitize rich text theo thư viện/quy ước đang dùng.
- Cập nhật last accessed lesson mà không tự động đánh dấu lesson hoàn thành.
- Tối ưu query để tránh N+1.
- Viết test authorization, IDOR, resource download, lesson unavailable, ordering và UI states.

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
