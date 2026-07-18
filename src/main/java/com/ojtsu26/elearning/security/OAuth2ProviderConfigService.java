package com.ojtsu26.elearning.security;

import org.springframework.core.env.Environment;
import org.springframework.stereotype.Service;

import java.util.Locale;
import java.util.Set;

@Service
public class OAuth2ProviderConfigService {

    private static final Set<String> SUPPORTED_PROVIDERS = Set.of("google", "github");
    private final Environment environment;

    public OAuth2ProviderConfigService(Environment environment) {
        this.environment = environment;
    }

    public boolean isConfigured(String registrationId) {
        if (!isSupportedProvider(registrationId)) {
            return false;
        }

        String key = normalize(registrationId);
        return hasUsableValue(property(key, "client-id"))
                && hasUsableValue(property(key, "client-secret"));
    }

    public boolean isSupportedProvider(String registrationId) {
        return registrationId != null && SUPPORTED_PROVIDERS.contains(normalize(registrationId));
    }

    private String property(String registrationId, String propertyName) {
        return environment.getProperty(
                "spring.security.oauth2.client.registration." + registrationId + "." + propertyName);
    }

    private boolean hasUsableValue(String value) {
        if (value == null || value.isBlank()) {
            return false;
        }

        String normalized = value.trim().toUpperCase(Locale.ROOT);
        return !normalized.contains("${")
                && !normalized.startsWith("UNCONFIGURED")
                && !normalized.startsWith("YOUR_");
    }

    private String normalize(String registrationId) {
        return registrationId.toLowerCase(Locale.ROOT);
    }
}
