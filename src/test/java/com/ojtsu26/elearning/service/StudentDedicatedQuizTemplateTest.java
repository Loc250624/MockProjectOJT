package com.ojtsu26.elearning.service;

import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class StudentDedicatedQuizTemplateTest {

    @Test
    void lessonQuizRendersOverviewWithoutInlineAttemptControls() throws Exception {
        String learning = Files.readString(
                Path.of("src/main/resources/templates/student/learning.html"));

        assertTrue(learning.contains("id=\"quiz-overview-state\""));
        assertTrue(learning.contains("quiz-overview-skeleton"));
        assertFalse(learning.contains("id=\"quiz-form\""));
        assertFalse(learning.contains("id=\"quiz-questions\""));
        assertFalse(learning.contains("id=\"quiz-save-button\""));
        assertFalse(learning.contains("id=\"quiz-submit-button\""));
        assertFalse(learning.contains("Quiz submitted"));
        assertFalse(learning.contains("Quiz unavailable"));
        assertFalse(learning.contains("Loading quiz"));
    }

    @Test
    void attemptPageHasDedicatedAccessibleNavigationAndExclusiveResultBranch()
            throws Exception {
        String quiz = Files.readString(
                Path.of("src/main/resources/templates/student/quiz.html"));

        assertTrue(quiz.contains("class=\"lumina-bg dedicated-quiz-page\""));
        assertTrue(quiz.contains("data-attempt-id=${attempt.id}"));
        assertTrue(quiz.contains("class=\"dedicated-quiz-nav-item\""));
        assertTrue(quiz.contains("aria-current="));
        assertTrue(quiz.contains("id=\"clear-selection-btn\""));
        assertTrue(quiz.contains("id=\"save-quiz-btn\""));
        assertTrue(quiz.contains("data-submit-quiz=\"true\""));
        assertTrue(quiz.contains("data-remaining-seconds=${attempt.remainingSeconds}"));
        assertTrue(quiz.contains("data-duration-minutes=${quiz.durationMinutes}"));
        assertTrue(quiz.contains("id=\"quiz-countdown\""));
        assertTrue(quiz.contains("role=\"timer\""));
        assertTrue(quiz.contains("All questions are required"));
        assertTrue(quiz.contains("th:if=\"${attempt.status.name() == 'DRAFT'}\""));
        assertTrue(quiz.contains("th:unless=\"${attempt.status.name() == 'DRAFT'}\""));
        assertFalse(quiz.contains("No timer"));
        assertFalse(quiz.contains("Quiz data is unavailable"));
    }

    @Test
    void quizJavascriptUsesOverviewAndAttemptRoutesWithoutStartingOnRefresh()
            throws Exception {
        String script = Files.readString(
                Path.of("src/main/resources/static/js/student/student.js"));
        String dedicatedFlow = script.substring(
                script.indexOf("function initDedicatedQuizAttempt()"),
                script.indexOf("function switchProfileTab(tabName)"));

        assertTrue(script.contains("function initQuizOverview(panel)"));
        assertTrue(script.contains("/quiz-overview"));
        assertTrue(script.contains("/quiz/attempt/"));
        assertTrue(dedicatedFlow.contains("data-attempt-id"));
        assertTrue(dedicatedFlow.contains("function firstUnansweredIndex()"));
        assertTrue(dedicatedFlow.contains("All questions are required."));
        assertTrue(dedicatedFlow.contains("function formatCountdown(totalSeconds)"));
        assertTrue(dedicatedFlow.contains("window.setInterval(updateCountdown, 250)"));
        assertTrue(dedicatedFlow.contains("submitQuiz(true)"));
        assertFalse(dedicatedFlow.contains("Submit anyway?"));
        assertFalse(dedicatedFlow.contains("ensureAttempt"));
    }
}
