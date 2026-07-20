package com.ojtsu26.elearning.service.ai;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.ojtsu26.elearning.config.AiTutorProperties;
import com.ojtsu26.elearning.dto.request.AiTutorChatRequestDTO;
import com.ojtsu26.elearning.dto.response.AiTutorChatResponseDTO;
import com.ojtsu26.elearning.model.entity.User;
import com.ojtsu26.elearning.model.enums.Role;
import com.ojtsu26.elearning.model.enums.UserStatus;
import com.ojtsu26.elearning.security.CustomUserDetails;
import com.ojtsu26.elearning.service.StudentLearningService;
import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class AiTutorEvalCasesTest {

    @Test
    void jsonlEvalCasesMatchExpectedLocalRefusalBehavior() throws Exception {
        StudentLearningService studentLearningService = mock(StudentLearningService.class);
        when(studentLearningService.getAuthorizedAiTutorLessonContext(anyInt())).thenReturn(new AiTutorLessonContext(
                10,
                101,
                "Java Basics",
                "Lesson 1",
                "Encapsulation",
                "Understand object-oriented programming",
                "Encapsulation keeps object state private and exposes controlled methods."
        ));
        AiTutorProperties properties = new AiTutorProperties();
        properties.setRateLimitMaxRequests(100);
        AiTutorService service = new AiTutorService(
                studentLearningService,
                new AiTutorTopicGuard(),
                new AiTutorPromptFactory(properties),
                prompt -> new AiTutorProviderResponse("Grounded lesson answer.", "eval_stub"),
                new AiTutorRateLimiter(properties),
                properties
        );
        CustomUserDetails principal = new CustomUserDetails(User.builder()
                .id(99)
                .role(Role.STUDENT)
                .status(UserStatus.ACTIVE)
                .email("eval.student@example.com")
                .build());

        ObjectMapper objectMapper = new ObjectMapper();
        List<String> lines = Files.readAllLines(Path.of("evals/ai_tutor_eval_cases.jsonl")).stream()
                .filter(line -> !line.isBlank())
                .toList();
        int passed = 0;
        for (String line : lines) {
            JsonNode node = objectMapper.readTree(line);
            AiTutorChatRequestDTO request = new AiTutorChatRequestDTO();
            request.setLessonId(node.path("lessonId").asInt());
            request.setMessage(node.path("message").asText());
            request.setAction(node.path("action").asText(""));

            AiTutorChatResponseDTO response = service.chat(principal, request);
            boolean expectedRefused = node.path("expectedRefused").asBoolean();
            assertEquals(expectedRefused, response.isRefused(), "Eval case failed: " + node.path("id").asText());
            passed++;
        }
        assertEquals(lines.size(), passed);
    }
}
