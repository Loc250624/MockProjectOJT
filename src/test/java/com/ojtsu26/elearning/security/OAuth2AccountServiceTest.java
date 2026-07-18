package com.ojtsu26.elearning.security;

import com.ojtsu26.elearning.dto.request.OAuth2CompleteRegistrationRequestDTO;
import com.ojtsu26.elearning.model.entity.User;
import com.ojtsu26.elearning.model.entity.UserProviderIdentity;
import com.ojtsu26.elearning.model.enums.AuthProvider;
import com.ojtsu26.elearning.model.enums.Role;
import com.ojtsu26.elearning.model.enums.UserStatus;
import com.ojtsu26.elearning.repository.PendingOAuthRegistrationRepository;
import com.ojtsu26.elearning.repository.UserProviderIdentityRepository;
import com.ojtsu26.elearning.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.test.context.TestPropertySource;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DataJpaTest
@Import({OAuth2AccountService.class, OAuth2AccountServiceTest.TestSecurityBeans.class})
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@TestPropertySource(properties = {
        "spring.datasource.url=jdbc:h2:mem:oauth2_account_test;MODE=MySQL;DATABASE_TO_LOWER=TRUE;DB_CLOSE_DELAY=-1",
        "spring.datasource.driver-class-name=org.h2.Driver",
        "spring.datasource.username=sa",
        "spring.datasource.password=",
        "spring.jpa.hibernate.ddl-auto=create-drop",
        "spring.jpa.properties.hibernate.dialect=org.hibernate.dialect.H2Dialect",
        "spring.jpa.show-sql=false",
        "spring.jpa.properties.hibernate.show_sql=false",
        "spring.jpa.properties.hibernate.format_sql=false"
})
class OAuth2AccountServiceTest {

    @Autowired
    private OAuth2AccountService service;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private UserProviderIdentityRepository identityRepository;

    @Autowired
    private PendingOAuthRegistrationRepository pendingRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @BeforeEach
    void setUp() {
        identityRepository.deleteAll();
        pendingRepository.deleteAll();
        userRepository.deleteAll();
    }

    @Test
    void firstTimeOAuth2UserStartsPendingRegistrationWithoutCreatingUser() {
        OAuth2LoginResult result = service.loginOrStartRegistration(profile(
                AuthProvider.GOOGLE,
                "google-sub-1",
                "New.User@Example.com",
                "New User"));

        assertThat(result.isPending()).isTrue();
        assertThat(result.pendingRegistrationToken()).isNotBlank();
        assertThat(userRepository.count()).isZero();

        var pending = pendingRepository.findByTokenAndUsedAtIsNull(result.pendingRegistrationToken()).orElseThrow();
        assertThat(pending.getEmail()).isEqualTo("new.user@example.com");
        assertThat(pending.getProvider()).isEqualTo(AuthProvider.GOOGLE);
        assertThat(pending.getProviderSubject()).isEqualTo("google-sub-1");
    }

    @Test
    void completionCreatesActiveStudentWithHashedPasswordAndProviderIdentity() {
        OAuth2LoginResult result = service.loginOrStartRegistration(profile(
                AuthProvider.GITHUB,
                "12345",
                "New.GitHub@Example.com",
                "New GitHub"));

        User user = service.completePendingRegistration(
                result.pendingRegistrationToken(),
                completeRequest("secret123", "secret123"));

        assertThat(user.getId()).isNotNull();
        assertThat(user.getEmail()).isEqualTo("new.github@example.com");
        assertThat(user.getRole()).isEqualTo(Role.STUDENT);
        assertThat(user.getStatus()).isEqualTo(UserStatus.ACTIVE);
        assertThat(user.getAuthProvider()).isEqualTo(AuthProvider.GITHUB);
        assertThat(user.getProviderId()).isEqualTo("12345");
        assertThat(passwordEncoder.matches("secret123", user.getPasswordHash())).isTrue();
        assertThat(pendingRepository.findByTokenAndUsedAtIsNull(result.pendingRegistrationToken())).isEmpty();

        UserProviderIdentity identity = identityRepository
                .findByProviderAndProviderSubject(AuthProvider.GITHUB, "12345")
                .orElseThrow();
        assertThat(identity.getUser().getId()).isEqualTo(user.getId());
        assertThat(identity.getEmailAtLink()).isEqualTo("new.github@example.com");
    }

    @Test
    void existingVerifiedEmailAutoLinksProviderAndPreservesRole() {
        User local = userRepository.saveAndFlush(User.builder()
                .fullName("Local Teacher")
                .email("teacher@example.com")
                .passwordHash("hash")
                .role(Role.TEACHER)
                .status(UserStatus.ACTIVE)
                .authProvider(AuthProvider.LOCAL)
                .build());

        OAuth2LoginResult result = service.loginOrStartRegistration(profile(
                AuthProvider.GOOGLE,
                "google-sub-2",
                "Teacher@Example.com",
                "Provider Name"));

        assertThat(result.isPending()).isFalse();
        assertThat(result.user().getId()).isEqualTo(local.getId());
        assertThat(result.user().getRole()).isEqualTo(Role.TEACHER);
        assertThat(result.user().getAuthProvider()).isEqualTo(AuthProvider.LOCAL);
        assertThat(identityRepository.findByProviderAndProviderSubject(AuthProvider.GOOGLE, "google-sub-2"))
                .isPresent();
        assertThat(userRepository.count()).isEqualTo(1);
    }

    @Test
    void existingProviderIdentityLogsInAndUpdatesMutableProviderMetadata() {
        User existing = userRepository.saveAndFlush(User.builder()
                .fullName("Old Name")
                .email("old.email@example.com")
                .role(Role.TEACHER)
                .status(UserStatus.ACTIVE)
                .authProvider(AuthProvider.GOOGLE)
                .providerId("google-sub-3")
                .build());

        OAuth2LoginResult result = service.loginOrStartRegistration(profile(
                AuthProvider.GOOGLE,
                "google-sub-3",
                "new.email@example.com",
                "New Name"));

        assertThat(result.isPending()).isFalse();
        assertThat(result.user().getId()).isEqualTo(existing.getId());
        assertThat(result.user().getFullName()).isEqualTo("New Name");
        assertThat(identityRepository.findByProviderAndProviderSubject(AuthProvider.GOOGLE, "google-sub-3"))
                .isPresent();
        assertThat(userRepository.count()).isEqualTo(1);
    }

    @Test
    void sameProviderExistingUserWithoutProviderIdIsLinked() {
        User existing = userRepository.saveAndFlush(User.builder()
                .fullName("Google User")
                .email("google.user@example.com")
                .role(Role.STUDENT)
                .status(UserStatus.ACTIVE)
                .authProvider(AuthProvider.GOOGLE)
                .build());

        OAuth2LoginResult result = service.loginOrStartRegistration(profile(
                AuthProvider.GOOGLE,
                "google-sub-4",
                "google.user@example.com",
                "Google User"));

        assertThat(result.user().getId()).isEqualTo(existing.getId());
        assertThat(result.user().getProviderId()).isEqualTo("google-sub-4");
        assertThat(identityRepository.findByProviderAndProviderSubject(AuthProvider.GOOGLE, "google-sub-4"))
                .isPresent();
    }

    @Test
    void activeProviderCollisionOnSameUserRequiresSafeError() {
        User user = userRepository.saveAndFlush(User.builder()
                .fullName("Google User")
                .email("google.user@example.com")
                .role(Role.STUDENT)
                .status(UserStatus.ACTIVE)
                .authProvider(AuthProvider.GOOGLE)
                .build());
        identityRepository.saveAndFlush(UserProviderIdentity.builder()
                .user(user)
                .provider(AuthProvider.GOOGLE)
                .providerSubject("original-sub")
                .emailAtLink("google.user@example.com")
                .build());

        assertThatThrownBy(() -> service.loginOrStartRegistration(profile(
                AuthProvider.GOOGLE,
                "different-sub",
                "google.user@example.com",
                "Google User")))
                .isInstanceOfSatisfying(OAuth2AuthenticationException.class, ex ->
                        assertThat(ex.getError().getErrorCode())
                                .isEqualTo(OAuth2AccountService.ERROR_ACCOUNT_LINK_REQUIRED));
    }

    @Test
    void inactiveEmailCollisionDoesNotCreatePendingOrMutateProviderLink() {
        userRepository.saveAndFlush(User.builder()
                .fullName("Blocked Local")
                .email("blocked@example.com")
                .passwordHash("hash")
                .role(Role.STUDENT)
                .status(UserStatus.BLOCKED)
                .authProvider(AuthProvider.LOCAL)
                .build());

        assertThatThrownBy(() -> service.loginOrStartRegistration(profile(
                AuthProvider.GOOGLE,
                "google-sub-6",
                "blocked@example.com",
                "Blocked Local")))
                .isInstanceOfSatisfying(OAuth2AuthenticationException.class, ex ->
                        assertThat(ex.getError().getErrorCode())
                                .isEqualTo(OAuth2AccountService.ERROR_ACCOUNT_BLOCKED));

        User user = userRepository.findByEmailIgnoreCase("blocked@example.com").orElseThrow();
        assertThat(user.getAuthProvider()).isEqualTo(AuthProvider.LOCAL);
        assertThat(user.getProviderId()).isNull();
        assertThat(identityRepository.findByProviderAndProviderSubject(AuthProvider.GOOGLE, "google-sub-6"))
                .isEmpty();
        assertThat(pendingRepository.count()).isZero();
    }

    private OAuth2ProviderProfile profile(AuthProvider provider, String providerId, String email, String name) {
        return new OAuth2ProviderProfile(provider, providerId, email, name, "https://cdn.example/avatar.png");
    }

    private OAuth2CompleteRegistrationRequestDTO completeRequest(String password, String confirmPassword) {
        OAuth2CompleteRegistrationRequestDTO request = new OAuth2CompleteRegistrationRequestDTO();
        request.setPassword(password);
        request.setConfirmPassword(confirmPassword);
        return request;
    }

    static class TestSecurityBeans {
        @Bean
        PasswordEncoder passwordEncoder() {
            return new BCryptPasswordEncoder();
        }
    }
}
