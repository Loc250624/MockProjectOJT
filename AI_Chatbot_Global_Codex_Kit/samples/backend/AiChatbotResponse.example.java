package example.aichatbot;

public record AiChatbotResponse(
        String conversationId,
        String answer,
        String scope,
        boolean usedPageContext
) {}
