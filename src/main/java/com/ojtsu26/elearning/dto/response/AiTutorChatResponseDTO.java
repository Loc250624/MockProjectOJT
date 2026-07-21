package com.ojtsu26.elearning.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AiTutorChatResponseDTO {
    private String answer;
    private boolean refused;
    private String reasonCode;
    private List<String> suggestedQuestions;
    private String requestId;
}
