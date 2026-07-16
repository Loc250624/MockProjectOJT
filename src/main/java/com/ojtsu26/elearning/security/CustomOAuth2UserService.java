package com.ojtsu26.elearning.security;

import lombok.extern.slf4j.Slf4j;
import org.springframework.security.oauth2.client.userinfo.DefaultOAuth2UserService;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserRequest;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserService;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.OAuth2Error;
import org.springframework.security.oauth2.core.user.DefaultOAuth2User;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

/**
 * Extends the standard OAuth2 user-info loading to handle GitHub's
 * private-email policy.
 *
 * <p>GitHub users who have set their email to private will NOT have an
 * {@code email} attribute in the standard {@code /user} endpoint response.
 * Spring Security passes that null straight through to
 * {@link OAuth2LoginSuccessHandler}, which would then crash with a
 * NullPointerException when it tries to look up the user by email.</p>
 *
 * <p>This service detects the missing email and calls GitHub's
 * {@code /user/emails} endpoint (which is covered by the {@code user:email}
 * scope we request in application.properties) to retrieve the primary
 * verified email address.</p>
 */
@Slf4j
@Service
public class CustomOAuth2UserService implements OAuth2UserService<OAuth2UserRequest, OAuth2User> {

    private final DefaultOAuth2UserService delegate = new DefaultOAuth2UserService();

    @Override
    public OAuth2User loadUser(OAuth2UserRequest userRequest) throws OAuth2AuthenticationException {
        OAuth2User oAuth2User = delegate.loadUser(userRequest);

        String registrationId = userRequest.getClientRegistration().getRegistrationId();

        if ("github".equalsIgnoreCase(registrationId)) {
            return enrichGitHubUser(userRequest, oAuth2User);
        }

        // Google and other providers always return a non-null, verified email
        return oAuth2User;
    }

    /**
     * If the GitHub user's email is null (private email setting), fetches the
     * primary verified email via {@code GET /user/emails} using the access token
     * that was just obtained during the OAuth code-exchange.
     */
    private OAuth2User enrichGitHubUser(OAuth2UserRequest userRequest, OAuth2User oAuth2User) {
        String email = oAuth2User.getAttribute("email");

        if (email != null && !email.isBlank()) {
            // Email already present — nothing extra needed
            return oAuth2User;
        }

        log.debug("GitHub user has no public email; fetching from /user/emails endpoint");

        String accessToken = userRequest.getAccessToken().getTokenValue();
        String primaryEmail = fetchPrimaryGitHubEmail(accessToken);

        if (primaryEmail == null) {
            log.error("Could not retrieve any verified email from GitHub /user/emails");
            throw new OAuth2AuthenticationException(
                    new OAuth2Error("email_not_found",
                            "No verified email address found on this GitHub account. "
                                    + "Please add a verified email in your GitHub settings.",
                            null));
        }

        // Copy all existing attributes, then overwrite the email key
        Map<String, Object> attributes = new HashMap<>(oAuth2User.getAttributes());
        attributes.put("email", primaryEmail);

        // The name-attribute-key for GitHub is "id" (numeric)
        String nameAttributeKey = userRequest.getClientRegistration()
                .getProviderDetails()
                .getUserInfoEndpoint()
                .getUserNameAttributeName();

        return new DefaultOAuth2User(
                oAuth2User.getAuthorities(),
                attributes,
                nameAttributeKey);
    }

    /**
     * Calls {@code GET https://api.github.com/user/emails} and returns the
     * primary verified email address, or {@code null} if none is found.
     */
    private String fetchPrimaryGitHubEmail(String accessToken) {
        RestTemplate restTemplate = new RestTemplate();

        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(accessToken);
        headers.set("Accept", "application/vnd.github+json");
        headers.set("X-GitHub-Api-Version", "2022-11-28");

        HttpEntity<Void> entity = new HttpEntity<>(headers);

        try {
            ResponseEntity<GitHubEmail[]> response = restTemplate.exchange(
                    "https://api.github.com/user/emails",
                    HttpMethod.GET,
                    entity,
                    GitHubEmail[].class);

            GitHubEmail[] emails = response.getBody();
            if (emails == null || emails.length == 0) {
                return null;
            }

            // Prefer: primary + verified
            return Arrays.stream(emails)
                    .filter(e -> Boolean.TRUE.equals(e.primary()) && Boolean.TRUE.equals(e.verified()))
                    .map(GitHubEmail::email)
                    .findFirst()
                    // Fallback: any verified address
                    .orElseGet(() -> Arrays.stream(emails)
                            .filter(e -> Boolean.TRUE.equals(e.verified()))
                            .map(GitHubEmail::email)
                            .findFirst()
                            .orElse(null));

        } catch (RestClientException e) {
            log.error("Failed to call GitHub /user/emails: {}", e.getMessage());
            return null;
        }
    }

    /**
     * Internal DTO matching the GitHub /user/emails JSON array elements.
     */
    private record GitHubEmail(String email, Boolean primary, Boolean verified, String visibility) {
    }
}
