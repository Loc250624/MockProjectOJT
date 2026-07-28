package com.ojtsu26.elearning.service;

import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class TeacherAssessmentTemplateTest {

    @Test
    void removedAssessmentTemplatesDoNotExist() {
        Path teacherTemplates = Path.of(
                "src/main/resources/templates", "teacher");
        assertFalse(Files.exists(teacherTemplates.resolve("grading.html")));
        assertFalse(Files.exists(teacherTemplates.resolve("assignments.html")));
        assertFalse(Files.exists(teacherTemplates.resolve("testcases.html")));
    }

    @Test
    void teacherAssessmentJavascriptSendsCsrfHeadersForFetchMutations() throws Exception {
        String script = Files.readString(Path.of("src/main/resources/static/js/teacher/teacher.js"));

        assertTrue(script.contains("function teacherCsrfHeaders"));
        assertTrue(script.contains("XSRF-TOKEN"));
        assertTrue(script.contains("X-XSRF-TOKEN"));
        assertTrue(script.contains("function teacherFetch"));
        assertFalse(script.contains("fetch('/api/teacher"));
    }

    @Test
    void sharedSidebarLogoutUsesPostFormWithoutBroadJavascriptSelector() throws Exception {
        String layout = Files.readString(Path.of("src/main/resources/templates/fragments/layout.html"));
        String authScript = Files.readString(Path.of("src/main/resources/static/js/auth/auth.js"));

        assertTrue(layout.contains("th:fragment=\"logout-form\""));
        assertTrue(layout.contains("th:action=\"@{/auth/logout}\""));
        assertTrue(layout.contains("method=\"post\""));
        assertTrue(layout.contains("type=\"submit\""));
        assertTrue(layout.contains("th:name=\"${_csrf != null ? _csrf.parameterName : '_csrf'}\""));
        assertFalse(layout.contains("id=\"logoutBtn\""));
        assertFalse(authScript.contains("querySelectorAll(\".logout-btn, #logoutBtn\")"));
        assertTrue(authScript.contains("a.logout-btn[href$=\"/auth/logout\"]"));
    }
}
