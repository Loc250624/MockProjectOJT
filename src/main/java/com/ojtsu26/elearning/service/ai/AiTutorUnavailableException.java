package com.ojtsu26.elearning.service.ai;

public class AiTutorUnavailableException extends RuntimeException {
    public AiTutorUnavailableException(String message) {
        super(message);
    }

    public AiTutorUnavailableException(String message, Throwable cause) {
        super(message, cause);
    }
}
