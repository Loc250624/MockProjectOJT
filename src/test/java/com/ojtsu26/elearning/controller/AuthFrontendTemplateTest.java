package com.ojtsu26.elearning.controller;

import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AuthFrontendTemplateTest {

    @Test
    void loginKeepsLocalFormAndSeparateProviderButtons() throws Exception {
        String template = Files.readString(Path.of("src/main/resources/templates/auth/login.html"));

        assertTrue(template.contains("id=\"loginForm\""));
        assertTrue(template.contains("/oauth2/authorization/google"));
        assertTrue(template.contains("/oauth2/authorization/github"));
        assertTrue(template.contains("data-oauth-provider=\"google\""));
        assertTrue(template.contains("data-oauth-provider=\"github\""));
        assertFalse(template.contains("name=\"role\""));
    }

    @Test
    void registerKeepsLocalStudentRegistrationWithoutRoleSelector() throws Exception {
        String template = Files.readString(Path.of("src/main/resources/templates/auth/register.html"));

        assertTrue(template.contains("id=\"registerForm\""));
        assertTrue(template.contains("name=\"fullName\""));
        assertTrue(template.contains("name=\"password\""));
        assertTrue(template.contains("name=\"confirmPassword\""));
        assertFalse(template.contains("name=\"role\""));
    }

    @Test
    void oauthCompletionTemplateUsesReadonlyVerifiedEmailAndPasswordFields() throws Exception {
        String template = Files.readString(Path.of("src/main/resources/templates/auth/oauth2-complete.html"));

        assertTrue(template.contains("id=\"oauthCompleteForm\""));
        assertTrue(template.contains("id=\"verifiedEmail\""));
        assertTrue(template.contains("readonly"));
        assertTrue(template.contains("name=\"password\""));
        assertTrue(template.contains("name=\"confirmPassword\""));
        assertTrue(template.contains("data-status-message=\"Creating your student account...\""));
        assertFalse(template.contains("name=\"provider\""));
        assertFalse(template.contains("name=\"role\""));
    }

    @Test
    void authJavascriptHandlesCompletionAndSafeErrors() throws Exception {
        String script = Files.readString(Path.of("src/main/resources/static/js/auth/auth.js"));

        assertTrue(script.contains("initOAuthCompleteForm"));
        assertTrue(script.contains("'/api/auth/oauth2/complete'"));
        assertTrue(script.contains("validatePasswordPair"));
        assertTrue(script.contains("Your security token expired"));
        assertTrue(script.contains("This registration session expired"));
    }
}
