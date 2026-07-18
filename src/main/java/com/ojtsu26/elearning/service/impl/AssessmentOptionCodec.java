package com.ojtsu26.elearning.service.impl;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.ojtsu26.elearning.dto.assessment.AssessmentDtos.OptionPayload;
import com.ojtsu26.elearning.exception.BusinessException;
import com.ojtsu26.elearning.exception.ErrorCode;
import com.ojtsu26.elearning.model.entity.Question;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

final class AssessmentOptionCodec {
    private AssessmentOptionCodec() {
    }

    static List<ParsedOption> readOptions(Question question, ObjectMapper objectMapper) {
        List<ParsedOption> parsed = parseRawOptions(question == null ? null : question.getOptionsJson(), objectMapper);
        Set<Integer> correctIndexes = correctIndexes(question == null ? null : question.getCorrectAnswer(), parsed);
        List<ParsedOption> merged = new ArrayList<>();
        for (ParsedOption option : parsed) {
            merged.add(new ParsedOption(option.id(), option.content(), option.correct() || correctIndexes.contains(option.id())));
        }
        return merged;
    }

    static List<Integer> correctIndexes(Question question, ObjectMapper objectMapper) {
        return readOptions(question, objectMapper).stream()
                .filter(ParsedOption::correct)
                .map(ParsedOption::id)
                .toList();
    }

    static List<Integer> correctIndexes(List<OptionPayload> options) {
        List<Integer> indexes = new ArrayList<>();
        List<OptionPayload> safeOptions = Optional.ofNullable(options).orElse(List.of());
        for (int i = 0; i < safeOptions.size(); i++) {
            if (Boolean.TRUE.equals(safeOptions.get(i).getCorrect())) {
                indexes.add(i);
            }
        }
        return indexes;
    }

    static String correctAnswerValue(List<OptionPayload> options) {
        return correctIndexes(options).stream()
                .map(String::valueOf)
                .collect(Collectors.joining(","));
    }

    static List<String> studentOptionContents(Question question, ObjectMapper objectMapper) {
        return readOptions(question, objectMapper).stream()
                .map(ParsedOption::content)
                .toList();
    }

    static String studentOptionsJson(Question question, ObjectMapper objectMapper) {
        return writeJson(studentOptionContents(question, objectMapper), objectMapper);
    }

    static boolean normalizeQuestion(Question question, ObjectMapper objectMapper) {
        List<ParsedOption> parsed = readOptions(question, objectMapper);
        if (parsed.isEmpty()) {
            return false;
        }
        List<OptionPayload> payloads = parsed.stream().map(option -> {
            OptionPayload payload = new OptionPayload();
            payload.setContent(option.content());
            payload.setCorrect(option.correct());
            return payload;
        }).toList();
        String normalizedOptions = writeJson(payloads, objectMapper);
        String normalizedCorrect = correctAnswerValue(payloads);
        boolean changed = !Objects.equals(nullToEmpty(question.getOptionsJson()), normalizedOptions)
                || !Objects.equals(nullToEmpty(question.getCorrectAnswer()), normalizedCorrect);
        if (changed) {
            question.setOptionsJson(normalizedOptions);
            question.setCorrectAnswer(normalizedCorrect);
        }
        return changed;
    }

    static String writeJson(Object value, ObjectMapper objectMapper) {
        try {
            return objectMapper.writeValueAsString(value);
        } catch (JsonProcessingException e) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "Invalid JSON payload");
        }
    }

    private static List<ParsedOption> parseRawOptions(String raw, ObjectMapper objectMapper) {
        if (raw == null || raw.isBlank()) {
            return List.of();
        }
        String trimmed = raw.trim();
        if (trimmed.startsWith("[")) {
            List<ParsedOption> fromJson = parseJsonOptions(trimmed, objectMapper);
            if (!fromJson.isEmpty()) {
                return fromJson;
            }
        }
        return parseDelimitedOptions(trimmed);
    }

    private static List<ParsedOption> parseJsonOptions(String raw, ObjectMapper objectMapper) {
        try {
            List<Map<String, Object>> values = objectMapper.readValue(raw, new TypeReference<>() {
            });
            List<ParsedOption> parsed = new ArrayList<>();
            for (int i = 0; i < values.size(); i++) {
                Map<String, Object> value = values.get(i);
                if (value == null) {
                    continue;
                }
                String content = stringValue(value.get("content"));
                if (content != null) {
                    parsed.add(new ParsedOption(i, content, Boolean.TRUE.equals(value.get("correct"))));
                }
            }
            if (!parsed.isEmpty()) {
                return parsed;
            }
        } catch (Exception ignored) {
            // Try legacy string array below.
        }
        try {
            List<String> values = objectMapper.readValue(raw, new TypeReference<>() {
            });
            List<ParsedOption> parsed = new ArrayList<>();
            for (int i = 0; i < values.size(); i++) {
                String content = stripMarker(values.get(i));
                if (content != null) {
                    parsed.add(new ParsedOption(i, content, isMarkedCorrect(values.get(i))));
                }
            }
            return parsed;
        } catch (Exception ignored) {
            return List.of();
        }
    }

    private static List<ParsedOption> parseDelimitedOptions(String raw) {
        String[] parts = raw.split("\\|");
        List<ParsedOption> parsed = new ArrayList<>();
        for (String part : parts) {
            String content = stripMarker(part);
            if (content != null) {
                parsed.add(new ParsedOption(parsed.size(), content, isMarkedCorrect(part)));
            }
        }
        return parsed;
    }

    private static Set<Integer> correctIndexes(String correctAnswer, List<ParsedOption> options) {
        Set<Integer> indexes = new LinkedHashSet<>();
        if (correctAnswer == null || correctAnswer.isBlank()) {
            return indexes;
        }
        Map<String, Integer> byContent = options.stream()
                .collect(Collectors.toMap(
                        option -> option.content().trim().toLowerCase(Locale.ROOT),
                        ParsedOption::id,
                        (left, right) -> left));
        for (String part : correctAnswer.split(",")) {
            String token = part == null ? "" : part.trim();
            if (token.isEmpty()) {
                continue;
            }
            try {
                indexes.add(Integer.parseInt(token));
                continue;
            } catch (NumberFormatException ignored) {
                // Fall through to legacy content matching.
            }
            String contentToken = stripMarker(token);
            if (contentToken != null) {
                Integer index = byContent.get(contentToken.toLowerCase(Locale.ROOT));
                if (index != null) {
                    indexes.add(index);
                }
            }
        }
        return indexes;
    }

    private static boolean isMarkedCorrect(String value) {
        return value != null && value.trim().startsWith("*");
    }

    private static String stripMarker(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        if (trimmed.startsWith("*")) {
            trimmed = trimmed.substring(1).trim();
        }
        return trimmed.isBlank() ? null : trimmed;
    }

    private static String stringValue(Object value) {
        if (value == null) {
            return null;
        }
        String string = String.valueOf(value).trim();
        return string.isBlank() ? null : string;
    }

    private static String nullToEmpty(String value) {
        return value == null ? "" : value;
    }

    record ParsedOption(Integer id, String content, Boolean correct) {
    }
}
