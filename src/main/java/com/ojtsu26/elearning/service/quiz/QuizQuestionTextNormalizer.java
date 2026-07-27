package com.ojtsu26.elearning.service.quiz;

import java.util.regex.Pattern;

public final class QuizQuestionTextNormalizer {
    private static final Pattern CHECKPOINT_PREFIX =
            Pattern.compile("^\\s*Checkpoint\\s+\\d+\\s*:\\s*", Pattern.CASE_INSENSITIVE);

    private QuizQuestionTextNormalizer() {
    }

    public static String stripCheckpointPrefix(String questionText) {
        if (questionText == null || questionText.isBlank()) {
            return questionText;
        }
        return CHECKPOINT_PREFIX.matcher(questionText).replaceFirst("");
    }
}
