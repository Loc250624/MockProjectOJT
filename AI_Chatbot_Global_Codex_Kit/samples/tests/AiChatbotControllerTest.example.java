package example.aichatbot;

import org.junit.jupiter.api.Test;

class AiChatbotControllerTest {

    @Test
    void acceptsSiteWideRequestWithoutLessonId() {
        // Build a request containing message + pageContext, with lessonId null.
        // Expect success and a SITE or PUBLIC scope.
    }

    @Test
    void ignoresClientDeclaredRole() {
        // The real request contract must not allow client role to override
        // Spring Security Authentication.
    }

    @Test
    void rejectsUnauthorizedLessonContextWithoutLeakingContent() {
        // Use a lesson belonging to another user/private course.
        // Expect a safe response and no lesson content in the model context.
    }

    @Test
    void anonymousModeUsesOnlyPublicContext() {
        // No principal. Expect public help only.
    }
}
