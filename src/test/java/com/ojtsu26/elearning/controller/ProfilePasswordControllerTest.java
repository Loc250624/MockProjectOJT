package com.ojtsu26.elearning.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.ojtsu26.elearning.model.entity.User;
import com.ojtsu26.elearning.model.enums.AuthProvider;
import com.ojtsu26.elearning.model.enums.Role;
import com.ojtsu26.elearning.model.enums.UserStatus;
import com.ojtsu26.elearning.repository.UserRepository;
import com.ojtsu26.elearning.security.CustomUserDetails;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;

import java.util.LinkedHashMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@TestPropertySource(properties = {
        "spring.datasource.url=jdbc:h2:mem:profile_password_test;MODE=MySQL;DATABASE_TO_LOWER=TRUE;DB_CLOSE_DELAY=-1",
        "spring.datasource.driver-class-name=org.h2.Driver",
        "spring.datasource.username=sa",
        "spring.datasource.password=",
        "spring.jpa.hibernate.ddl-auto=create-drop",
        "spring.jpa.properties.hibernate.dialect=org.hibernate.dialect.H2Dialect"
})
class ProfilePasswordControllerTest {

    @Autowired private MockMvc mockMvc;
    @Autowired private ObjectMapper objectMapper;
    @Autowired private UserRepository userRepository;
    @Autowired private PasswordEncoder passwordEncoder;

    @BeforeEach
    void setUp() {
        userRepository.deleteAll();
    }

    @ParameterizedTest
    @EnumSource(Role.class)
    void authenticatedLocalAccountInEveryRoleCanChangeItsPassword(Role role) throws Exception {
        User account = saveAccount(role, AuthProvider.LOCAL, "Current@123");

        mockMvc.perform(put("/api/profile/password")
                        .with(user(new CustomUserDetails(account)))
                        .with(csrf())
                        .contentType("application/json")
                        .content(requestJson("Current@123", "Better@456", "Better@456", null)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Password changed successfully"));

        User updated = userRepository.findById(account.getId()).orElseThrow();
        assertThat(passwordEncoder.matches("Better@456", updated.getPasswordHash())).isTrue();
        assertThat(passwordEncoder.matches("Current@123", updated.getPasswordHash())).isFalse();

        mockMvc.perform(post("/api/auth/login")
                        .with(csrf())
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(Map.of(
                                "email", account.getEmail(),
                                "password", "Current@123"))))
                .andExpect(status().isUnauthorized());
        mockMvc.perform(post("/api/auth/login")
                        .with(csrf())
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(Map.of(
                                "email", account.getEmail(),
                                "password", "Better@456"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.role").value(role.name()));
    }

    @Test
    void endpointRequiresAuthentication() throws Exception {
        mockMvc.perform(put("/api/profile/password")
                        .with(csrf())
                        .contentType("application/json")
                        .content(requestJson("Current@123", "Better@456", "Better@456", null)))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void endpointRequiresCsrf() throws Exception {
        User account = saveAccount(Role.STUDENT, AuthProvider.LOCAL, "Current@123");

        mockMvc.perform(put("/api/profile/password")
                        .with(user(new CustomUserDetails(account)))
                        .contentType("application/json")
                        .content(requestJson("Current@123", "Better@456", "Better@456", null)))
                .andExpect(status().isForbidden());

        assertThat(passwordEncoder.matches("Current@123",
                userRepository.findById(account.getId()).orElseThrow().getPasswordHash())).isTrue();
    }

    @Test
    void weakPayloadIsRejectedByServerValidation() throws Exception {
        User account = saveAccount(Role.STUDENT, AuthProvider.LOCAL, "Current@123");

        mockMvc.perform(put("/api/profile/password")
                        .with(user(new CustomUserDetails(account)))
                        .with(csrf())
                        .contentType("application/json")
                        .content(requestJson("Current@123", "weakpass", "weakpass", null)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void adminCanChangePasswordButOauthManagedAccountsAreRejected() throws Exception {
        User admin = saveAccount(Role.ADMIN, AuthProvider.LOCAL, "Current@123");
        mockMvc.perform(put("/api/profile/password")
                        .with(user(new CustomUserDetails(admin)))
                        .with(csrf())
                        .contentType("application/json")
                        .content(requestJson("Current@123", "Better@456", "Better@456", null)))
                .andExpect(status().isOk());
        assertThat(passwordEncoder.matches("Better@456",
                userRepository.findById(admin.getId()).orElseThrow().getPasswordHash())).isTrue();

        userRepository.deleteAll();
        User oauthStudent = saveAccount(Role.STUDENT, AuthProvider.GOOGLE, "Current@123");
        mockMvc.perform(put("/api/profile/password")
                        .with(user(new CustomUserDetails(oauthStudent)))
                        .with(csrf())
                        .contentType("application/json")
                        .content(requestJson("Current@123", "Better@456", "Better@456", null)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void injectedUserIdCannotChangeAnotherAccount() throws Exception {
        User current = saveAccount(Role.STUDENT, AuthProvider.LOCAL, "Current@123");
        User target = saveAccount(Role.STUDENT, AuthProvider.LOCAL, "Target@123");

        mockMvc.perform(put("/api/profile/password")
                        .with(user(new CustomUserDetails(current)))
                        .with(csrf())
                        .contentType("application/json")
                        .content(requestJson("Current@123", "Better@456", "Better@456", target.getId())))
                .andExpect(status().isOk());

        assertThat(passwordEncoder.matches("Better@456",
                userRepository.findById(current.getId()).orElseThrow().getPasswordHash())).isTrue();
        assertThat(passwordEncoder.matches("Target@123",
                userRepository.findById(target.getId()).orElseThrow().getPasswordHash())).isTrue();
    }

    @Test
    void everyLocalRoleRendersThePasswordForm() throws Exception {
        User student = saveAccount(Role.STUDENT, AuthProvider.LOCAL, "Current@123");
        User teacher = saveAccount(Role.TEACHER, AuthProvider.LOCAL, "Current@123");
        User admin = saveAccount(Role.ADMIN, AuthProvider.LOCAL, "Current@123");

        mockMvc.perform(get("/student/profile").with(user(new CustomUserDetails(student))))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("id=\"profile-password-form\"")));
        mockMvc.perform(get("/teacher/profile").with(user(new CustomUserDetails(teacher))))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("id=\"profile-password-form\"")));
        mockMvc.perform(get("/admin/profile").with(user(new CustomUserDetails(admin))))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("id=\"profile-password-form\"")));
    }

    @Test
    void oauthManagedStudentRendersProviderNoticeWithoutPasswordInputs() throws Exception {
        User oauthStudent = saveAccount(Role.STUDENT, AuthProvider.GITHUB, "Current@123");

        mockMvc.perform(get("/student/profile").with(user(new CustomUserDetails(oauthStudent))))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("managed by your Google or GitHub sign-in provider")))
                .andExpect(content().string(not(containsString("id=\"profile-password-form\""))));
    }

    @Test
    void newLocalRegistrationUsesTheSameStrongPasswordPolicy() throws Exception {
        Map<String, Object> weakRegistration = Map.of(
                "fullName", "New Student",
                "email", "weak.registration@example.com",
                "password", "weakpass",
                "confirmPassword", "weakpass");
        mockMvc.perform(post("/api/auth/register")
                        .with(csrf())
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(weakRegistration)))
                .andExpect(status().isBadRequest());
        assertThat(userRepository.findByAuthProviderAndEmailIgnoreCase(
                AuthProvider.LOCAL, "weak.registration@example.com")).isEmpty();

        Map<String, Object> strongRegistration = Map.of(
                "fullName", "New Student",
                "email", "strong.registration@example.com",
                "password", "Strong@123",
                "confirmPassword", "Strong@123");
        mockMvc.perform(post("/api/auth/register")
                        .with(csrf())
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(strongRegistration)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.role").value("STUDENT"));

        User registered = userRepository.findByAuthProviderAndEmailIgnoreCase(
                AuthProvider.LOCAL, "strong.registration@example.com").orElseThrow();
        assertThat(passwordEncoder.matches("Strong@123", registered.getPasswordHash())).isTrue();
    }

    private User saveAccount(Role role, AuthProvider provider, String rawPassword) {
        return userRepository.save(User.builder()
                .fullName(role.name() + " Account")
                .email(role.name().toLowerCase() + "-" + provider.name().toLowerCase()
                        + "-" + System.nanoTime() + "@example.com")
                .passwordHash(passwordEncoder.encode(rawPassword))
                .role(role)
                .authProvider(provider)
                .status(UserStatus.ACTIVE)
                .build());
    }

    private String requestJson(String current, String next, String confirmation, Integer userId) throws Exception {
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("currentPassword", current);
        payload.put("newPassword", next);
        payload.put("confirmPassword", confirmation);
        if (userId != null) {
            payload.put("userId", userId);
        }
        return objectMapper.writeValueAsString(payload);
    }
}
