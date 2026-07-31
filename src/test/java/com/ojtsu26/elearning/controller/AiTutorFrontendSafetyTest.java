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

        assertTrue(script.contains("renderMarkdown(bubble, text || '');"));
        assertTrue(script.contains("document.createElement('strong')"));
        assertTrue(script.contains("document.createElement(nextListType)"));
        assertFalse(script.contains("innerHTML"));
        assertFalse(script.contains("insertAdjacentHTML"));
        assertTrue(script.contains("sessionStorage"));
        assertFalse(script.contains("quick-actions-consumed"));
        assertFalse(script.contains("consumeQuickActions"));
        assertTrue(script.contains("function beginSuggestionTransition()"));
        assertTrue(script.contains("quickActionsContainer.hidden = history.some"));
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
        assertFalse(script.contains("document.body"));
        assertFalse(script.contains("document.documentElement.innerHTML"));
        assertTrue(styles.contains(".ai-chatbot-bubble strong"));
        assertTrue(styles.contains(".ai-chatbot-panel[hidden]"));
        assertTrue(styles.contains("display: none;"));
        assertTrue(styles.contains("@media (max-width: 640px)"));
        assertTrue(styles.contains(".ai-chatbot-content"));
        assertTrue(styles.contains("scrollbar-gutter: stable"));
        assertFalse(styles.contains("max-height: 15rem"));

        int greeting = fragment.indexOf("data-ai-chatbot-greeting");
        int actions = fragment.indexOf("data-ai-chatbot-quick-actions");
        assertTrue(greeting >= 0 && actions > greeting, "Greeting must render before quick actions");
        assertTrue(fragment.contains("data-ai-chatbot-related-actions"));
        assertTrue(fragment.contains("data-ai-chatbot-content"));
        assertTrue(fragment.contains("data-chat-authenticated"));
        assertTrue(fragment.contains("AI Chatbot"));
        assertTrue(fragment.contains("placeholder=\"Ask about this website\""));
        assertFalse(fragment.contains("AI Tutor"));
        assertFalse(fragment.contains("Ask about this lesson"));
    }

    @Test
    void everyPageTemplateGetsExactlyOneSharedChatbotMountPath() throws Exception {
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
                assertTrue(directMounts + sharedFooterMounts == 1,
                        () -> path + " must have exactly one direct or shared-footer chatbot mount");
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
