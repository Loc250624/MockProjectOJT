package com.ojtsu26.elearning.service;

import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CourseEnrollmentFrontendTemplateTest {

    @Test
    void studentCourseDetailRendersEnrollmentStates() throws Exception {
        String template = Files.readString(Path.of("src/main/resources/templates/student/course-detail.html"));

        assertTrue(template.contains("Enroll for free"));
        assertTrue(template.contains("Buy course"));
        assertTrue(template.contains("Payment pending"));
        assertTrue(template.contains("Continue learning"));
        assertTrue(template.contains("Course unavailable"));
        assertTrue(template.contains("data-enrollment-action=\"free\""));
    }

    @Test
    void studentJavascriptSubmitsAndRefreshesEnrollmentState() throws Exception {
        String script = Files.readString(Path.of("src/main/resources/static/js/student/student.js"));

        assertTrue(script.contains("initCourseEnrollmentCta"));
        assertTrue(script.contains("'/student/courses/' + courseId + '/enroll'"));
        assertTrue(script.contains("'/student/courses/' + courseId + '/enrollment-state'"));
        assertTrue(script.contains("enrollButton.disabled = true"));
    }

    @Test
    void studentLearningTemplateUsesRealCurriculumAndSafeContent() throws Exception {
        String template = Files.readString(Path.of("src/main/resources/templates/student/learning.html"));

        assertTrue(template.contains("learningCourse.lessons"));
        assertTrue(template.contains("learningCourse.activeLesson"));
        assertTrue(template.contains("th:utext=\"${learningCourse.activeLesson.sanitizedContent}\""));
        assertTrue(template.contains("Previous"));
        assertTrue(template.contains("Next lesson"));
        assertTrue(template.contains("No resources"));
        assertTrue(template.contains("learning-player-page"));
        assertTrue(template.contains("learning-player-header"));
        assertFalse(template.contains("sidebar-student"));
        assertFalse(template.contains("layout-container"));
        assertFalse(template.contains("main-content"));
    }

    @Test
    void studentLearningCssIncludesResponsiveCurriculum() throws Exception {
        String css = Files.readString(Path.of("src/main/resources/static/css/student/student.css"));

        assertTrue(css.contains(".learning-player-page"));
        assertTrue(css.contains(".learning-player-header"));
        assertTrue(css.contains(".learning-layout"));
        assertTrue(css.contains(".learning-mobile-curriculum"));
        assertTrue(css.contains("@media (max-width: 900px)"));
    }

    @Test
    void studentSidebarKeepsSingleProfileEntryInAccountSection() throws Exception {
        String layout = Files.readString(Path.of("src/main/resources/templates/fragments/layout.html"));

        assertFalse(layout.contains("nav-student-profile-learning"));
        assertTrue(layout.contains("id=\"nav-student-profile\""));
        assertTrue(layout.contains("My Profile"));
    }

    @Test
    void notificationTopbarsUseSharedAuthenticatedComponent() throws Exception {
        String layout = Files.readString(Path.of("src/main/resources/templates/fragments/layout.html"));
        String script = Files.readString(Path.of("src/main/resources/static/js/notifications.js"));
        String studentCenter = Files.readString(Path.of("src/main/resources/templates/student/notifications.html"));

        assertTrue(layout.contains("data-notification-bell=\"true\""));
        assertTrue(layout.contains("data-notification-count"));
        assertTrue(layout.contains("data-notification-popover"));
        assertTrue(layout.contains("/js/notifications.js"));

        assertTrue(script.contains("/api/notifications/unread-count"));
        assertTrue(script.contains("/api/notifications/read-all"));
        assertTrue(script.contains("textContent"));
        assertFalse(script.contains("innerHTML = item.message"));

        assertTrue(studentCenter.contains("data-notification-center"));
        assertTrue(studentCenter.contains("data-notification-center-list"));
        assertTrue(studentCenter.contains("data-notification-load-more"));
        assertFalse(studentCenter.contains("Midterm Assessment Deadline Approaching"));
    }
}
