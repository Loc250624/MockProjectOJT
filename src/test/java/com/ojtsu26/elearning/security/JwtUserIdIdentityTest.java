package com.ojtsu26.elearning.security;

import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.security.Keys;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Date;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class JwtUserIdIdentityTest {

    private static final String SECRET =
            "testSecretKeyWithAtLeast256BitsForHMACSHA256SignatureAlgorithm123456789";

    private JwtUtils jwtUtils;

    @BeforeEach
    void setUp() {
        jwtUtils = new JwtUtils();
        ReflectionTestUtils.setField(jwtUtils, "jwtSecret", SECRET);
        ReflectionTestUtils.setField(jwtUtils, "jwtExpirationMs", 60_000);
    }

    @Test
    void tokensForAccountsSharingEmailRemainDistinctByInternalUserId() {
        String googleToken = jwtUtils.generateTokenFromUserId(101);
        String githubToken = jwtUtils.generateTokenFromUserId(202);

        assertThat(jwtUtils.getUserIdFromJwtToken(googleToken)).isEqualTo(101);
        assertThat(jwtUtils.getUserIdFromJwtToken(githubToken)).isEqualTo(202);
        assertThat(googleToken).isNotEqualTo(githubToken);
    }

    @Test
    void legacyEmailSubjectTokenIsRejectedByUserIdResolution() {
        String legacyToken = Jwts.builder()
                .setSubject("same@example.com")
                .setIssuedAt(new Date())
                .setExpiration(new Date(System.currentTimeMillis() + 60_000))
                .signWith(Keys.hmacShaKeyFor(SECRET.getBytes()), SignatureAlgorithm.HS256)
                .compact();

        assertThat(jwtUtils.validateJwtToken(legacyToken)).isTrue();
        assertThatThrownBy(() -> jwtUtils.getUserIdFromJwtToken(legacyToken))
                .isInstanceOf(JwtException.class);
    }
}
