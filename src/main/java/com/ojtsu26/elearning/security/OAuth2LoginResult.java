package com.ojtsu26.elearning.security;

import com.ojtsu26.elearning.model.entity.User;

public record OAuth2LoginResult(User user, String pendingRegistrationToken) {

    public static OAuth2LoginResult authenticated(User user) {
        return new OAuth2LoginResult(user, null);
    }

    public static OAuth2LoginResult pending(String pendingRegistrationToken) {
        return new OAuth2LoginResult(null, pendingRegistrationToken);
    }

    public boolean isPending() {
        return pendingRegistrationToken != null;
    }
}
