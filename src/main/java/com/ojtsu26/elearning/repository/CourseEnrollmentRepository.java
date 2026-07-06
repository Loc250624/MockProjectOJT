package com.ojtsu26.elearning.repository;

import com.ojtsu26.elearning.model.entity.CourseEnrollment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface CourseEnrollmentRepository extends JpaRepository<CourseEnrollment, Integer> {
    boolean existsByStudentIdAndCourseId(Integer studentId, Integer courseId);
    List<CourseEnrollment> findByStudentId(Integer studentId);
    long countByStudentId(Integer studentId);
    long countByStudentIdAndIsCompletedTrue(Integer studentId);
    long countByCourseId(Integer courseId);
    List<CourseEnrollment> findTop5ByStudentIdOrderByEnrolledAtDesc(Integer studentId);

    @Query("select e from CourseEnrollment e where e.course.instructor.id = :instructorId order by e.enrolledAt desc")
    List<CourseEnrollment> findByInstructorIdOrderByEnrolledAtDesc(Integer instructorId);

    @Query("""
            select count(distinct ce.student.id)
            from CourseEnrollment ce
            where ce.course.instructor.id = :teacherId
            """)
    long countDistinctStudentsByTeacherId(@Param("teacherId") Integer teacherId);
}

