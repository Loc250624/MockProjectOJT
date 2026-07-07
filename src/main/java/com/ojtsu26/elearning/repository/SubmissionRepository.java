package com.ojtsu26.elearning.repository;

import com.ojtsu26.elearning.model.entity.Submission;
import com.ojtsu26.elearning.model.enums.SubmissionStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

@Repository
public interface SubmissionRepository extends JpaRepository<Submission, Integer> {
    @Query("""
            select count(s)
            from Submission s
            where s.lesson.course.instructor.id = :teacherId
            and s.status = :status
            """)
    long countByTeacherIdAndStatus(@Param("teacherId") Integer teacherId, @Param("status") SubmissionStatus status);

    long countByStudentId(Integer studentId);

    @Query("select coalesce(avg(s.score), 0) from Submission s where s.student.id = :studentId and s.score is not null")
    BigDecimal averageScoreByStudentId(Integer studentId);

    @Query("select count(s) from Submission s where s.lesson.course.instructor.id = :instructorId and s.status = :status")
    long countByInstructorIdAndStatus(Integer instructorId, SubmissionStatus status);

    @Query("select count(distinct s.lesson.id) from Submission s where s.student.id = :studentId and s.lesson.course.id = :courseId and s.status = com.ojtsu26.elearning.model.enums.SubmissionStatus.PASSED and (s.lesson.type = com.ojtsu26.elearning.model.enums.LessonType.QUIZ or s.lesson.quiz is not null or s.lesson.codingassignment is not null)")
    long countPassedRequiredAssessmentLessons(@Param("studentId") Integer studentId, @Param("courseId") Integer courseId);

    @Query("select count(distinct s.lesson.id) from Submission s where s.student.id = :studentId and s.lesson.course.id = :courseId and (s.lesson.type = com.ojtsu26.elearning.model.enums.LessonType.QUIZ or s.lesson.quiz is not null or s.lesson.codingassignment is not null)")
    long countSubmittedAssessmentLessons(@Param("studentId") Integer studentId, @Param("courseId") Integer courseId);

    List<Submission> findByStudentIdOrderBySubmittedAtDesc(Integer studentId);

    Optional<Submission> findTopByAssignmentIdAndStudentIdOrderByUpdatedAtDesc(Integer assignmentId, Integer studentId);

    @Query("select s from Submission s join fetch s.assignment a join fetch a.lesson l join fetch l.course c left join fetch c.instructor where s.id = :submissionId")
    Optional<Submission> findByIdWithAssignmentCourse(@Param("submissionId") Integer submissionId);

    @Query("select s from Submission s join fetch s.student join fetch s.assignment a join fetch a.lesson l where a.id = :assignmentId order by s.submittedAt desc")
    List<Submission> findByAssignmentIdWithStudent(@Param("assignmentId") Integer assignmentId);
}
