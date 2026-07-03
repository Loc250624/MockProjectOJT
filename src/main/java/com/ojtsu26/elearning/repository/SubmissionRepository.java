package com.ojtsu26.elearning.repository;

import com.ojtsu26.elearning.model.entity.Submission;
import com.ojtsu26.elearning.model.enums.SubmissionStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;

@Repository
public interface SubmissionRepository extends JpaRepository<Submission, Integer> {
    long countByStudentId(Integer studentId);

    @Query("select coalesce(avg(s.score), 0) from Submission s where s.student.id = :studentId and s.score is not null")
    BigDecimal averageScoreByStudentId(Integer studentId);

    @Query("select count(s) from Submission s where s.lesson.course.instructor.id = :instructorId and s.status = :status")
    long countByInstructorIdAndStatus(Integer instructorId, SubmissionStatus status);

    @Query("select count(distinct s.lesson.id) from Submission s where s.student.id = :studentId and s.lesson.course.id = :courseId and s.status = com.ojtsu26.elearning.model.enums.SubmissionStatus.PASSED and (s.lesson.type = com.ojtsu26.elearning.model.enums.LessonType.QUIZ or s.lesson.quiz is not null or s.lesson.codingassignment is not null)")
    long countPassedRequiredAssessmentLessons(@Param("studentId") Integer studentId, @Param("courseId") Integer courseId);
}
