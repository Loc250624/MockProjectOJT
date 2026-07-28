package com.ojtsu26.elearning.service;

import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ResponsiveFrontendTemplateTest {

    private static String resource(String relativePath) throws Exception {
        return Files.readString(Path.of("src/main/resources", relativePath));
    }

    @Test
    void sharedHeaderUsesOneBrandAndAnAccessibleOverlayMenu() throws Exception {
        String layout = resource("templates/fragments/layout.html");
        String publicScript = resource("static/js/public/public.js");
        String designSystem = resource("static/css/lumina-design-system.css");

        assertTrue(layout.contains("fragments/brand :: mark"));
        assertTrue(layout.contains("class=\"public-nav-panel\""));
        assertTrue(layout.contains("aria-controls=\"public-navigation\""));
        assertTrue(layout.contains("/js/public/public.js"));
        assertTrue(publicScript.contains("event.key !== 'Escape'"));
        assertTrue(publicScript.contains("publicNavReady"));
        assertTrue(designSystem.contains("@media (max-width: 980px)"));
        assertTrue(designSystem.contains(".lumina-header.public-nav-open .public-nav-panel"));
    }

    @Test
    void coursePlayerUsesSharedBrandAndMobileActionGrid() throws Exception {
        String learning = resource("templates/student/learning.html");
        String studentCss = resource("static/css/student/student.css");

        assertTrue(learning.contains("fragments/brand :: mark"));
        assertFalse(learning.contains("learning-brand-mark\">L"));
        assertTrue(studentCss.contains(".learning-brand .logo-icon"));
        assertTrue(studentCss.contains("grid-template-columns: repeat(2, minmax(0, 1fr))"));
        assertTrue(studentCss.contains("@media (max-width: 360px)"));
    }

    @Test
    void targetDataListsRenderAsLabelledCardsWithExpandableDetails() throws Exception {
        String teacherCourses = resource("templates/teacher/courses.html");
        String roadmaps = resource("templates/teacher/roadmap.html");
        String adminCourses = resource("templates/admin/courses.html");
        String dashboard = resource("templates/teacher/dashboard.html");
        String designSystem = resource("static/css/lumina-design-system.css");

        assertTrue(teacherCourses.contains("responsive-data-table"));
        assertTrue(teacherCourses.contains("responsive-row-details"));
        assertTrue(teacherCourses.contains("class=\"responsive-row-details\" open"));
        assertTrue(teacherCourses.contains("data-label=\"Course\""));
        assertTrue(roadmaps.contains("responsive-data-table"));
        assertTrue(roadmaps.contains("View description"));
        assertTrue(adminCourses.contains("responsive-data-table"));
        assertTrue(dashboard.contains("responsive-data-table"));
        assertTrue(designSystem.contains("content: attr(data-label)"));
        assertTrue(designSystem.contains(".responsive-row-details:not([open])"));
    }

    @Test
    void responsiveLayoutDoesNotMaskPageOverflowGlobally() throws Exception {
        String designSystem = resource("static/css/lumina-design-system.css");
        String studentCss = resource("static/css/student/student.css");

        assertFalse(designSystem.contains(".lumina-portal {\n  position: relative;\n  display: grid;\n  grid-template-columns: var(--sidebar-width) 1fr;\n  min-height: 100vh;\n  width: 100%;\n  overflow-x: hidden;"));
        assertFalse(studentCss.contains(".learning-player-page {\n  min-height: 100vh;\n  margin: 0;\n  background: #f6f9ff;\n  color: var(--lumina-gray-900);\n  overflow-x: hidden;"));
    }
}
