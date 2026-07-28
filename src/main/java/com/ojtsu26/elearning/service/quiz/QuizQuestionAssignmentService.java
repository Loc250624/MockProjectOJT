package com.ojtsu26.elearning.service.quiz;

import com.ojtsu26.elearning.exception.BusinessException;
import com.ojtsu26.elearning.exception.ErrorCode;
import com.ojtsu26.elearning.model.entity.*;
import com.ojtsu26.elearning.model.enums.QuestionDifficulty;
import com.ojtsu26.elearning.model.enums.QuestionReviewStatus;
import com.ojtsu26.elearning.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class QuizQuestionAssignmentService {
    private static final int COMPATIBILITY_QUESTION_LIMIT = 10;

    private final QuestionRepository questionRepository;
    private final QuizBlueprintItemRepository blueprintRepository;
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
        List<Question> bank = approvedBank(quiz.getId());
        List<StratifiedQuestionSampler.Bucket> blueprint = effectiveBlueprint(quiz.getId(), bank);
        Map<Integer, Question> questionsById = bank.stream()
                .collect(Collectors.toMap(Question::getId, Function.identity()));
        List<StratifiedQuestionSampler.Candidate> candidates = bank.stream()
                .map(question -> new StratifiedQuestionSampler.Candidate(
                        question.getId(),
                        question.getTopicCode(),
                        question.getDifficulty(),
                        question.getReviewStatus() == QuestionReviewStatus.APPROVED,
                        Boolean.TRUE.equals(question.getActive())))
                .toList();

        Set<Integer> seen = attemptQuestionRepository.findPreviouslySeenQuestionIds(
                attempt.getStudent().getId(), quiz.getId());
        Map<Integer, Long> usage = usageCounts(quiz.getId());
        List<StratifiedQuestionSampler.Candidate> selected;
        try {
            selected = sampler.select(candidates, blueprint, seen, usage);
        } catch (StratifiedQuestionSampler.InsufficientQuestionBankException ex) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, ex.getMessage());
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
                    .questionTypeSnapshot(question.getQuestionType() == null ? null : question.getQuestionType().name())
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

    @Transactional(readOnly = true)
    public Readiness readiness(Integer quizId) {
        List<Question> bank = approvedBank(quizId);
        List<QuizBlueprintItem> configured =
                blueprintRepository.findByQuizIdOrderByDisplayOrderAscIdAsc(quizId);
        if (configured.isEmpty() && bank.isEmpty()) {
            return new Readiness(false, List.of(new BucketReadiness(
                    "GENERAL", QuestionDifficulty.MEDIUM, 1, 0, false)));
        }
        List<StratifiedQuestionSampler.Bucket> blueprint = effectiveBlueprint(quizId, bank);
        List<BucketReadiness> buckets = blueprint.stream().map(bucket -> {
            long available = bank.stream()
                    .filter(question -> StratifiedQuestionSampler.normalizeTopic(question.getTopicCode())
                            .equals(StratifiedQuestionSampler.normalizeTopic(bucket.topicCode())))
                    .filter(question -> Objects.requireNonNullElse(
                            question.getDifficulty(), QuestionDifficulty.MEDIUM) == bucket.difficulty())
                    .count();
            return new BucketReadiness(
                    StratifiedQuestionSampler.normalizeTopic(bucket.topicCode()),
                    bucket.difficulty(),
                    bucket.questionCount(),
                    available,
                    available >= bucket.questionCount());
        }).toList();
        return new Readiness(buckets.stream().allMatch(BucketReadiness::ready), buckets);
    }

    private List<Question> approvedBank(Integer quizId) {
        return questionRepository.findByQuizIdAndReviewStatusAndActiveTrueOrderByIdAsc(
                quizId, QuestionReviewStatus.APPROVED);
    }

    private List<StratifiedQuestionSampler.Bucket> effectiveBlueprint(Integer quizId, List<Question> bank) {
        List<QuizBlueprintItem> configured =
                blueprintRepository.findByQuizIdOrderByDisplayOrderAscIdAsc(quizId);
        if (!configured.isEmpty()) {
            return configured.stream()
                    .map(item -> new StratifiedQuestionSampler.Bucket(
                            item.getTopicCode(),
                            item.getDifficulty(),
                            Objects.requireNonNullElse(item.getQuestionCount(), 0)))
                    .toList();
        }
        if (bank.isEmpty()) {
            throw new BusinessException(
                    ErrorCode.BAD_REQUEST,
                    "This quiz has no approved active questions.");
        }
        int count = Math.min(COMPATIBILITY_QUESTION_LIMIT, bank.size());
        boolean allGeneralMedium = bank.stream().allMatch(question ->
                "GENERAL".equals(StratifiedQuestionSampler.normalizeTopic(question.getTopicCode()))
                        && Objects.requireNonNullElse(
                        question.getDifficulty(), QuestionDifficulty.MEDIUM) == QuestionDifficulty.MEDIUM);
        if (allGeneralMedium) {
            return List.of(new StratifiedQuestionSampler.Bucket(
                    "GENERAL", QuestionDifficulty.MEDIUM, count));
        }

        // Compatibility for pre-blueprint quizzes with mixed metadata: preserve each
        // real bucket, never substitute from a different bucket.
        Map<String, List<Question>> groups = bank.stream().collect(Collectors.groupingBy(question ->
                StratifiedQuestionSampler.normalizeTopic(question.getTopicCode()) + "\u0000"
                        + Objects.requireNonNullElse(question.getDifficulty(), QuestionDifficulty.MEDIUM).name(),
                LinkedHashMap::new,
                Collectors.toList()));
        List<StratifiedQuestionSampler.Bucket> buckets = new ArrayList<>();
        int remaining = count;
        for (List<Question> group : groups.values()) {
            if (remaining == 0) {
                break;
            }
            Question first = group.get(0);
            int take = Math.min(remaining, group.size());
            buckets.add(new StratifiedQuestionSampler.Bucket(
                    first.getTopicCode(),
                    Objects.requireNonNullElse(first.getDifficulty(), QuestionDifficulty.MEDIUM),
                    take));
            remaining -= take;
        }
        return buckets;
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
        return points == null || points.compareTo(BigDecimal.ZERO) <= 0 ? BigDecimal.ONE : points;
    }

    public record Readiness(boolean ready, List<BucketReadiness> buckets) {
    }

    public record BucketReadiness(String topicCode,
                                  QuestionDifficulty difficulty,
                                  int required,
                                  long available,
                                  boolean ready) {
    }
}
