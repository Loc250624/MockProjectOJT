package com.ojtsu26.elearning.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.ojtsu26.elearning.dto.request.OAuth2CompleteRegistrationRequestDTO;
import com.ojtsu26.elearning.model.enums.AuthProvider;
import com.ojtsu26.elearning.repository.PendingOAuthRegistrationRepository;
import com.ojtsu26.elearning.repository.UserProviderIdentityRepository;
import com.ojtsu26.elearning.repository.UserRepository;
import com.ojtsu26.elearning.security.OAuth2AccountService;
import com.ojtsu26.elearning.security.OAuth2LoginResult;
import com.ojtsu26.elearning.security.OAuth2LoginSuccessHandler;
import com.ojtsu26.elearning.security.OAuth2ProviderProfile;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.security.oauth2.core.user.DefaultOAuth2User;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.cookie;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@TestPropertySource(properties = {
        "spring.datasource.url=jdbc:h2:mem:oauth2_completion_test;MODE=MySQL;DATABASE_TO_LOWER=TRUE;DB_CLOSE_DELAY=-1",
        "spring.datasource.driver-class-name=org.h2.Driver",
        "spring.datasource.username=sa",
        "spring.datasource.password=",
        "spring.jpa.hibernate.ddl-auto=create-drop",
        "spring.jpa.properties.hibernate.dialect=org.hibernate.dialect.H2Dialect"
})
class OAuth2CompletionControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private OAuth2AccountService oAuth2AccountService;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private UserProviderIdentityRepository identityRepository;

    @Autowired
    private PendingOAuthRegistrationRepository pendingRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @BeforeEach
    void setUp() {
        SecurityContextHolder.clearContext();
        identityRepository.deleteAll();
        pendingRepository.deleteAll();
        userRepository.deleteAll();
    }

    @Test
    void completionPostRequiresCsrf() throws Exception {
        OAuth2LoginResult pending = startPending("no-csrf@example.com", "google-sub-no-csrf");

        mockMvc.perform(post("/api/auth/oauth2/complete")
                        .sessionAttr(OAuth2LoginSuccessHandler.PENDING_OAUTH_SESSION_ATTRIBUTE,
                                pending.pendingRegistrationToken())
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(request("secret123", "secret123"))))
                .andExpect(status().isForbidden());

        assertThat(userRepository.count()).isZero();
    }

    @Test
    void completionPageRendersVerifiedProviderProfileFromPendingSession() throws Exception {
        OAuth2LoginResult pending = startPending("Render.Me@Example.com", "google-sub-render");

        mockMvc.perform(get("/auth/oauth2/complete")
                        .sessionAttr(OAuth2LoginSuccessHandler.PENDING_OAUTH_SESSION_ATTRIBUTE,
                                pending.pendingRegistrationToken()))
                .andExpect(status().isOk())
                .andExpect(content().string(org.hamcrest.Matchers.containsString("render.me@example.com")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("GOOGLE")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("verified account")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("id=\"oauthCompleteForm\"")));
    }

    @Test
    void homeNavbarShowsGuestActionsForPendingProviderOnlyAuthentication() throws Exception {
        OAuth2LoginResult pending = startPending("Pending.Home@Example.com", "google-sub-home");
        MockHttpSession session = pendingSessionWithProviderAuthentication(
                pending.pendingRegistrationToken(),
                "pending.home@example.com",
                "google-sub-home");

        mockMvc.perform(get("/").session(session))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("href=\"/auth/login\"")))
                .andExpect(content().string(containsString("href=\"/auth/register\"")))
                .andExpect(content().string(not(containsString("user-avatar-container"))));
    }

    @Test
    void pendingProviderOnlyAuthenticationCannotOpenStudentRoutes() throws Exception {
        OAuth2LoginResult pending = startPending("Pending.Student@Example.com", "google-sub-student-route");
        MockHttpSession session = pendingSessionWithProviderAuthentication(
                pending.pendingRegistrationToken(),
                "pending.student@example.com",
                "google-sub-student-route");

        mockMvc.perform(get("/student/dashboard").session(session))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/"));
    }

    @Test
    void completionPostCreatesStudentWithPasswordProviderIdentityAndJwtCookie() throws Exception {
        OAuth2LoginResult pending = startPending("Complete.Me@Example.com", "google-sub-complete");

        mockMvc.perform(post("/api/auth/oauth2/complete")
                        .with(csrf())
                        .sessionAttr(OAuth2LoginSuccessHandler.PENDING_OAUTH_SESSION_ATTRIBUTE,
                                pending.pendingRegistrationToken())
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(request("secret123", "secret123"))))
                .andExpect(status().isOk())
                .andExpect(cookie().exists("jwt_token"))
                .andExpect(jsonPath("$.data.email").value("complete.me@example.com"))
                .andExpect(jsonPath("$.data.role").value("STUDENT"))
                .andExpect(jsonPath("$.data.redirectUrl").value("/student/dashboard"));

        var user = userRepository.findByEmailIgnoreCase("complete.me@example.com").orElseThrow();
        assertThat(passwordEncoder.matches("secret123", user.getPasswordHash())).isTrue();
        assertThat(identityRepository.findByProviderAndProviderSubject(AuthProvider.GOOGLE, "google-sub-complete"))
                .isPresent();
        assertThat(pendingRepository.findByTokenAndUsedAtIsNull(pending.pendingRegistrationToken())).isEmpty();
    }

    private OAuth2LoginResult startPending(String email, String providerId) {
        return oAuth2AccountService.loginOrStartRegistration(new OAuth2ProviderProfile(
                AuthProvider.GOOGLE,
                providerId,
                email,
                "Complete Me",
                "https://cdn.example/avatar.png"));
    }

    private OAuth2CompleteRegistrationRequestDTO request(String password, String confirmPassword) {
        OAuth2CompleteRegistrationRequestDTO request = new OAuth2CompleteRegistrationRequestDTO();
        request.setPassword(password);
        request.setConfirmPassword(confirmPassword);
        return request;
    }

    private MockHttpSession pendingSessionWithProviderAuthentication(String pendingToken, String email, String subject) {
        MockHttpSession session = new MockHttpSession();
        session.setAttribute(OAuth2LoginSuccessHandler.PENDING_OAUTH_SESSION_ATTRIBUTE, pendingToken);

        OAuth2User principal = new DefaultOAuth2User(
                List.of(new SimpleGrantedAuthority("OAUTH2_USER")),
                Map.of("email", email, "name", "Pending User", "sub", subject),
                "email");
        OAuth2AuthenticationToken authentication = new OAuth2AuthenticationToken(
                principal,
                principal.getAuthorities(),
                "google");
        SecurityContext context = SecurityContextHolder.createEmptyContext();
        context.setAuthentication(authentication);
        session.setAttribute(HttpSessionSecurityContextRepository.SPRING_SECURITY_CONTEXT_KEY, context);
        return session;
    }
}
