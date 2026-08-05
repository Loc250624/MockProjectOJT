package com.ojtsu26.elearning.service;

import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class LuminaActionDialogTemplateTest {

    @Test
    void sharedLayoutLoadsSingleCustomActionDialogSystem() throws Exception {
        String layout = Files.readString(
                Path.of("src/main/resources/templates/fragments/layout.html"));
        String fragment = Files.readString(
                Path.of("src/main/resources/templates/fragments/lumina-action-dialog.html"));
        String script = Files.readString(
                Path.of("src/main/resources/static/js/components/lumina-action-dialog.js"));
        String css = Files.readString(
                Path.of("src/main/resources/static/css/components/lumina-action-dialog.css"));

        assertTrue(layout.contains("fragments/layout :: action-dialog"));
        assertTrue(layout.contains("lumina-action-dialog.css"));
        assertTrue(layout.contains("lumina-action-dialog.js"));
        assertTrue(layout.contains("lumina-action-bindings.js"));
        assertTrue(fragment.contains("<dialog id=\"luminaActionDialog\""));
        assertTrue(script.contains("global.LuminaActionDialog"));
        assertTrue(script.contains("focusInitial"));
        assertTrue(script.contains("prefers-reduced-motion"));
        assertTrue(css.contains("@media (max-width: 640px)"));
        assertTrue(css.contains("html[data-theme=\"dark\"]"));
    }

    @Test
    void migratedMainResourcesContainNoBrowserConfirmOrAlert() throws Exception {
        try (var paths = Files.walk(Path.of("src/main"))) {
            String source = paths
                    .filter(Files::isRegularFile)
                    .filter(path -> path.toString().endsWith(".html")
                            || path.toString().endsWith(".js"))
                    .map(path -> {
                        try {
                            return Files.readString(path);
                        } catch (Exception exception) {
                            throw new RuntimeException(exception);
                        }
                    })
                    .reduce("", (left, right) -> left + "\n" + right);

            assertFalse(source.contains("window.confirm("));
            assertFalse(source.contains("return confirm("));
            assertFalse(source.contains("window.alert("));
        }
    }

    @Test
    void actionDialogsContainNoReasonFieldOrReasonConfiguration() throws Exception {
        String fragment = Files.readString(
                Path.of("src/main/resources/templates/fragments/lumina-action-dialog.html"));
        String script = Files.readString(
                Path.of("src/main/resources/static/js/components/lumina-action-dialog.js"));
        String bindings = Files.readString(
                Path.of("src/main/resources/static/js/components/lumina-action-bindings.js"));

        assertFalse(fragment.toLowerCase().contains("reason"));
        assertFalse(script.toLowerCase().contains("reason"));
        assertFalse(bindings.toLowerCase().contains("reason"));

        try (var paths = Files.walk(Path.of("src/main/resources/templates"))) {
            String templates = paths
                    .filter(Files::isRegularFile)
                    .filter(path -> path.toString().endsWith(".html"))
                    .map(path -> {
                        try {
                            return Files.readString(path);
                        } catch (Exception exception) {
                            throw new RuntimeException(exception);
                        }
                    })
                    .reduce("", (left, right) -> left + "\n" + right);

            assertFalse(templates.contains("data-dialog-show-reason"));
            assertFalse(templates.contains("data-dialog-require-reason"));
            assertFalse(templates.contains("data-dialog-reason-"));
            assertFalse(templates.contains("data-dialog-rejection-reason"));
        }
    }

    @Test
    void lowRiskFormsRemainConfirmationFree() throws Exception {
        String feedback = Files.readString(
                Path.of("src/main/resources/templates/student/feedback.html"));
        String publicBlog = Files.readString(
                Path.of("src/main/resources/templates/public/blog-detail.html"));
        String profile = Files.readString(
                Path.of("src/main/resources/templates/fragments/profile.html"));

        assertFalse(feedback.contains("data-lumina-confirm"));
        assertFalse(publicBlog.contains("data-lumina-confirm"));
        assertFalse(profile.contains("data-lumina-confirm"));
    }
}
