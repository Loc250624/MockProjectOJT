package com.ojtsu26.elearning.service.ai;

public class AiTutorRateLimitException extends RuntimeException {
    public AiTutorRateLimitException(String message) {
        super(message);
    }
}
