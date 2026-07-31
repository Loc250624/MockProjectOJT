package com.ojtsu26.elearning.service.ai;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AiChatSuggestionServiceTest {

    private final AiChatSuggestionService service = new AiChatSuggestionService();

    @Test
    void normalizationRejectsCaseWhitespacePunctuationAndUnicodeCompatibilityDuplicates() {
        List<String> existing = List.of("How do I enroll?");

        assertTrue(service.isDuplicateQuestion("  HOW   do I enroll!!! ", existing));
        assertTrue(service.isDuplicateQuestion(
                "\uFF28\uFF4F\uFF57 \uFF44\uFF4F \uFF29 \uFF45\uFF4E\uFF52\uFF4F\uFF4C\uFF4C\uFF1F",
                existing));
        assertEquals("how do i enroll", service.normalizeQuestion(" How do I enroll?! "));
    }

    @Test
    void nearDuplicatesAreRejectedButDistinctQuestionsRemain() {
        List<String> existing = List.of("How can I enroll in a LumiNa course");

        assertTrue(service.isDuplicateQuestion(
                "How can I enroll in a LumiNa course today?",
                existing));
        assertFalse(service.isDuplicateQuestion(
                "Where can I review my certificate eligibility?",
                existing));
    }

    @Test
    void selectionExcludesInitialUserAndPreviouslyShownQuestionsAndCapsAtFour() {
        List<String> selected = service.selectUniqueQuestions(
                List.of(
                        "Summarize?",
                        "How do I enroll!",
                        "Where is my dashboard?",
                        "How are lessons marked complete?",
                        "When is a certificate issued?",
                        "How can I review quiz results?",
                        "Where can I update my profile?"),
                List.of(
                        "Summarize",
                        "Explain simply",
                        "Give an example",
                        "Quiz me",
                        "How do I enroll",
                        "where is my dashboard"),
                4);

        assertEquals(4, selected.size());
        assertFalse(selected.stream().anyMatch(question ->
                service.isDuplicateQuestion(question, AiChatSuggestionService.INITIAL_QUESTIONS)));
        assertFalse(selected.stream().anyMatch(question ->
                service.isDuplicateQuestion(question, List.of("How do I enroll", "where is my dashboard"))));
    }

    @Test
    void filteringNeverInventsTemplateQuestionsWhenModelReturnsNoCandidates() {
        List<String> related = service.selectUniqueQuestions(
                List.of(),
                List.of("How do I find my certificates?"),
                AiChatSuggestionService.MAX_RELATED_QUESTIONS);

        assertTrue(related.isEmpty());
    }

    @Test
    void selectionRejectsAssistantLedOffersAndKeepsStudentAuthoredMessages() {
        List<String> related = service.selectUniqueQuestions(
                List.of(
                        "Do you need help finding the Certificates section?",
                        "Would you like step-by-step instructions for LinkedIn?",
                        "Are you interested in sharing your achievement?",
                        "Do you have any questions about certificate requirements?",
                        "Where can I find Certificates?",
                        "How do I add the PDF to LinkedIn?",
                        "Which certificate requirements remain?",
                        "Show me how to share my certificate."),
                List.of(),
                AiChatSuggestionService.MAX_RELATED_QUESTIONS);

        assertEquals(List.of(
                "Where can I find Certificates?",
                "How do I add the PDF to LinkedIn?",
                "Which certificate requirements remain?",
                "Show me how to share my certificate."), related);
    }

    @Test
    void selectionRejectsSuggestionsThatAreTooLongForAQuickAction() {
        String tooLong = "How do I " + "review ".repeat(20) + "my certificate?";

        assertTrue(tooLong.length() > AiChatSuggestionService.MAX_SUGGESTION_CHARS);
        assertTrue(service.selectUniqueQuestions(
                List.of(tooLong),
                List.of(),
                AiChatSuggestionService.MAX_RELATED_QUESTIONS).isEmpty());
    }
}
