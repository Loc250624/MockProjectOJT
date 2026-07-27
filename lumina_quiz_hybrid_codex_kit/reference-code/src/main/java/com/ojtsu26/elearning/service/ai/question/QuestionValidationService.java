package com.ojtsu26.elearning.service.ai.question;

import java.util.HashSet;
import java.util.Locale;
import java.util.Set;

public final class QuestionValidationService {

    public void validate(GeneratedQuestion question) {
        if (question == null) {
            throw new IllegalArgumentException(
                "Generated question is required");
        }

        if (question.questionText() == null
                || question.questionText().isBlank()) {
            throw new IllegalArgumentException(
                "Question text is required");
        }

        if (question.questionText().length() > 4000) {
            throw new IllegalArgumentException(
                "Question text is too long");
        }

        if (question.options() == null
                || question.options().size() < 2) {
            throw new IllegalArgumentException(
                "At least two options are required");
        }

        if (question.correctAnswerTokens() == null
                || question.correctAnswerTokens().isEmpty()) {
            throw new IllegalArgumentException(
                "Correct answer is required");
        }

        Set<String> optionKeys = new HashSet<>();

        question.options().forEach(option -> {
            if (option == null
                    || option.key() == null
                    || option.key().isBlank()
                    || option.content() == null
                    || option.content().isBlank()) {
                throw new IllegalArgumentException("Invalid option");
            }

            String normalized = option.key()
                    .trim()
                    .toLowerCase(Locale.ROOT);

            if (!optionKeys.add(normalized)) {
                throw new IllegalArgumentException(
                    "Duplicate option key");
            }
        });

        boolean allAnswersExist =
                question.correctAnswerTokens().stream()
                    .map(token -> token == null
                        ? ""
                        : token.trim().toLowerCase(Locale.ROOT))
                    .allMatch(optionKeys::contains);

        if (!allAnswersExist) {
            throw new IllegalArgumentException(
                "Correct answer must reference existing option keys");
        }
    }
}
