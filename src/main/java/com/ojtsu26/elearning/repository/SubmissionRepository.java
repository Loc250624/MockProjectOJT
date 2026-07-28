package com.ojtsu26.elearning.repository;

import com.ojtsu26.elearning.model.entity.Submission;
import com.ojtsu26.elearning.model.enums.SubmissionStatus;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

@Repository
public interface SubmissionRepository extends JpaRepository<Submission, Integer> {
    long countByStudentId(Integer studentId);

    @Query("select coalesce(avg(s.score), 0) from Submission s where s.student.id = :studentId and s.score is not null")
    BigDecimal averageScoreByStudentId(Integer studentId);

    @Query("""
            select count(distinct s.lesson.id)
            from Submission s
            where s.student.id = :studentId
              and s.lesson.course.id = :courseId
              and s.status in (
                  com.ojtsu26.elearning.model.enums.SubmissionStatus.PASSED,
                  com.ojtsu26.elearning.model.enums.SubmissionStatus.AUTO_GRADED
              )
              and (s.lesson.type = com.ojtsu26.elearning.model.enums.LessonType.QUIZ
                   or s.lesson.quiz is not null)
            """)
    long countPassedRequiredAssessmentLessons(@Param("studentId") Integer studentId,
                                              @Param("courseId") Integer courseId);

    @Query("""
            select count(distinct s.lesson.id)
            from Submission s
            where s.student.id = :studentId
              and s.lesson.course.id = :courseId
              and (s.lesson.type = com.ojtsu26.elearning.model.enums.LessonType.QUIZ
                   or s.lesson.quiz is not null)
            """)
    long countSubmittedAssessmentLessons(@Param("studentId") Integer studentId,
                                         @Param("courseId") Integer courseId);

    Optional<Submission> findTopByStudentIdAndLessonIdAndStatusOrderByIdDesc(
            Integer studentId, Integer lessonId, SubmissionStatus status);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
            select s from Submission s
            where s.student.id = :studentId
              and s.lesson.id = :lessonId
              and s.status = :status
            order by s.id desc
            """)
    List<Submission> findDraftsForUpdate(@Param("studentId") Integer studentId,
                                         @Param("lessonId") Integer lessonId,
                                         @Param("status") SubmissionStatus status);

    Optional<Submission> findTopByStudentIdAndLessonIdOrderByIdDesc(Integer studentId, Integer lessonId);

    List<Submission> findByStudentIdAndLessonIdOrderByIdDesc(Integer studentId, Integer lessonId);

    @Query("""
            select s from Submission s
            where s.id = :submissionId
              and s.student.id = :studentId
              and s.lesson.id = :lessonId
            """)
    Optional<Submission> findOwnedLessonSubmission(@Param("submissionId") Integer submissionId,
                                                   @Param("studentId") Integer studentId,
                                                   @Param("lessonId") Integer lessonId);

    List<Submission> findByStudentIdOrderBySubmittedAtDesc(Integer studentId);
}
