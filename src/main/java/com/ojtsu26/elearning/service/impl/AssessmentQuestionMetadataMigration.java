package com.ojtsu26.elearning.service.impl;

import com.ojtsu26.elearning.model.enums.QuestionDifficulty;
import com.ojtsu26.elearning.model.enums.QuestionGenerationSource;
import com.ojtsu26.elearning.model.enums.QuestionReviewStatus;
import com.ojtsu26.elearning.repository.QuestionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Compatibility backfill for databases that were upgraded by Hibernate
 * ddl-auto before the canonical quiz migration was applied.
 */
@Service
@RequiredArgsConstructor
@Slf4j
class AssessmentQuestionMetadataMigration {
    private final QuestionRepository questionRepository;

    @EventListener(ApplicationReadyEvent.class)
    @Transactional
    public void activateLegacyQuizQuestions() {
        int updated = questionRepository.activateLegacyQuizQuestions(
                "GENERAL",
                QuestionDifficulty.MEDIUM,
                QuestionReviewStatus.APPROVED,
                1,
                QuestionGenerationSource.MANUAL);
        if (updated > 0) {
            log.info("Activated and normalized {} legacy quiz question row(s).", updated);
        }
    }
}
