package com.ojtsu26.elearning.service;

import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class TeacherCourseStudentsTemplateTest {

    @Test
    void teacherStudentsTemplateUsesServerSideFiltersAndResponsiveStates() throws Exception {
        String template = Files.readString(Path.of("src/main/resources/templates/teacher/students.html"));

        assertTrue(template.contains("th:action=\"@{/teacher/students}\""));
        assertTrue(template.contains("name=\"courseId\""));
        assertTrue(template.contains("name=\"search\""));
        assertTrue(template.contains("name=\"enrollmentStatus\""));
        assertTrue(template.contains("name=\"progressState\""));
        assertTrue(template.contains("role=\"progressbar\""));
        assertTrue(template.contains("teacher-student-cards"));
        assertTrue(template.contains("No matching students."));
        assertFalse(template.contains("Student A"));
        assertFalse(template.contains("a@a.com"));
    }

    @Test
    void teacherProgressReportTemplateUsesRealModelAndResponsiveStates() throws Exception {
        String template = Files.readString(Path.of("src/main/resources/templates/teacher/progress-report.html"));

        assertTrue(template.contains("th:action=\"@{/teacher/reports/progress}\""));
        assertTrue(template.contains("name=\"courseId\""));
        assertTrue(template.contains("name=\"search\""));
        assertTrue(template.contains("name=\"enrollmentStatus\""));
        assertTrue(template.contains("name=\"progressState\""));
        assertTrue(template.contains("name=\"lastActivityFrom\""));
        assertTrue(template.contains("name=\"lastActivityTo\""));
        assertTrue(template.contains("${overview.totalStudents}"));
        assertTrue(template.contains("role=\"progressbar\""));
        assertTrue(template.contains("teacher-student-cards"));
        assertTrue(template.contains("studentDetail.lessons"));
        assertTrue(template.contains("Assessment data unavailable"));
        assertFalse(template.contains("Student A"));
        assertFalse(template.contains("Java Basics</option>"));
        assertFalse(template.contains("Quiz Score"));
    }
}
