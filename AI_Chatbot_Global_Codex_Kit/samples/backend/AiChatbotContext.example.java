package example.aichatbot;

import java.util.List;
import java.util.Map;

public record AiChatbotContext(
        String scope,
        String pageKey,
        Map<String, Object> publicSiteContext,
        Map<String, Object> authorizedUserContext,
        Map<String, Object> pageContext,
        Map<String, Object> learningContext,
        List<String> safetyNotes
) {
    /*
     * Build this record on the server.
     * Never populate it from client-declared userId/role.
     * Resolve only the minimum context needed for the current request.
     */
}
