package com.ojtsu26.elearning;

import com.ojtsu26.elearning.dto.request.LoginRequestDTO;
import com.ojtsu26.elearning.dto.request.OAuth2CompleteRegistrationRequestDTO;
import com.ojtsu26.elearning.dto.request.RegisterRequestDTO;
import com.ojtsu26.elearning.dto.response.AuthResponseDTO;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;

class AuthDtoToStringRedactionTest {

    @Test
    void authDtoToStringDoesNotExposeCredentialsOrPersonalIdentifiers() {
        LoginRequestDTO loginRequest = new LoginRequestDTO();
        loginRequest.setEmail("student@example.test");
        loginRequest.setPassword("plain-password");

        RegisterRequestDTO registerRequest = new RegisterRequestDTO();
        registerRequest.setFullName("Student Name");
        registerRequest.setEmail("new-student@example.test");
        registerRequest.setPassword("register-password");
        registerRequest.setConfirmPassword("register-password");

        OAuth2CompleteRegistrationRequestDTO oauthRequest = new OAuth2CompleteRegistrationRequestDTO();
        oauthRequest.setPassword("oauth-password");
        oauthRequest.setConfirmPassword("oauth-password");

        AuthResponseDTO authResponse = AuthResponseDTO.builder()
                .token("jwt-token-value")
                .id(99)
                .email("student@example.test")
                .fullName("Student Name")
                .role("STUDENT")
                .redirectUrl("/student/dashboard")
                .build();

        String combined = loginRequest + "\n" + registerRequest + "\n" + oauthRequest + "\n" + authResponse;

        assertFalse(combined.contains("plain-password"));
        assertFalse(combined.contains("register-password"));
        assertFalse(combined.contains("oauth-password"));
        assertFalse(combined.contains("jwt-token-value"));
        assertFalse(combined.contains("student@example.test"));
        assertFalse(combined.contains("new-student@example.test"));
        assertFalse(combined.contains("Student Name"));
        assertFalse(combined.contains("id=99"));
    }
}
