package com.ojtsu26.elearning.service.ai;

import org.springframework.stereotype.Service;

import java.text.Normalizer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.regex.Pattern;

@Service
public class AiChatSuggestionService {

    public static final List<String> INITIAL_QUESTIONS = List.of(
            "Summarize",
            "Explain simply",
            "Give an example",
            "Quiz me"
    );
    static final double NEAR_DUPLICATE_THRESHOLD = 0.82d;
    public static final int MAX_RELATED_QUESTIONS = 4;
    static final int MAX_SUGGESTION_CHARS = 120;
    private static final Pattern PUNCTUATION_OR_SYMBOL = Pattern.compile("[\\p{P}\\p{S}]+");
    private static final Pattern ASSISTANT_LED_PROMPT = Pattern.compile(
            "^(?:do you (?:need|want|have)|would you like|are you (?:interested|ready|curious)|can i help|shall i)\\b",
            Pattern.CASE_INSENSITIVE | Pattern.UNICODE_CASE);

    public List<String> selectUniqueQuestions(List<String> candidates,
                                              List<String> excluded,
                                              int maxItems) {
        if (candidates == null || candidates.isEmpty() || maxItems <= 0) {
            return List.of();
        }
        List<String> selected = new ArrayList<>();
        List<String> existing = new ArrayList<>();
        if (excluded != null) {
            existing.addAll(excluded);
        }
        for (String rawCandidate : candidates) {
            String candidate = readableQuestion(rawCandidate);
            if (candidate.isBlank()
                    || candidate.length() > MAX_SUGGESTION_CHARS
                    || isAssistantLedPrompt(candidate)
                    || isDuplicateQuestion(candidate, existing)
                    || isDuplicateQuestion(candidate, selected)) {
                continue;
            }
            selected.add(candidate);
            if (selected.size() >= Math.min(MAX_RELATED_QUESTIONS, maxItems)) {
                break;
            }
        }
        return List.copyOf(selected);
    }

    public boolean isDuplicateQuestion(String candidate, List<String> existing) {
        String normalizedCandidate = normalizeQuestion(candidate);
        if (normalizedCandidate.isBlank()) {
            return true;
        }
        if (existing == null) {
            return false;
        }
        for (String item : existing) {
            String normalizedExisting = normalizeQuestion(item);
            if (normalizedCandidate.equals(normalizedExisting)
                    || jaccardSimilarity(normalizedCandidate, normalizedExisting) >= NEAR_DUPLICATE_THRESHOLD) {
                return true;
            }
        }
        return false;
    }

    boolean isAssistantLedPrompt(String value) {
        String readable = readableQuestion(value);
        return !readable.isBlank() && ASSISTANT_LED_PROMPT.matcher(readable).find();
    }

    public String normalizeQuestion(String value) {
        String compatibilityNormalized = Normalizer.normalize(
                value == null ? "" : value,
                Normalizer.Form.NFKC);
        return PUNCTUATION_OR_SYMBOL.matcher(compatibilityNormalized.toLowerCase(Locale.ROOT))
                .replaceAll(" ")
                .replaceAll("\\s+", " ")
                .trim();
    }

    double jaccardSimilarity(String left, String right) {
        Set<String> leftTokens = tokenSet(left);
        Set<String> rightTokens = tokenSet(right);
        if (leftTokens.isEmpty() || rightTokens.isEmpty()) {
            return 0d;
        }
        Set<String> intersection = new LinkedHashSet<>(leftTokens);
        intersection.retainAll(rightTokens);
        Set<String> union = new LinkedHashSet<>(leftTokens);
        union.addAll(rightTokens);
        return (double) intersection.size() / union.size();
    }

    private Set<String> tokenSet(String value) {
        String normalized = normalizeQuestion(value);
        if (normalized.isBlank()) {
            return Set.of();
        }
        return new LinkedHashSet<>(Arrays.asList(normalized.split(" ")));
    }

    private String readableQuestion(String value) {
        return Normalizer.normalize(value == null ? "" : value, Normalizer.Form.NFKC)
                .replaceAll("\\s+", " ")
                .trim();
    }
}
