package com.ojtsu26.elearning.service.quiz;

import com.ojtsu26.elearning.exception.BusinessException;
import com.ojtsu26.elearning.exception.ErrorCode;
import com.ojtsu26.elearning.model.entity.Question;
import com.ojtsu26.elearning.model.entity.Quiz;
import com.ojtsu26.elearning.model.entity.QuizAttempt;
import com.ojtsu26.elearning.model.entity.QuizAttemptQuestion;
import com.ojtsu26.elearning.model.enums.QuestionDifficulty;
import com.ojtsu26.elearning.model.enums.QuestionReviewStatus;
import com.ojtsu26.elearning.repository.QuestionRepository;
import com.ojtsu26.elearning.repository.QuizAttemptQuestionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class QuizQuestionAssignmentService {
    private final QuestionRepository questionRepository;
    private final QuizAttemptQuestionRepository attemptQuestionRepository;
    private final StratifiedQuestionSampler sampler;

    @Transactional
    public List<QuizAttemptQuestion> assign(QuizAttempt attempt) {
        List<QuizAttemptQuestion> existing =
                attemptQuestionRepository.findByAttemptIdOrderByDisplayOrderAsc(attempt.getId());
        if (!existing.isEmpty()) {
            return existing;
        }

        Quiz quiz = attempt.getQuiz();
        List<Question> bank = eligibleBank(quiz.getId());
        if (bank.size() < QuizRules.QUESTIONS_PER_ATTEMPT) {
            throw new BusinessException(
                    ErrorCode.QUIZ_INSUFFICIENT_QUESTIONS,
                    "Quiz requires at least 10 valid active questions before an attempt can start.");
        }
        Map<Integer, Question> questionsById = bank.stream()
                .collect(Collectors.toMap(Question::getId, Function.identity()));
        List<StratifiedQuestionSampler.Candidate> candidates = bank.stream()
                .map(question -> new StratifiedQuestionSampler.Candidate(
                        question.getId(),
                        question.getTopicCode(),
                        question.getDifficulty(),
                        true,
                        true))
                .toList();
        Set<Integer> seen = attemptQuestionRepository.findPreviouslySeenQuestionIds(
                attempt.getStudent().getId(), quiz.getId());
        List<StratifiedQuestionSampler.Candidate> selected;
        try {
            selected = sampler.selectAny(
                    candidates,
                    QuizRules.QUESTIONS_PER_ATTEMPT,
                    seen,
                    usageCounts(quiz.getId()));
        } catch (StratifiedQuestionSampler.InsufficientQuestionBankException exception) {
            throw new BusinessException(
                    ErrorCode.QUIZ_INSUFFICIENT_QUESTIONS,
                    "Quiz requires at least 10 valid active questions before an attempt can start.");
        }

        List<QuizAttemptQuestion> assignments = new ArrayList<>();
        BigDecimal total = BigDecimal.ZERO;
        for (int index = 0; index < selected.size(); index++) {
            Question question = questionsById.get(selected.get(index).id());
            BigDecimal points = positivePoints(question.getPoints());
            total = total.add(points);
            assignments.add(QuizAttemptQuestion.builder()
                    .attempt(attempt)
                    .question(question)
                    .displayOrder(index + 1)
                    .pointsSnapshot(points)
                    .questionVersion(Objects.requireNonNullElse(question.getVersion(), 1))
                    .questionTextSnapshot(QuizQuestionTextNormalizer.stripCheckpointPrefix(
                            question.getQuestionText()))
                    .optionsJsonSnapshot(question.getOptionsJson())
                    .correctAnswerSnapshot(question.getCorrectAnswer())
                    .questionTypeSnapshot(question.getQuestionType() == null
                            ? null
                            : question.getQuestionType().name())
                    .topicSnapshot(StratifiedQuestionSampler.normalizeTopic(question.getTopicCode()))
                    .difficultySnapshot(Objects.requireNonNullElse(
                            question.getDifficulty(), QuestionDifficulty.MEDIUM).name())
                    .build());
        }
        attempt.setTotalPoints(total);
        return attemptQuestionRepository.saveAll(assignments);
    }

    @Transactional(readOnly = true)
    public List<QuizAttemptQuestion> loadAssigned(Integer attemptId) {
        return attemptQuestionRepository.findByAttemptIdOrderByDisplayOrderAsc(attemptId);
    }

    private List<Question> eligibleBank(Integer quizId) {
        return questionRepository.findByQuizIdAndReviewStatusAndActiveTrueOrderByIdAsc(
                        quizId, QuestionReviewStatus.APPROVED)
                .stream()
                .filter(question -> question.getQuestionText() != null
                        && !question.getQuestionText().isBlank())
                .filter(question -> question.getOptionsJson() != null
                        && !question.getOptionsJson().isBlank())
                .filter(question -> question.getCorrectAnswer() != null
                        && !question.getCorrectAnswer().isBlank())
                .toList();
    }

    private Map<Integer, Long> usageCounts(Integer quizId) {
        Map<Integer, Long> counts = new HashMap<>();
        for (Object[] row : attemptQuestionRepository.countUsageByQuizId(quizId)) {
            if (row.length >= 2 && row[0] instanceof Number id && row[1] instanceof Number count) {
                counts.put(id.intValue(), count.longValue());
            }
        }
        return counts;
    }

    private BigDecimal positivePoints(BigDecimal points) {
        return points == null || points.compareTo(BigDecimal.ZERO) <= 0
                ? BigDecimal.ONE
                : points;
    }
}
