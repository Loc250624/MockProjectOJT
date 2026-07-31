package com.ojtsu26.elearning.service.ai;

import com.ojtsu26.elearning.config.AiTutorProperties;
import com.ojtsu26.elearning.dto.request.AiTutorChatMessageDTO;
import com.ojtsu26.elearning.dto.response.AiChatConversationSummaryDTO;
import com.ojtsu26.elearning.dto.response.AiChatMessageDTO;
import com.ojtsu26.elearning.dto.response.AiChatMessagePageDTO;
import com.ojtsu26.elearning.dto.response.AiTutorChatResponseDTO;
import com.ojtsu26.elearning.exception.BusinessException;
import com.ojtsu26.elearning.exception.ErrorCode;
import com.ojtsu26.elearning.model.entity.AiChatConversation;
import com.ojtsu26.elearning.model.entity.AiChatMessage;
import com.ojtsu26.elearning.model.entity.AiChatSuggestion;
import com.ojtsu26.elearning.model.entity.User;
import com.ojtsu26.elearning.model.enums.AiChatMessageRole;
import com.ojtsu26.elearning.repository.AiChatConversationRepository;
import com.ojtsu26.elearning.repository.AiChatMessageRepository;
import com.ojtsu26.elearning.repository.AiChatSuggestionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class AiChatHistoryService {

    public static final int MAX_UI_MESSAGES = 50;
    private static final int MAX_ASSISTANT_CONTENT_CHARS = 16000;
    private static final String NOT_FOUND_MESSAGE = "Conversation not found.";

    private final AiChatConversationRepository conversationRepository;
    private final AiChatMessageRepository messageRepository;
    private final AiChatSuggestionRepository suggestionRepository;
    private final AiChatSuggestionService suggestionService;
    private final AiTutorProperties properties;

    @Transactional(readOnly = true)
    public AiChatConversation resolveOwnedConversation(Integer userId, String requestedConversationId) {
        if (requestedConversationId == null || requestedConversationId.isBlank()) {
            return conversationRepository.findFirstByUserIdOrderByLastMessageAtDescIdDesc(userId).orElse(null);
        }
        String conversationId = canonicalConversationId(requestedConversationId);
        return conversationRepository.findByIdAndUserId(conversationId, userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND, NOT_FOUND_MESSAGE));
    }

    @Transactional(readOnly = true)
    public List<AiTutorChatMessageDTO> loadRecentContext(AiChatConversation conversation, Integer userId) {
        if (conversation == null) {
            return List.of();
        }
        int limit = Math.max(0, properties.getMaxHistoryTurns());
        if (limit == 0) {
            return List.of();
        }
        List<AiChatMessage> recent = new ArrayList<>(messageRepository.findOwnedMessagesBefore(
                conversation.getId(),
                userId,
                null,
                PageRequest.of(0, limit)));
        Collections.reverse(recent);
        return recent.stream()
                .map(message -> new AiTutorChatMessageDTO(
                        message.getRole().name().toLowerCase(Locale.ROOT),
                        limitContent(message.getContent(), Math.max(0, properties.getMaxHistoryChars()))))
                .toList();
    }

    @Transactional(readOnly = true)
    public List<String> loadQuestionAndSuggestionHistory(AiChatConversation conversation, Integer userId) {
        if (conversation == null) {
            return List.of();
        }
        List<String> excluded = new ArrayList<>(messageRepository.findOwnedContentByRole(
                conversation.getId(),
                userId,
                AiChatMessageRole.USER));
        excluded.addAll(suggestionRepository.findOwnedDisplayText(conversation.getId(), userId));
        return excluded;
    }

    @Transactional
    public AiChatConversation appendTurn(User user,
                                         AiChatConversation conversation,
                                         String userContent,
                                         AiTutorChatResponseDTO response,
                                         List<String> relatedQuestions) {
        LocalDateTime now = LocalDateTime.now();
        AiChatConversation ownedConversation = conversation;
        if (ownedConversation == null) {
            ownedConversation = conversationRepository.save(AiChatConversation.builder()
                    .id(UUID.randomUUID().toString())
                    .user(user)
                    .title(titleFrom(userContent))
                    .lastMessageAt(now)
                    .build());
        }

        AiChatMessage userMessage = messageRepository.save(AiChatMessage.builder()
                .conversation(ownedConversation)
                .role(AiChatMessageRole.USER)
                .content(limitContent(userContent, Math.max(1, properties.getMaxMessageChars())))
                .build());
        AiChatMessage assistantMessage = messageRepository.save(AiChatMessage.builder()
                .conversation(ownedConversation)
                .role(AiChatMessageRole.ASSISTANT)
                .content(limitContent(response.getAnswer(), assistantContentLimit()))
                .refused(response.isRefused())
                .reasonCode(limitNullable(response.getReasonCode(), 64))
                .requestId(limitNullable(response.getRequestId(), 128))
                .build());

        if (relatedQuestions != null) {
            for (String question : relatedQuestions) {
                String displayText = limitContent(question, 500);
                String normalizedText = suggestionService.normalizeQuestion(displayText);
                if (displayText.isBlank() || normalizedText.isBlank()) {
                    continue;
                }
                suggestionRepository.save(AiChatSuggestion.builder()
                        .conversation(ownedConversation)
                        .assistantMessage(assistantMessage)
                        .normalizedText(normalizedText)
                        .displayText(displayText)
                        .build());
            }
        }

        ownedConversation.setLastMessageAt(now);
        conversationRepository.save(ownedConversation);
        return ownedConversation;
    }

    @Transactional(readOnly = true)
    public AiChatConversationSummaryDTO latestConversation(User user) {
        AiChatConversation conversation = conversationRepository
                .findFirstByUserIdOrderByLastMessageAtDescIdDesc(user.getId())
                .orElse(null);
        if (conversation == null) {
            return null;
        }
        List<String> latestSuggestions = messageRepository
                .findFirstByConversationIdAndConversationUserIdAndRoleOrderByIdDesc(
                        conversation.getId(),
                        user.getId(),
                        AiChatMessageRole.ASSISTANT)
                .map(message -> suggestionRepository.findByAssistantMessageIdOrderByIdAsc(message.getId()).stream()
                        .map(AiChatSuggestion::getDisplayText)
                        .toList())
                .orElseGet(List::of);
        return AiChatConversationSummaryDTO.builder()
                .conversationId(conversation.getId())
                .title(conversation.getTitle())
                .updatedAt(conversation.getLastMessageAt())
                .suggestedQuestions(latestSuggestions)
                .build();
    }

    @Transactional(readOnly = true)
    public AiChatMessagePageDTO messages(User user,
                                         String requestedConversationId,
                                         int limit,
                                         Long beforeId) {
        if (limit < 1 || limit > MAX_UI_MESSAGES) {
            throw new BusinessException(
                    ErrorCode.BAD_REQUEST,
                    "Message limit must be between 1 and " + MAX_UI_MESSAGES + ".");
        }
        if (beforeId != null && beforeId <= 0) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "beforeId must be positive.");
        }
        String conversationId = canonicalConversationId(requestedConversationId);
        conversationRepository.findByIdAndUserId(conversationId, user.getId())
                .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND, NOT_FOUND_MESSAGE));

        List<AiChatMessage> descending = messageRepository.findOwnedMessagesBefore(
                conversationId,
                user.getId(),
                beforeId,
                PageRequest.of(0, limit + 1));
        boolean hasMore = descending.size() > limit;
        List<AiChatMessage> selected = new ArrayList<>(
                descending.subList(0, Math.min(limit, descending.size())));
        Collections.reverse(selected);
        Long nextBeforeId = hasMore && !selected.isEmpty() ? selected.get(0).getId() : null;
        return AiChatMessagePageDTO.builder()
                .conversationId(conversationId)
                .messages(selected.stream().map(this::toDto).toList())
                .hasMore(hasMore)
                .nextBeforeId(nextBeforeId)
                .build();
    }

    private AiChatMessageDTO toDto(AiChatMessage message) {
        return AiChatMessageDTO.builder()
                .id(message.getId())
                .role(message.getRole().name())
                .content(message.getContent())
                .refused(message.isRefused())
                .reasonCode(message.getReasonCode())
                .createdAt(message.getCreatedAt())
                .build();
    }

    private String canonicalConversationId(String value) {
        try {
            return UUID.fromString(value == null ? "" : value.trim()).toString();
        } catch (IllegalArgumentException ex) {
            throw new BusinessException(ErrorCode.NOT_FOUND, NOT_FOUND_MESSAGE);
        }
    }

    private String titleFrom(String value) {
        String title = value == null ? "" : value.replaceAll("\\s+", " ").trim();
        if (title.isBlank()) {
            return "AI Chat";
        }
        return limitContent(title, 160);
    }

    private String limitContent(String value, int maxChars) {
        String normalized = value == null ? "" : value.trim();
        return normalized.length() <= maxChars ? normalized : normalized.substring(0, maxChars);
    }

    private String limitNullable(String value, int maxChars) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return limitContent(value, maxChars);
    }

    private int assistantContentLimit() {
        int configuredApproximation = Math.max(2000, properties.getMaxOutputTokens() * 8);
        return Math.min(MAX_ASSISTANT_CONTENT_CHARS, configuredApproximation);
    }
}
