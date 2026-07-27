package com.ojtsu26.elearning.service.quiz;

import com.ojtsu26.elearning.dto.assessment.AssessmentDtos.BlueprintBucketView;
import com.ojtsu26.elearning.dto.assessment.AssessmentDtos.BlueprintItemPayload;
import com.ojtsu26.elearning.dto.assessment.AssessmentDtos.QuizReadinessView;
import com.ojtsu26.elearning.exception.BusinessException;
import com.ojtsu26.elearning.exception.ErrorCode;
import com.ojtsu26.elearning.model.entity.Question;
import com.ojtsu26.elearning.model.entity.Quiz;
import com.ojtsu26.elearning.model.entity.QuizBlueprintItem;
import com.ojtsu26.elearning.model.entity.User;
import com.ojtsu26.elearning.model.enums.QuestionDifficulty;
import com.ojtsu26.elearning.model.enums.QuestionGenerationSource;
import com.ojtsu26.elearning.model.enums.QuestionReviewStatus;
import com.ojtsu26.elearning.repository.QuestionRepository;
import com.ojtsu26.elearning.repository.QuizBlueprintItemRepository;
import com.ojtsu26.elearning.repository.QuizRepository;
import com.ojtsu26.elearning.service.CurrentUserService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;

@Service
@RequiredArgsConstructor
public class TeacherQuestionBankService {
    private final QuizRepository quizRepository;
    private final QuestionRepository questionRepository;
    private final QuizBlueprintItemRepository blueprintRepository;
    private final QuizQuestionAssignmentService assignmentService;
    private final CurrentUserService currentUserService;

    @Transactional(readOnly = true)
    public List<QuestionBankItem> list(Integer quizId,
                                       QuestionReviewStatus status,
                                       String topic,
                                       QuestionDifficulty difficulty,
                                       QuestionGenerationSource source) {
        requireOwnedQuiz(quizId);
        String normalizedTopic = topic == null || topic.isBlank()
                ? null
                : StratifiedQuestionSampler.normalizeTopic(topic);
        return questionRepository.findByQuizIdOrderByDisplayOrderAscIdAsc(quizId).stream()
                .filter(question -> status == null || question.getReviewStatus() == status)
                .filter(question -> normalizedTopic == null || normalizedTopic.equals(
                        StratifiedQuestionSampler.normalizeTopic(question.getTopicCode())))
                .filter(question -> difficulty == null || question.getDifficulty() == difficulty)
                .filter(question -> source == null || question.getGenerationSource() == source)
                .map(this::toItem)
                .toList();
    }

    @Transactional
    public QuestionBankItem approve(Integer questionId) {
        Question question = requireOwnedQuestion(questionId);
        validateApprovable(question);
        question.setReviewStatus(QuestionReviewStatus.APPROVED);
        question.setActive(true);
        return toItem(question);
    }

    @Transactional
    public QuestionBankItem reject(Integer questionId) {
        Question question = requireOwnedQuestion(questionId);
        question.setReviewStatus(QuestionReviewStatus.REJECTED);
        question.setActive(false);
        return toItem(question);
    }

    @Transactional
    public QuestionBankItem archive(Integer questionId) {
        Question question = requireOwnedQuestion(questionId);
        question.setReviewStatus(QuestionReviewStatus.ARCHIVED);
        question.setActive(false);
        return toItem(question);
    }

    @Transactional
    public List<QuestionBankItem> bulkApprove(Integer quizId, List<Integer> questionIds) {
        requireOwnedQuiz(quizId);
        Set<Integer> requested = new LinkedHashSet<>(Optional.ofNullable(questionIds).orElse(List.of()));
        if (requested.isEmpty()) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "At least one question is required");
        }
        List<Question> questions = questionRepository.findAllById(requested);
        if (questions.size() != requested.size()
                || questions.stream().anyMatch(question -> question.getQuiz() == null
                || !Objects.equals(question.getQuiz().getId(), quizId))) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "Bulk selection contains an invalid question");
        }
        questions.forEach(question -> {
            validateApprovable(question);
            question.setReviewStatus(QuestionReviewStatus.APPROVED);
            question.setActive(true);
        });
        return questions.stream().map(this::toItem).toList();
    }

    @Transactional
    public QuizReadinessView configureBlueprint(Integer quizId, List<BlueprintItemPayload> payloads) {
        Quiz quiz = requireOwnedQuiz(quizId);
        List<BlueprintItemPayload> items = Optional.ofNullable(payloads).orElse(List.of());
        if (items.isEmpty()) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "Quiz blueprint requires at least one bucket");
        }
        Set<String> keys = new HashSet<>();
        List<QuizBlueprintItem> entities = new ArrayList<>();
        for (int index = 0; index < items.size(); index++) {
            BlueprintItemPayload item = items.get(index);
            if (item == null || item.getDifficulty() == null
                    || item.getQuestionCount() == null || item.getQuestionCount() <= 0) {
                throw new BusinessException(ErrorCode.BAD_REQUEST, "Every blueprint bucket must be complete");
            }
            String topic = StratifiedQuestionSampler.normalizeTopic(item.getTopicCode());
            String key = topic + "\u0000" + item.getDifficulty();
            if (!keys.add(key)) {
                throw new BusinessException(ErrorCode.BAD_REQUEST, "Blueprint contains a duplicate bucket");
            }
            entities.add(QuizBlueprintItem.builder()
                    .quiz(quiz)
                    .topicCode(topic)
                    .difficulty(item.getDifficulty())
                    .questionCount(item.getQuestionCount())
                    .displayOrder(Objects.requireNonNullElse(item.getDisplayOrder(), index + 1))
                    .build());
        }
        blueprintRepository.deleteByQuizId(quizId);
        blueprintRepository.flush();
        blueprintRepository.saveAll(entities);
        return readiness(quizId);
    }

    @Transactional(readOnly = true)
    public QuizReadinessView readiness(Integer quizId) {
        requireOwnedQuiz(quizId);
        QuizQuestionAssignmentService.Readiness readiness = assignmentService.readiness(quizId);
        QuizReadinessView view = new QuizReadinessView();
        view.setReady(readiness.ready());
        view.setBuckets(readiness.buckets().stream().map(bucket -> {
            BlueprintBucketView item = new BlueprintBucketView();
            item.setTopicCode(bucket.topicCode());
            item.setDifficulty(bucket.difficulty());
            item.setRequired(bucket.required());
            item.setAvailable(bucket.available());
            item.setReady(bucket.ready());
            return item;
        }).toList());
        return view;
    }

    private Quiz requireOwnedQuiz(Integer quizId) {
        Quiz quiz = quizRepository.findByIdWithCourse(quizId)
                .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND, "Quiz not found"));
        User teacher = currentUserService.getCurrentUser();
        if (quiz.getLesson() == null || quiz.getLesson().getCourse() == null
                || quiz.getLesson().getCourse().getInstructor() == null
                || !Objects.equals(quiz.getLesson().getCourse().getInstructor().getId(), teacher.getId())) {
            throw new BusinessException(ErrorCode.ACCESS_DENIED, "Quiz access denied");
        }
        return quiz;
    }

    private Question requireOwnedQuestion(Integer questionId) {
        Question question = questionRepository.findById(questionId)
                .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND, "Question not found"));
        if (question.getQuiz() == null) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "Question is not part of a quiz");
        }
        requireOwnedQuiz(question.getQuiz().getId());
        return question;
    }

    private void validateApprovable(Question question) {
        if (question.getQuestionText() == null || question.getQuestionText().isBlank()
                || question.getOptionsJson() == null || question.getOptionsJson().isBlank()
                || question.getCorrectAnswer() == null || question.getCorrectAnswer().isBlank()) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "Question is incomplete and cannot be approved");
        }
    }

    private QuestionBankItem toItem(Question question) {
        return new QuestionBankItem(
                question.getId(),
                question.getQuestionText(),
                question.getOptionsJson(),
                question.getCorrectAnswer(),
                StratifiedQuestionSampler.normalizeTopic(question.getTopicCode()),
                Objects.requireNonNullElse(question.getDifficulty(), QuestionDifficulty.MEDIUM),
                Objects.requireNonNullElse(question.getReviewStatus(), QuestionReviewStatus.APPROVED),
                Objects.requireNonNullElse(question.getVersion(), 1),
                !Boolean.FALSE.equals(question.getActive()),
                Objects.requireNonNullElse(
                        question.getGenerationSource(), QuestionGenerationSource.MANUAL));
    }

    public record QuestionBankItem(Integer id,
                                   String questionText,
                                   String optionsJson,
                                   String correctAnswer,
                                   String topicCode,
                                   QuestionDifficulty difficulty,
                                   QuestionReviewStatus reviewStatus,
                                   Integer version,
                                   Boolean active,
                                   QuestionGenerationSource generationSource) {
    }
}
