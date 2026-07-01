package com.ojtsu26.elearning.repository;

import com.ojtsu26.elearning.model.entity.Course;
import com.ojtsu26.elearning.model.enums.CourseStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.util.List;

@Repository
public interface CourseRepository extends JpaRepository<Course, Integer> {
    long countByInstructorId(Integer instructorId);
    long countByInstructorIdAndStatus(Integer instructorId, CourseStatus status);
    long countByStatus(CourseStatus status);
    List<Course> findTop5ByInstructorIdOrderByCreatedAtDesc(Integer instructorId);
    List<Course> findTop5ByOrderByCreatedAtDesc();

    @Query("select count(distinct e.student.id) from CourseEnrollment e where e.course.instructor.id = :instructorId")
    long countDistinctStudentsByInstructorId(Integer instructorId);

    @Query("select count(e) from CourseEnrollment e where e.course.instructor.id = :instructorId")
    long countEnrollmentsByInstructorId(Integer instructorId);

    @Query("select coalesce(avg(e.progressPercentage), 0) from CourseEnrollment e where e.course.id = :courseId")
    BigDecimal averageProgressByCourseId(Integer courseId);
}
