package com.ojtsu26.elearning.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Getter
@Setter
@Component
@ConfigurationProperties(prefix = "app.code-judge")
public class CodeJudgeProperties {
    private String endpoint;
    private int timeoutMs = 5000;
}
