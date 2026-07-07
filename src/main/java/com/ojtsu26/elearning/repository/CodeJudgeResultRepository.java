package com.ojtsu26.elearning.repository;

import com.ojtsu26.elearning.model.entity.CodeJudgeResult;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface CodeJudgeResultRepository extends JpaRepository<CodeJudgeResult, Integer> {
    Optional<CodeJudgeResult> findTopBySubmissionIdOrderByCreatedAtDesc(Integer submissionId);
}
