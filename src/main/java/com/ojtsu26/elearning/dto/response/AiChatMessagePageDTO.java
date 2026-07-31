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
public class AiChatMessagePageDTO {
    private String conversationId;
    private List<AiChatMessageDTO> messages;
    private boolean hasMore;
    private Long nextBeforeId;
}
