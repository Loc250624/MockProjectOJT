package com.ojtsu26.elearning.controller;

import com.ojtsu26.elearning.model.entity.User;
import com.ojtsu26.elearning.model.enums.AuthProvider;
import com.ojtsu26.elearning.repository.UserRepository;
import com.ojtsu26.elearning.security.CustomUserDetails;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ModelAttribute;

import java.util.Locale;

/**
 * Exposes the persisted user profile to every server-rendered page, regardless
 * of the concrete principal type used by Local, JWT, Google, or GitHub login.
 */
@ControllerAdvice(
        assignableTypes = {
                AdminViewController.class,
                AuthController.class,
                FooterPageController.class,
                PublicController.class,
                StudentFeedbackController.class,
                StudentViewController.class,
                TeacherViewController.class
        })
@RequiredArgsConstructor
public class PortalUserModelAdvice {

    private final UserRepository userRepository;

    @ModelAttribute("portalUser")
    public User portalUser(Authentication authentication) {
        if (authentication == null
                || !authentication.isAuthenticated()
                || authentication instanceof AnonymousAuthenticationToken) {
            return null;
        }

        Object principal = authentication.getPrincipal();
        if (principal instanceof CustomUserDetails customUserDetails) {
            return userRepository.findById(customUserDetails.getUser().getId()).orElse(null);
        }

        if (authentication instanceof OAuth2AuthenticationToken oauthToken
                && principal instanceof OAuth2User oauthUser) {
            AuthProvider provider = provider(oauthToken.getAuthorizedClientRegistrationId());
            String providerId = providerId(provider, oauthUser);
            if (provider != null && StringUtils.hasText(providerId)) {
                return userRepository.findByProviderIdAndAuthProvider(providerId, provider).orElse(null);
            }
            return null;
        }

        if (principal instanceof UserDetails userDetails) {
            return findLocalUser(userDetails.getUsername());
        }
        if (principal instanceof String username && !"anonymousUser".equals(username)) {
            return findLocalUser(username);
        }
        return null;
    }

    private User findLocalUser(String email) {
        if (!StringUtils.hasText(email)) {
            return null;
        }
        return userRepository.findByAuthProviderAndEmailIgnoreCase(AuthProvider.LOCAL, email).orElse(null);
    }

    private AuthProvider provider(String registrationId) {
        if (!StringUtils.hasText(registrationId)) {
            return null;
        }
        try {
            return AuthProvider.valueOf(registrationId.toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException ignored) {
            return null;
        }
    }

    private String providerId(AuthProvider provider, OAuth2User oauthUser) {
        if (provider == null) {
            return null;
        }
        Object value = switch (provider) {
            case GOOGLE -> oauthUser.getAttribute("sub");
            case GITHUB -> oauthUser.getAttribute("id");
            case LOCAL -> null;
        };
        return value == null ? null : String.valueOf(value);
    }
}
