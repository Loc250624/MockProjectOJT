package com.ojtsu26.elearning.security;

import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
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

import java.time.Duration;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

@Slf4j
@Service
public class CustomOAuth2UserService implements OAuth2UserService<OAuth2UserRequest, OAuth2User> {

    private final DefaultOAuth2UserService delegate = new DefaultOAuth2UserService();
    private final RestTemplate restTemplate;

    public CustomOAuth2UserService(RestTemplateBuilder restTemplateBuilder) {
        this.restTemplate = restTemplateBuilder
                .setConnectTimeout(Duration.ofSeconds(3))
                .setReadTimeout(Duration.ofSeconds(3))
                .build();
    }

    @Override
    public OAuth2User loadUser(OAuth2UserRequest userRequest) throws OAuth2AuthenticationException {
        OAuth2User oAuth2User = delegate.loadUser(userRequest);
        String registrationId = userRequest.getClientRegistration().getRegistrationId();

        if ("github".equalsIgnoreCase(registrationId)) {
            return enrichGitHubUser(userRequest, oAuth2User);
        }

        return oAuth2User;
    }

    private OAuth2User enrichGitHubUser(OAuth2UserRequest userRequest, OAuth2User oAuth2User) {
        String email = oAuth2User.getAttribute("email");
        if (email != null && !email.isBlank()) {
            return oAuth2User;
        }

        log.debug("GitHub user has no public email; fetching primary verified email");

        String primaryEmail = fetchPrimaryGitHubEmail(userRequest.getAccessToken().getTokenValue());
        if (primaryEmail == null) {
            throw new OAuth2AuthenticationException(
                    new OAuth2Error(
                            OAuth2AccountService.ERROR_EMAIL_NOT_FOUND,
                            "No primary verified email address was found on this GitHub account.",
                            null));
        }

        Map<String, Object> attributes = new HashMap<>(oAuth2User.getAttributes());
        attributes.put("email", primaryEmail);

        String nameAttributeKey = userRequest.getClientRegistration()
                .getProviderDetails()
                .getUserInfoEndpoint()
                .getUserNameAttributeName();

        return new DefaultOAuth2User(oAuth2User.getAuthorities(), attributes, nameAttributeKey);
    }

    private String fetchPrimaryGitHubEmail(String accessToken) {
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(accessToken);
        headers.set("Accept", "application/vnd.github+json");
        headers.set("X-GitHub-Api-Version", "2022-11-28");

        try {
            ResponseEntity<GitHubEmail[]> response = restTemplate.exchange(
                    "https://api.github.com/user/emails",
                    HttpMethod.GET,
                    new HttpEntity<Void>(headers),
                    GitHubEmail[].class);

            GitHubEmail[] emails = response.getBody();
            if (emails == null || emails.length == 0) {
                return null;
            }

            return Arrays.stream(emails)
                    .filter(email -> Boolean.TRUE.equals(email.primary()) && Boolean.TRUE.equals(email.verified()))
                    .map(GitHubEmail::email)
                    .findFirst()
                    .orElse(null);
        } catch (RestClientException e) {
            log.warn("Failed to call GitHub /user/emails: {}", e.getClass().getSimpleName());
            return null;
        }
    }

    private record GitHubEmail(String email, Boolean primary, Boolean verified, String visibility) {
    }
}
