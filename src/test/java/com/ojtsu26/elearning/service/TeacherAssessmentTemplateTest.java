package com.ojtsu26.elearning.service;

import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class TeacherAssessmentTemplateTest {

    @Test
    void teacherGradingTemplateUsesPortalLayoutAndRealModelState() throws Exception {
        String template = Files.readString(Path.of("src/main/resources/templates/teacher/grading.html"));

        assertTrue(template.contains("fragments/layout :: sidebar-teacher"));
        assertTrue(template.contains("fragments/layout :: topbar-teacher('Grading')"));
        assertTrue(template.contains("@{/css/lumina-design-system.css}"));
        assertTrue(template.contains("@{/css/teacher/teacher.css}"));
        assertTrue(template.contains("@{/js/teacher/teacher.js}"));
        assertTrue(template.contains("name=\"_csrf\""));
        assertTrue(template.contains("name=\"_csrf_header\""));
        assertTrue(template.contains("th:each=\"submission : ${submissions}\""));
        assertTrue(template.contains("No submissions need attention."));
        assertFalse(template.contains("layout-container"));
        assertFalse(template.contains("main-content"));
        assertFalse(template.contains("Run mock judge"));
    }

    @Test
    void teacherAssignmentsTemplateUsesPortalLayoutAndNoHardCodedRows() throws Exception {
        String template = Files.readString(Path.of("src/main/resources/templates/teacher/assignments.html"));

        assertTrue(template.contains("fragments/layout :: sidebar-teacher"));
        assertTrue(template.contains("fragments/layout :: topbar-teacher('Assignments')"));
        assertTrue(template.contains("@{/css/lumina-design-system.css}"));
        assertTrue(template.contains("@{/css/teacher/teacher.css}"));
        assertTrue(template.contains("@{/js/teacher/teacher.js}"));
        assertTrue(template.contains("name=\"_csrf\""));
        assertTrue(template.contains("name=\"_csrf_header\""));
        assertTrue(template.contains("th:each=\"assignment : ${assignments}\""));
        assertTrue(template.contains("id=\"teacher-assignment-form\""));
        assertTrue(template.contains("data-assignment-id"));
        assertTrue(template.contains("teacher-assignment-message"));
        assertTrue(template.contains("No assignments to show yet."));
        assertFalse(template.contains("layout-container"));
        assertFalse(template.contains("main-content"));
        assertFalse(template.contains("Student A"));
        assertFalse(template.contains("Assign 1"));
        assertFalse(template.contains("btn-info"));
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
