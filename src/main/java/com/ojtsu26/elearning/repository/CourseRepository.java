package com.ojtsu26.elearning.repository;

import com.ojtsu26.elearning.model.entity.Course;
import com.ojtsu26.elearning.model.enums.CourseStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface CourseRepository extends JpaRepository<Course, Integer> {
    long countByInstructorId(Integer instructorId);
    long countByInstructorIdAndStatus(Integer instructorId, CourseStatus status);
    long countByStatus(CourseStatus status);
}
