package com.ojtsu26.elearning.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Data
@Component
@ConfigurationProperties(prefix = "app.ai-tutor")
public class AiTutorProperties {

    private boolean enabled = true;
    private String apiKey = "";
    private String baseUrl = "https://api.openai.com/v1";
    private String model = "gpt-5-mini";
    private int timeoutMs = 8000;
    private int maxMessageChars = 1200;
    private int maxHistoryTurns = 6;
    private int maxHistoryChars = 700;
    private int maxContextChars = 6000;
    private int maxOutputTokens = 700;
    private int rateLimitMaxRequests = 12;
    private int rateLimitWindowSeconds = 60;
}
