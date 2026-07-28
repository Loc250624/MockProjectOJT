package com.ojtsu26.elearning.service.ai.question;

public interface QuestionGenerationProvider {
    GeneratedQuestionBatch generate(QuestionGenerationRequest request);
    String providerName();
    String modelName();
}
