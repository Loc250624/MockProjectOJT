package com.ojtsu26.elearning.repository;

import com.ojtsu26.elearning.model.entity.Question;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.List;

@Repository
public interface QuestionRepository extends JpaRepository<Question, Integer> {
    List<Question> findByQuizIdOrderByDisplayOrderAscIdAsc(Integer quizId);
    List<Question> findByQuizIdAndIdIn(Integer quizId, Collection<Integer> questionIds);

    @Query("select q from Question q where q.quiz.id = :quizId order by function('RAND')")
    List<Question> findRandomByQuizId(@Param("quizId") Integer quizId, Pageable pageable);

}
