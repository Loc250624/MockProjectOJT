package com.ojtsu26.elearning.service;

import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AdminSettingsFrontendTemplateTest {

    @Test
    void settingsPageUsesAdminFriendlyInformationArchitecture() throws Exception {
        String template = Files.readString(Path.of("src/main/resources/templates/admin/settings.html"));
        String script = Files.readString(Path.of("src/main/resources/static/js/admin/settings.js"));

        assertTrue(template.contains("System Settings"));
        assertTrue(template.contains("Manage platform-wide preferences and operational controls."));
        assertTrue(template.contains("data-settings-tab=\"general\""));
        assertTrue(template.contains("data-settings-tab=\"learning\""));
        assertTrue(template.contains("data-settings-tab=\"payments\""));
        assertTrue(template.contains("data-settings-tab=\"maintenance\""));
        assertTrue(template.contains("data-settings-tab=\"advanced\""));

        assertTrue(script.contains("Site name"));
        assertTrue(script.contains("Support email"));
        assertTrue(script.contains("Default enrollment"));
        assertTrue(script.contains("Platform currency"));
        assertTrue(script.contains("Teacher revenue share"));
        assertTrue(script.contains("Maintenance mode"));
        assertTrue(script.contains("Activate immediately"));
        assertTrue(script.contains("Vietnamese Dong (VND)"));
        assertTrue(script.contains("percentToFraction"));
        assertTrue(script.contains("fractionToPercent"));
        assertTrue(script.contains("LuminaActionDialog.open"));
        assertFalse(script.contains("confirm("));
    }
}
