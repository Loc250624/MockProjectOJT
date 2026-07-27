package com.ojtsu26.elearning.service.ai.question;

import java.math.BigDecimal;
import java.util.List;

public record GeneratedQuestion(
        String topicCode,
        String difficulty,
        String questionType,
        String questionText,
        List<Option> options,
        List<String> correctAnswerTokens,
        String explanation,
        BigDecimal points) {

    public record Option(
            String key,
            String content) {
    }
}
