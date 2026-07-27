package com.ojtsu26.elearning.service.quiz;

import com.ojtsu26.elearning.model.enums.QuestionDifficulty;

import java.security.SecureRandom;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Pure selection component.
 * Persistence, locking and authorization stay outside this class.
 */
public final class StratifiedQuestionSampler {

    private final SecureRandom random;

    public StratifiedQuestionSampler() {
        this(new SecureRandom());
    }

    StratifiedQuestionSampler(SecureRandom random) {
        this.random = Objects.requireNonNull(random);
    }

    public List<Candidate> select(
            List<Candidate> candidates,
            List<Bucket> blueprint,
            Set<Integer> previouslySeen,
            Map<Integer, Long> usageCount) {

        Objects.requireNonNull(candidates, "candidates");
        Objects.requireNonNull(blueprint, "blueprint");

        Set<Integer> seen = previouslySeen == null
                ? Set.of()
                : Set.copyOf(previouslySeen);

        Map<Integer, Long> usage = usageCount == null
                ? Map.of()
                : Map.copyOf(usageCount);

        Map<BucketKey, List<Candidate>> byBucket = candidates.stream()
                .filter(Candidate::approved)
                .filter(Candidate::active)
                .collect(Collectors.groupingBy(candidate ->
                        new BucketKey(
                            normalize(candidate.topicCode()),
                            candidate.difficulty())));

        List<Candidate> selected = new ArrayList<>();
        Set<Integer> selectedIds = new HashSet<>();

        for (Bucket bucket : blueprint) {
            BucketKey key = new BucketKey(
                    normalize(bucket.topicCode()),
                    bucket.difficulty());

            List<Candidate> pool = new ArrayList<>(
                    byBucket.getOrDefault(key, List.of()));

            if (pool.size() < bucket.questionCount()) {
                throw new InsufficientQuestionBankException(
                    "Insufficient approved questions for " + key
                    + ": required=" + bucket.questionCount()
                    + ", available=" + pool.size());
            }

            Collections.shuffle(pool, random);

            pool.sort(Comparator
                    .comparing((Candidate c) -> seen.contains(c.id()))
                    .thenComparingLong(
                        c -> usage.getOrDefault(c.id(), 0L))
                    .thenComparingInt(Candidate::id));

            int bucketSelected = 0;

            for (Candidate candidate : pool) {
                if (selectedIds.add(candidate.id())) {
                    selected.add(candidate);
                    bucketSelected++;
                }

                if (bucketSelected == bucket.questionCount()) {
                    break;
                }
            }

            if (bucketSelected != bucket.questionCount()) {
                throw new InsufficientQuestionBankException(
                    "Cannot select unique questions for " + key);
            }
        }

        return List.copyOf(selected);
    }

    private static String normalize(String topicCode) {
        if (topicCode == null || topicCode.isBlank()) {
            return "GENERAL";
        }
        return topicCode.trim().toUpperCase(Locale.ROOT);
    }

    public record Candidate(
            Integer id,
            String topicCode,
            QuestionDifficulty difficulty,
            boolean approved,
            boolean active) {
    }

    public record Bucket(
            String topicCode,
            QuestionDifficulty difficulty,
            int questionCount) {

        public Bucket {
            if (questionCount <= 0) {
                throw new IllegalArgumentException(
                    "questionCount must be positive");
            }
        }
    }

    private record BucketKey(
            String topicCode,
            QuestionDifficulty difficulty) {
    }

    public static final class InsufficientQuestionBankException
            extends RuntimeException {

        public InsufficientQuestionBankException(String message) {
            super(message);
        }
    }
}
