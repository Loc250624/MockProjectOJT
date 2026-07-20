package com.ojtsu26.elearning.service.ai;

import org.springframework.stereotype.Component;

import java.text.Normalizer;
import java.util.Arrays;
import java.util.Locale;
import java.util.Set;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@Component
public class AiTutorTopicGuard {

    private static final Pattern INJECTION_PATTERN = Pattern.compile(
            "(?i)(ignore|disregard|bypass|override|jailbreak|system prompt|developer message|api key|secret|password|token|hidden instruction|reveal.*prompt|show.*prompt|bo qua|he thong|khoa api|mat khau|tiet lo)");
    private static final Pattern UNRELATED_PATTERN = Pattern.compile(
            "(?i)(weather|stock|bitcoin|lottery|football|soccer|movie|recipe|travel|dating|thoi tiet|chung khoan|xoso|xo so|bong da|du lich)");

    public GuardResult evaluate(String message, AiTutorLessonContext context) {
        String normalizedMessage = normalize(message);
        if (INJECTION_PATTERN.matcher(normalizedMessage).find()) {
            return GuardResult.deny("PROMPT_INJECTION");
        }
        if (UNRELATED_PATTERN.matcher(normalizedMessage).find() && !hasContextOverlap(normalizedMessage, context)) {
            return GuardResult.deny("OUT_OF_SCOPE");
        }
        return GuardResult.allow();
    }

    private boolean hasContextOverlap(String message, AiTutorLessonContext context) {
        Set<String> contextTerms = Arrays.stream(normalize(String.join(" ",
                        safe(context.courseTitle()),
                        safe(context.sectionTitle()),
                        safe(context.lessonTitle()),
                        safe(context.learningObjectives()),
                        safe(context.lessonText()))).split("\\s+"))
                .filter(term -> term.length() >= 5)
                .collect(Collectors.toSet());
        return Arrays.stream(message.split("\\s+"))
                .filter(term -> term.length() >= 5)
                .anyMatch(contextTerms::contains);
    }

    private String normalize(String value) {
        String lowered = safe(value).toLowerCase(Locale.ROOT);
        String decomposed = Normalizer.normalize(lowered, Normalizer.Form.NFD);
        return decomposed.replaceAll("\\p{M}", "");
    }

    private String safe(String value) {
        return value == null ? "" : value;
    }

    public record GuardResult(boolean allowed, String reasonCode) {
        static GuardResult allow() {
            return new GuardResult(true, null);
        }

        static GuardResult deny(String reasonCode) {
            return new GuardResult(false, reasonCode);
        }
    }
}
