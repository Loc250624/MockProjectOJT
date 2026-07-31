package com.ojtsu26.elearning.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AiChatConversationSummaryDTO {
    private String conversationId;
    private String title;
    private LocalDateTime updatedAt;
    private List<String> suggestedQuestions;
}
