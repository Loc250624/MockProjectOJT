package com.ojtsu26.elearning.service;

import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PieChartFrontendTemplateTest {

    @Test
    void dashboardPieChartsUseInteractiveSvgAndLinkedLegends() throws Exception {
        String script = Files.readString(Path.of("src/main/resources/static/js/admin/analytics.js"));
        String stylesheet = Files.readString(Path.of("src/main/resources/static/css/admin/admin.css"));
        String template = Files.readString(Path.of("src/main/resources/templates/admin/dashboard.html"));

        assertTrue(script.contains("admin-pie-segment"));
        assertTrue(script.contains("admin-pie-legend-meter"));
        assertTrue(script.contains("function setActiveSlice"));
        assertTrue(script.contains("classList.toggle('is-active'"));
        assertTrue(script.contains("classList.toggle('is-muted'"));
        assertTrue(script.contains("addEventListener('mouseenter'"));
        assertTrue(script.contains("addEventListener('focus'"));
        assertFalse(script.contains("visual.style.background = 'conic-gradient("));

        assertTrue(stylesheet.contains(".admin-pie-segment.is-muted"));
        assertTrue(stylesheet.contains(".admin-pie-legend-item.is-active"));
        assertTrue(stylesheet.contains(".admin-pie-legend-item.is-muted"));
        assertTrue(template.contains("admin-pie-card"));
        assertTrue(template.contains("dashboardStudentsChart"));
        assertTrue(template.contains("dashboardRevenueChart"));
    }
}
