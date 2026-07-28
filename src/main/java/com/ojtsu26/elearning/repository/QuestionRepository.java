package com.ojtsu26.elearning.repository;

import com.ojtsu26.elearning.model.entity.Question;
import com.ojtsu26.elearning.model.enums.QuestionDifficulty;
import com.ojtsu26.elearning.model.enums.QuestionGenerationSource;
import com.ojtsu26.elearning.model.enums.QuestionReviewStatus;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.List;

@Repository
public interface QuestionRepository extends JpaRepository<Question, Integer> {
    List<Question> findByQuizIdOrderByDisplayOrderAscIdAsc(Integer quizId);

    List<Question> findByQuizIdAndReviewStatusAndActiveTrueOrderByIdAsc(
            Integer quizId,
            QuestionReviewStatus reviewStatus);

    long countByQuizIdAndTopicCodeIgnoreCaseAndDifficultyAndReviewStatusAndActiveTrue(
            Integer quizId,
            String topicCode,
            QuestionDifficulty difficulty,
            QuestionReviewStatus reviewStatus);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("""
            update Question q
               set q.topicCode = :topicCode,
                   q.difficulty = :difficulty,
                   q.reviewStatus = :reviewStatus,
                   q.version = :version,
                   q.active = true,
                   q.generationSource = :generationSource
             where (q.version is null or q.version <= 0)
               and (q.topicCode is null or trim(q.topicCode) = '')
            """)
    int activateLegacyQuizQuestions(
            @Param("topicCode") String topicCode,
            @Param("difficulty") QuestionDifficulty difficulty,
            @Param("reviewStatus") QuestionReviewStatus reviewStatus,
            @Param("version") Integer version,
            @Param("generationSource") QuestionGenerationSource generationSource);

    List<Question> findByQuizIdAndIdIn(Integer quizId, Collection<Integer> questionIds);

    @Query("select q from Question q where q.quiz.id = :quizId order by function('RAND')")
    List<Question> findRandomByQuizId(@Param("quizId") Integer quizId, Pageable pageable);
}
