package com.ojtsu26.elearning.repository;

import com.ojtsu26.elearning.model.entity.Testcase;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface TestcaseRepository extends JpaRepository<Testcase, Integer> {
    List<Testcase> findByAssignmentIdOrderByIdAsc(Integer assignmentId);

    @Query("select t from Testcase t join fetch t.assignment a join fetch a.lesson l join fetch l.course c left join fetch c.instructor where t.id = :testcaseId")
    Optional<Testcase> findByIdWithAssignmentCourse(@Param("testcaseId") Integer testcaseId);
}
