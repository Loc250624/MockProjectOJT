package com.ojtsu26.elearning.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AiChatMessageDTO {
    private Long id;
    private String role;
    private String content;
    private boolean refused;
    private String reasonCode;
    private LocalDateTime createdAt;
}
