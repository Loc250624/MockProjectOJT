package com.ojtsu26.elearning.service.ai;

public interface AiTutorProvider {
    AiTutorProviderResponse generate(AiTutorPrompt prompt);

    default boolean isAvailable() {
        return true;
    }
}
