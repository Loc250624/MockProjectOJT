package com.ojtsu26.elearning.service.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.ojtsu26.elearning.model.entity.Question;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class AssessmentOptionCodecTest {
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    void preservesLiteralCssAsteriskWhenCorrectIndexIsExplicit() {
        Question question = Question.builder()
                .optionsJson("[\"p\",\".note\",\"#notice\",\"*\"]")
                .correctAnswer("2")
                .build();

        assertThat(AssessmentOptionCodec.studentOptionContents(question, objectMapper))
                .containsExactly("p", ".note", "#notice", "*");
        assertThat(AssessmentOptionCodec.correctIndexes(question, objectMapper))
                .containsExactly(2);
    }

    @Test
    void stillSupportsAsteriskAsLegacyMarkerWithoutExplicitCorrectAnswer() {
        Question question = Question.builder()
                .optionsJson("[\"A\",\"*B\"]")
                .build();

        assertThat(AssessmentOptionCodec.studentOptionContents(question, objectMapper))
                .containsExactly("A", "B");
        assertThat(AssessmentOptionCodec.correctIndexes(question, objectMapper))
                .isEqualTo(List.of(1));
    }
}
