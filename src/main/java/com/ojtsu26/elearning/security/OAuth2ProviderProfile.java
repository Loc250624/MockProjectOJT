package com.ojtsu26.elearning.security;

import com.ojtsu26.elearning.model.enums.AuthProvider;

public record OAuth2ProviderProfile(
        AuthProvider provider,
        String providerId,
        String email,
        String name,
        String avatarUrl) {
}
