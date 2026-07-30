package com.ojtsu26.elearning.service.ai;

import com.ojtsu26.elearning.config.AiTutorProperties;
import com.ojtsu26.elearning.dto.request.AiChatbotPageContextDTO;
import com.ojtsu26.elearning.dto.request.AiTutorChatMessageDTO;
import com.ojtsu26.elearning.dto.request.AiTutorChatRequestDTO;
import com.ojtsu26.elearning.dto.response.AiTutorChatResponseDTO;
import com.ojtsu26.elearning.exception.BusinessException;
import com.ojtsu26.elearning.exception.ErrorCode;
import com.ojtsu26.elearning.model.entity.AiChatConversation;
import com.ojtsu26.elearning.model.entity.User;
import com.ojtsu26.elearning.model.enums.Role;
import com.ojtsu26.elearning.security.CustomUserDetails;
import com.ojtsu26.elearning.service.StudentLearningService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;
import java.util.regex.Pattern;

@Slf4j
@Service
@RequiredArgsConstructor
public class AiTutorService {

    private static final String FIXED_REFUSAL = "I can only help with the LumiNa website, its learning features, and authorized lesson content. I cannot perform account actions, reveal protected data, or answer unrelated requests.";
    private static final Set<String> ALLOWED_ROLES = Set.of("user", "assistant");
    private static final Set<String> ALLOWED_ACTIONS = Set.of("", "SUMMARY", "EXPLAIN_SIMPLY", "EXAMPLE", "QUIZ_ME");
    private static final Set<String> ALLOWED_ENTITY_TYPES = Set.of(
            "course", "lesson", "quiz", "coding-assignment", "certificate", "enrollment",
            "payment", "blog", "profile", "dashboard", "navigation", "authentication");
    private static final Pattern PAGE_KEY_PATTERN = Pattern.compile("[a-z0-9-]{1,64}");
    private static final Pattern ENTITY_ID_PATTERN = Pattern.compile("\\d{1,18}");
    private static final Pattern PATH_PATTERN = Pattern.compile("/[A-Za-z0-9/_\\-.]*");
    private static final int MAX_PAGE_SNIPPETS = 12;
    private static final int MAX_PAGE_SNIPPET_CHARS = 320;
    private static final int MAX_PAGE_CONTEXT_CHARS = 2800;

    private final StudentLearningService studentLearningService;
    private final AiTutorTopicGuard topicGuard;
    private final AiTutorPromptFactory promptFactory;
    private final AiTutorProvider provider;
    private final AiTutorRateLimiter rateLimiter;
    private final AiTutorProperties properties;
    private final AiChatHistoryService historyService;
    private final AiChatSuggestionService suggestionService;

    public AiTutorChatResponseDTO chat(CustomUserDetails principal, AiTutorChatRequestDTO request) {
        User user = principal == null ? null : principal.getUser();
        if (user == null) {
            throw new BusinessException(ErrorCode.UNAUTHORIZED);
        }
        if (request == null || request.getLessonId() == null) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "Lesson ID is required.");
        }
        return chatInternal(principal, request, "user:" + user.getId(), true, null, null);
    }

    public AiTutorChatResponseDTO chatGlobal(CustomUserDetails principal,
                                             AiTutorChatRequestDTO request,
                                             String anonymousSessionKey) {
        User user = principal == null ? null : principal.getUser();
        String clientKey = user == null ? "session:" + safeKey(anonymousSessionKey) : "user:" + user.getId();
        if (user == null) {
            AiTutorChatResponseDTO response = chatInternal(
                    principal,
                    request,
                    clientKey,
                    false,
                    null,
                    null);
            List<String> exclusions = sanitizeHistory(request == null ? null : request.getHistory()).stream()
                    .filter(message -> "user".equals(message.getRole()))
                    .map(AiTutorChatMessageDTO::getContent)
                    .collect(java.util.stream.Collectors.toCollection(ArrayList::new));
            exclusions.add(normalizeMessage(request == null ? null : request.getMessage()));
            response.setSuggestedQuestions(suggestionService.selectUniqueQuestions(
                    response.getSuggestedQuestions(),
                    exclusions,
                    AiChatSuggestionService.MAX_RELATED_QUESTIONS));
            return response;
        }

        String message = normalizeMessage(request == null ? null : request.getMessage());
        validateMessage(message);
        AiChatConversation conversation = historyService.resolveOwnedConversation(
                user.getId(),
                request == null ? null : request.getConversationId());
        List<AiTutorChatMessageDTO> serverHistory = historyService.loadRecentContext(conversation, user.getId());
        AiTutorChatResponseDTO response = chatInternal(
                principal,
                request,
                clientKey,
                false,
                serverHistory,
                conversation == null ? null : conversation.getId());

        List<String> exclusions = new ArrayList<>(
                historyService.loadQuestionAndSuggestionHistory(conversation, user.getId()));
        exclusions.add(message);
        List<String> relatedQuestions = suggestionService.selectUniqueQuestions(
                response.getSuggestedQuestions(),
                exclusions,
                AiChatSuggestionService.MAX_RELATED_QUESTIONS);
        AiChatConversation persistedConversation = historyService.appendTurn(
                user,
                conversation,
                message,
                response,
                relatedQuestions);
        response.setConversationId(persistedConversation.getId());
        response.setSuggestedQuestions(relatedQuestions);
        return response;
    }

    private AiTutorChatResponseDTO chatInternal(CustomUserDetails principal,
                                                AiTutorChatRequestDTO request,
                                                String clientKey,
                                                boolean requireLessonContext,
                                                List<AiTutorChatMessageDTO> authoritativeHistory,
                                                String authoritativeConversationId) {
        User user = principal == null ? null : principal.getUser();
        String message = normalizeMessage(request == null ? null : request.getMessage());
        String action = normalizeAction(request == null ? null : request.getAction());
        validateMessage(message);
        rateLimiter.check(clientKey);

        AiTutorLessonContext context = resolveLessonContext(user, request, requireLessonContext);
        AiChatbotPageContextDTO pageContext = sanitizePageContext(request == null ? null : request.getPageContext());
        AiTutorTopicGuard.GuardResult guard = topicGuard.evaluate(message, context);
        if (!guard.allowed()) {
            return refusal(
                    guard.reasonCode(),
                    authoritativeConversationId == null ? conversationId(request) : authoritativeConversationId,
                    context,
                    pageContext);
        }

        List<AiTutorChatMessageDTO> history = authoritativeHistory == null
                ? sanitizeHistory(request.getHistory())
                : sanitizeHistory(authoritativeHistory);
        String verifiedRole = user == null || user.getRole() == null ? "ANONYMOUS" : user.getRole().name();
        AiTutorPrompt prompt = promptFactory.create(
                context,
                pageContext,
                verifiedRole,
                user != null,
                message,
                action,
                history);
        AiTutorProviderResponse providerResponse = provider.generate(prompt);
        if (providerResponse == null) {
            throw new AiTutorUnavailableException("AI Chatbot is temporarily unavailable.");
        }
        String answer = providerResponse.answer() == null ? "" : providerResponse.answer().trim();
        if (AiTutorPromptFactory.OUT_OF_SCOPE_SENTINEL.equals(answer)) {
            return refusal(
                    "OUT_OF_SCOPE",
                    authoritativeConversationId == null ? conversationId(request) : authoritativeConversationId,
                    context,
                    pageContext);
        }
        if (answer.isBlank()) {
            throw new AiTutorUnavailableException("AI Chatbot returned an empty response.");
        }

        log.info("AI Chatbot response: authenticated={}, role={}, scope={}, requestId={}, inputChars={}, outputChars={}",
                user != null, verifiedRole, context == null ? "SITE" : "LESSON",
                providerResponse.requestId(), message.length(), answer.length());
        return AiTutorChatResponseDTO.builder()
                .conversationId(authoritativeConversationId == null
                        ? conversationId(request)
                        : authoritativeConversationId)
                .answer(answer)
                .refused(false)
                .suggestedQuestions(suggestionService.selectUniqueQuestions(
                        providerResponse.suggestedQuestions(),
                        AiChatSuggestionService.INITIAL_QUESTIONS,
                        AiChatSuggestionService.MAX_RELATED_QUESTIONS))
                .requestId(providerResponse.requestId())
                .scope(context == null ? "SITE" : "LESSON")
                .usedPageContext(pageContext != null)
                .build();
    }

    private AiTutorLessonContext resolveLessonContext(User user,
                                                      AiTutorChatRequestDTO request,
                                                      boolean requireLessonContext) {
        Integer lessonId = request == null ? null : request.getLessonId();
        if (requireLessonContext) {
            return studentLearningService.getAuthorizedAiTutorLessonContext(lessonId);
        }
        if (lessonId == null || user == null || user.getRole() != Role.STUDENT) {
            return null;
        }
        try {
            return studentLearningService.getAuthorizedAiTutorLessonContext(lessonId);
        } catch (BusinessException ex) {
            log.info("AI Chatbot omitted an unavailable optional lesson context for an authenticated request.");
            return null;
        }
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
            throw new BusinessException(ErrorCode.BAD_REQUEST, "Unsupported AI Chatbot action.");
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

    private AiTutorChatResponseDTO refusal(String reasonCode,
                                           String conversationId,
                                           AiTutorLessonContext context,
                                           AiChatbotPageContextDTO pageContext) {
        return AiTutorChatResponseDTO.builder()
                .conversationId(conversationId)
                .answer(FIXED_REFUSAL)
                .refused(true)
                .reasonCode(reasonCode == null ? "OUT_OF_SCOPE" : reasonCode)
                .suggestedQuestions(List.of())
                .scope(context == null ? "SITE" : "LESSON")
                .usedPageContext(pageContext != null)
                .build();
    }

    private String conversationId(AiTutorChatRequestDTO request) {
        String value = request == null ? null : request.getConversationId();
        if (value != null) {
            try {
                return UUID.fromString(value.trim()).toString();
            } catch (IllegalArgumentException ignored) {
                // Replace malformed client identifiers with an opaque server-generated id.
            }
        }
        return UUID.randomUUID().toString();
    }

    private AiChatbotPageContextDTO sanitizePageContext(AiChatbotPageContextDTO source) {
        if (source == null) {
            return null;
        }
        String path = sanitizePath(source.getPath());
        String pageKey = sanitizeToken(source.getPageKey(), PAGE_KEY_PATTERN);
        String entityType = sanitizeToken(source.getEntityType(), PAGE_KEY_PATTERN);
        String entityId = sanitizeToken(source.getEntityId(), ENTITY_ID_PATTERN);
        List<String> visibleText = sanitizeVisibleText(source.getVisibleText());
        if (!ALLOWED_ENTITY_TYPES.contains(entityType)) {
            entityType = "";
            entityId = "";
        }
        if (path.isBlank() && pageKey.isBlank() && entityType.isBlank() && visibleText.isEmpty()) {
            return null;
        }
        return new AiChatbotPageContextDTO(path, pageKey, entityType, entityId, visibleText);
    }

    private List<String> sanitizeVisibleText(List<String> source) {
        if (source == null || source.isEmpty()) {
            return List.of();
        }
        Set<String> unique = new LinkedHashSet<>();
        int totalChars = 0;
        for (String value : source) {
            String normalized = value == null ? "" : value.replaceAll("\\s+", " ").trim();
            if (normalized.isBlank()) {
                continue;
            }
            if (normalized.length() > MAX_PAGE_SNIPPET_CHARS) {
                normalized = normalized.substring(0, MAX_PAGE_SNIPPET_CHARS) + " [TRUNCATED]";
            }
            if (totalChars + normalized.length() > MAX_PAGE_CONTEXT_CHARS) {
                break;
            }
            if (unique.add(normalized)) {
                totalChars += normalized.length();
            }
            if (unique.size() >= MAX_PAGE_SNIPPETS) {
                break;
            }
        }
        return new ArrayList<>(unique);
    }

    private String sanitizePath(String value) {
        String normalized = value == null ? "" : value.trim();
        if (normalized.length() > 180 || !PATH_PATTERN.matcher(normalized).matches()) {
            return "";
        }
        return normalized;
    }

    private String sanitizeToken(String value, Pattern pattern) {
        String normalized = value == null ? "" : value.trim().toLowerCase(Locale.ROOT);
        return pattern.matcher(normalized).matches() ? normalized : "";
    }

    private String safeKey(String value) {
        String normalized = value == null ? "" : value.trim();
        return normalized.matches("[A-Za-z0-9-]{8,128}") ? normalized : UUID.randomUUID().toString();
    }
}
