package com.ojtsu26.elearning.service.ai.question;

import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

@Service
public class QuestionValidationService {
    public List<GeneratedQuestion> validate(GeneratedQuestionBatch batch,
                                            Collection<String> existingQuestionTexts,
                                            int expectedCount) {
        if (batch == null || batch.questions() == null || batch.questions().size() != expectedCount) {
            throw new IllegalArgumentException("Provider returned an unexpected question count");
        }
        Set<String> accepted = Optional.ofNullable(existingQuestionTexts).orElse(List.of()).stream()
                .map(this::normalize)
                .collect(Collectors.toCollection(LinkedHashSet::new));
        List<GeneratedQuestion> valid = new ArrayList<>();
        for (GeneratedQuestion question : batch.questions()) {
            validateOne(question);
            String normalized = normalize(question.questionText());
            if (accepted.stream().anyMatch(existing -> similarity(existing, normalized) >= 0.90d)) {
                throw new IllegalArgumentException("Generated batch contains a duplicate or very similar question");
            }
            accepted.add(normalized);
            valid.add(question);
        }
        return List.copyOf(valid);
    }

    private void validateOne(GeneratedQuestion question) {
        if (question == null || question.questionText() == null
                || question.questionText().isBlank() || question.questionText().length() > 1000) {
            throw new IllegalArgumentException("Question text must be between 1 and 1000 characters");
        }
        List<String> options = question.options();
        if (options == null || options.size() < 2 || options.size() > 6
                || options.stream().anyMatch(option -> option == null
                || option.isBlank() || option.length() > 500)) {
            throw new IllegalArgumentException("Each question must contain 2 to 6 valid options");
        }
        Set<String> normalizedOptions = options.stream().map(this::normalize).collect(Collectors.toSet());
        if (normalizedOptions.size() != options.size()) {
            throw new IllegalArgumentException("Question options must be unique");
        }
        if (question.correctAnswer() == null
                || !normalizedOptions.contains(normalize(question.correctAnswer()))) {
            throw new IllegalArgumentException("Correct answer must match one option");
        }
    }

    private double similarity(String left, String right) {
        Set<String> a = new HashSet<>(Arrays.asList(left.split(" ")));
        Set<String> b = new HashSet<>(Arrays.asList(right.split(" ")));
        if (a.isEmpty() || b.isEmpty()) {
            return 0;
        }
        Set<String> intersection = new HashSet<>(a);
        intersection.retainAll(b);
        Set<String> union = new HashSet<>(a);
        union.addAll(b);
        return (double) intersection.size() / union.size();
    }

    private String normalize(String value) {
        return value == null
                ? ""
                : value.trim().toLowerCase(Locale.ROOT).replaceAll("\\s+", " ");
    }
}
