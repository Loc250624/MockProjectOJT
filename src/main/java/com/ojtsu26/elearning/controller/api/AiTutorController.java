package com.ojtsu26.elearning.controller.api;

import com.ojtsu26.elearning.common.ApiResponse;
import com.ojtsu26.elearning.dto.request.AiTutorChatRequestDTO;
import com.ojtsu26.elearning.dto.response.AiTutorChatResponseDTO;
import com.ojtsu26.elearning.security.CustomUserDetails;
import com.ojtsu26.elearning.service.ai.AiTutorRateLimitException;
import com.ojtsu26.elearning.service.ai.AiTutorService;
import com.ojtsu26.elearning.service.ai.AiTutorUnavailableException;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/student/ai-tutor")
@RequiredArgsConstructor
public class AiTutorController {

    private final AiTutorService aiTutorService;

    @PostMapping("/chat")
    public ResponseEntity<ApiResponse<AiTutorChatResponseDTO>> chat(
            @AuthenticationPrincipal CustomUserDetails currentUser,
            @Valid @RequestBody AiTutorChatRequestDTO request) {
        try {
            return ResponseEntity.ok(ApiResponse.success(aiTutorService.chat(currentUser, request)));
        } catch (AiTutorRateLimitException ex) {
            return ResponseEntity.status(HttpStatus.TOO_MANY_REQUESTS)
                    .body(ApiResponse.error(HttpStatus.TOO_MANY_REQUESTS.value(), ex.getMessage()));
        } catch (AiTutorUnavailableException ex) {
            return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
                    .body(ApiResponse.error(HttpStatus.SERVICE_UNAVAILABLE.value(), ex.getMessage()));
        }
    }
}
