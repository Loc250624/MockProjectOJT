package com.ojtsu26.elearning.controller;

import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ProfilePasswordFrontendTemplateTest {

    @Test
    void passwordFormIsASeparateSiblingInsideEditPanel() throws Exception {
        String template = Files.readString(Path.of("src/main/resources/templates/student/profile.html"));
        int editPanel = template.indexOf("data-profile-panel=\"edit\"");
        int profileForm = template.indexOf("id=\"profile-edit-form\"", editPanel);
        int profileFormClose = template.indexOf("</form>", profileForm);
        int passwordForm = template.indexOf("id=\"profile-password-form\"", profileFormClose);
        int editPanelClose = template.indexOf("data-profile-panel=\"security\"", passwordForm);

        assertTrue(editPanel >= 0);
        assertTrue(profileFormClose < passwordForm);
        assertTrue(passwordForm < editPanelClose);
        assertFalse(template.contains("normalizedProfileRole == 'student' or normalizedProfileRole == 'teacher'"));
        assertTrue(template.contains("AuthProvider).LOCAL"));
        assertTrue(template.contains("profile-password-provider-note"));
        assertTrue(template.contains("/js/common/profile-password.js"));
        assertFalse(template.contains("name=\"userId\""));
    }

    @Test
    void teacherAndAdminProfilesReuseTheSharedPasswordCard() throws Exception {
        String fragment = Files.readString(Path.of("src/main/resources/templates/fragments/profile.html"));
        String teacher = Files.readString(Path.of("src/main/resources/templates/teacher/profile.html"));
        String admin = Files.readString(Path.of("src/main/resources/templates/admin/profile.html"));

        assertTrue(fragment.contains("th:fragment=\"password-card\""));
        for (String template : new String[]{teacher, admin}) {
            assertTrue(template.contains("fragments/profile :: password-card"));
            assertTrue(template.contains("/css/common/profile-password.css"));
            assertTrue(template.contains("/js/common/profile-password.js"));
        }
    }

    @Test
    void accountCreationFormsExposeTheSameStrongPasswordChecklist() throws Exception {
        String register = Files.readString(Path.of("src/main/resources/templates/auth/register.html"));
        String oauth = Files.readString(Path.of("src/main/resources/templates/auth/oauth2-complete.html"));

        for (String template : new String[]{register, oauth}) {
            assertTrue(template.contains("data-auth-password-rules"));
            assertTrue(template.contains("data-auth-password-rule=\"lowercase\""));
            assertTrue(template.contains("data-auth-password-rule=\"uppercase\""));
            assertTrue(template.contains("data-auth-password-rule=\"special\""));
            assertTrue(template.contains("minlength=\"8\""));
            assertTrue(template.contains("maxlength=\"72\""));
        }
    }
}
