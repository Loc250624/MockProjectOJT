package com.ojtsu26.elearning.controller;

import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AdminUsersFeedbackThemeTest {

    @Test
    void adminUserAccountMessagesUseThemeAwareFeedbackClasses() throws Exception {
        String template = Files.readString(Path.of("src/main/resources/templates/admin/users.html"));
        String script = Files.readString(Path.of("src/main/resources/static/js/admin/admin.js"));
        String stylesheet = Files.readString(Path.of("src/main/resources/static/css/admin/admin.css"));

        assertTrue(template.contains("id=\"users-feedback\" class=\"admin-users-feedback\""));
        assertTrue(script.contains("classList.toggle('is-success'"));
        assertTrue(script.contains("classList.toggle('is-error'"));
        assertFalse(script.contains("elements.feedback.style.color"));
        assertTrue(stylesheet.contains(".admin-users-feedback.is-success"));
        assertTrue(stylesheet.contains(".admin-users-feedback.is-error"));
        assertTrue(stylesheet.contains("html[data-theme=\"dark\"] .admin-users-feedback.is-success"));
        assertTrue(stylesheet.contains("html[data-theme=\"dark\"] :is(.admin-users-feedback.is-error, .admin-user-form-message)"));
    }
}
