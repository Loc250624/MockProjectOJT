package com.ojtsu26.elearning.repository;

import com.ojtsu26.elearning.model.entity.Question;
import com.ojtsu26.elearning.model.enums.QuestionDifficulty;
import com.ojtsu26.elearning.model.enums.QuestionGenerationSource;
import com.ojtsu26.elearning.model.enums.QuestionReviewStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface QuestionRepository extends JpaRepository<Question, Integer> {
    List<Question> findByQuizIdOrderByDisplayOrderAscIdAsc(Integer quizId);
    List<Question> findByAssignmentIdOrderByDisplayOrderAscIdAsc(Integer assignmentId);

    List<Question> findByQuizIdAndReviewStatusAndActiveTrueOrderByIdAsc(
            Integer quizId,
            QuestionReviewStatus reviewStatus);

    long countByQuizIdAndTopicCodeIgnoreCaseAndDifficultyAndReviewStatusAndActiveTrue(
            Integer quizId,
            String topicCode,
            QuestionDifficulty difficulty,
            QuestionReviewStatus reviewStatus);

    /**
     * Hibernate ddl-auto initializes newly added NOT NULL columns with zero/first
     * enum values when an old Questions table already contains rows. The
     * version=0 + blank-topic pair identifies those legacy rows without touching
     * questions that a teacher intentionally archived.
     */
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
}
