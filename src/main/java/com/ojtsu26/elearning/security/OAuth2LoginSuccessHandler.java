package com.ojtsu26.elearning.security;

import com.ojtsu26.elearning.model.entity.User;
import com.ojtsu26.elearning.model.enums.AuthProvider;
import com.ojtsu26.elearning.model.enums.Role;
import com.ojtsu26.elearning.model.enums.UserStatus;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.io.IOException;
import java.util.Locale;

@Slf4j
@Component
@RequiredArgsConstructor
public class OAuth2LoginSuccessHandler implements AuthenticationSuccessHandler {

    public static final String PENDING_OAUTH_SESSION_ATTRIBUTE = "PENDING_OAUTH_REGISTRATION_TOKEN";

    private final OAuth2AccountService oAuth2AccountService;
    private final JwtCookieService jwtCookieService;

    @Override
    public void onAuthenticationSuccess(
            HttpServletRequest request,
            HttpServletResponse response,
            Authentication authentication) throws IOException, ServletException {

        OAuth2AuthenticationToken authToken = (OAuth2AuthenticationToken) authentication;
        OAuth2User oAuth2User = authToken.getPrincipal();

        String registrationId = authToken.getAuthorizedClientRegistrationId();
        AuthProvider authProvider = resolveAuthProvider(registrationId);
        if (authProvider == null) {
            SecurityContextHolder.clearContext();
            log.warn("OAuth2 login attempted with unsupported provider: {}", registrationId);
            response.sendRedirect("/auth/login?error=oauth2");
            return;
        }

        String email = oAuth2User.getAttribute("email");
        if (!StringUtils.hasText(email)) {
            SecurityContextHolder.clearContext();
            log.warn("OAuth2 user from provider '{}' has no email; cannot authenticate", registrationId);
            response.sendRedirect("/auth/login?error=oauth2_no_email");
            return;
        }

        OAuth2ProviderProfile profile = new OAuth2ProviderProfile(
                authProvider,
                resolveProviderId(oAuth2User, registrationId),
                email,
                resolveName(oAuth2User, registrationId, email),
                resolveAvatarUrl(oAuth2User, registrationId));

        OAuth2LoginResult loginResult;
        try {
            loginResult = oAuth2AccountService.loginOrStartRegistration(profile);
        } catch (OAuth2AuthenticationException e) {
            SecurityContextHolder.clearContext();
            log.warn("OAuth2 account resolution failed: provider={}, errorCode={}",
                    registrationId,
                    e.getError() != null ? e.getError().getErrorCode() : null);
            response.sendRedirect("/auth/login?error=" + resolveLoginError(e));
            return;
        }

        if (loginResult.isPending()) {
            SecurityContextHolder.clearContext();
            request.getSession(true).setAttribute(
                    PENDING_OAUTH_SESSION_ATTRIBUTE,
                    loginResult.pendingRegistrationToken());
            log.info("OAuth2 registration completion required: provider={}", registrationId);
            response.sendRedirect("/auth/oauth2/complete");
            return;
        }

        User user = loginResult.user();
        if (user.getStatus() == UserStatus.DELETED) {
            SecurityContextHolder.clearContext();
            log.warn("OAuth2 login attempt by deleted account: {}", user.getEmail());
            response.sendRedirect("/auth/login?error=deleted");
            return;
        }
        if (user.getStatus() != UserStatus.ACTIVE) {
            SecurityContextHolder.clearContext();
            log.warn("OAuth2 login attempt by inactive account: {}", user.getEmail());
            response.sendRedirect("/auth/login?error=blocked");
            return;
        }
        if (user.getRole() == null) {
            SecurityContextHolder.clearContext();
            log.warn("OAuth2 login attempt by account without role: {}", user.getEmail());
            response.sendRedirect("/auth/login?error=role");
            return;
        }

        jwtCookieService.addJwtCookie(response, user.getEmail());
        log.info("OAuth2 login success: provider={}, email={}, role={}", registrationId, user.getEmail(), user.getRole());
        response.sendRedirect(resolveRedirectUrl(user.getRole()));
    }

    private AuthProvider resolveAuthProvider(String registrationId) {
        if (!StringUtils.hasText(registrationId)) {
            return null;
        }
        try {
            return AuthProvider.valueOf(registrationId.toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException e) {
            return null;
        }
    }

    private String resolveRedirectUrl(Role role) {
        return switch (role) {
            case STUDENT -> "/student/dashboard";
            case TEACHER -> "/teacher/dashboard";
            case ADMIN -> "/admin/dashboard";
        };
    }

    private String resolveName(OAuth2User oAuth2User, String registrationId, String email) {
        String name = oAuth2User.getAttribute("name");
        if (StringUtils.hasText(name)) {
            return name;
        }
        if ("github".equalsIgnoreCase(registrationId)) {
            String login = oAuth2User.getAttribute("login");
            if (StringUtils.hasText(login)) {
                return login;
            }
        }
        return email;
    }

    private String resolveAvatarUrl(OAuth2User oAuth2User, String registrationId) {
        return switch (registrationId.toLowerCase(Locale.ROOT)) {
            case "google" -> oAuth2User.getAttribute("picture");
            case "github" -> oAuth2User.getAttribute("avatar_url");
            default -> null;
        };
    }

    private String resolveProviderId(OAuth2User oAuth2User, String registrationId) {
        return switch (registrationId.toLowerCase(Locale.ROOT)) {
            case "google" -> oAuth2User.getAttribute("sub");
            case "github" -> {
                Object id = oAuth2User.getAttribute("id");
                yield id != null ? String.valueOf(id) : null;
            }
            default -> null;
        };
    }

    private String resolveLoginError(OAuth2AuthenticationException exception) {
        String errorCode = exception.getError() != null ? exception.getError().getErrorCode() : null;
        if (OAuth2AccountService.ERROR_EMAIL_NOT_FOUND.equals(errorCode)) {
            return "oauth2_no_email";
        }
        if (OAuth2AccountService.ERROR_ACCOUNT_LINK_REQUIRED.equals(errorCode)) {
            return "oauth2_link_required";
        }
        if (OAuth2AccountService.ERROR_DUPLICATE_RETRY.equals(errorCode)) {
            return "oauth2_retry";
        }
        if (OAuth2AccountService.ERROR_ACCOUNT_DELETED.equals(errorCode)) {
            return "deleted";
        }
        if (OAuth2AccountService.ERROR_ACCOUNT_BLOCKED.equals(errorCode)) {
            return "blocked";
        }
        if ("role_missing".equals(errorCode)) {
            return "role";
        }
        return "oauth2";
    }
}
