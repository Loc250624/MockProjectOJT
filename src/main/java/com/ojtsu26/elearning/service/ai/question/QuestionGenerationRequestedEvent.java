package com.ojtsu26.elearning.service.ai.question;

public record QuestionGenerationRequestedEvent(Integer jobId, QuestionGenerationRequest request) {
}
