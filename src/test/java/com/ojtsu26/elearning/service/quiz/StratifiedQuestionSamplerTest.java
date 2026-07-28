package com.ojtsu26.elearning.service.quiz;

import com.ojtsu26.elearning.model.enums.QuestionDifficulty;
import org.junit.jupiter.api.Test;

import java.util.*;

import static org.assertj.core.api.Assertions.*;

class StratifiedQuestionSamplerTest {
    private final StratifiedQuestionSampler sampler = new StratifiedQuestionSampler(new Random(7));

    @Test
    void selectsExactBlueprintDistributionAndUniqueQuestions() {
        List<StratifiedQuestionSampler.Candidate> candidates = List.of(
                candidate(1, "A", QuestionDifficulty.EASY),
                candidate(2, "A", QuestionDifficulty.EASY),
                candidate(3, "A", QuestionDifficulty.MEDIUM),
                candidate(4, "A", QuestionDifficulty.MEDIUM),
                candidate(5, "B", QuestionDifficulty.HARD),
                candidate(6, "B", QuestionDifficulty.HARD));
        List<StratifiedQuestionSampler.Bucket> blueprint = List.of(
                bucket("A", QuestionDifficulty.EASY, 2),
                bucket("A", QuestionDifficulty.MEDIUM, 1),
                bucket("B", QuestionDifficulty.HARD, 2));

        List<StratifiedQuestionSampler.Candidate> selected =
                sampler.select(candidates, blueprint, Set.of(), Map.of());

        assertThat(selected).hasSize(5);
        assertThat(selected).extracting(StratifiedQuestionSampler.Candidate::id).doesNotHaveDuplicates();
        assertThat(selected).filteredOn(item -> item.topicCode().equals("A")
                && item.difficulty() == QuestionDifficulty.EASY).hasSize(2);
        assertThat(selected).filteredOn(item -> item.topicCode().equals("A")
                && item.difficulty() == QuestionDifficulty.MEDIUM).hasSize(1);
        assertThat(selected).filteredOn(item -> item.topicCode().equals("B")
                && item.difficulty() == QuestionDifficulty.HARD).hasSize(2);
    }

    @Test
    void excludesDraftRejectedAndInactiveQuestions() {
        List<StratifiedQuestionSampler.Candidate> candidates = List.of(
                candidate(1, "A", QuestionDifficulty.EASY),
                new StratifiedQuestionSampler.Candidate(
                        2, "A", QuestionDifficulty.EASY, false, true),
                new StratifiedQuestionSampler.Candidate(
                        3, "A", QuestionDifficulty.EASY, true, false));

        List<StratifiedQuestionSampler.Candidate> selected = sampler.select(
                candidates,
                List.of(bucket("A", QuestionDifficulty.EASY, 1)),
                Set.of(),
                Map.of());

        assertThat(selected).extracting(StratifiedQuestionSampler.Candidate::id).containsExactly(1);
    }

    @Test
    void prefersPreviouslyUnseenQuestionsWhenSufficient() {
        List<StratifiedQuestionSampler.Candidate> selected = sampler.select(
                List.of(
                        candidate(1, "A", QuestionDifficulty.MEDIUM),
                        candidate(2, "A", QuestionDifficulty.MEDIUM),
                        candidate(3, "A", QuestionDifficulty.MEDIUM),
                        candidate(4, "A", QuestionDifficulty.MEDIUM)),
                List.of(bucket("A", QuestionDifficulty.MEDIUM, 2)),
                Set.of(1, 2),
                Map.of(1, 0L, 2, 0L, 3, 10L, 4, 10L));

        assertThat(selected).extracting(StratifiedQuestionSampler.Candidate::id)
                .containsExactlyInAnyOrder(3, 4);
    }

    @Test
    void reusesSeenQuestionsOnlyInsideTheRequestedBucket() {
        List<StratifiedQuestionSampler.Candidate> selected = sampler.select(
                List.of(
                        candidate(1, "A", QuestionDifficulty.MEDIUM),
                        candidate(2, "A", QuestionDifficulty.MEDIUM),
                        candidate(3, "B", QuestionDifficulty.MEDIUM)),
                List.of(bucket("A", QuestionDifficulty.MEDIUM, 2)),
                Set.of(1),
                Map.of());

        assertThat(selected).extracting(StratifiedQuestionSampler.Candidate::id)
                .containsExactlyInAnyOrder(1, 2);
    }

    @Test
    void neverFallsBackToWrongTopicOrDifficulty() {
        assertThatThrownBy(() -> sampler.select(
                List.of(
                        candidate(1, "A", QuestionDifficulty.EASY),
                        candidate(2, "B", QuestionDifficulty.HARD)),
                List.of(bucket("A", QuestionDifficulty.EASY, 2)),
                Set.of(),
                Map.of()))
                .isInstanceOf(StratifiedQuestionSampler.InsufficientQuestionBankException.class)
                .hasMessageContaining("required=2")
                .hasMessageContaining("available=1");
    }

    private StratifiedQuestionSampler.Candidate candidate(
            int id, String topic, QuestionDifficulty difficulty) {
        return new StratifiedQuestionSampler.Candidate(id, topic, difficulty, true, true);
    }

    private StratifiedQuestionSampler.Bucket bucket(
            String topic, QuestionDifficulty difficulty, int count) {
        return new StratifiedQuestionSampler.Bucket(topic, difficulty, count);
    }
}
