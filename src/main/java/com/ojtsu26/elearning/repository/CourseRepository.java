package com.ojtsu26.elearning.repository;

import com.ojtsu26.elearning.model.entity.Course;
import com.ojtsu26.elearning.model.enums.CourseStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

import com.ojtsu26.elearning.model.enums.CourseStatus;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.data.domain.Sort;

@Repository
public interface CourseRepository extends JpaRepository<Course, Integer> {
    List<Course> findByInstructorId(Integer instructorId);
    List<Course> findByStatus(CourseStatus status);
    List<Course> findByInstructorIdAndStatus(Integer instructorId, CourseStatus status);

    @Query("SELECT c FROM Course c WHERE c.status = com.ojtsu26.elearning.model.enums.CourseStatus.APPROVED " +
           "AND (:categoryId IS NULL OR c.category.id = :categoryId) " +
           "AND (:keyword IS NULL OR LOWER(c.title) LIKE LOWER(CONCAT('%', :keyword, '%')) OR LOWER(c.description) LIKE LOWER(CONCAT('%', :keyword, '%')))")
    List<Course> findApprovedCourses(@Param("categoryId") Integer categoryId, @Param("keyword") String keyword, Sort sort);

    long countByInstructorId(Integer instructorId);
    long countByInstructorIdAndStatus(Integer instructorId, CourseStatus status);
    long countByStatus(CourseStatus status);
}
