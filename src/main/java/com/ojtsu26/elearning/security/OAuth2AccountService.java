package com.ojtsu26.elearning.security;

import com.ojtsu26.elearning.dto.request.OAuth2CompleteRegistrationRequestDTO;
import com.ojtsu26.elearning.exception.BusinessException;
import com.ojtsu26.elearning.exception.ErrorCode;
import com.ojtsu26.elearning.model.entity.PendingOAuthRegistration;
import com.ojtsu26.elearning.model.entity.User;
import com.ojtsu26.elearning.model.entity.UserProviderIdentity;
import com.ojtsu26.elearning.model.enums.AuthProvider;
import com.ojtsu26.elearning.model.enums.Role;
import com.ojtsu26.elearning.model.enums.UserStatus;
import com.ojtsu26.elearning.repository.PendingOAuthRegistrationRepository;
import com.ojtsu26.elearning.repository.UserProviderIdentityRepository;
import com.ojtsu26.elearning.repository.UserRepository;
import com.ojtsu26.elearning.validation.PasswordPolicy;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.OAuth2Error;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.security.SecureRandom;
import java.net.URI;
import java.net.URISyntaxException;
import java.time.LocalDateTime;
import java.util.Base64;
import java.util.Locale;
import java.util.Optional;

@Slf4j
@Service
@RequiredArgsConstructor
public class OAuth2AccountService {

    public static final String ERROR_EMAIL_NOT_FOUND = "email_not_found";
    public static final String ERROR_PROVIDER_ID_MISSING = "provider_id_missing";
    public static final String ERROR_ACCOUNT_LINK_REQUIRED = "account_link_required";
    public static final String ERROR_DUPLICATE_RETRY = "duplicate_retry";
    public static final String ERROR_ACCOUNT_BLOCKED = "account_blocked";
    public static final String ERROR_ACCOUNT_DELETED = "account_deleted";
    public static final String ERROR_PENDING_INVALID = "pending_invalid";

    private static final int PENDING_TOKEN_BYTES = 32;
    private static final long PENDING_TTL_MINUTES = 15;

    private final UserRepository userRepository;
    private final UserProviderIdentityRepository identityRepository;
    private final PendingOAuthRegistrationRepository pendingRepository;
    private final PasswordEncoder passwordEncoder;
    private final TransactionTemplate transactionTemplate;
    private final SecureRandom secureRandom = new SecureRandom();

    @Transactional
    public OAuth2LoginResult loginOrStartRegistration(OAuth2ProviderProfile profile) {
        validate(profile);

        String email = normalizeEmail(profile.email());
        OAuth2ProviderProfile normalizedProfile = normalizedProfile(profile, email);

        Optional<UserProviderIdentity> identity = identityRepository.findByProviderAndProviderSubject(
                normalizedProfile.provider(),
                normalizedProfile.providerId());
        if (identity.isPresent() && removeMismatchedIdentity(identity.get(), normalizedProfile.provider())) {
            identity = Optional.empty();
        }
        if (identity.isPresent()) {
            User user = identity.get().getUser();
            assertActiveForOAuth(user);
            updateLoginMetadata(user, normalizedProfile);
            updateIdentityMetadata(identity.get(), email);
            log.info("OAuth2 provider identity login: provider={}, userId={}",
                    normalizedProfile.provider(), user.getId());
            return OAuth2LoginResult.authenticated(userRepository.save(user));
        }

        Optional<User> legacyProviderUser = userRepository.findByProviderIdAndAuthProvider(
                normalizedProfile.providerId(),
                normalizedProfile.provider());
        if (legacyProviderUser.isPresent()) {
            User user = legacyProviderUser.get();
            assertActiveForOAuth(user);
            linkProviderIdentity(user, normalizedProfile);
            updateLoginMetadata(user, normalizedProfile);
            log.info("Migrated legacy OAuth2 provider identity: provider={}, userId={}",
                    normalizedProfile.provider(), user.getId());
            return OAuth2LoginResult.authenticated(userRepository.save(user));
        }

        PendingOAuthRegistration pending = createPendingRegistration(normalizedProfile);
        log.info("Started pending OAuth2 registration: provider={}", normalizedProfile.provider());
        return OAuth2LoginResult.pending(pending.getToken());
    }

    @Transactional(readOnly = true)
    public PendingOAuthRegistration requireActivePending(String token) {
        PendingOAuthRegistration pending = pendingRepository.findByTokenAndUsedAtIsNull(token)
                .orElseThrow(() -> new BusinessException(ErrorCode.BAD_REQUEST, "OAuth registration has expired. Please sign in again."));
        if (pending.getExpiresAt().isBefore(LocalDateTime.now())) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "OAuth registration has expired. Please sign in again.");
        }
        return pending;
    }

    public User completePendingRegistration(String token, OAuth2CompleteRegistrationRequestDTO request) {
        PendingOAuthRegistration pendingSnapshot = requireActivePending(token);
        try {
            return transactionTemplate.execute(status ->
                    completePendingRegistrationInTransaction(pendingSnapshot.getId(), request));
        } catch (DataIntegrityViolationException collision) {
            User recovered = transactionTemplate.execute(status ->
                    recoverConcurrentRegistration(pendingSnapshot));
            if (recovered != null) {
                return recovered;
            }
            throw new BusinessException(
                    ErrorCode.USER_ALREADY_EXISTS,
                    "Account creation collided with another request. Please sign in again.");
        }
    }

    private User completePendingRegistrationInTransaction(
            Integer pendingId,
            OAuth2CompleteRegistrationRequestDTO request) {
        PendingOAuthRegistration pending = pendingRepository.findById(pendingId)
                .filter(candidate -> candidate.getUsedAt() == null)
                .filter(candidate -> !candidate.getExpiresAt().isBefore(LocalDateTime.now()))
                .orElseThrow(() -> new BusinessException(
                        ErrorCode.BAD_REQUEST,
                        "OAuth registration has expired. Please sign in again."));
        validatePasswordConfirmation(request);

        OAuth2ProviderProfile profile = new OAuth2ProviderProfile(
                pending.getProvider(),
                pending.getProviderSubject(),
                pending.getEmail(),
                pending.getFullName(),
                pending.getAvatarUrl());

        Optional<UserProviderIdentity> identity = identityRepository.findByProviderAndProviderSubject(
                profile.provider(),
                profile.providerId());
        if (identity.isPresent() && removeMismatchedIdentity(identity.get(), profile.provider())) {
            identity = Optional.empty();
        }
        if (identity.isPresent()) {
            User user = identity.get().getUser();
            assertActiveForCompletion(user);
            updateLoginMetadata(user, profile);
            markPendingUsed(pending);
            return userRepository.save(user);
        }

        if (userRepository.existsByAuthProviderAndEmailIgnoreCase(
                pending.getProvider(), pending.getEmail())) {
            throw new BusinessException(
                    ErrorCode.USER_ALREADY_EXISTS,
                    "An account already exists for this sign-in provider and email. Please sign in with that provider.");
        }

        User newUser = User.builder()
                .email(pending.getEmail())
                .fullName(resolveDisplayName(pending))
                .avatarUrl(normalizeAvatarUrl(pending.getAvatarUrl()))
                .passwordHash(passwordEncoder.encode(request.getPassword()))
                .providerId(pending.getProviderSubject())
                .authProvider(pending.getProvider())
                .role(Role.STUDENT)
                .status(UserStatus.ACTIVE)
                .lastLoginAt(LocalDateTime.now())
                .build();

        User saved = userRepository.saveAndFlush(newUser);
        createRegistrationIdentity(saved, profile);
        markPendingUsed(pending);
        log.info("Completed OAuth2 registration: provider={}, userId={}", pending.getProvider(), saved.getId());
        return saved;
    }

    private User recoverConcurrentRegistration(PendingOAuthRegistration pendingSnapshot) {
        Optional<UserProviderIdentity> identity = identityRepository.findByProviderAndProviderSubject(
                pendingSnapshot.getProvider(), pendingSnapshot.getProviderSubject());
        if (identity.isEmpty()) {
            return null;
        }

        if (removeMismatchedIdentity(identity.get(), pendingSnapshot.getProvider())) {
            return null;
        }

        User user = identity.get().getUser();
        assertActiveForCompletion(user);
        pendingRepository.findById(pendingSnapshot.getId()).ifPresent(pending -> {
            if (pending.getUsedAt() == null) {
                markPendingUsed(pending);
            }
        });
        log.info("Recovered concurrent OAuth2 registration: provider={}, userId={}",
                pendingSnapshot.getProvider(), user.getId());
        return user;
    }

    private PendingOAuthRegistration createPendingRegistration(OAuth2ProviderProfile profile) {
        PendingOAuthRegistration pending = PendingOAuthRegistration.builder()
                .token(generatePendingToken())
                .provider(profile.provider())
                .providerSubject(profile.providerId())
                .email(profile.email())
                .fullName(normalizeOptional(profile.name()))
                .avatarUrl(normalizeOptional(profile.avatarUrl()))
                .expiresAt(LocalDateTime.now().plusMinutes(PENDING_TTL_MINUTES))
                .build();
        return pendingRepository.saveAndFlush(pending);
    }

    private void linkProviderIdentity(User user, OAuth2ProviderProfile profile) {
        Optional<UserProviderIdentity> existingForUser =
                identityRepository.findByUserIdAndProvider(user.getId(), profile.provider());
        if (existingForUser.isPresent()) {
            UserProviderIdentity identity = existingForUser.get();
            if (!identity.getProviderSubject().equals(profile.providerId())) {
                throw oauthError(ERROR_ACCOUNT_LINK_REQUIRED, "This account is already linked to a different provider identity.");
            }
            updateIdentityMetadata(identity, profile.email());
            return;
        }

        UserProviderIdentity identity = UserProviderIdentity.builder()
                .user(user)
                .provider(profile.provider())
                .providerSubject(profile.providerId())
                .emailAtLink(profile.email())
                .lastLoginAt(LocalDateTime.now())
                .build();
        try {
            identityRepository.saveAndFlush(identity);
        } catch (DataIntegrityViolationException e) {
            throw oauthError(ERROR_ACCOUNT_LINK_REQUIRED, "This provider identity is already linked.");
        }

        if (user.getAuthProvider() == profile.provider() && !StringUtils.hasText(user.getProviderId())) {
            user.setProviderId(profile.providerId());
        }
    }

    private void updateIdentityMetadata(UserProviderIdentity identity, String email) {
        identity.setEmailAtLink(email);
        identity.setLastLoginAt(LocalDateTime.now());
        identityRepository.save(identity);
    }

    private boolean removeMismatchedIdentity(UserProviderIdentity identity, AuthProvider loginProvider) {
        User owner = identity.getUser();
        if (owner != null && owner.getAuthProvider() == loginProvider) {
            return false;
        }

        log.warn(
                "Removed mismatched OAuth2 identity: loginProvider={}, ownerProvider={}, userId={}",
                loginProvider,
                owner == null ? null : owner.getAuthProvider(),
                owner == null ? null : owner.getId());
        identityRepository.delete(identity);
        identityRepository.flush();
        return true;
    }

    private void updateLoginMetadata(User user, OAuth2ProviderProfile profile) {
        String avatarUrl = normalizeAvatarUrl(profile.avatarUrl());
        if (avatarUrl != null) {
            user.setAvatarUrl(avatarUrl);
        }
        if (StringUtils.hasText(profile.name()) && !profile.name().trim().equals(user.getFullName())) {
            user.setFullName(profile.name().trim());
        }
        user.setLastLoginAt(LocalDateTime.now());
    }

    private void markPendingUsed(PendingOAuthRegistration pending) {
        pending.setUsedAt(LocalDateTime.now());
        pendingRepository.save(pending);
    }

    private void validate(OAuth2ProviderProfile profile) {
        if (profile == null || profile.provider() == null) {
            throw oauthError("unsupported_provider", "Unsupported OAuth2 provider.");
        }
        if (!StringUtils.hasText(profile.providerId())) {
            throw oauthError(ERROR_PROVIDER_ID_MISSING, "OAuth2 provider did not return a stable subject.");
        }
        if (!StringUtils.hasText(profile.email())) {
            throw oauthError(ERROR_EMAIL_NOT_FOUND, "OAuth2 provider did not return an email.");
        }
        if (profile.provider() == AuthProvider.LOCAL) {
            throw oauthError("unsupported_provider", "LOCAL is not an OAuth2 provider.");
        }
    }

    private void validatePasswordConfirmation(OAuth2CompleteRegistrationRequestDTO request) {
        if (request == null || !StringUtils.hasText(request.getPassword())) {
            throw new BusinessException(ErrorCode.VALIDATION_ERROR, "Password is required");
        }
        if (!PasswordPolicy.isStrong(request.getPassword())) {
            throw new BusinessException(ErrorCode.PASSWORD_POLICY_VIOLATION);
        }
        if (!request.getPassword().equals(request.getConfirmPassword())) {
            throw new BusinessException(ErrorCode.VALIDATION_ERROR, "Passwords do not match");
        }
    }

    private void assertActiveForOAuth(User user) {
        if (user.getStatus() == UserStatus.DELETED) {
            throw oauthError(ERROR_ACCOUNT_DELETED, "Account is deleted.");
        }
        if (user.getStatus() != UserStatus.ACTIVE) {
            throw oauthError(ERROR_ACCOUNT_BLOCKED, "Account is not active.");
        }
        if (user.getRole() == null) {
            throw oauthError("role_missing", "Account role is not supported.");
        }
    }

    private void assertActiveForCompletion(User user) {
        if (user.getStatus() == UserStatus.DELETED) {
            throw new BusinessException(ErrorCode.ACCESS_DENIED, "This account is no longer available.");
        }
        if (user.getStatus() != UserStatus.ACTIVE) {
            throw new BusinessException(ErrorCode.ACCESS_DENIED, "This account cannot sign in.");
        }
        if (user.getRole() == null) {
            throw new BusinessException(ErrorCode.ACCESS_DENIED, "Account role is not supported.");
        }
    }

    private OAuth2ProviderProfile normalizedProfile(OAuth2ProviderProfile profile, String normalizedEmail) {
        return new OAuth2ProviderProfile(
                profile.provider(),
                profile.providerId().trim(),
                normalizedEmail,
                normalizeOptional(profile.name()),
                normalizeAvatarUrl(profile.avatarUrl()));
    }

    private String normalizeEmail(String email) {
        return email.trim().toLowerCase(Locale.ROOT);
    }

    private String normalizeOptional(String value) {
        return StringUtils.hasText(value) ? value.trim() : null;
    }

    private void createRegistrationIdentity(User user, OAuth2ProviderProfile profile) {
        identityRepository.saveAndFlush(UserProviderIdentity.builder()
                .user(user)
                .provider(profile.provider())
                .providerSubject(profile.providerId())
                .emailAtLink(profile.email())
                .lastLoginAt(LocalDateTime.now())
                .build());
    }

    private String resolveDisplayName(PendingOAuthRegistration pending) {
        if (StringUtils.hasText(pending.getFullName())
                && !pending.getFullName().trim().equalsIgnoreCase(pending.getEmail())) {
            return pending.getFullName().trim();
        }
        String providerName = pending.getProvider().name().toLowerCase(Locale.ROOT);
        return Character.toUpperCase(providerName.charAt(0)) + providerName.substring(1) + " User";
    }

    private String normalizeAvatarUrl(String avatarUrl) {
        if (!StringUtils.hasText(avatarUrl)) {
            return null;
        }
        try {
            URI uri = new URI(avatarUrl.trim());
            String scheme = uri.getScheme();
            if (("http".equalsIgnoreCase(scheme) || "https".equalsIgnoreCase(scheme))
                    && StringUtils.hasText(uri.getHost())) {
                return uri.toString();
            }
        } catch (URISyntaxException ignored) {
            // Invalid provider metadata must not become an executable or malformed image URL.
        }
        return null;
    }

    private String generatePendingToken() {
        byte[] bytes = new byte[PENDING_TOKEN_BYTES];
        secureRandom.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    private OAuth2AuthenticationException oauthError(String code, String description) {
        return new OAuth2AuthenticationException(new OAuth2Error(code, description, null));
    }
}
