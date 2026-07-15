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
        assertTrue(template.contains("th:each=\"assignment : ${assignments}\""));
        assertTrue(template.contains("No assignments to show yet."));
        assertFalse(template.contains("layout-container"));
        assertFalse(template.contains("main-content"));
        assertFalse(template.contains("Student A"));
        assertFalse(template.contains("Assign 1"));
        assertFalse(template.contains("btn-info"));
    }
}
