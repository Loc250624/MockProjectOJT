package com.ojtsu26.elearning.dto.request;

import com.fasterxml.jackson.annotation.JsonAlias;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class AiTutorChatRequestDTO {

    private Integer lessonId;
    private String message;
    private String action;

    @JsonAlias("recentMessages")
    private List<AiTutorChatMessageDTO> history;

    private String conversationId;
    private AiChatbotPageContextDTO pageContext;
}
