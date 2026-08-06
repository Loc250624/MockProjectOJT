package com.ojtsu26.elearning.repository;

import com.ojtsu26.elearning.model.entity.Lesson;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface LessonRepository extends JpaRepository<Lesson, Integer> {
    List<Lesson> findByCourseIdOrderByOrderIndexAsc(Integer courseId);

    @Query("select l from Lesson l left join fetch l.quiz left join fetch l.video where l.course.id = :courseId and (l.type is null or l.type <> com.ojtsu26.elearning.model.enums.LessonType.RETIRED) order by l.orderIndex asc")
    List<Lesson> findByCourseIdWithAssessmentOrderByOrderIndexAsc(@Param("courseId") Integer courseId);

    @Query("select l from Lesson l left join fetch l.quiz left join fetch l.course c left join fetch c.instructor where l.id = :lessonId and (l.type is null or l.type <> com.ojtsu26.elearning.model.enums.LessonType.RETIRED)")
    Optional<Lesson> findByIdWithCourseAndAssessment(@Param("lessonId") Integer lessonId);

    @Query("select l from Lesson l left join fetch l.video where l.course.id = :courseId and (l.type is null or l.type <> com.ojtsu26.elearning.model.enums.LessonType.RETIRED) order by l.orderIndex asc")
    List<Lesson> findByCourseIdWithVideoOrderByOrderIndexAsc(@Param("courseId") Integer courseId);

    @Query("select l from Lesson l left join fetch l.video where l.id = :lessonId and l.course.id = :courseId")
    Optional<Lesson> findByIdAndCourseIdWithVideo(@Param("lessonId") Integer lessonId, @Param("courseId") Integer courseId);

    @Query("select count(l) from Lesson l where l.course.id = :courseId and (l.type is null or l.type = com.ojtsu26.elearning.model.enums.LessonType.VIDEO) and l.quiz is null")
    long countRequiredContentLessons(@Param("courseId") Integer courseId);

    @Query("select count(distinct l.id) from Lesson l join LessonProgress p on p.lesson.id = l.id where l.course.id = :courseId and p.enrollment.id = :enrollmentId and p.isCompleted = true and (l.type is null or l.type = com.ojtsu26.elearning.model.enums.LessonType.VIDEO) and l.quiz is null")
    long countCompletedRequiredContentLessons(@Param("courseId") Integer courseId, @Param("enrollmentId") Integer enrollmentId);

    @Query("select count(l) from Lesson l where l.course.id = :courseId and (l.type = com.ojtsu26.elearning.model.enums.LessonType.QUIZ or l.quiz is not null)")
    long countRequiredAssessmentLessons(@Param("courseId") Integer courseId);
}
