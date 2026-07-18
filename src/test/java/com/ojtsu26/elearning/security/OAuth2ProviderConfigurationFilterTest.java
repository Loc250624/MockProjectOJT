package com.ojtsu26.elearning.security;

import jakarta.servlet.ServletException;
import org.junit.jupiter.api.Test;
import org.springframework.mock.env.MockEnvironment;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import java.io.IOException;

import static org.assertj.core.api.Assertions.assertThat;

class OAuth2ProviderConfigurationFilterTest {

    @Test
    void redirectsConfiguredProviderRouteWhenProviderIsUnavailable() throws ServletException, IOException {
        OAuth2ProviderConfigService service = new OAuth2ProviderConfigService(new MockEnvironment()
                .withProperty("spring.security.oauth2.client.registration.google.client-id",
                        "UNCONFIGURED_GOOGLE_CLIENT_ID")
                .withProperty("spring.security.oauth2.client.registration.google.client-secret",
                        "UNCONFIGURED_GOOGLE_CLIENT_SECRET"));
        OAuth2ProviderConfigurationFilter filter = new OAuth2ProviderConfigurationFilter(service);

        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/oauth2/authorization/google");
        MockHttpServletResponse response = new MockHttpServletResponse();

        filter.doFilter(request, response, new MockFilterChain());

        assertThat(response.getStatus()).isEqualTo(302);
        assertThat(response.getRedirectedUrl())
                .isEqualTo("/auth/login?error=oauth2_provider_unconfigured");
    }
}
