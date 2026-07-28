package com.ojtsu26.elearning.repository;

import com.ojtsu26.elearning.model.entity.QuizBlueprintItem;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface QuizBlueprintItemRepository extends JpaRepository<QuizBlueprintItem, Integer> {
    List<QuizBlueprintItem> findByQuizIdOrderByDisplayOrderAscIdAsc(Integer quizId);
    void deleteByQuizId(Integer quizId);
}
