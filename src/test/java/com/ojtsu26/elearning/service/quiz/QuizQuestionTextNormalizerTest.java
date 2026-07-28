package com.ojtsu26.elearning.service.quiz;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class QuizQuestionTextNormalizerTest {

    @Test
    void removesNumberedCheckpointPrefixOnlyAtTheStart() {
        assertThat(QuizQuestionTextNormalizer.stripCheckpointPrefix(
                "Checkpoint 42: Which answer is correct?"))
                .isEqualTo("Which answer is correct?");
        assertThat(QuizQuestionTextNormalizer.stripCheckpointPrefix(
                "  checkpoint 7 :  Choose one"))
                .isEqualTo("Choose one");
        assertThat(QuizQuestionTextNormalizer.stripCheckpointPrefix(
                "Explain checkpoint behavior"))
                .isEqualTo("Explain checkpoint behavior");
    }
}
