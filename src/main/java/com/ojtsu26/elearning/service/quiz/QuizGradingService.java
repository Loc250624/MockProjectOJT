package com.ojtsu26.elearning.service.quiz;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.ojtsu26.elearning.model.entity.QuizAnswer;
import com.ojtsu26.elearning.model.entity.QuizAttemptQuestion;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.*;

@Service
@RequiredArgsConstructor
public class QuizGradingService {
    private final ObjectMapper objectMapper;

    public GradeResult grade(List<QuizAttemptQuestion> assignments, List<QuizAnswer> answers) {
        Map<Integer, QuizAnswer> byQuestion = new HashMap<>();
        for (QuizAnswer answer : answers) {
            byQuestion.put(answer.getQuestion().getId(), answer);
        }

        BigDecimal earned = BigDecimal.ZERO;
        BigDecimal total = BigDecimal.ZERO;
        for (QuizAttemptQuestion assignment : assignments) {
            BigDecimal points = Objects.requireNonNullElse(assignment.getPointsSnapshot(), BigDecimal.ZERO);
            total = total.add(points);
            QuizAnswer answer = byQuestion.get(assignment.getQuestion().getId());
            if (answer != null && isCorrect(assignment, answer)) {
                earned = earned.add(points);
            }
        }
        BigDecimal percentage = total.compareTo(BigDecimal.ZERO) == 0
                ? BigDecimal.ZERO
                : earned.multiply(new BigDecimal("100")).divide(total, 2, RoundingMode.HALF_UP);
        return new GradeResult(earned, total, percentage);
    }

    private boolean isCorrect(QuizAttemptQuestion assignment, QuizAnswer answer) {
        Set<String> expectedTokens = correctTokens(
                assignment.getCorrectAnswerSnapshot(),
                assignment.getOptionsJsonSnapshot());
        String text = normalize(answer.getAnswerText());
        if (text != null && expectedTokens.stream().anyMatch(token -> token.equalsIgnoreCase(text))) {
            return true;
        }
        Set<Integer> selected = readIntegerSet(answer.getSelectedOptionsJson());
        Set<Integer> correctIndexes = new LinkedHashSet<>();
        for (String token : expectedTokens) {
            try {
                correctIndexes.add(Integer.parseInt(token));
            } catch (NumberFormatException ignored) {
                // Content tokens are used by the lesson adapter.
            }
        }
        return !selected.isEmpty() && !correctIndexes.isEmpty() && selected.equals(correctIndexes);
    }

    private Set<String> correctTokens(String correctAnswer, String optionsJson) {
        Set<String> tokens = new LinkedHashSet<>();
        if (correctAnswer != null) {
            Arrays.stream(correctAnswer.split(","))
                    .map(this::normalize)
                    .filter(Objects::nonNull)
                    .forEach(tokens::add);
        }
        try {
            JsonNode options = objectMapper.readTree(optionsJson == null ? "[]" : optionsJson);
            if (options.isArray()) {
                for (int index = 0; index < options.size(); index++) {
                    JsonNode option = options.get(index);
                    String content = option.isTextual()
                            ? normalize(stripMarker(option.asText()))
                            : normalize(option.path("content").asText(null));
                    boolean marked = option.isTextual() && option.asText().trim().startsWith("*");
                    boolean correct = marked || option.path("correct").asBoolean(false)
                            || tokens.contains(String.valueOf(index))
                            || (content != null && tokens.stream().anyMatch(content::equalsIgnoreCase));
                    if (correct) {
                        tokens.add(String.valueOf(index));
                        if (content != null) {
                            tokens.add(content);
                        }
                    }
                }
            }
        } catch (Exception ignored) {
            // The raw correct-answer snapshot remains authoritative.
        }
        return tokens;
    }

    private Set<Integer> readIntegerSet(String value) {
        if (value == null || value.isBlank()) {
            return Set.of();
        }
        try {
            return new LinkedHashSet<>(objectMapper.readValue(value, new TypeReference<List<Integer>>() {
            }));
        } catch (Exception ignored) {
            return Set.of();
        }
    }

    private String stripMarker(String value) {
        String normalized = value == null ? null : value.trim();
        return normalized != null && normalized.startsWith("*")
                ? normalized.substring(1).trim()
                : normalized;
    }

    private String normalize(String value) {
        if (value == null) {
            return null;
        }
        String normalized = value.trim();
        return normalized.isEmpty() ? null : normalized;
    }

    public record GradeResult(BigDecimal earnedPoints,
                              BigDecimal totalPoints,
                              BigDecimal percentage) {
    }
}
