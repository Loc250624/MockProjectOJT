package com.ojtsu26.elearning.service.ai.question;

import java.util.List;

public record GeneratedQuestionBatch(
        String providerRequestId,
        List<GeneratedQuestion> questions) {
}
