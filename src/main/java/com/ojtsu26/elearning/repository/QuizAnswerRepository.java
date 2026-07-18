package com.ojtsu26.elearning.repository;

import com.ojtsu26.elearning.model.entity.QuizAnswer;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface QuizAnswerRepository extends JpaRepository<QuizAnswer, Integer> {
    long countByQuestionId(Integer questionId);

    List<QuizAnswer> findByAttemptId(Integer attemptId);

    List<QuizAnswer> findBySubmissionId(Integer submissionId);

    Optional<QuizAnswer> findByAttemptIdAndQuestionId(Integer attemptId, Integer questionId);
}
