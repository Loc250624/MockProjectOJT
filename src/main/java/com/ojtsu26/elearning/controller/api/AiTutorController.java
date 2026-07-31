package com.ojtsu26.elearning.controller.api;

import com.ojtsu26.elearning.common.ApiResponse;
import com.ojtsu26.elearning.dto.request.AiTutorChatRequestDTO;
import com.ojtsu26.elearning.dto.response.AiChatConversationSummaryDTO;
import com.ojtsu26.elearning.dto.response.AiChatMessagePageDTO;
import com.ojtsu26.elearning.dto.response.AiTutorChatResponseDTO;
import com.ojtsu26.elearning.exception.BusinessException;
import com.ojtsu26.elearning.exception.ErrorCode;
import com.ojtsu26.elearning.model.entity.User;
import com.ojtsu26.elearning.security.CustomUserDetails;
import com.ojtsu26.elearning.service.ai.AiChatHistoryService;
import com.ojtsu26.elearning.service.ai.AiTutorRateLimitException;
import com.ojtsu26.elearning.service.ai.AiTutorService;
import com.ojtsu26.elearning.service.ai.AiTutorUnavailableException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class AiTutorController {

    private final AiTutorService aiTutorService;
    private final AiChatHistoryService aiChatHistoryService;

    @GetMapping("/ai-chatbot/conversations/latest")
    public ResponseEntity<ApiResponse<AiChatConversationSummaryDTO>> latestConversation(
            @AuthenticationPrincipal CustomUserDetails currentUser) {
        return ResponseEntity.ok(ApiResponse.success(
                aiChatHistoryService.latestConversation(authenticatedUser(currentUser))));
    }

    @GetMapping("/ai-chatbot/conversations/{conversationId}/messages")
    public ResponseEntity<ApiResponse<AiChatMessagePageDTO>> messages(
            @AuthenticationPrincipal CustomUserDetails currentUser,
            @PathVariable String conversationId,
            @RequestParam(defaultValue = "50") int limit,
            @RequestParam(required = false) Long beforeId) {
        return ResponseEntity.ok(ApiResponse.success(aiChatHistoryService.messages(
                authenticatedUser(currentUser),
                conversationId,
                limit,
                beforeId)));
    }

    @PostMapping({"/student/ai-tutor/chat", "/ai-chatbot/chat"})
    public ResponseEntity<ApiResponse<AiTutorChatResponseDTO>> chat(
            @AuthenticationPrincipal CustomUserDetails currentUser,
            HttpServletRequest servletRequest,
            @Valid @RequestBody AiTutorChatRequestDTO request) {
        try {
            boolean legacyRoute = servletRequest.getRequestURI().startsWith("/api/student/ai-tutor/");
            AiTutorChatResponseDTO response = legacyRoute
                    ? aiTutorService.chat(currentUser, request)
                    : aiTutorService.chatGlobal(currentUser, request, servletRequest.getSession(true).getId());
            return ResponseEntity.ok(ApiResponse.success(response));
        } catch (AiTutorRateLimitException ex) {
            return ResponseEntity.status(HttpStatus.TOO_MANY_REQUESTS)
                    .body(ApiResponse.error(HttpStatus.TOO_MANY_REQUESTS.value(), ex.getMessage()));
        } catch (AiTutorUnavailableException ex) {
            return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
                    .body(ApiResponse.error(HttpStatus.SERVICE_UNAVAILABLE.value(), ex.getMessage()));
        }
    }

    private User authenticatedUser(CustomUserDetails currentUser) {
        if (currentUser == null || currentUser.getUser() == null) {
            throw new BusinessException(ErrorCode.UNAUTHORIZED);
        }
        return currentUser.getUser();
    }
}
