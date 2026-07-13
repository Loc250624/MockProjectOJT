package com.ojtsu26.elearning.repository;

import com.ojtsu26.elearning.model.entity.Testcase;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface TestcaseRepository extends JpaRepository<Testcase, Integer> {
    List<Testcase> findByAssignmentIdOrderByIdAsc(Integer assignmentId);
}
