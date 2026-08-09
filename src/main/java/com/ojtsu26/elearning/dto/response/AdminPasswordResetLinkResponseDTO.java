package com.ojtsu26.elearning.dto.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.ojtsu26.elearning.model.enums.AuthProvider;
import lombok.Builder;
import lombok.Value;

import java.time.LocalDateTime;

@Value
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class AdminPasswordResetLinkResponseDTO {
    Integer userId;
    AuthProvider authProvider;
    String resetLink;
    LocalDateTime expiresAt;
}
