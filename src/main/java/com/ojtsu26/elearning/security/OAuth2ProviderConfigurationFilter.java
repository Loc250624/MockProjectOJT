package com.ojtsu26.elearning.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

@Component
public class OAuth2ProviderConfigurationFilter extends OncePerRequestFilter {

    public static final String ERROR_PROVIDER_UNCONFIGURED = "oauth2_provider_unconfigured";

    private static final String AUTHORIZATION_PREFIX = "/oauth2/authorization/";

    private final OAuth2ProviderConfigService providerConfigService;

    public OAuth2ProviderConfigurationFilter(OAuth2ProviderConfigService providerConfigService) {
        this.providerConfigService = providerConfigService;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        String registrationId = resolveAuthorizationRegistrationId(request);
        if (providerConfigService.isSupportedProvider(registrationId)
                && !providerConfigService.isConfigured(registrationId)) {
            response.sendRedirect(request.getContextPath()
                    + "/auth/login?error=" + ERROR_PROVIDER_UNCONFIGURED);
            return;
        }

        filterChain.doFilter(request, response);
    }

    private String resolveAuthorizationRegistrationId(HttpServletRequest request) {
        String path = request.getRequestURI();
        String contextPath = request.getContextPath();
        if (contextPath != null && !contextPath.isBlank() && path.startsWith(contextPath)) {
            path = path.substring(contextPath.length());
        }
        if (!path.startsWith(AUTHORIZATION_PREFIX)) {
            return null;
        }
        String registrationId = path.substring(AUTHORIZATION_PREFIX.length());
        int slash = registrationId.indexOf('/');
        return slash >= 0 ? registrationId.substring(0, slash) : registrationId;
    }
}
