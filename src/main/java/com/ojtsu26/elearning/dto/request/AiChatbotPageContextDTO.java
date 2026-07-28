package com.ojtsu26.elearning.dto.request;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class AiChatbotPageContextDTO {
    private String path;
    private String pageKey;
    private String entityType;
    private String entityId;
    private List<String> visibleText;
}
