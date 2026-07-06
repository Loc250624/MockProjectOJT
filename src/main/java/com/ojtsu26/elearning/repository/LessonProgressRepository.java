package com.ojtsu26.elearning.repository;

import com.ojtsu26.elearning.model.entity.LessonProgress;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.util.List;

@Repository
public interface LessonProgressRepository extends JpaRepository<LessonProgress, Integer> {
    long countByEnrollmentStudentIdAndIsCompletedTrue(Integer studentId);
    List<LessonProgress> findTop5ByEnrollmentStudentIdOrderByCompletedAtDesc(Integer studentId);

    @Query("select coalesce(avg(e.progressPercentage), 0) from CourseEnrollment e where e.student.id = :studentId")
    BigDecimal averageStudentProgress(Integer studentId);
}
