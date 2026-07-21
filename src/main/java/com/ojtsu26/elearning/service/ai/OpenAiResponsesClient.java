package com.ojtsu26.elearning.service.ai;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.ojtsu26.elearning.config.AiTutorProperties;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.LinkedHashMap;
import java.util.Map;

@Slf4j
@Component
@RequiredArgsConstructor
public class OpenAiResponsesClient implements AiTutorProvider {

    private final AiTutorProperties properties;
    private final ObjectMapper objectMapper;

    @Override
    public AiTutorProviderResponse generate(AiTutorPrompt prompt) {
        if (!properties.isEnabled()) {
            throw new AiTutorUnavailableException("AI Tutor is disabled.");
        }
        if (properties.getApiKey() == null || properties.getApiKey().isBlank()) {
            throw new AiTutorUnavailableException("AI Tutor is not configured.");
        }

        try {
            HttpResponse<String> response = send(prompt);
            if (shouldRetry(response.statusCode())) {
                response = send(prompt);
            }
            if (response.statusCode() < 200 || response.statusCode() >= 300) {
                OpenAiError error = parseProviderError(response.body());
                log.warn("OpenAI Responses API returned status={} errorCode={} errorType={} bodyChars={}",
                        response.statusCode(),
                        error.code(),
                        error.type(),
                        response.body() == null ? 0 : response.body().length());
                throw new AiTutorUnavailableException(safeUnavailableMessage(response.statusCode(), error));
            }
            JsonNode root = objectMapper.readTree(response.body());
            String answer = extractText(root);
            String requestId = root.path("id").asText(null);
            return new AiTutorProviderResponse(answer, requestId);
        } catch (IOException e) {
            throw new AiTutorUnavailableException("AI Tutor is temporarily unavailable.", e);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new AiTutorUnavailableException("AI Tutor request was interrupted.", e);
        }
    }

    private HttpResponse<String> send(AiTutorPrompt prompt) throws IOException, InterruptedException {
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("model", properties.getModel());
        payload.put("instructions", prompt.instructions());
        payload.put("input", prompt.input());
        payload.put("max_output_tokens", prompt.maxOutputTokens());

        String baseUrl = properties.getBaseUrl() == null || properties.getBaseUrl().isBlank()
                ? "https://api.openai.com/v1"
                : properties.getBaseUrl().replaceAll("/+$", "");
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(baseUrl + "/responses"))
                .timeout(Duration.ofMillis(Math.max(1000, properties.getTimeoutMs())))
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + properties.getApiKey())
                .header(HttpHeaders.CONTENT_TYPE, "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(objectMapper.writeValueAsString(payload)))
                .build();
        HttpClient client = HttpClient.newBuilder()
                .connectTimeout(Duration.ofMillis(Math.max(1000, properties.getTimeoutMs())))
                .build();
        return client.send(request, HttpResponse.BodyHandlers.ofString());
    }

    private boolean shouldRetry(int statusCode) {
        return statusCode == 500 || statusCode == 502 || statusCode == 503 || statusCode == 504;
    }

    private OpenAiError parseProviderError(String body) {
        if (body == null || body.isBlank()) {
            return new OpenAiError("", "");
        }
        try {
            JsonNode error = objectMapper.readTree(body).path("error");
            return new OpenAiError(error.path("code").asText(""), error.path("type").asText(""));
        } catch (IOException ex) {
            return new OpenAiError("", "");
        }
    }

    private String safeUnavailableMessage(int statusCode, OpenAiError error) {
        String code = error.code() == null ? "" : error.code();
        String type = error.type() == null ? "" : error.type();
        if (statusCode == 401) {
            return "AI Tutor OpenAI API key is invalid or unauthorized.";
        }
        if (statusCode == 404 || "model_not_found".equals(code)) {
            return "AI Tutor OpenAI model is not available. Check OPENAI_MODEL.";
        }
        if (statusCode == 429 && ("insufficient_quota".equals(code) || "insufficient_quota".equals(type))) {
            return "AI Tutor OpenAI quota or billing credit is unavailable.";
        }
        if (statusCode == 429) {
            return "AI Tutor OpenAI rate limit was reached. Please try again later.";
        }
        return "AI Tutor is temporarily unavailable.";
    }

    private String extractText(JsonNode root) {
        if (root.hasNonNull("output_text")) {
            return root.path("output_text").asText();
        }
        StringBuilder builder = new StringBuilder();
        JsonNode output = root.path("output");
        if (output.isArray()) {
            for (JsonNode item : output) {
                JsonNode content = item.path("content");
                if (!content.isArray()) {
                    continue;
                }
                for (JsonNode contentItem : content) {
                    String type = contentItem.path("type").asText();
                    if ("output_text".equals(type) || "text".equals(type)) {
                        String text = contentItem.path("text").asText();
                        if (!text.isBlank()) {
                            if (!builder.isEmpty()) {
                                builder.append('\n');
                            }
                            builder.append(text);
                        }
                    }
                }
            }
        }
        return builder.toString();
    }

    private record OpenAiError(String code, String type) {
    }
}
