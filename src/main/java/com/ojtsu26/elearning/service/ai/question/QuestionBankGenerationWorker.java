package com.ojtsu26.elearning.service.ai.question;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.ojtsu26.elearning.model.entity.Question;
import com.ojtsu26.elearning.model.entity.QuestionGenerationJob;
import com.ojtsu26.elearning.model.enums.*;
import com.ojtsu26.elearning.repository.QuestionGenerationJobRepository;
import com.ojtsu26.elearning.repository.QuestionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Component
@RequiredArgsConstructor
public class QuestionBankGenerationWorker {
    private final QuestionGenerationJobRepository jobRepository;
    private final QuestionRepository questionRepository;
    private final QuestionGenerationProvider provider;
    private final QuestionValidationService validationService;
    private final ObjectMapper objectMapper;

    @Async("questionGenerationExecutor")
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void generate(QuestionGenerationRequestedEvent event) {
        QuestionGenerationJob job = jobRepository.findById(event.jobId()).orElse(null);
        if (job == null || job.getStatus() != QuestionGenerationJobStatus.PENDING) {
            return;
        }
        job.setStatus(QuestionGenerationJobStatus.RUNNING);
        job.setStartedAt(LocalDateTime.now());
        job.setProviderName(provider.providerName());
        job.setModelName(provider.modelName());
        try {
            GeneratedQuestionBatch batch = provider.generate(event.request());
            List<String> existing = questionRepository
                    .findByQuizIdOrderByDisplayOrderAscIdAsc(job.getQuiz().getId())
                    .stream()
                    .map(Question::getQuestionText)
                    .toList();
            List<GeneratedQuestion> generated = validationService.validate(
                    batch, existing, job.getRequestedCount());
            int displayOrder = questionRepository
                    .findByQuizIdOrderByDisplayOrderAscIdAsc(job.getQuiz().getId()).size();
            for (GeneratedQuestion item : generated) {
                questionRepository.save(Question.builder()
                        .quiz(job.getQuiz())
                        .questionText(item.questionText().trim())
                        .optionsJson(writeJson(item.options()))
                        .correctAnswer(item.correctAnswer().trim())
                        .questionType(QuestionType.SINGLE_CHOICE)
                        .points(BigDecimal.ONE)
                        .displayOrder(++displayOrder)
                        .topicCode(item.topicCode() == null || item.topicCode().isBlank()
                                ? job.getTopicCode()
                                : item.topicCode().trim().toUpperCase(java.util.Locale.ROOT))
                        .difficulty(item.difficulty() == null ? job.getDifficulty() : item.difficulty())
                        .reviewStatus(QuestionReviewStatus.DRAFT)
                        .version(1)
                        .active(true)
                        .generationSource(QuestionGenerationSource.AI)
                        .build());
            }
            job.setGeneratedCount(generated.size());
            job.setStatus(QuestionGenerationJobStatus.COMPLETED);
        } catch (Exception ex) {
            job.setStatus(QuestionGenerationJobStatus.FAILED);
            job.setErrorMessage(safeError(ex));
        }
        job.setCompletedAt(LocalDateTime.now());
    }

    private String writeJson(Object value) {
        try {
            return objectMapper.writeValueAsString(value);
        } catch (JsonProcessingException ex) {
            throw new IllegalArgumentException("Generated options are invalid", ex);
        }
    }

    private String safeError(Exception ex) {
        String message = ex.getMessage();
        if (message == null || message.isBlank()) {
            return "Question generation failed";
        }
        return message.length() > 1000 ? message.substring(0, 1000) : message;
    }
}
