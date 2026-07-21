package com.ojtsu26.elearning.security;

import com.ojtsu26.elearning.model.entity.User;
import com.ojtsu26.elearning.model.enums.AuthProvider;
import com.ojtsu26.elearning.model.enums.Role;
import com.ojtsu26.elearning.model.enums.UserStatus;
import jakarta.servlet.http.HttpSession;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.security.oauth2.core.user.DefaultOAuth2User;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class OAuth2LoginSuccessHandlerTest {

    private final OAuth2AccountService accountService = mock(OAuth2AccountService.class);
    private final JwtCookieService jwtCookieService = mock(JwtCookieService.class);
    private final OAuth2LoginSuccessHandler handler =
            new OAuth2LoginSuccessHandler(accountService, jwtCookieService);

    @AfterEach
    void clearSecurityContext() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void pendingOAuthRegistrationClearsSavedProviderAuthenticationAndDoesNotIssueAppSession() throws Exception {
        OAuth2AuthenticationToken authentication = googleAuthentication("new.user@example.com", "google-sub-new");
        when(accountService.loginOrStartRegistration(any(OAuth2ProviderProfile.class)))
                .thenReturn(OAuth2LoginResult.pending("pending-token"));

        MockHttpServletRequest request = requestWithSavedProviderAuthentication(authentication);
        MockHttpServletResponse response = new MockHttpServletResponse();

        handler.onAuthenticationSuccess(request, response, authentication);

        HttpSession session = request.getSession(false);
        assertThat(response.getRedirectedUrl()).isEqualTo("/auth/oauth2/complete");
        assertThat(session).isNotNull();
        assertThat(session.getAttribute(OAuth2LoginSuccessHandler.PENDING_OAUTH_SESSION_ATTRIBUTE))
                .isEqualTo("pending-token");
        assertThat(session.getAttribute(HttpSessionSecurityContextRepository.SPRING_SECURITY_CONTEXT_KEY))
                .isNull();
        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
        verify(jwtCookieService).clearJwtCookie(response);
        verify(jwtCookieService, never()).addJwtCookie(any(), any());
    }

    @Test
    void existingLinkedOAuthAccountGetsJwtCookieAndClearsProviderSessionAuthentication() throws Exception {
        OAuth2AuthenticationToken authentication = googleAuthentication("teacher@example.com", "google-sub-teacher");
        User teacher = User.builder()
                .id(42)
                .email("teacher@example.com")
                .fullName("Teacher Example")
                .authProvider(AuthProvider.GOOGLE)
                .providerId("google-sub-teacher")
                .role(Role.TEACHER)
                .status(UserStatus.ACTIVE)
                .build();
        when(accountService.loginOrStartRegistration(any(OAuth2ProviderProfile.class)))
                .thenReturn(OAuth2LoginResult.authenticated(teacher));

        MockHttpServletRequest request = requestWithSavedProviderAuthentication(authentication);
        MockHttpServletResponse response = new MockHttpServletResponse();

        handler.onAuthenticationSuccess(request, response, authentication);

        HttpSession session = request.getSession(false);
        assertThat(response.getRedirectedUrl()).isEqualTo("/teacher/dashboard");
        assertThat(session.getAttribute(OAuth2LoginSuccessHandler.PENDING_OAUTH_SESSION_ATTRIBUTE))
                .isNull();
        assertThat(session.getAttribute(HttpSessionSecurityContextRepository.SPRING_SECURITY_CONTEXT_KEY))
                .isNull();
        verify(jwtCookieService).addJwtCookie(response, "teacher@example.com");
        verify(jwtCookieService, never()).clearJwtCookie(response);
    }

    private MockHttpServletRequest requestWithSavedProviderAuthentication(OAuth2AuthenticationToken authentication) {
        MockHttpServletRequest request = new MockHttpServletRequest();
        SecurityContext context = SecurityContextHolder.createEmptyContext();
        context.setAuthentication(authentication);
        request.getSession(true).setAttribute(
                HttpSessionSecurityContextRepository.SPRING_SECURITY_CONTEXT_KEY,
                context);
        SecurityContextHolder.setContext(context);
        return request;
    }

    private OAuth2AuthenticationToken googleAuthentication(String email, String subject) {
        OAuth2User principal = new DefaultOAuth2User(
                List.of(new SimpleGrantedAuthority("OAUTH2_USER")),
                Map.of(
                        "email", email,
                        "name", "OAuth User",
                        "sub", subject,
                        "picture", "https://cdn.example/avatar.png"),
                "email");
        return new OAuth2AuthenticationToken(
                principal,
                principal.getAuthorities(),
                "google");
    }
}
