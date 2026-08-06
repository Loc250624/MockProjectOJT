package com.ojtsu26.elearning.repository;

import com.ojtsu26.elearning.model.entity.Video;
import com.ojtsu26.elearning.repository.projection.CourseDurationProjection;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

@Repository
public interface VideoRepository extends JpaRepository<Video, Integer> {
    Optional<Video> findByLessonId(Integer lessonId);

    @Query("select v from Video v join fetch v.lesson l where l.course.id in :courseIds and l.type = com.ojtsu26.elearning.model.enums.LessonType.VIDEO")
    List<Video> findVideoLessonsByCourseIds(@Param("courseIds") Collection<Integer> courseIds);

    @Query("""
            select v.lesson.course.id as courseId,
                   coalesce(sum(v.durationSeconds), 0) as estimatedDurationSeconds
            from Video v
            where v.lesson.course.id in :courseIds
              and v.lesson.type = com.ojtsu26.elearning.model.enums.LessonType.VIDEO
              and v.durationSeconds is not null
              and v.durationSeconds > 0
            group by v.lesson.course.id
            """)
    List<CourseDurationProjection> sumDurationSecondsByCourseIds(@Param("courseIds") Collection<Integer> courseIds);
}
