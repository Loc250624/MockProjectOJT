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
        assertTrue(template.contains("fragments/layout :: brand-mark"));
        assertTrue(template.contains("data-csrf-token"));
        assertTrue(template.contains("/js/csrf-fetch.js"));
        assertTrue(template.contains("/js/student/student.js"));
        assertTrue(template.indexOf("/js/csrf-fetch.js") < template.indexOf("/js/student/student.js"));
        assertFalse(template.contains("learning-brand-mark\">L</span>"));
        assertFalse(template.contains("sidebar-student"));
        assertFalse(template.contains("layout-container"));
        assertFalse(template.contains("main-content"));
    }

    @Test
    void sharedBrandFragmentIsUsedByCoursePlayerAndRoleSidebars() throws Exception {
        String layout = Files.readString(Path.of("src/main/resources/templates/fragments/layout.html"));
        String learning = Files.readString(Path.of("src/main/resources/templates/student/learning.html"));

        assertTrue(layout.contains("th:fragment=\"brand-mark\""));
        assertTrue(layout.contains("th:fragment=\"sidebar-brand(homeHref, brandName, brandSub)\""));
        assertTrue(layout.contains("sidebar-brand('/', 'LumiNa Portal', 'Admin Governance')"));
        assertTrue(layout.contains("sidebar-brand('/', 'LumiNa Portal', 'Instructor Portal')"));
        assertTrue(layout.contains("sidebar-brand('/', 'LumiNa Portal', 'Student Learning')"));
        assertTrue(learning.contains("fragments/layout :: brand-mark"));
        assertFalse(learning.contains("<span class=\"learning-brand-mark\">L</span>"));
    }

    @Test
    void enrolledCourseCardsShowCompletedLabelWithoutChangingLearningLink() throws Exception {
        String myCourses = Files.readString(Path.of("src/main/resources/templates/student/my-courses.html"));
        String dashboard = Files.readString(Path.of("src/main/resources/templates/student/dashboard.html"));
        String expectedLabel = "course.completed ? 'Course Completed' : 'Continue Learning'";
        String expectedRoute = "@{/student/learning(courseId=${course.courseId},lessonId=${course.resumeLessonId})}";

        assertTrue(myCourses.contains(expectedLabel));
        assertTrue(myCourses.contains(expectedRoute));
        assertFalse(myCourses.contains("pointer-events: none"));

        assertTrue(dashboard.contains(expectedLabel));
        assertTrue(dashboard.contains(expectedRoute));
    }

    @Test
    void quizPanelJavascriptUsesExclusiveViewModeAndStaleRequestGuard() throws Exception {
        String script = Files.readString(Path.of("src/main/resources/static/js/student/student.js"));
        String quizScript = script.substring(
                script.indexOf("function initQuizPanel"),
                script.indexOf("function initCodePanel"));

        assertTrue(quizScript.contains("function setQuizViewMode(mode, label)"));
        assertTrue(quizScript.contains("mode !== 'loading'"));
        assertTrue(quizScript.contains("mode !== 'unavailable'"));
        assertTrue(quizScript.contains("mode !== 'taking'"));
        assertTrue(quizScript.contains("mode !== 'review'"));
        assertTrue(quizScript.contains("var requestSequence = 0"));
        assertTrue(quizScript.contains("function isCurrentRequest(requestId)"));
        assertTrue(quizScript.contains("setQuizViewMode('review'"));
        assertTrue(quizScript.contains("setQuizViewMode('taking'"));
        assertTrue(quizScript.contains("setQuizViewMode('unavailable'"));
        assertFalse(quizScript.contains("unavailable.querySelector('span').textContent"));
    }

    @Test
    void studentLearningCssIncludesResponsiveCurriculum() throws Exception {
        String css = Files.readString(Path.of("src/main/resources/static/css/student/student.css"));

        assertTrue(css.contains(".learning-player-page"));
        assertTrue(css.contains(".learning-player-header"));
        assertTrue(css.contains(".learning-layout"));
        assertTrue(css.contains(".learning-mobile-curriculum"));
        assertTrue(css.contains(".learning-assessment [hidden]"));
        assertTrue(css.contains("display: none !important"));
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
        String studentCss = Files.readString(Path.of("src/main/resources/static/css/student/student.css"));
        String studentCenter = Files.readString(Path.of("src/main/resources/templates/student/notifications.html"));

        assertTrue(layout.contains("data-notification-bell=\"true\""));
        assertTrue(layout.contains("data-notification-count"));
        assertTrue(layout.contains("data-notification-popover"));
        assertTrue(layout.contains("/js/csrf-fetch.js"));
        assertTrue(layout.contains("/js/notifications.js"));

        assertTrue(script.contains("/api/notifications/unread-count"));
        assertTrue(script.contains("/api/notifications/read-all"));
        assertTrue(script.contains("updateAllCounts"));
        assertTrue(script.contains("data.hasNext"));
        assertTrue(script.contains("setButtonBusy(loadMore"));
        assertTrue(script.contains("textContent"));
        assertFalse(script.contains("innerHTML = item.message"));

        assertTrue(studentCenter.contains("data-notification-center"));
        assertTrue(studentCenter.contains("data-notification-center-list"));
        assertTrue(studentCenter.contains("data-notification-load-more"));
        assertFalse(studentCenter.contains("Midterm Assessment Deadline Approaching"));
        assertTrue(studentCss.contains("[data-notification-load-more].btn[hidden]"));
    }

    @Test
    void portalFetchAddsCsrfToSameOriginMutatingRequests() throws Exception {
        String script = Files.readString(Path.of("src/main/resources/static/js/csrf-fetch.js"));

        assertTrue(script.contains("window.fetch = function"));
        assertTrue(script.contains("POST: true"));
        assertTrue(script.contains("PATCH: true"));
        assertTrue(script.contains("isSameOrigin"));
        assertTrue(script.contains("ensureCsrfToken"));
        assertTrue(script.contains("refreshCsrfToken"));
        assertTrue(script.contains("Promise.resolve(false)"));
        assertTrue(script.contains("CSRF_TOKEN_INVALID"));
        assertTrue(script.contains("CSRF_TOKEN_MISSING"));
        assertTrue(script.contains("XSRF-TOKEN"));
        assertTrue(script.contains("data-csrf-token"));
        assertFalse(script.contains("merged.set(headerName, String(token))"));
        assertFalse(script.contains("X-XSRF-TOKEN: null"));
        assertFalse(script.contains("X-XSRF-TOKEN: undefined"));
    }

    @Test
    void studentCourseDetailLoadsCsrfFetchBeforeEnrollmentScript() throws Exception {
        String template = Files.readString(Path.of("src/main/resources/templates/student/course-detail.html"));

        assertTrue(template.contains("/js/csrf-fetch.js"));
        assertTrue(template.contains("/js/student/student.js"));
        assertTrue(template.indexOf("/js/csrf-fetch.js") < template.indexOf("/js/student/student.js"));
    }
}
