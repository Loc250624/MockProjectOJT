package com.ojtsu26.elearning.service.ai;

import java.util.List;

public record AiTutorProviderResponse(String answer,
                                      String requestId,
                                      List<String> suggestedQuestions) {

    public AiTutorProviderResponse {
        suggestedQuestions = suggestedQuestions == null ? List.of() : List.copyOf(suggestedQuestions);
    }

    public AiTutorProviderResponse(String answer, String requestId) {
        this(answer, requestId, List.of());
    }
}
