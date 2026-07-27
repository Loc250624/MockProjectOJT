package com.ojtsu26.elearning.repository;

import com.ojtsu26.elearning.model.entity.QuizAttemptQuestion;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface QuizAttemptQuestionRepository
        extends JpaRepository<QuizAttemptQuestion, Integer> {

    List<QuizAttemptQuestion>
        findByAttemptIdOrderByDisplayOrderAsc(Integer attemptId);

    @Query("""
        select distinct aq.question.id
        from QuizAttemptQuestion aq
        where aq.attempt.student.id = :studentId
          and aq.attempt.quiz.id = :quizId
          and (:excludedAttemptId is null
               or aq.attempt.id <> :excludedAttemptId)
    """)
    List<Integer> findPreviouslySeenQuestionIds(
        @Param("studentId") Integer studentId,
        @Param("quizId") Integer quizId,
        @Param("excludedAttemptId") Integer excludedAttemptId);

    long countByQuestionId(Integer questionId);
}
