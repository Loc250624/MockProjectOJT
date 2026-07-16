package com.ojtsu26.elearning;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.oauth2.client.registration.ClientRegistration;
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;
import org.springframework.security.oauth2.client.registration.InMemoryClientRegistrationRepository;

import java.util.List;
import java.util.stream.StreamSupport;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Verifies that the Spring Environment resolves all OAuth2 client credentials
 * correctly and that ClientRegistrationRepository contains valid registrations
 * for both google and github.
 *
 * <p>These tests use the real application context (MySQL + full Spring context)
 * so they prove actual runtime behaviour, not assumptions.</p>
 *
 * <p>NOTE on redirect URI: Spring stores the URI as a template
 * {@code {baseUrl}/{action}/oauth2/code/{registrationId}} in the
 * ClientRegistration bean. The template is resolved to a concrete URL
 * (e.g. http://localhost:8080/login/oauth2/code/github) at request time
 * inside OAuth2LoginAuthenticationFilter. The template format is correct
 * and expected.</p>
 */
@SpringBootTest
class OAuth2RegistrationVerificationTest {

    @Autowired
    private ClientRegistrationRepository clientRegistrationRepository;

    // -----------------------------------------------------------------------
    // 1. Both registrations are present
    // -----------------------------------------------------------------------

    @Test
    @DisplayName("ClientRegistrationRepository contains exactly 'google' and 'github'")
    void bothRegistrationsExist() {
        assertThat(clientRegistrationRepository).isNotNull();

        List<String> ids = StreamSupport
                .stream(((InMemoryClientRegistrationRepository) clientRegistrationRepository).spliterator(), false)
                .map(ClientRegistration::getRegistrationId)
                .toList();

        System.out.println("[VERIFY] Loaded OAuth2 registration IDs: " + ids);

        assertThat(ids)
                .as("Both 'google' and 'github' must be present in ClientRegistrationRepository")
                .containsExactlyInAnyOrder("google", "github");
    }

    // -----------------------------------------------------------------------
    // 2. Google registration: real client-id, correct scopes, URI template
    // -----------------------------------------------------------------------

    @Test
    @DisplayName("Google ClientRegistration has a real client-id and correct scopes")
    void googleRegistrationIsValid() {
        ClientRegistration google = clientRegistrationRepository.findByRegistrationId("google");

        assertThat(google).as("Google ClientRegistration must exist").isNotNull();

        String clientId = google.getClientId();
        System.out.println("[VERIFY] google.client-id  = " + clientId);
        System.out.println("[VERIFY] google.scope       = " + google.getScopes());
        System.out.println("[VERIFY] google.redirectUri = " + google.getRedirectUri());
        System.out.println("[VERIFY] google.authUri     = "
                + google.getProviderDetails().getAuthorizationUri());

        // client-id must be the real credential, not an unresolved placeholder
        assertThat(clientId)
                .as("client-id must not contain an unresolved ${...} placeholder")
                .doesNotContain("${")
                .as("client-id must not be the UNCONFIGURED sentinel")
                .doesNotContain("UNCONFIGURED");

        // Google client-ids end with .apps.googleusercontent.com
        assertThat(clientId)
                .as("Google client-id must end with .apps.googleusercontent.com")
                .endsWith(".apps.googleusercontent.com");

        assertThat(google.getScopes())
                .as("Google scopes must include openid, profile and email")
                .containsExactlyInAnyOrder("openid", "profile", "email");

        // Redirect URI stored as template — this is expected Spring Security behaviour
        assertThat(google.getRedirectUri())
                .as("Redirect URI is stored as Spring template, resolved at request time")
                .contains("{registrationId}");
    }

    // -----------------------------------------------------------------------
    // 3. GitHub registration: real client-id, correct scopes, URI template
    // -----------------------------------------------------------------------

    @Test
    @DisplayName("GitHub ClientRegistration has a real client-id and correct scopes")
    void githubRegistrationIsValid() {
        ClientRegistration github = clientRegistrationRepository.findByRegistrationId("github");

        assertThat(github).as("GitHub ClientRegistration must exist").isNotNull();

        String clientId = github.getClientId();
        System.out.println("[VERIFY] github.client-id  = " + clientId);
        System.out.println("[VERIFY] github.scope       = " + github.getScopes());
        System.out.println("[VERIFY] github.redirectUri = " + github.getRedirectUri());
        System.out.println("[VERIFY] github.authUri     = "
                + github.getProviderDetails().getAuthorizationUri());

        // client-id must be the real credential, not an unresolved placeholder
        assertThat(clientId)
                .as("client-id must not contain an unresolved ${...} placeholder")
                .doesNotContain("${")
                .as("client-id must not be the UNCONFIGURED sentinel")
                .doesNotContain("UNCONFIGURED");

        // GitHub client secrets are typically 40-char hex strings
        assertThat(clientId).as("GitHub client-id must be non-blank").isNotBlank();

        assertThat(github.getScopes())
                .as("GitHub scopes must include read:user and user:email for private email access")
                .contains("read:user", "user:email");

        // Redirect URI stored as template — resolved to /login/oauth2/code/github at request time
        assertThat(github.getRedirectUri())
                .as("Redirect URI is stored as Spring template, resolved at request time")
                .contains("{registrationId}");
    }
}
