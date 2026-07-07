package com.ojtsu26.elearning.repository;

import com.ojtsu26.elearning.model.entity.GradeFeedback;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface GradeFeedbackRepository extends JpaRepository<GradeFeedback, Integer> {
    Optional<GradeFeedback> findTopBySubmissionIdOrderByGradedAtDesc(Integer submissionId);
}
