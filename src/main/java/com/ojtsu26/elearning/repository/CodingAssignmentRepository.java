package com.ojtsu26.elearning.repository;

import com.ojtsu26.elearning.model.entity.CodingAssignment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface CodingAssignmentRepository extends JpaRepository<CodingAssignment, Integer> {
    Optional<CodingAssignment> findByLessonId(Integer lessonId);

    @Query("select a from CodingAssignment a join fetch a.lesson l join fetch l.course c where c.id = :courseId order by l.orderIndex asc, a.id asc")
    List<CodingAssignment> findByCourseId(Integer courseId);

    @Query("select a from CodingAssignment a join fetch a.lesson l join fetch l.course c left join fetch c.instructor where a.id = :assignmentId")
    Optional<CodingAssignment> findByIdWithCourse(Integer assignmentId);
}
