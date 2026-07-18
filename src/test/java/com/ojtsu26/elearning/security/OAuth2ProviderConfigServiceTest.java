package com.ojtsu26.elearning.security;

import org.junit.jupiter.api.Test;
import org.springframework.mock.env.MockEnvironment;

import static org.assertj.core.api.Assertions.assertThat;

class OAuth2ProviderConfigServiceTest {

    @Test
    void treatsSentinelDefaultsAsUnconfigured() {
        MockEnvironment environment = new MockEnvironment()
                .withProperty("spring.security.oauth2.client.registration.google.client-id",
                        "UNCONFIGURED_GOOGLE_CLIENT_ID")
                .withProperty("spring.security.oauth2.client.registration.google.client-secret",
                        "UNCONFIGURED_GOOGLE_CLIENT_SECRET");

        OAuth2ProviderConfigService service = new OAuth2ProviderConfigService(environment);

        assertThat(service.isConfigured("google")).isFalse();
    }

    @Test
    void treatsExamplePlaceholdersAsUnconfigured() {
        MockEnvironment environment = new MockEnvironment()
                .withProperty("spring.security.oauth2.client.registration.github.client-id",
                        "YOUR_GITHUB_CLIENT_ID")
                .withProperty("spring.security.oauth2.client.registration.github.client-secret",
                        "YOUR_GITHUB_CLIENT_SECRET");

        OAuth2ProviderConfigService service = new OAuth2ProviderConfigService(environment);

        assertThat(service.isConfigured("github")).isFalse();
    }

    @Test
    void requiresBothClientIdAndSecret() {
        MockEnvironment environment = new MockEnvironment()
                .withProperty("spring.security.oauth2.client.registration.google.client-id",
                        "real-google-client.apps.googleusercontent.com");

        OAuth2ProviderConfigService service = new OAuth2ProviderConfigService(environment);

        assertThat(service.isConfigured("google")).isFalse();
    }

    @Test
    void acceptsConfiguredSupportedProvider() {
        MockEnvironment environment = new MockEnvironment()
                .withProperty("spring.security.oauth2.client.registration.github.client-id",
                        "Ov23liExampleClient")
                .withProperty("spring.security.oauth2.client.registration.github.client-secret",
                        "github-secret-from-env");

        OAuth2ProviderConfigService service = new OAuth2ProviderConfigService(environment);

        assertThat(service.isConfigured("github")).isTrue();
        assertThat(service.isSupportedProvider("github")).isTrue();
        assertThat(service.isSupportedProvider("unknown")).isFalse();
    }
}
