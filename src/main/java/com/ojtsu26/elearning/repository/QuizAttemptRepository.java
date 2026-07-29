package com.ojtsu26.elearning.repository;

import com.ojtsu26.elearning.model.entity.QuizAttempt;
import com.ojtsu26.elearning.model.enums.QuizAttemptStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.math.BigDecimal;

@Repository
public interface QuizAttemptRepository extends JpaRepository<QuizAttempt, Integer> {
    long countByQuizId(Integer quizId);

    long countByQuizIdAndStudentIdAndStatusIn(Integer quizId, Integer studentId, List<QuizAttemptStatus> statuses);

    Optional<QuizAttempt> findTopByQuizIdAndStudentIdAndStatusOrderByStartedAtDesc(Integer quizId, Integer studentId, QuizAttemptStatus status);

    Optional<QuizAttempt> findTopByQuizIdAndStudentIdOrderByStartedAtDesc(Integer quizId, Integer studentId);

    List<QuizAttempt> findByStudentIdOrderByStartedAtDesc(Integer studentId);

    @Query("select a from QuizAttempt a join fetch a.quiz q join fetch q.lesson l join fetch l.course c where a.id = :attemptId")
    Optional<QuizAttempt> findByIdWithQuizCourse(@Param("attemptId") Integer attemptId);

    @Query("""
            select distinct l.id
            from QuizAttempt a
            join a.quiz q
            join q.lesson l
            where a.student.id = :studentId
              and l.course.id = :courseId
              and a.status = com.ojtsu26.elearning.model.enums.QuizAttemptStatus.GRADED
              and a.score is not null
              and a.score >= coalesce(q.passingScore, :defaultPassingScore)
            """)
    List<Integer> findPassedQuizLessonIds(
            @Param("studentId") Integer studentId,
            @Param("courseId") Integer courseId,
            @Param("defaultPassingScore") BigDecimal defaultPassingScore);
}
