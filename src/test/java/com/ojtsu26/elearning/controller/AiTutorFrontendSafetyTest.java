package com.ojtsu26.elearning.controller;

import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AiTutorFrontendSafetyTest {

    @Test
    void aiChatbotSafelyRendersMarkdownAndSendsBoundedPageContext() throws Exception {
        String script = Files.readString(Path.of("src/main/resources/static/js/ai-chatbot.js"));
        String styles = Files.readString(Path.of("src/main/resources/static/css/ai-chatbot.css"));
        String fragment = Files.readString(Path.of("src/main/resources/templates/fragments/ai-chatbot.html"));
        String promptFactory = Files.readString(Path.of("src/main/java/com/ojtsu26/elearning/service/ai/AiTutorPromptFactory.java"));

        assertTrue(script.contains("renderMarkdown(bubble, text || '');"));
        assertTrue(script.contains("document.createElement('strong')"));
        assertTrue(script.contains("document.createElement(nextListType)"));
        assertFalse(script.contains("innerHTML"));
        assertFalse(script.contains("insertAdjacentHTML"));
        assertTrue(script.contains("sessionStorage"));
        assertFalse(script.contains("quick-actions-consumed"));
        assertFalse(script.contains("consumeQuickActions"));
        assertTrue(script.contains("function beginSuggestionTransition()"));
        assertTrue(script.contains("data-ai-chatbot-suggestions-toggle"));
        assertTrue(script.contains("quickActionsContainer.hidden = hasUserHistory"));
        assertTrue(script.contains("quickActionsContainer.hidden = true;"));
        assertTrue(script.contains("if (!authenticated)"));
        assertTrue(script.contains("fetch('/api/ai-chatbot/chat'"));
        assertTrue(script.contains("fetch('/api/ai-chatbot/conversations/latest'"));
        assertTrue(script.contains("renderRelatedQuestions(data.suggestedQuestions || [])"));
        assertTrue(script.contains("content.scrollTop = content.scrollHeight"));
        assertTrue(script.contains("normalize('NFKC')"));
        assertTrue(script.contains("window.location.pathname"));
        assertTrue(script.contains("visibleText: collectVisiblePageText(pageKey)"));
        assertTrue(script.contains("'main [data-ai-page-context]'"));
        assertTrue(script.contains("selectUniqueActions(candidateActions, excluded, 3)"));
        assertTrue(script.contains("setAvailability('unavailable')"));
        assertTrue(promptFactory.contains("For active graded quiz questions"));
        assertFalse(script.contains("document.body"));
        assertFalse(script.contains("document.documentElement.innerHTML"));
        assertTrue(styles.contains(".ai-chatbot-bubble strong"));
        assertTrue(styles.contains(".ai-chatbot-panel[hidden]"));
        assertTrue(styles.contains("display: none;"));
        assertTrue(styles.contains("@media (max-width: 640px)"));
        assertTrue(styles.contains(".ai-chatbot-content"));
        assertTrue(styles.contains("scrollbar-gutter: stable"));
        assertTrue(styles.contains(".ai-chatbot-launcher-status"));
        assertTrue(styles.contains(".ai-chatbot-message-tools"));
        assertFalse(styles.contains("max-height: 15rem"));

        int greeting = fragment.indexOf("data-ai-chatbot-greeting");
        int actions = fragment.indexOf("data-ai-chatbot-quick-actions");
        assertTrue(greeting >= 0 && actions > greeting, "Greeting must render before quick actions");
        assertTrue(fragment.contains("data-ai-chatbot-related-actions"));
        assertTrue(fragment.contains("data-ai-chatbot-content"));
        assertTrue(fragment.contains("data-chat-authenticated"));
        assertTrue(fragment.contains("data-ai-chatbot-header-status"));
        assertTrue(fragment.contains("data-ai-chatbot-context-badge"));
        assertTrue(fragment.contains("data-ai-chatbot-suggestions-toggle"));
        assertTrue(fragment.contains("AI Chatbot"));
        assertTrue(fragment.contains("placeholder=\"Ask about this website\""));
        assertFalse(fragment.contains("AI Tutor"));
        assertFalse(fragment.contains("Ask about this lesson"));
    }

    @Test
    void everyMountedPageTemplateGetsExactlyOneSharedChatbotMountPath() throws Exception {
        Path templates = Path.of("src/main/resources/templates");
        try (var paths = Files.walk(templates)) {
            for (Path path : paths.filter(candidate -> candidate.toString().endsWith(".html"))
                    .filter(candidate -> !candidate.toString().contains("fragments"))
                    .filter(candidate -> !candidate.toString().contains("templates" + java.io.File.separator + "mail"))
                    .toList()) {
                String template = Files.readString(path);
                int directMounts = occurrences(template, "fragments/ai-chatbot :: widget");
                int sharedFooterMounts = occurrences(template, "fragments/layout :: footer")
                        + occurrences(template, "fragments/layout :: portal-footer");
                int totalMountPaths = directMounts + sharedFooterMounts;
                assertTrue(totalMountPaths <= 1,
                        () -> path + " must not render duplicate chatbot mounts");
            }
        }
    }

    private int occurrences(String value, String needle) {
        int count = 0;
        int index = 0;
        while ((index = value.indexOf(needle, index)) >= 0) {
            count++;
            index += needle.length();
        }
        return count;
    }
}
