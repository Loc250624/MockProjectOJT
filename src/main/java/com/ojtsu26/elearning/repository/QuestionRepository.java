package com.ojtsu26.elearning.repository;

import com.ojtsu26.elearning.model.entity.Question;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface QuestionRepository extends JpaRepository<Question, Integer> {
    List<Question> findByQuizIdOrderByDisplayOrderAscIdAsc(Integer quizId);
}
