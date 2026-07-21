package com.ojtsu26.elearning.service.ai;

import com.ojtsu26.elearning.config.AiTutorProperties;
import com.ojtsu26.elearning.dto.request.AiTutorChatMessageDTO;
import com.ojtsu26.elearning.dto.request.AiTutorChatRequestDTO;
import com.ojtsu26.elearning.dto.response.AiTutorChatResponseDTO;
import com.ojtsu26.elearning.exception.BusinessException;
import com.ojtsu26.elearning.exception.ErrorCode;
import com.ojtsu26.elearning.model.entity.User;
import com.ojtsu26.elearning.security.CustomUserDetails;
import com.ojtsu26.elearning.service.StudentLearningService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Locale;
import java.util.Set;

@Slf4j
@Service
@RequiredArgsConstructor
public class AiTutorService {

    private static final String FIXED_REFUSAL = "I can only help with the current lesson or course. You can ask me to summarize the lesson, explain a concept, give an example, or quiz you.";
    private static final Set<String> ALLOWED_ROLES = Set.of("user", "assistant");
    private static final Set<String> ALLOWED_ACTIONS = Set.of("", "SUMMARY", "EXPLAIN_SIMPLY", "EXAMPLE", "QUIZ_ME");

    private final StudentLearningService studentLearningService;
    private final AiTutorTopicGuard topicGuard;
    private final AiTutorPromptFactory promptFactory;
    private final AiTutorProvider provider;
    private final AiTutorRateLimiter rateLimiter;
    private final AiTutorProperties properties;

    public AiTutorChatResponseDTO chat(CustomUserDetails principal, AiTutorChatRequestDTO request) {
        User student = principal == null ? null : principal.getUser();
        if (student == null) {
            throw new BusinessException(ErrorCode.UNAUTHORIZED);
        }
        String message = normalizeMessage(request == null ? null : request.getMessage());
        String action = normalizeAction(request == null ? null : request.getAction());
        validateMessage(message);
        rateLimiter.check(student.getId());

        AiTutorLessonContext context = studentLearningService.getAuthorizedAiTutorLessonContext(request.getLessonId());
        AiTutorTopicGuard.GuardResult guard = topicGuard.evaluate(message, context);
        if (!guard.allowed()) {
            return refusal(guard.reasonCode());
        }

        List<AiTutorChatMessageDTO> history = sanitizeHistory(request.getHistory());
        AiTutorPrompt prompt = promptFactory.create(context, message, action, history);
        AiTutorProviderResponse providerResponse = provider.generate(prompt);
        String answer = providerResponse.answer() == null ? "" : providerResponse.answer().trim();
        if (AiTutorPromptFactory.OUT_OF_SCOPE_SENTINEL.equals(answer)) {
            return refusal("OUT_OF_SCOPE");
        }
        if (answer.isBlank()) {
            throw new AiTutorUnavailableException("AI Tutor returned an empty response.");
        }

        log.info("AI Tutor response: studentId={}, lessonId={}, requestId={}, inputChars={}, outputChars={}",
                student.getId(), request.getLessonId(), providerResponse.requestId(), message.length(), answer.length());
        return AiTutorChatResponseDTO.builder()
                .answer(answer)
                .refused(false)
                .suggestedQuestions(suggestions())
                .requestId(providerResponse.requestId())
                .build();
    }

    private String normalizeMessage(String message) {
        return message == null ? "" : message.trim().replaceAll("\\s+", " ");
    }

    private void validateMessage(String message) {
        if (message.isBlank()) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "Message is required.");
        }
        if (message.length() > properties.getMaxMessageChars()) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "Message is too long.");
        }
    }

    private String normalizeAction(String action) {
        String normalized = action == null ? "" : action.trim().toUpperCase(Locale.ROOT);
        if (!ALLOWED_ACTIONS.contains(normalized)) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "Unsupported AI Tutor action.");
        }
        return normalized;
    }

    private List<AiTutorChatMessageDTO> sanitizeHistory(List<AiTutorChatMessageDTO> history) {
        if (history == null || history.isEmpty()) {
            return List.of();
        }
        return history.stream()
                .filter(turn -> turn != null && turn.getRole() != null && turn.getContent() != null)
                .map(turn -> new AiTutorChatMessageDTO(
                        turn.getRole().trim().toLowerCase(Locale.ROOT),
                        turn.getContent().trim()))
                .filter(turn -> ALLOWED_ROLES.contains(turn.getRole()) && !turn.getContent().isBlank())
                .limit(Math.max(0, properties.getMaxHistoryTurns()))
                .toList();
    }

    private AiTutorChatResponseDTO refusal(String reasonCode) {
        return AiTutorChatResponseDTO.builder()
                .answer(FIXED_REFUSAL)
                .refused(true)
                .reasonCode(reasonCode == null ? "OUT_OF_SCOPE" : reasonCode)
                .suggestedQuestions(suggestions())
                .build();
    }

    private List<String> suggestions() {
        return List.of(
                "Summarize this lesson",
                "Explain the main concept",
                "Give me an example",
                "Quiz me"
        );
    }
}
