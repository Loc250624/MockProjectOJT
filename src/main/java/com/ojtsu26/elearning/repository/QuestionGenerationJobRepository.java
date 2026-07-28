package com.ojtsu26.elearning.repository;

import com.ojtsu26.elearning.model.entity.QuestionGenerationJob;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface QuestionGenerationJobRepository extends JpaRepository<QuestionGenerationJob, Integer> {
    Optional<QuestionGenerationJob> findByIdAndRequestedById(Integer id, Integer requestedById);
}
