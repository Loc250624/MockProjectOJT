package com.ojtsu26.elearning.service.impl;

import com.ojtsu26.elearning.model.enums.QuestionDifficulty;
import com.ojtsu26.elearning.model.enums.QuestionGenerationSource;
import com.ojtsu26.elearning.model.enums.QuestionReviewStatus;
import com.ojtsu26.elearning.repository.QuestionRepository;
import org.junit.jupiter.api.Test;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

class AssessmentQuestionMetadataMigrationTest {

    @Test
    void normalizesOnlyRepositoryIdentifiedLegacyRows() {
        QuestionRepository repository = mock(QuestionRepository.class);
        AssessmentQuestionMetadataMigration migration =
                new AssessmentQuestionMetadataMigration(repository);

        migration.activateLegacyQuizQuestions();

        verify(repository).activateLegacyQuizQuestions(
                "GENERAL",
                QuestionDifficulty.MEDIUM,
                QuestionReviewStatus.APPROVED,
                1,
                QuestionGenerationSource.MANUAL);
    }
}
