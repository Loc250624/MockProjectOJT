package com.ojtsu26.elearning.repository;

import com.ojtsu26.elearning.model.entity.LessonProgress;
import com.ojtsu26.elearning.repository.projection.AdminStudentEventProjection;
import com.ojtsu26.elearning.repository.projection.EnrollmentProgressSummaryProjection;
import com.ojtsu26.elearning.repository.projection.LessonProgressDetailProjection;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import jakarta.persistence.LockModeType;
import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

@Repository
public interface LessonProgressRepository extends JpaRepository<LessonProgress, Integer> {
    long countByEnrollmentStudentIdAndIsCompletedTrue(Integer studentId);
    List<LessonProgress> findTop5ByEnrollmentStudentIdOrderByCompletedAtDesc(Integer studentId);
    List<LessonProgress> findByEnrollmentId(Integer enrollmentId);
    Optional<LessonProgress> findByEnrollmentIdAndLessonId(Integer enrollmentId, Integer lessonId);
    Optional<LessonProgress> findTopByEnrollmentIdAndLastAccessedAtIsNotNullOrderByLastAccessedAtDesc(Integer enrollmentId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select p from LessonProgress p where p.enrollment.id = :enrollmentId and p.lesson.id = :lessonId")
    Optional<LessonProgress> findByEnrollmentIdAndLessonIdForUpdate(@Param("enrollmentId") Integer enrollmentId,
                                                                    @Param("lessonId") Integer lessonId);

    @Query("select coalesce(avg(e.progressPercentage), 0) from CourseEnrollment e where e.student.id = :studentId")
    BigDecimal averageStudentProgress(Integer studentId);

    @Query("""
            select p.enrollment.id as enrollmentId,
                   count(distinct case
                       when p.isCompleted = true
                        and p.lesson.course.id = :courseId
                        and (p.lesson.type is null or p.lesson.type <> com.ojtsu26.elearning.model.enums.LessonType.QUIZ)
                        and p.lesson.quiz is null
                        and p.lesson.codingassignment is null
                       then p.lesson.id
                       else null
                   end) as completedLessons,
                   max(p.lastAccessedAt) as lastActivityAt
            from LessonProgress p
            where p.enrollment.id in :enrollmentIds
            group by p.enrollment.id
            """)
    List<EnrollmentProgressSummaryProjection> summarizeProgressForEnrollments(@Param("courseId") Integer courseId,
                                                                               @Param("enrollmentIds") List<Integer> enrollmentIds);

    @Query("select max(p.lastAccessedAt) from LessonProgress p where p.lesson.course.id = :courseId and p.enrollment.course.id = :courseId")
    Optional<java.time.LocalDateTime> findLatestActivityAtByCourseId(@Param("courseId") Integer courseId);

    @Query("""
            select count(distinct p.enrollment.student.id)
            from LessonProgress p
            where coalesce(p.lastAccessedAt, p.lastUpdatedAt) >= :from
              and coalesce(p.lastAccessedAt, p.lastUpdatedAt) < :to
            """)
    long countActiveStudentsForAnalytics(@Param("from") java.time.LocalDateTime from,
                                         @Param("to") java.time.LocalDateTime to);

    @Query("""
            select count(distinct p.enrollment.student.id)
            from LessonProgress p
            where p.enrollment.course.instructor.id = :teacherId
              and coalesce(p.lastAccessedAt, p.lastUpdatedAt) >= :from
              and coalesce(p.lastAccessedAt, p.lastUpdatedAt) < :to
            """)
    long countTeacherActiveStudentsForAnalytics(@Param("teacherId") Integer teacherId,
                                                @Param("from") java.time.LocalDateTime from,
                                                @Param("to") java.time.LocalDateTime to);

    @Query("""
            select p.enrollment.student.id as studentId,
                   coalesce(p.lastAccessedAt, p.lastUpdatedAt) as occurredAt
            from LessonProgress p
            where coalesce(p.lastAccessedAt, p.lastUpdatedAt) >= :from
              and coalesce(p.lastAccessedAt, p.lastUpdatedAt) < :to
            order by coalesce(p.lastAccessedAt, p.lastUpdatedAt) asc, p.enrollment.student.id asc
            """)
    List<AdminStudentEventProjection> findActiveStudentEventsForAnalytics(@Param("from") java.time.LocalDateTime from,
                                                                          @Param("to") java.time.LocalDateTime to);

    @Query("""
            select l.id as lessonId,
                   l.title as title,
                   l.type as type,
                   l.orderIndex as orderIndex,
                   coalesce(p.isCompleted, false) as completed,
                   p.completedAt as completedAt,
                   p.lastAccessedAt as lastAccessedAt,
                   p.watchedSeconds as watchedSeconds
            from Lesson l
            left join LessonProgress p on p.lesson.id = l.id and p.enrollment.id = :enrollmentId
            where l.course.id = :courseId
            order by l.orderIndex asc, l.id asc
            """)
    List<LessonProgressDetailProjection> findCourseLessonProgressDetail(@Param("courseId") Integer courseId,
                                                                         @Param("enrollmentId") Integer enrollmentId);
}
