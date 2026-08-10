package com.ojtsu26.elearning.service.impl;

import com.ojtsu26.elearning.dto.response.AdminPasswordResetLinkResponseDTO;
import com.ojtsu26.elearning.exception.BusinessException;
import com.ojtsu26.elearning.exception.ErrorCode;
import com.ojtsu26.elearning.model.entity.PasswordResetToken;
import com.ojtsu26.elearning.model.entity.User;
import com.ojtsu26.elearning.model.enums.AuthProvider;
import com.ojtsu26.elearning.model.enums.UserStatus;
import com.ojtsu26.elearning.repository.PasswordResetTokenRepository;
import com.ojtsu26.elearning.repository.UserRepository;
import com.ojtsu26.elearning.service.PasswordResetService;
import com.ojtsu26.elearning.validation.PasswordPolicy;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.util.UriComponentsBuilder;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.Base64;
import java.util.HexFormat;

@Service
@RequiredArgsConstructor
public class PasswordResetServiceImpl implements PasswordResetService {

    private static final int TOKEN_BYTES = 32;
    private static final int EXPIRY_MINUTES = 30;

    private final UserRepository userRepository;
    private final PasswordResetTokenRepository tokenRepository;
    private final PasswordEncoder passwordEncoder;
    private final SecureRandom secureRandom = new SecureRandom();

    @Value("${app.base-url:http://localhost:8080}")
    private String appBaseUrl;

    @Override
    @Transactional
    public AdminPasswordResetLinkResponseDTO createAdminResetLink(Integer userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));
        if (user.getAuthProvider() != AuthProvider.LOCAL || user.getPasswordHash() == null || user.getPasswordHash().isBlank()) {
            throw new BusinessException(
                    ErrorCode.PASSWORD_CHANGE_NOT_AVAILABLE,
                    "Password is managed by " + providerName(user.getAuthProvider())
            );
        }
        if (user.getStatus() == UserStatus.DELETED) {
            throw new BusinessException(ErrorCode.USER_ALREADY_DELETED);
        }

        LocalDateTime now = LocalDateTime.now();
        tokenRepository.findByUserAndUsedAtIsNull(user)
                .forEach(existing -> existing.setUsedAt(now));

        String rawToken = randomToken();
        LocalDateTime expiresAt = now.plusMinutes(EXPIRY_MINUTES);
        tokenRepository.save(PasswordResetToken.builder()
                .user(user)
                .tokenHash(hash(rawToken))
                .expiresAt(expiresAt)
                .build());

        return AdminPasswordResetLinkResponseDTO.builder()
                .userId(user.getId())
                .authProvider(user.getAuthProvider())
                .resetLink(resetUrl(rawToken))
                .expiresAt(expiresAt)
                .build();
    }

    @Override
    @Transactional
    public void resetPassword(String token, String password, String confirmPassword) {
        if (token == null || token.isBlank()) {
            throw new BusinessException(ErrorCode.PASSWORD_RESET_TOKEN_INVALID);
        }
        if (!PasswordPolicy.isStrong(password)) {
            throw new BusinessException(ErrorCode.PASSWORD_POLICY_VIOLATION);
        }
        if (!password.equals(confirmPassword)) {
            throw new BusinessException(ErrorCode.PASSWORD_CONFIRMATION_MISMATCH);
        }

        PasswordResetToken resetToken = tokenRepository.findByTokenHashAndUsedAtIsNull(hash(token))
                .orElseThrow(() -> new BusinessException(ErrorCode.PASSWORD_RESET_TOKEN_INVALID));
        LocalDateTime now = LocalDateTime.now();
        if (resetToken.getExpiresAt().isBefore(now)) {
            resetToken.setUsedAt(now);
            throw new BusinessException(ErrorCode.PASSWORD_RESET_TOKEN_INVALID);
        }

        User user = resetToken.getUser();
        if (user.getAuthProvider() != AuthProvider.LOCAL || user.getStatus() == UserStatus.DELETED) {
            resetToken.setUsedAt(now);
            throw new BusinessException(ErrorCode.PASSWORD_CHANGE_NOT_AVAILABLE);
        }
        if (passwordEncoder.matches(password, user.getPasswordHash())) {
            throw new BusinessException(ErrorCode.NEW_PASSWORD_REUSED);
        }

        user.setPasswordHash(passwordEncoder.encode(password));
        resetToken.setUsedAt(now);
        userRepository.save(user);
    }

    private String randomToken() {
        byte[] bytes = new byte[TOKEN_BYTES];
        secureRandom.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    private String hash(String rawToken) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            return HexFormat.of().formatHex(digest.digest(rawToken.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException ex) {
            throw new IllegalStateException("SHA-256 is not available", ex);
        }
    }

    private String resetUrl(String rawToken) {
        return UriComponentsBuilder.fromHttpUrl(appBaseUrl)
                .path("/auth/reset-password")
                .queryParam("token", rawToken)
                .build()
                .toUriString();
    }

    private String providerName(AuthProvider provider) {
        if (provider == null) {
            return "your sign-in provider";
        }
        String name = provider.name().toLowerCase();
        return Character.toUpperCase(name.charAt(0)) + name.substring(1);
    }
}
