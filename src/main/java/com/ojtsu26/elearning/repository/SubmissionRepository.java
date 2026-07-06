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
}
