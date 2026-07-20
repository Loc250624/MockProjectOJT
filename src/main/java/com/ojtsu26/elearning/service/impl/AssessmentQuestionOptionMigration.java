package com.ojtsu26.elearning.service.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.ojtsu26.elearning.repository.QuestionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Slf4j
class AssessmentQuestionOptionMigration {
    private final QuestionRepository questionRepository;
    private final ObjectMapper objectMapper;
    @Value("${assessment.question-option-migration.enabled:false}")
    private boolean enabled;

    @EventListener(ApplicationReadyEvent.class)
    @Transactional
    public void normalizeLegacyQuestionOptions() {
        if (!enabled) {
            log.info("Legacy question option migration is disabled; legacy values are normalized lazily on read/write.");
            return;
        }
        long normalized = questionRepository.findAll().stream()
                .filter(question -> AssessmentOptionCodec.normalizeQuestion(question, objectMapper))
                .count();
        log.info("Normalized {} legacy question option row(s).", normalized);
    }
}
