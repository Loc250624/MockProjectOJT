package com.ojtsu26.elearning.service;

import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class StudentFeedbackFrontendTemplateTest {

    @Test
    void simplifiedFormContainsNoCategoryControlsOrCategoryValidation() throws Exception {
        String template = Files.readString(Path.of("src/main/resources/templates/student/feedback.html"));
        String controller = Files.readString(Path.of(
                "src/main/java/com/ojtsu26/elearning/controller/StudentFeedbackController.java"));

        assertFalse(template.contains("feedback-category-panel"));
        assertFalse(template.contains("category-grid"));
        assertFalse(template.contains("*{category}"));
        assertFalse(template.contains("feedbackCategories"));
        assertFalse(template.contains("Choose a topic"));
        assertFalse(controller.contains("\"category\","));
        assertFalse(controller.contains("feedbackCategories"));
    }

    @Test
    void ratingMatrixUsesCenteredResponsiveGridAndTouchSizedStars() throws Exception {
        String template = Files.readString(Path.of("src/main/resources/templates/student/feedback.html"));
        String css = Files.readString(Path.of("src/main/resources/static/css/student/student.css"));

        assertTrue(template.contains("rating-row rating-row--overall"));
        assertTrue(template.contains("aria-label=\"Rating scale from 1 to 5\""));
        assertTrue(css.contains("width: min(960px, 100%);"));
        assertTrue(css.contains("grid-template-columns: repeat(2, minmax(0, 1fr));"));
        assertTrue(css.contains("@media (max-width: 767px)"));
        assertTrue(css.contains("width: 2.75rem;"));
        assertTrue(css.contains("width: 2.625rem;"));
        assertFalse(css.contains(".category-option"));
        assertFalse(css.contains(".category-grid"));
    }
}
