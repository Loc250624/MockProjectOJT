package com.ojtsu26.elearning.security;

import com.ojtsu26.elearning.model.entity.User;
import com.ojtsu26.elearning.model.enums.AuthProvider;
import com.ojtsu26.elearning.model.enums.Role;
import com.ojtsu26.elearning.model.enums.UserStatus;
import com.ojtsu26.elearning.repository.UserRepository;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.io.IOException;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Optional;

/**
 * Handles the successful completion of an OAuth2 authorization code flow.
 *
 * <p>Responsibilities:
 * <ol>
 *   <li>Extract email and profile data from the OAuth2 principal.</li>
 *   <li>Look up the user by {@code providerId} first, then fall back to email.
 *       Using {@code providerId} prevents duplicate accounts when the user changes
 *       their email on the provider side.</li>
 *   <li>Auto-register new users so they never have to click "Register" manually.</li>
 *   <li>Update {@code avatarUrl} and {@code lastLoginAt} on every login.</li>
 *   <li>Issue a JWT HTTP-Only cookie and redirect to the appropriate dashboard.</li>
 * </ol>
 * </p>
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class OAuth2LoginSuccessHandler implements AuthenticationSuccessHandler {

    private final UserRepository userRepository;
    private final JwtUtils jwtUtils;

    @Override
    public void onAuthenticationSuccess(
            HttpServletRequest request,
            HttpServletResponse response,
            Authentication authentication) throws IOException, ServletException {

        OAuth2AuthenticationToken authToken = (OAuth2AuthenticationToken) authentication;
        OAuth2User oAuth2User = authToken.getPrincipal();

        String registrationId = authToken.getAuthorizedClientRegistrationId(); // "google" | "github"
        AuthProvider authProvider = AuthProvider.valueOf(registrationId.toUpperCase());

        // ------------------------------------------------------------------ //
        // 1. Extract attributes from the provider.                             //
        //    CustomOAuth2UserService already resolved a null GitHub email, so  //
        //    "email" is guaranteed to be non-null here.                        //
        // ------------------------------------------------------------------ //
        String email      = oAuth2User.getAttribute("email");
        String name       = oAuth2User.getAttribute("name");
        String avatar     = resolveAvatarUrl(oAuth2User, registrationId);
        String providerId = resolveProviderId(oAuth2User, registrationId);

        // Defensive: CustomOAuth2UserService should have thrown before we get here,
        // but guard anyway to produce a clear error message.
        if (!StringUtils.hasText(email)) {
            log.error("OAuth2 user from provider '{}' has no email — cannot authenticate", registrationId);
            SecurityContextHolder.clearContext();
            response.sendRedirect("/auth/login?error=oauth2_no_email");
            return;
        }

        // ------------------------------------------------------------------ //
        // 2. Find or create the user.                                          //
        // ------------------------------------------------------------------ //
        User user = findOrCreateUser(email, name, avatar, providerId, authProvider);

        // ------------------------------------------------------------------ //
        // 3. Guard against blocked / deleted accounts.                         //
        // ------------------------------------------------------------------ //
        if (user.getStatus() == UserStatus.DELETED) {
            SecurityContextHolder.clearContext();
            log.warn("OAuth2 login attempt by deleted account: {}", email);
            response.sendRedirect("/auth/login?error=deleted");
            return;
        }
        if (user.getStatus() != UserStatus.ACTIVE) {
            SecurityContextHolder.clearContext();
            log.warn("OAuth2 login attempt by blocked account: {}", email);
            response.sendRedirect("/auth/login?error=blocked");
            return;
        }

        // ------------------------------------------------------------------ //
        // 4. Issue JWT and redirect.                                           //
        // ------------------------------------------------------------------ //
        String jwt = jwtUtils.generateTokenFromEmail(user.getEmail());

        ResponseCookie cookie = ResponseCookie.from("jwt_token", jwt)
                .httpOnly(true)
                .path("/")
                .maxAge(Duration.ofDays(1))
                .sameSite("Lax")
                .build();
        response.addHeader(HttpHeaders.SET_COOKIE, cookie.toString());

        String redirectUrl = switch (user.getRole()) {
            case ADMIN   -> "/admin/dashboard";
            case TEACHER -> "/teacher/dashboard";
            default      -> "/student/dashboard";
        };

        log.info("OAuth2 login success: provider={}, email={}, role={}", registrationId, email, user.getRole());
        response.sendRedirect(redirectUrl);
    }

    // ---------------------------------------------------------------------- //
    // Private helpers                                                          //
    // ---------------------------------------------------------------------- //

    /**
     * Looks up the user first by {@code providerId + provider}, then by email.
     *
     * <ul>
     *   <li>If found by providerId  → update profile & lastLoginAt, return.</li>
     *   <li>If found by email only  → link providerId, update profile, return.</li>
     *   <li>If not found at all     → create a new account.</li>
     * </ul>
     */
    private User findOrCreateUser(
            String email,
            String name,
            String avatar,
            String providerId,
            AuthProvider authProvider) {

        // -- Try providerId lookup first (most reliable) --
        if (StringUtils.hasText(providerId)) {
            Optional<User> byProvider = userRepository.findByProviderIdAndAuthProvider(providerId, authProvider);
            if (byProvider.isPresent()) {
                User user = byProvider.get();
                if (user.getStatus() != UserStatus.ACTIVE) {
                    return user;
                }
                updateLoginMetadata(user, name, avatar, email);
                return userRepository.save(user);
            }
        }

        // -- Fall back to email lookup --
        Optional<User> byEmail = userRepository.findByEmail(email);
        if (byEmail.isPresent()) {
            User user = byEmail.get();
            if (user.getStatus() != UserStatus.ACTIVE) {
                return user;
            }
            // Link the OAuth provider to an existing local account transparently
            if (user.getAuthProvider() == AuthProvider.LOCAL || user.getProviderId() == null) {
                user.setAuthProvider(authProvider);
                user.setProviderId(providerId);
            }
            updateLoginMetadata(user, name, avatar, email);
            return userRepository.save(user);
        }

        // -- Auto-register new user --
        User newUser = User.builder()
                .email(email)
                .fullName(StringUtils.hasText(name) ? name : email)
                .avatarUrl(avatar)
                .providerId(providerId)
                .authProvider(authProvider)
                .role(Role.STUDENT)
                .status(UserStatus.ACTIVE)
                .lastLoginAt(LocalDateTime.now())
                .build();

        userRepository.save(newUser);
        log.info("Auto-registered new OAuth2 user: provider={}, email={}", authProvider, email);
        return newUser;
    }

    /**
     * Updates mutable profile fields and the last-login timestamp.
     * Only overwrites {@code avatarUrl} when the provider returns a non-blank value.
     */
    private void updateLoginMetadata(User user, String name, String avatar, String email) {
        if (StringUtils.hasText(avatar)) {
            user.setAvatarUrl(avatar);
        }
        // Keep the name in sync only if it changed and is non-blank
        if (StringUtils.hasText(name) && !name.equals(user.getFullName())) {
            user.setFullName(name);
        }
        user.setLastLoginAt(LocalDateTime.now());
    }

    /**
     * Resolves the provider-specific avatar URL.
     * <ul>
     *   <li>Google: attribute {@code "picture"}</li>
     *   <li>GitHub: attribute {@code "avatar_url"}</li>
     * </ul>
     */
    private String resolveAvatarUrl(OAuth2User oAuth2User, String registrationId) {
        return switch (registrationId.toLowerCase()) {
            case "google" -> oAuth2User.getAttribute("picture");
            case "github" -> oAuth2User.getAttribute("avatar_url");
            default -> null;
        };
    }

    /**
     * Resolves the provider-specific opaque user ID.
     * <ul>
     *   <li>Google: attribute {@code "sub"} (String)</li>
     *   <li>GitHub: attribute {@code "id"}  (Integer → converted to String)</li>
     * </ul>
     */
    private String resolveProviderId(OAuth2User oAuth2User, String registrationId) {
        return switch (registrationId.toLowerCase()) {
            case "google" -> oAuth2User.getAttribute("sub");
            case "github" -> {
                Object id = oAuth2User.getAttribute("id");
                yield id != null ? String.valueOf(id) : null;
            }
            default -> null;
        };
    }
}
