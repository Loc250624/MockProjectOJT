package com.ojtsu26.elearning.repository;

import com.ojtsu26.elearning.model.entity.CourseEnrollment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface CourseEnrollmentRepository extends JpaRepository<CourseEnrollment, Integer> {
    boolean existsByStudentIdAndCourseId(Integer studentId, Integer courseId);
    java.util.List<CourseEnrollment> findByStudentId(Integer studentId);

    @Query("""
            select count(distinct ce.student.id)
            from CourseEnrollment ce
            where ce.course.instructor.id = :teacherId
            """)
    long countDistinctStudentsByTeacherId(@Param("teacherId") Integer teacherId);
}

