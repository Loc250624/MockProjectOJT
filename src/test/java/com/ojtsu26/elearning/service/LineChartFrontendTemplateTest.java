package com.ojtsu26.elearning.service;

import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class LineChartFrontendTemplateTest {

    @Test
    void remainingRevenueChartsUseLineRenderers() throws Exception {
        String adminScript = Files.readString(Path.of("src/main/resources/static/js/admin/analytics.js"));
        String teacherScript = Files.readString(Path.of("src/main/resources/static/js/teacher/analytics.js"));
        String adminTemplate = Files.readString(Path.of("src/main/resources/templates/admin/revenue-report.html"));
        String teacherTemplate = Files.readString(Path.of("src/main/resources/templates/teacher/analytics.html"));

        assertTrue(adminScript.contains("renderSingleLineChart"));
        assertTrue(adminScript.contains("Revenue line chart by period"));
        assertFalse(adminScript.contains("renderSingleBarChart"));
        assertFalse(adminScript.contains("Revenue bar chart by period"));

        assertTrue(teacherScript.contains("analytics-line-path"));
        assertTrue(teacherScript.contains("Revenue line chart by period"));
        assertFalse(teacherScript.contains("analytics-bar"));
        assertFalse(teacherScript.contains("Revenue bar chart by period"));

        assertTrue(adminTemplate.contains("admin-revenue-line-chart"));
        assertTrue(teacherTemplate.contains("analytics-line-chart"));
    }
}
