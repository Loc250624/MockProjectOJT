package example.aichatbot;

import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/ai-chatbot")
public class AiChatbotController {

    private final AiChatbotService service;

    public AiChatbotController(AiChatbotService service) {
        this.service = service;
    }

    @PostMapping("/chat")
    public ResponseEntity<AiChatbotResponse> chat(
            @Valid @RequestBody AiChatbotRequest request,
            Authentication authentication
    ) {
        // Authentication may be null for an approved anonymous/public mode.
        // The service must derive permissions from the server-side principal.
        return ResponseEntity.ok(service.chat(request, authentication));
    }
}

/*
Reference only. Adapt to the repository's existing controller/service.
Prefer extending the current AI Tutor endpoint instead of duplicating it.
*/
