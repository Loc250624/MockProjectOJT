package com.ojtsu26.elearning.repository;

import com.ojtsu26.elearning.model.entity.Course;
import com.ojtsu26.elearning.model.entity.Lesson;
import com.ojtsu26.elearning.model.entity.Video;
import com.ojtsu26.elearning.model.enums.LessonType;
import com.ojtsu26.elearning.repository.projection.CourseDurationProjection;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertEquals;

@DataJpaTest(properties = {
        "spring.jpa.database-platform=org.hibernate.dialect.H2Dialect",
        "spring.jpa.properties.hibernate.dialect=org.hibernate.dialect.H2Dialect",
        "spring.jpa.hibernate.ddl-auto=create-drop"
})
class VideoRepositoryDurationTest {

    @Autowired
    private TestEntityManager entityManager;

    @Autowired
    private VideoRepository videoRepository;

    @Test
    void aggregatesOnlyPositiveVideoLessonDurationsForEachCourse() {
        Course firstCourse = persistCourse("Course A");
        Course secondCourse = persistCourse("Course B");

        persistVideo(firstCourse, LessonType.VIDEO, 600);
        persistVideo(firstCourse, LessonType.VIDEO, 900);
        persistVideo(firstCourse, LessonType.VIDEO, 1200);
        persistVideo(firstCourse, LessonType.QUIZ, 500);
        persistVideo(firstCourse, LessonType.RETIRED, 700);
        persistVideo(firstCourse, LessonType.VIDEO, null);
        persistVideo(firstCourse, LessonType.VIDEO, 0);
        persistVideo(firstCourse, LessonType.VIDEO, -10);
        persistVideo(secondCourse, LessonType.VIDEO, 3000);
        entityManager.flush();
        entityManager.clear();

        Map<Integer, Long> totals = videoRepository
                .sumDurationSecondsByCourseIds(List.of(firstCourse.getId(), secondCourse.getId()))
                .stream()
                .collect(Collectors.toMap(
                        CourseDurationProjection::getCourseId,
                        CourseDurationProjection::getEstimatedDurationSeconds));

        assertEquals(2700L, totals.get(firstCourse.getId()));
        assertEquals(3000L, totals.get(secondCourse.getId()));
        assertEquals(7, videoRepository.findVideoLessonsByCourseIds(
                List.of(firstCourse.getId(), secondCourse.getId())).size());
    }

    private Course persistCourse(String title) {
        return entityManager.persistAndFlush(Course.builder().title(title).build());
    }

    private void persistVideo(Course course, LessonType type, Integer durationSeconds) {
        Lesson lesson = entityManager.persistAndFlush(Lesson.builder()
                .course(course)
                .title(type + " lesson")
                .type(type)
                .build());
        entityManager.persist(Video.builder()
                .lesson(lesson)
                .videoUrl("https://example.com/video.mp4")
                .durationSeconds(durationSeconds)
                .build());
    }
}
