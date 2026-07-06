package com.ojtsu26.elearning.dto.response;

import com.ojtsu26.elearning.model.enums.NotificationType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class NotificationResponseDTO {
    private Integer id;
    private NotificationType type;
    private String title;
    private String message;
    private String targetPath;
    private Boolean read;
    private LocalDateTime createdAt;
    private LocalDateTime readAt;
}
