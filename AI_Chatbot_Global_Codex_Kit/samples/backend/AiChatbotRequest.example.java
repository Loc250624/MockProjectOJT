package example.aichatbot;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.util.List;

public record AiChatbotRequest(
        @NotBlank
        @Size(max = 4000)
        String message,

        @Size(max = 100)
        String conversationId,

        Long lessonId,

        @Valid
        PageContext pageContext,

        @Size(max = 12)
        List<ChatMessage> recentMessages
) {
    public record PageContext(
            @Size(max = 500) String path,
            @Size(max = 100) String pageKey,
            @Size(max = 150) String title,
            @Size(max = 50) String entityType,
            @Size(max = 100) String entityId
    ) {}

    public record ChatMessage(
            @Size(max = 20) String role,
            @Size(max = 4000) String content
    ) {}
}
