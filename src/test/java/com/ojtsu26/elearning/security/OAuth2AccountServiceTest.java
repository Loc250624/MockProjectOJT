package com.ojtsu26.elearning.security;

import com.ojtsu26.elearning.dto.request.OAuth2CompleteRegistrationRequestDTO;
import com.ojtsu26.elearning.exception.BusinessException;
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
import org.springframework.test.context.TestPropertySource;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

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
                completeRequest("Secret@123", "Secret@123"));

        assertThat(user.getId()).isNotNull();
        assertThat(user.getEmail()).isEqualTo("new.github@example.com");
        assertThat(user.getRole()).isEqualTo(Role.STUDENT);
        assertThat(user.getStatus()).isEqualTo(UserStatus.ACTIVE);
        assertThat(user.getAuthProvider()).isEqualTo(AuthProvider.GITHUB);
        assertThat(user.getProviderId()).isEqualTo("12345");
        assertThat(passwordEncoder.matches("Secret@123", user.getPasswordHash())).isTrue();
        assertThat(pendingRepository.findByTokenAndUsedAtIsNull(result.pendingRegistrationToken())).isEmpty();

        UserProviderIdentity identity = identityRepository
                .findByProviderAndProviderSubject(AuthProvider.GITHUB, "12345")
                .orElseThrow();
        assertThat(identity.getUser().getId()).isEqualTo(user.getId());
        assertThat(identity.getEmailAtLink()).isEqualTo("new.github@example.com");
    }

    @Test
    void existingLocalEmailDoesNotAutoLinkOAuthProvider() {
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

        assertThat(result.isPending()).isTrue();
        assertThat(identityRepository.findByProviderAndProviderSubject(AuthProvider.GOOGLE, "google-sub-2"))
                .isEmpty();
        assertThat(userRepository.count()).isEqualTo(1);
        assertThat(userRepository.findById(local.getId())).isPresent();
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
    void sameProviderEmailWithoutStableSubjectMatchIsNotLinked() {
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

        assertThat(result.isPending()).isTrue();
        assertThat(existing.getProviderId()).isNull();
        assertThat(identityRepository.findByProviderAndProviderSubject(AuthProvider.GOOGLE, "google-sub-4"))
                .isEmpty();
    }

    @Test
    void differentSubjectWithSameProviderEmailDoesNotTakeOverExistingUser() {
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

        OAuth2LoginResult result = service.loginOrStartRegistration(profile(
                AuthProvider.GOOGLE,
                "different-sub",
                "google.user@example.com",
                "Google User"));

        assertThat(result.isPending()).isTrue();
        assertThatThrownBy(() -> service.completePendingRegistration(
                result.pendingRegistrationToken(), completeRequest("Secret@123", "Secret@123")))
                .isInstanceOf(BusinessException.class);
        assertThat(identityRepository.findByProviderAndProviderSubject(AuthProvider.GOOGLE, "original-sub"))
                .isPresent();
        assertThat(identityRepository.findByProviderAndProviderSubject(AuthProvider.GOOGLE, "different-sub"))
                .isEmpty();
    }

    @Test
    void inactiveLocalEmailDoesNotBlockIndependentOAuthRegistration() {
        userRepository.saveAndFlush(User.builder()
                .fullName("Blocked Local")
                .email("blocked@example.com")
                .passwordHash("hash")
                .role(Role.STUDENT)
                .status(UserStatus.BLOCKED)
                .authProvider(AuthProvider.LOCAL)
                .build());

        OAuth2LoginResult result = service.loginOrStartRegistration(profile(
                AuthProvider.GOOGLE,
                "google-sub-6",
                "blocked@example.com",
                "Blocked Local"));

        User user = userRepository.findByAuthProviderAndEmailIgnoreCase(
                AuthProvider.LOCAL, "blocked@example.com").orElseThrow();
        assertThat(result.isPending()).isTrue();
        assertThat(user.getAuthProvider()).isEqualTo(AuthProvider.LOCAL);
        assertThat(user.getProviderId()).isNull();
        assertThat(identityRepository.findByProviderAndProviderSubject(AuthProvider.GOOGLE, "google-sub-6"))
                .isEmpty();
        assertThat(pendingRepository.count()).isEqualTo(1);
    }

    @Test
    void googleAndGithubWithSameEmailCreateAndResolveSeparateUsersAndAvatars() {
        String sharedEmail = "same@example.com";
        OAuth2LoginResult googlePending = service.loginOrStartRegistration(profileWithAvatar(
                AuthProvider.GOOGLE, "google-123", sharedEmail, "Google Person", "https://google.example/avatar.png"));
        User googleUser = service.completePendingRegistration(
                googlePending.pendingRegistrationToken(), completeRequest("Secret@123", "Secret@123"));

        OAuth2LoginResult githubPending = service.loginOrStartRegistration(profileWithAvatar(
                AuthProvider.GITHUB, "987654", sharedEmail, "GitHub Person", "https://github.example/avatar.png"));
        User githubUser = service.completePendingRegistration(
                githubPending.pendingRegistrationToken(), completeRequest("Secret@123", "Secret@123"));

        assertThat(googleUser.getId()).isNotEqualTo(githubUser.getId());
        assertThat(googleUser.getAvatarUrl()).isEqualTo("https://google.example/avatar.png");
        assertThat(githubUser.getAvatarUrl()).isEqualTo("https://github.example/avatar.png");
        assertThat(userRepository.count()).isEqualTo(2);

        OAuth2LoginResult googleAgain = service.loginOrStartRegistration(profileWithAvatar(
                AuthProvider.GOOGLE, "google-123", sharedEmail, "Google Person", "https://google.example/avatar-2.png"));
        OAuth2LoginResult githubAgain = service.loginOrStartRegistration(profileWithAvatar(
                AuthProvider.GITHUB, "987654", sharedEmail, "GitHub Person", "https://github.example/avatar.png"));

        assertThat(googleAgain.user().getId()).isEqualTo(googleUser.getId());
        assertThat(githubAgain.user().getId()).isEqualTo(githubUser.getId());
        assertThat(googleAgain.user().getAvatarUrl()).isEqualTo("https://google.example/avatar-2.png");
        assertThat(githubAgain.user().getAvatarUrl()).isEqualTo("https://github.example/avatar.png");
        assertThat(userRepository.count()).isEqualTo(2);
        assertThat(identityRepository.count()).isEqualTo(2);
    }

    @Test
    void githubLoginRepairsCrossProviderIdentityWithoutChangingGoogleAccount() {
        String sharedEmail = "same@example.com";
        User googleUser = userRepository.saveAndFlush(User.builder()
                .fullName("Google Person")
                .email(sharedEmail)
                .passwordHash(passwordEncoder.encode("Secret@123"))
                .avatarUrl("https://google.example/avatar.png")
                .providerId("google-123")
                .authProvider(AuthProvider.GOOGLE)
                .role(Role.STUDENT)
                .status(UserStatus.ACTIVE)
                .build());
        identityRepository.saveAndFlush(UserProviderIdentity.builder()
                .user(googleUser)
                .provider(AuthProvider.GOOGLE)
                .providerSubject("google-123")
                .emailAtLink(sharedEmail)
                .build());
        identityRepository.saveAndFlush(UserProviderIdentity.builder()
                .user(googleUser)
                .provider(AuthProvider.GITHUB)
                .providerSubject("github-456")
                .emailAtLink(sharedEmail)
                .build());

        OAuth2LoginResult githubPending = service.loginOrStartRegistration(profileWithAvatar(
                AuthProvider.GITHUB,
                "github-456",
                sharedEmail,
                "GitHub Person",
                "https://github.example/avatar.png"));

        assertThat(githubPending.isPending()).isTrue();
        assertThat(identityRepository.findByProviderAndProviderSubject(
                AuthProvider.GITHUB, "github-456")).isEmpty();
        assertThat(identityRepository.findByProviderAndProviderSubject(
                AuthProvider.GOOGLE, "google-123")).isPresent();

        User unchangedGoogle = userRepository.findById(googleUser.getId()).orElseThrow();
        assertThat(unchangedGoogle.getFullName()).isEqualTo("Google Person");
        assertThat(unchangedGoogle.getAvatarUrl()).isEqualTo("https://google.example/avatar.png");

        User githubUser = service.completePendingRegistration(
                githubPending.pendingRegistrationToken(), completeRequest("Secret@123", "Secret@123"));

        assertThat(githubUser.getId()).isNotEqualTo(googleUser.getId());
        assertThat(githubUser.getAuthProvider()).isEqualTo(AuthProvider.GITHUB);
        assertThat(githubUser.getFullName()).isEqualTo("GitHub Person");
        assertThat(githubUser.getAvatarUrl()).isEqualTo("https://github.example/avatar.png");
        assertThat(userRepository.count()).isEqualTo(2);

        unchangedGoogle = userRepository.findById(googleUser.getId()).orElseThrow();
        assertThat(unchangedGoogle.getFullName()).isEqualTo("Google Person");
        assertThat(unchangedGoogle.getAvatarUrl()).isEqualTo("https://google.example/avatar.png");
    }

    @Test
    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    void concurrentCompletionForSameProviderSubjectReturnsOneInternalUser() throws Exception {
        OAuth2ProviderProfile providerProfile = profile(
                AuthProvider.GOOGLE, "concurrent-google-sub", "concurrent@example.com", "Concurrent User");
        OAuth2LoginResult firstPending = service.loginOrStartRegistration(providerProfile);
        OAuth2LoginResult secondPending = service.loginOrStartRegistration(providerProfile);
        CountDownLatch startGate = new CountDownLatch(1);
        ExecutorService executor = Executors.newFixedThreadPool(2);

        try {
            Future<User> first = executor.submit(() -> {
                startGate.await();
                return service.completePendingRegistration(
                        firstPending.pendingRegistrationToken(), completeRequest("Secret@123", "Secret@123"));
            });
            Future<User> second = executor.submit(() -> {
                startGate.await();
                return service.completePendingRegistration(
                        secondPending.pendingRegistrationToken(), completeRequest("Secret@123", "Secret@123"));
            });

            startGate.countDown();
            User firstUser = first.get(15, TimeUnit.SECONDS);
            User secondUser = second.get(15, TimeUnit.SECONDS);

            assertThat(firstUser.getId()).isEqualTo(secondUser.getId());
            assertThat(userRepository.count()).isEqualTo(1);
            assertThat(identityRepository.count()).isEqualTo(1);
        } finally {
            executor.shutdownNow();
        }
    }

    private OAuth2ProviderProfile profile(AuthProvider provider, String providerId, String email, String name) {
        return profileWithAvatar(provider, providerId, email, name, "https://cdn.example/avatar.png");
    }

    private OAuth2ProviderProfile profileWithAvatar(
            AuthProvider provider, String providerId, String email, String name, String avatarUrl) {
        return new OAuth2ProviderProfile(provider, providerId, email, name, avatarUrl);
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
