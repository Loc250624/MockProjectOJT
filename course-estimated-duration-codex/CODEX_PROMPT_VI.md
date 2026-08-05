# NHIỆM VỤ CHO CODEX

Bạn đang làm việc trên dự án Spring Boot E-Learning hiện có. Hãy triển khai tính năng hiển thị thời gian hoàn thành dự tính cho từng course với phạm vi tối thiểu và không sửa khu vực không liên quan.

## 1. Yêu cầu nghiệp vụ bắt buộc

Mỗi course phải có giá trị:

- `estimatedDurationSeconds`: tổng số giây video của course.
- `estimatedDurationDisplay`: chuỗi thân thiện để hiển thị, ví dụ `2 giờ 15 phút`.

Công thức:

```text
estimatedDurationSeconds =
    SUM(videoDurationSeconds của tất cả lesson có video thuộc course hiện tại)
```

Chỉ tính lesson thỏa tất cả điều kiện sau:

1. Thuộc đúng course đang tính, kể cả khi quan hệ đi qua chapter/module/section.
2. Là lesson video theo kiểu dữ liệu chuẩn đang dùng trong dự án:
   - `lessonType == VIDEO`; hoặc
   - lesson có media/video record hợp lệ; hoặc
   - lesson có `videoUrl` hợp lệ nếu dự án không có enum loại lesson.
3. Lesson không bị soft-delete/archive/inactive theo quy tắc sẵn có.
4. Duration khác null và lớn hơn 0.

Không được tính:

- Quiz.
- Text lesson.
- Tài liệu/resource.
- Assignment/coding.
- Thời gian đọc dự tính.
- Lesson của course khác.
- Video bị xoá, inactive hoặc soft-deleted.
- Duration âm/null.
- Cùng một lesson nhiều lần do JOIN với resource, quiz hoặc bảng con khác.

## 2. Khảo sát trước khi sửa

Tìm và xác định chính xác:

- Entity course.
- Entity lesson.
- Entity chapter/module/section trung gian.
- Nơi lưu URL video.
- Nơi lưu duration video.
- Course repository/service/DTO/view model.
- Các template hoặc JavaScript render course card/course detail.
- Fragment dùng chung cho metadata course.
- Quy tắc active, published, archived và soft-delete.

Tìm kiếm các từ khóa:

```text
Course
Lesson
Chapter
Module
Section
videoUrl
videoDuration
durationSeconds
duration
lessonType
VIDEO
CourseDto
CourseResponse
course-card
course-detail
my-courses
```

Không đoán tên file hoặc tên cột.

## 3. Chiến lược dữ liệu

### Trường hợp A — Lesson đã lưu duration

Tái sử dụng trường hiện có. Không tạo cột mới và không lưu tổng duration vào Course.

### Trường hợp B — Lesson chưa lưu duration

Chỉ khi xác nhận repository hoàn toàn chưa có duration video:

1. Thêm một trường nullable theo đơn vị giây, ưu tiên:
   `videoDurationSeconds BIGINT`.
2. Tạo migration theo công cụ migration thực tế của dự án.
3. Cập nhật form/API tạo và sửa video lesson để lưu duration.
4. Không đặt giá trị giả.
5. Dữ liệu cũ chưa có duration được xem là `0` và UI hiển thị trạng thái chưa có thời lượng khi tổng bằng 0.

Không tự động gọi API YouTube/Vimeo mới nếu dự án chưa dùng các API đó. Không thêm dependency lớn chỉ để đọc duration.

## 4. Backend

Ưu tiên tính tổng tại repository/query hoặc một service dùng chung để tránh N+1.

Yêu cầu:

- Query trả về tổng theo `courseId`.
- Với danh sách course, dùng batch aggregation theo danh sách ID hoặc projection group-by; không chạy một query riêng cho từng course.
- Dùng `COALESCE(..., 0)`.
- Tránh nhân duration do JOIN nhiều bảng.
- Không cộng cached total cũ nếu course đã thay đổi lesson.
- Không thêm cột `estimated_duration` vào Course trừ khi repository hiện tại đã có cơ chế denormalized metadata nhất quán.

Bổ sung dữ liệu duration vào DTO/view model hiện đang phục vụ các màn hình course. Không tạo DTO song song nếu đã có DTO phù hợp.

Tên field đề xuất:

```java
private long estimatedDurationSeconds;
private String estimatedDurationDisplay;
```

Nếu dự án dùng record/projection/MapStruct, follow đúng convention hiện có.

## 5. Quy tắc format

Format từ tổng giây sang chuỗi tiếng Việt:

- Tổng <= 0: `Chưa có thời lượng video`
- 1–59 giây: `< 1 phút`
- Dưới 60 phút: làm tròn lên phút, ví dụ 61 giây → `2 phút`
- Từ 60 phút:
  - `60 phút` → `1 giờ`
  - `75 phút` → `1 giờ 15 phút`
  - Nếu làm tròn phút thành 60 thì chuyển carry sang giờ.

Đặt formatter tại một utility/service dùng chung, không lặp logic ở nhiều controller hoặc template.

## 6. Frontend

Hiển thị gần các metadata hiện có như số lesson, level, rating hoặc instructor.

Nhãn thống nhất:

```text
Thời lượng dự tính: 2 giờ 15 phút
```

Phạm vi hiển thị:

- Course catalog/card công khai.
- Course detail.
- Student My Courses hoặc dashboard card nếu đang dùng course card/metadata tương tự.
- Các danh sách course khác dành cho người học nếu dùng chung fragment/component.

Không bắt buộc thêm vào bảng quản trị Teacher/Admin nếu các màn hình đó không phải nơi người dùng xem thông tin course. Nếu có fragment course card dùng chung thì sửa fragment thay vì copy markup.

Yêu cầu UI:

- Follow design system hiện có.
- Dùng icon clock đang có trong dự án.
- Không thêm thư viện icon mới.
- Responsive, không làm vỡ card.
- Có thể ẩn dòng `Chưa có thời lượng video` trên card quá nhỏ chỉ khi UX hiện tại yêu cầu; course detail vẫn phải hiển thị rõ.

## 7. Cập nhật dữ liệu

Sau khi teacher:

- thêm video lesson,
- đổi video,
- sửa duration,
- xoá/archive video lesson,
- chuyển loại lesson,

thời gian dự tính phải phản ánh dữ liệu mới ngay trong lần đọc tiếp theo.

Ưu tiên tính động từ lesson data để không cần đồng bộ cache.

## 8. Kiểm thử bắt buộc

Viết unit test/service test/repository test theo test stack hiện có.

Tối thiểu phải có:

1. Course có 3 video: 600 + 900 + 1200 giây = 2700 giây = `45 phút`.
2. Course có video và quiz: chỉ cộng video.
3. Hai course khác nhau: không trộn duration.
4. Course không có video: 0 và `Chưa có thời lượng video`.
5. Video duration null/0/âm: bỏ qua.
6. Lesson soft-deleted/inactive: bỏ qua.
7. JOIN lesson với nhiều resource không làm duration bị nhân đôi.
8. Danh sách nhiều course không phát sinh N+1.
9. Formatter kiểm tra mốc 59 giây, 60 giây, 3599 giây, 3600 giây, 3660 giây.
10. Course card và course detail render đúng field.

Chạy toàn bộ test liên quan và build Maven.

## 9. Ràng buộc phạm vi

Không sửa:

- Quiz flow.
- Payment.
- Authentication/OAuth.
- AI chatbot.
- Certificate.
- Blog.
- User profile.
- Dark/light mode.
- Các chức năng không liên quan.

Không refactor diện rộng. Không đổi schema ngoài trường duration video khi thật sự thiếu. Không thay đổi tên endpoint công khai nếu không bắt buộc.

## 10. Kết quả Codex phải báo cáo

Sau khi hoàn thành, trả về:

1. Danh sách file đã sửa/tạo.
2. Trường duration thực tế được dùng.
3. Query/cách aggregation.
4. Các màn hình đã hiển thị.
5. Migration có hay không và lý do.
6. Test đã chạy và kết quả.
7. Những dữ liệu video cũ chưa có duration cần backfill, nếu có.
