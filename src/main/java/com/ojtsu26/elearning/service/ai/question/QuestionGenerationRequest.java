package com.ojtsu26.elearning.service.ai.question;

import com.ojtsu26.elearning.model.enums.QuestionDifficulty;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record QuestionGenerationRequest(
        @NotBlank String lessonContent,
        @NotBlank String topicCode,
        @NotNull QuestionDifficulty difficulty,
        @Min(1) @Max(25) int questionCount) {
}
