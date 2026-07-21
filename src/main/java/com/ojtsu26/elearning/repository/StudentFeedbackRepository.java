package com.ojtsu26.elearning.repository;

import com.ojtsu26.elearning.model.entity.StudentFeedback;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface StudentFeedbackRepository extends JpaRepository<StudentFeedback, Integer> {

    @EntityGraph(attributePaths = "student")
    Page<StudentFeedback> findAllByOrderByCreatedAtDesc(Pageable pageable);

    @EntityGraph(attributePaths = "student")
    @Query("select feedback from StudentFeedback feedback where feedback.id = :id")
    Optional<StudentFeedback> findWithStudentById(@Param("id") Integer id);
}
