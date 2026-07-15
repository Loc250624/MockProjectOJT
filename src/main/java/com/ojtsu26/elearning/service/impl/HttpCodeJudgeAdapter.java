package com.ojtsu26.elearning.service.impl;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.ojtsu26.elearning.config.CodeJudgeProperties;
import com.ojtsu26.elearning.model.entity.CodingAssignment;
import com.ojtsu26.elearning.model.entity.Submission;
import com.ojtsu26.elearning.model.entity.Testcase;
import com.ojtsu26.elearning.model.enums.CodeJudgeStatus;
import com.ojtsu26.elearning.service.CodeJudgeAdapter;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Primary;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.List;

@Component
@Primary
@RequiredArgsConstructor
@ConditionalOnProperty(prefix = "app.code-judge", name = "endpoint")
public class HttpCodeJudgeAdapter implements CodeJudgeAdapter {
    private final CodeJudgeProperties properties;
    private final ObjectMapper objectMapper;

    @Override
    public JudgeOutcome judge(CodingAssignment assignment, Submission submission) {
        if (properties.getEndpoint() == null || properties.getEndpoint().isBlank()) {
            return unavailable(assignment);
        }
        long started = System.nanoTime();
        try {
            HttpClient client = HttpClient.newBuilder()
                    .connectTimeout(timeout())
                    .build();
            HttpRequest request = HttpRequest.newBuilder(URI.create(properties.getEndpoint()))
                    .timeout(timeout())
                    .header("Content-Type", MediaType.APPLICATION_JSON_VALUE)
                    .POST(HttpRequest.BodyPublishers.ofString(objectMapper.writeValueAsString(toRequest(assignment, submission))))
                    .build();
            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
            long elapsedMs = elapsedMs(started);
            if (response.statusCode() < 200 || response.statusCode() >= 300) {
                return new JudgeOutcome(
                        CodeJudgeStatus.ERROR,
                        testcaseCount(assignment),
                        0,
                        List.of(),
                        "Code judge returned HTTP " + response.statusCode(),
                        elapsedMs
                );
            }
            JudgeResponse body = objectMapper.readValue(response.body(), JudgeResponse.class);
            CodeJudgeStatus status = body.status == null ? CodeJudgeStatus.ERROR : body.status;
            List<JudgeCaseOutcome> cases = body.results == null
                    ? List.of()
                    : body.results.stream()
                    .map(result -> new JudgeCaseOutcome(result.testcaseId, Boolean.TRUE.equals(result.passed)))
                    .toList();
            int total = body.totalTests == null ? testcaseCount(assignment) : body.totalTests;
            int passed = body.passedTests == null
                    ? (int) cases.stream().filter(JudgeCaseOutcome::passed).count()
                    : body.passedTests;
            return new JudgeOutcome(status, total, passed, cases, body.outputLog, body.executionTimeMs == null ? elapsedMs : body.executionTimeMs);
        } catch (java.net.http.HttpTimeoutException e) {
            return new JudgeOutcome(CodeJudgeStatus.TIMEOUT, testcaseCount(assignment), 0, List.of(),
                    "Code judge timed out.", elapsedMs(started));
        } catch (IOException | InterruptedException | IllegalArgumentException e) {
            if (e instanceof InterruptedException) {
                Thread.currentThread().interrupt();
            }
            return new JudgeOutcome(CodeJudgeStatus.ERROR, testcaseCount(assignment), 0, List.of(),
                    "Code judge failed: " + e.getClass().getSimpleName(), elapsedMs(started));
        }
    }

    private JudgeOutcome unavailable(CodingAssignment assignment) {
        return new JudgeOutcome(CodeJudgeStatus.UNAVAILABLE, testcaseCount(assignment), 0, List.of(),
                "Code judge is not configured. Set app.code-judge.endpoint to an external sandbox service.", 0L);
    }

    private JudgeRequest toRequest(CodingAssignment assignment, Submission submission) {
        List<JudgeTestcaseRequest> testcases = assignment == null || assignment.getTestcases() == null
                ? List.of()
                : assignment.getTestcases().stream()
                .map(this::toTestcaseRequest)
                .toList();
        return new JudgeRequest(
                assignment == null ? null : assignment.getId(),
                submission.getId(),
                submission.getCodeLanguage(),
                submission.getCodeContent(),
                assignment == null ? null : assignment.getTimeLimitMs(),
                testcases
        );
    }

    private JudgeTestcaseRequest toTestcaseRequest(Testcase testcase) {
        return new JudgeTestcaseRequest(
                testcase.getId(),
                testcase.getInputData(),
                testcase.getExpectedOutput(),
                Boolean.TRUE.equals(testcase.getIsHidden())
        );
    }

    private int testcaseCount(CodingAssignment assignment) {
        return assignment == null || assignment.getTestcases() == null ? 0 : assignment.getTestcases().size();
    }

    private Duration timeout() {
        return Duration.ofMillis(Math.max(1000, properties.getTimeoutMs()));
    }

    private long elapsedMs(long started) {
        return Duration.ofNanos(System.nanoTime() - started).toMillis();
    }

    private record JudgeRequest(Integer assignmentId, Integer submissionId, String language, String code,
                                Integer timeLimitMs, List<JudgeTestcaseRequest> testcases) {
    }

    private record JudgeTestcaseRequest(Integer id, String input, String expectedOutput, boolean hidden) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    private static class JudgeResponse {
        public CodeJudgeStatus status;
        public Integer totalTests;
        public Integer passedTests;
        public String outputLog;
        public Long executionTimeMs;
        public List<CaseResult> results;
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    private static class CaseResult {
        public Integer testcaseId;
        public Boolean passed;
    }
}
