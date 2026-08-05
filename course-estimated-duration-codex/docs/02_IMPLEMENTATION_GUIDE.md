# Implementation Guide

## 1. Mô hình khuyến nghị

Không lưu tổng duration vào Course. Tổng phải được suy ra từ các video lesson đang hợp lệ.

Luồng dữ liệu:

```text
Repository aggregation
        ↓
Course service
        ↓
Course DTO/view model
        ↓
Shared course metadata fragment/component
        ↓
Course catalog / detail / student course cards
```

## 2. Query aggregation

Codex phải điều chỉnh tên bảng/cột theo schema thật.

Mẫu logic:

```sql
SELECT
    course_id,
    COALESCE(SUM(video_duration_seconds), 0) AS estimated_duration_seconds
FROM (
    SELECT DISTINCT
        lesson.id,
        resolved_course_id AS course_id,
        CASE
            WHEN lesson_is_video
             AND lesson_is_active
             AND video_duration_seconds > 0
            THEN video_duration_seconds
            ELSE 0
        END AS video_duration_seconds
    FROM ...
    WHERE resolved_course_id IN (:courseIds)
) video_lessons
GROUP BY course_id;
```

Lớp `DISTINCT lesson.id` hoặc subquery riêng giúp tránh cộng lặp khi lesson JOIN với nhiều resource/quiz.

## 3. Danh sách course

Không dùng:

```java
courses.stream()
    .map(course -> durationRepository.sumByCourseId(course.getId()))
```

Cách trên tạo N+1.

Dùng một trong các cách:

- Group-by projection cho tất cả course IDs.
- Query course list kèm subquery duration.
- Batch query trả về `courseId -> totalSeconds`.

## 4. Course detail

Course detail có thể dùng cùng batch method với một ID hoặc projection riêng, nhưng formatter và filter phải dùng chung.

## 5. Duration nguồn

Thứ tự ưu tiên:

1. Duration đã lưu trong Lesson.
2. Duration trong LessonMedia/Video entity.
3. Metadata của dịch vụ upload đã được dự án lưu.
4. Chỉ khi hoàn toàn chưa có: thêm `videoDurationSeconds`.

Không lấy `Content-Length` để suy ra thời lượng video.

## 6. Visibility

Áp dụng cùng tiêu chí lesson đang được hệ thống xem là hợp lệ:

- active;
- không soft-deleted;
- không archived;
- published/visible khi tính cho public course.

Không tự tạo một quy tắc visibility trái với service hiện có.

## 7. UI

Ưu tiên sửa fragment/component metadata dùng chung.

Ví dụ Thymeleaf mang tính tham khảo:

```html
<span class="course-meta-item"
      th:if="${course.estimatedDurationDisplay != null}">
    <i class="existing-clock-icon" aria-hidden="true"></i>
    <span>Thời lượng dự tính:</span>
    <span th:text="${course.estimatedDurationDisplay}"></span>
</span>
```

Giữ class/icon theo design system thực tế.
