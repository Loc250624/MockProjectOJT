package com.ojtsu26.elearning.service.quiz;

import com.ojtsu26.elearning.model.enums.QuestionDifficulty;
import org.springframework.stereotype.Component;

import java.security.SecureRandom;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Pure in-memory selector. Persistence, authorization, and transaction locking
 * deliberately stay outside this component.
 */
@Component
public class StratifiedQuestionSampler {
    private final Random random;

    public StratifiedQuestionSampler() {
        this(new SecureRandom());
    }

    StratifiedQuestionSampler(Random random) {
        this.random = Objects.requireNonNull(random);
    }

    public List<Candidate> select(List<Candidate> candidates,
                                  List<Bucket> blueprint,
                                  Set<Integer> previouslySeen,
                                  Map<Integer, Long> usageCount) {
        Objects.requireNonNull(candidates, "candidates");
        Objects.requireNonNull(blueprint, "blueprint");
        Set<Integer> seen = previouslySeen == null ? Set.of() : Set.copyOf(previouslySeen);
        Map<Integer, Long> usage = usageCount == null ? Map.of() : Map.copyOf(usageCount);

        Map<BucketKey, List<Candidate>> byBucket = candidates.stream()
                .filter(Candidate::approved)
                .filter(Candidate::active)
                .collect(Collectors.groupingBy(candidate -> new BucketKey(
                        normalizeTopic(candidate.topicCode()),
                        Objects.requireNonNullElse(candidate.difficulty(), QuestionDifficulty.MEDIUM))));

        List<Candidate> selected = new ArrayList<>();
        Set<Integer> selectedIds = new HashSet<>();
        for (Bucket bucket : blueprint) {
            BucketKey key = new BucketKey(
                    normalizeTopic(bucket.topicCode()),
                    Objects.requireNonNullElse(bucket.difficulty(), QuestionDifficulty.MEDIUM));
            List<Candidate> pool = new ArrayList<>(byBucket.getOrDefault(key, List.of()));
            if (pool.size() < bucket.questionCount()) {
                throw new InsufficientQuestionBankException(
                        key.topicCode(), key.difficulty(), bucket.questionCount(), pool.size());
            }

            // Stable sort preserves the secure shuffle inside equal-priority groups.
            Collections.shuffle(pool, random);
            pool.sort(Comparator
                    .comparing((Candidate candidate) -> seen.contains(candidate.id()))
                    .thenComparingLong(candidate -> usage.getOrDefault(candidate.id(), 0L)));

            int before = selected.size();
            for (Candidate candidate : pool) {
                if (selectedIds.add(candidate.id())) {
                    selected.add(candidate);
                }
                if (selected.size() - before == bucket.questionCount()) {
                    break;
                }
            }
            if (selected.size() - before != bucket.questionCount()) {
                throw new InsufficientQuestionBankException(
                        key.topicCode(), key.difficulty(), bucket.questionCount(), selected.size() - before);
            }
        }
        return List.copyOf(selected);
    }

    public static String normalizeTopic(String topicCode) {
        return topicCode == null || topicCode.isBlank()
                ? "GENERAL"
                : topicCode.trim().toUpperCase(Locale.ROOT);
    }

    public record Candidate(Integer id,
                            String topicCode,
                            QuestionDifficulty difficulty,
                            boolean approved,
                            boolean active) {
    }

    public record Bucket(String topicCode, QuestionDifficulty difficulty, int questionCount) {
        public Bucket {
            if (questionCount <= 0) {
                throw new IllegalArgumentException("questionCount must be positive");
            }
        }
    }

    private record BucketKey(String topicCode, QuestionDifficulty difficulty) {
    }

    public static final class InsufficientQuestionBankException extends RuntimeException {
        private final String topicCode;
        private final QuestionDifficulty difficulty;
        private final int required;
        private final int available;

        public InsufficientQuestionBankException(String topicCode,
                                                 QuestionDifficulty difficulty,
                                                 int required,
                                                 int available) {
            super("Insufficient approved question bank for topic " + topicCode
                    + " and difficulty " + difficulty
                    + ": required=" + required + ", available=" + available);
            this.topicCode = topicCode;
            this.difficulty = difficulty;
            this.required = required;
            this.available = available;
        }

        public String getTopicCode() {
            return topicCode;
        }

        public QuestionDifficulty getDifficulty() {
            return difficulty;
        }

        public int getRequired() {
            return required;
        }

        public int getAvailable() {
            return available;
        }
    }
}
