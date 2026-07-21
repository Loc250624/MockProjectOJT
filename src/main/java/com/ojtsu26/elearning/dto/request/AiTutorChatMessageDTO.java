package com.ojtsu26.elearning.dto.request;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class AiTutorChatMessageDTO {
    private String role;
    private String content;
}
