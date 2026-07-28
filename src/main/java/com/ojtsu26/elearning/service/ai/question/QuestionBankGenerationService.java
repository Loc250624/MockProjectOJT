package com.ojtsu26.elearning.service.ai.question;

import com.ojtsu26.elearning.exception.BusinessException;
import com.ojtsu26.elearning.exception.ErrorCode;
import com.ojtsu26.elearning.model.entity.QuestionGenerationJob;
import com.ojtsu26.elearning.model.entity.Quiz;
import com.ojtsu26.elearning.model.entity.User;
import com.ojtsu26.elearning.model.enums.QuestionGenerationJobStatus;
import com.ojtsu26.elearning.repository.QuestionGenerationJobRepository;
import com.ojtsu26.elearning.repository.QuizRepository;
import com.ojtsu26.elearning.service.CurrentUserService;
import com.ojtsu26.elearning.service.quiz.StratifiedQuestionSampler;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Objects;

@Service
@RequiredArgsConstructor
public class QuestionBankGenerationService {
    private final QuizRepository quizRepository;
    private final QuestionGenerationJobRepository jobRepository;
    private final CurrentUserService currentUserService;
    private final ApplicationEventPublisher eventPublisher;

    @Transactional
    public JobView start(Integer quizId, QuestionGenerationRequest request) {
        Quiz quiz = requireOwnedQuiz(quizId);
        User teacher = currentUserService.getCurrentUser();
        QuestionGenerationRequest normalized = new QuestionGenerationRequest(
                request.lessonContent().trim(),
                StratifiedQuestionSampler.normalizeTopic(request.topicCode()),
                request.difficulty(),
                request.questionCount());
        QuestionGenerationJob job = jobRepository.save(QuestionGenerationJob.builder()
                .quiz(quiz)
                .requestedBy(teacher)
                .status(QuestionGenerationJobStatus.PENDING)
                .requestedCount(normalized.questionCount())
                .generatedCount(0)
                .topicCode(normalized.topicCode())
                .difficulty(normalized.difficulty())
                .lessonContentSnapshot(normalized.lessonContent())
                .build());
        eventPublisher.publishEvent(new QuestionGenerationRequestedEvent(job.getId(), normalized));
        return toView(job);
    }

    @Transactional(readOnly = true)
    public JobView get(Integer jobId) {
        User teacher = currentUserService.getCurrentUser();
        QuestionGenerationJob job = jobRepository.findByIdAndRequestedById(jobId, teacher.getId())
                .orElseThrow(() -> new BusinessException(
                        ErrorCode.NOT_FOUND, "Question generation job not found"));
        return toView(job);
    }

    private Quiz requireOwnedQuiz(Integer quizId) {
        Quiz quiz = quizRepository.findByIdWithCourse(quizId)
                .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND, "Quiz not found"));
        User teacher = currentUserService.getCurrentUser();
        if (quiz.getLesson() == null || quiz.getLesson().getCourse() == null
                || quiz.getLesson().getCourse().getInstructor() == null
                || !Objects.equals(
                quiz.getLesson().getCourse().getInstructor().getId(), teacher.getId())) {
            throw new BusinessException(ErrorCode.ACCESS_DENIED, "Quiz access denied");
        }
        return quiz;
    }

    private JobView toView(QuestionGenerationJob job) {
        return new JobView(
                job.getId(),
                job.getQuiz().getId(),
                job.getStatus(),
                job.getRequestedCount(),
                job.getGeneratedCount(),
                job.getTopicCode(),
                job.getDifficulty(),
                job.getProviderName(),
                job.getModelName(),
                job.getErrorMessage(),
                job.getCreatedAt(),
                job.getStartedAt(),
                job.getCompletedAt());
    }

    public record JobView(Integer id,
                          Integer quizId,
                          QuestionGenerationJobStatus status,
                          Integer requestedCount,
                          Integer generatedCount,
                          String topicCode,
                          com.ojtsu26.elearning.model.enums.QuestionDifficulty difficulty,
                          String provider,
                          String model,
                          String errorMessage,
                          java.time.LocalDateTime createdAt,
                          java.time.LocalDateTime startedAt,
                          java.time.LocalDateTime completedAt) {
    }
}
