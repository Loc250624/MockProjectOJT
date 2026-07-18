package com.ojtsu26.elearning.security;

import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;
import org.springframework.stereotype.Component;

import java.time.Duration;

@Component
@RequiredArgsConstructor
public class JwtCookieService {

    private final JwtUtils jwtUtils;

    @Value("${jwt.expiration:${JWT_EXPIRATION_MS:86400000}}")
    private long jwtExpirationMs;

    @Value("${app.security.cookie-secure:false}")
    private boolean cookieSecure;

    public void addJwtCookie(HttpServletResponse response, String email) {
        addJwtCookieFromToken(response, jwtUtils.generateTokenFromEmail(email));
    }

    public void addJwtCookieFromToken(HttpServletResponse response, String token) {
        ResponseCookie cookie = ResponseCookie.from("jwt_token", token)
                .httpOnly(true)
                .secure(cookieSecure)
                .path("/")
                .maxAge(Duration.ofMillis(jwtExpirationMs))
                .sameSite("Lax")
                .build();
        response.addHeader(HttpHeaders.SET_COOKIE, cookie.toString());
    }

    public void clearJwtCookie(HttpServletResponse response) {
        ResponseCookie cookie = ResponseCookie.from("jwt_token", "")
                .httpOnly(true)
                .secure(cookieSecure)
                .path("/")
                .maxAge(Duration.ZERO)
                .sameSite("Lax")
                .build();
        response.addHeader(HttpHeaders.SET_COOKIE, cookie.toString());
    }
}
