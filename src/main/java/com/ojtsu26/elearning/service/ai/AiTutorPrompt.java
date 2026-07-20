package com.ojtsu26.elearning.service.ai;

public record AiTutorPrompt(String instructions, String input, int maxOutputTokens) {
}
