package com.ojtsu26.elearning.service.ai.question;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;

import java.util.*;

@Component
public class OpenAiQuestionGenerationClient implements QuestionGenerationProvider {
    private final RestClient client;
    private final ObjectMapper objectMapper;
    private final boolean enabled;
    private final String apiKey;
    private final String model;
    private final int maxRetries;
    private final int rateLimitPerMinute;
    private long rateWindowStartedAt;
    private int rateWindowCount;

    public OpenAiQuestionGenerationClient(
            ObjectMapper objectMapper,
            @Value("${app.question-generation.enabled:false}") boolean enabled,
            @Value("${app.question-generation.api-key:}") String apiKey,
            @Value("${app.question-generation.base-url:https://api.openai.com}") String baseUrl,
            @Value("${app.question-generation.model:gpt-5.6-sol}") String model,
            @Value("${app.question-generation.timeout-ms:30000}") int timeoutMs,
            @Value("${app.question-generation.max-retries:2}") int maxRetries,
            @Value("${app.question-generation.rate-limit-per-minute:5}") int rateLimitPerMinute) {
        this.objectMapper = objectMapper;
        this.enabled = enabled;
        this.apiKey = apiKey;
        this.model = model;
        this.maxRetries = Math.max(0, maxRetries);
        this.rateLimitPerMinute = Math.max(1, rateLimitPerMinute);
        SimpleClientHttpRequestFactory requestFactory = new SimpleClientHttpRequestFactory();
        requestFactory.setConnectTimeout(timeoutMs);
        requestFactory.setReadTimeout(timeoutMs);
        this.client = RestClient.builder().baseUrl(baseUrl).requestFactory(requestFactory).build();
    }

    @Override
    public GeneratedQuestionBatch generate(QuestionGenerationRequest request) {
        if (!enabled || apiKey == null || apiKey.isBlank()) {
            throw new IllegalStateException("Question generation provider is not configured");
        }
        acquireRatePermit();
        Map<String, Object> body = requestBody(request);
        RestClientResponseException last = null;
        for (int attempt = 0; attempt <= maxRetries; attempt++) {
            try {
                JsonNode response = client.post()
                        .uri("/v1/responses")
                        .header("Authorization", "Bearer " + apiKey)
                        .body(body)
                        .retrieve()
                        .body(JsonNode.class);
                String outputText = extractOutputText(response);
                return objectMapper.readValue(outputText, GeneratedQuestionBatch.class);
            } catch (RestClientResponseException ex) {
                last = ex;
                if (attempt == maxRetries
                        || (ex.getStatusCode().value() != 429 && !ex.getStatusCode().is5xxServerError())) {
                    throw new IllegalStateException("Question generation provider request failed", ex);
                }
            } catch (Exception ex) {
                throw new IllegalStateException("Question generation provider returned invalid structured output", ex);
            }
        }
        throw new IllegalStateException("Question generation provider request failed", last);
    }

    private Map<String, Object> requestBody(QuestionGenerationRequest request) {
        Map<String, Object> questionSchema = new LinkedHashMap<>();
        questionSchema.put("type", "object");
        questionSchema.put("properties", Map.of(
                "questionText", Map.of("type", "string"),
                "options", Map.of("type", "array", "items", Map.of("type", "string"), "minItems", 2, "maxItems", 6),
                "correctAnswer", Map.of("type", "string"),
                "topicCode", Map.of("type", "string"),
                "difficulty", Map.of("type", "string", "enum", List.of("EASY", "MEDIUM", "HARD"))));
        questionSchema.put("required", List.of(
                "questionText", "options", "correctAnswer", "topicCode", "difficulty"));
        questionSchema.put("additionalProperties", false);
        Map<String, Object> schema = new LinkedHashMap<>();
        schema.put("type", "object");
        schema.put("properties", Map.of("questions", Map.of(
                "type", "array",
                "minItems", request.questionCount(),
                "maxItems", request.questionCount(),
                "items", questionSchema)));
        schema.put("required", List.of("questions"));
        schema.put("additionalProperties", false);

        String prompt = "Generate exactly " + request.questionCount()
                + " assessment questions grounded only in the supplied lesson content. "
                + "Topic code: " + request.topicCode()
                + ". Difficulty: " + request.difficulty()
                + ". Each correctAnswer must exactly equal one option. Lesson content:\n"
                + request.lessonContent();
        return Map.of(
                "model", model,
                "store", false,
                "input", List.of(
                        Map.of("role", "system", "content",
                                "Create concise, unambiguous e-learning multiple-choice questions."),
                        Map.of("role", "user", "content", prompt)),
                "text", Map.of("format", Map.of(
                        "type", "json_schema",
                        "name", "question_generation_batch",
                        "strict", true,
                        "schema", schema)));
    }

    private String extractOutputText(JsonNode response) {
        if (response == null || !"completed".equals(response.path("status").asText())) {
            throw new IllegalStateException("Question generation response was incomplete");
        }
        for (JsonNode output : response.path("output")) {
            if (!"message".equals(output.path("type").asText())) {
                continue;
            }
            for (JsonNode content : output.path("content")) {
                if ("refusal".equals(content.path("type").asText())) {
                    throw new IllegalStateException("Question generation request was refused");
                }
                if ("output_text".equals(content.path("type").asText())) {
                    return content.path("text").asText();
                }
            }
        }
        throw new IllegalStateException("Question generation response had no output text");
    }

    private synchronized void acquireRatePermit() {
        long now = System.currentTimeMillis();
        if (now - rateWindowStartedAt >= 60_000L) {
            rateWindowStartedAt = now;
            rateWindowCount = 0;
        }
        if (rateWindowCount >= rateLimitPerMinute) {
            throw new IllegalStateException("Question generation rate limit reached");
        }
        rateWindowCount++;
    }

    @Override
    public String providerName() {
        return "OPENAI_RESPONSES";
    }

    @Override
    public String modelName() {
        return model;
    }
}
