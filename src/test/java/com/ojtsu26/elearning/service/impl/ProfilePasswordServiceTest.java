package com.ojtsu26.elearning.service.impl;

import com.ojtsu26.elearning.dto.request.ChangePasswordRequestDTO;
import com.ojtsu26.elearning.exception.BusinessException;
import com.ojtsu26.elearning.exception.ErrorCode;
import com.ojtsu26.elearning.mapper.UserMapper;
import com.ojtsu26.elearning.model.entity.User;
import com.ojtsu26.elearning.model.enums.AuthProvider;
import com.ojtsu26.elearning.model.enums.Role;
import com.ojtsu26.elearning.repository.CertificateRepository;
import com.ojtsu26.elearning.repository.CourseEnrollmentRepository;
import com.ojtsu26.elearning.repository.CourseRepository;
import com.ojtsu26.elearning.repository.LessonProgressRepository;
import com.ojtsu26.elearning.repository.SubmissionRepository;
import com.ojtsu26.elearning.repository.UserRepository;
import com.ojtsu26.elearning.service.AvatarStorageService;
import com.ojtsu26.elearning.service.CurrentUserService;
import com.ojtsu26.elearning.service.ExternalImageService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ProfilePasswordServiceTest {

    @Mock private CurrentUserService currentUserService;
    @Mock private UserRepository userRepository;
    @Mock private UserMapper userMapper;
    @Mock private AvatarStorageService avatarStorageService;
    @Mock private ExternalImageService externalImageService;
    @Mock private CourseRepository courseRepository;
    @Mock private CourseEnrollmentRepository courseEnrollmentRepository;
    @Mock private LessonProgressRepository lessonProgressRepository;
    @Mock private CertificateRepository certificateRepository;
    @Mock private SubmissionRepository submissionRepository;
    @Mock private PasswordEncoder passwordEncoder;

    @InjectMocks
    private ProfileServiceImpl profileService;

    private User user;

    @BeforeEach
    void setUp() {
        user = User.builder()
                .id(10)
                .role(Role.STUDENT)
                .authProvider(AuthProvider.LOCAL)
                .passwordHash("stored-hash")
                .build();
        when(currentUserService.getCurrentUser()).thenReturn(user);
    }

    @ParameterizedTest
    @EnumSource(Role.class)
    void everyLocalRoleCanStoreOnlyAnEncodedNewPassword(Role role) {
        user.setRole(role);
        ChangePasswordRequestDTO request = request("Current@123", "Better@456", "Better@456");
        when(passwordEncoder.matches("Current@123", "stored-hash")).thenReturn(true);
        when(passwordEncoder.matches("Better@456", "stored-hash")).thenReturn(false);
        when(passwordEncoder.encode("Better@456")).thenReturn("new-encoded-hash");

        profileService.changeCurrentPassword(request);

        assertThat(user.getPasswordHash()).isEqualTo("new-encoded-hash").isNotEqualTo(request.getNewPassword());
        verify(userRepository).save(user);
    }

    @Test
    void rejectsIncorrectCurrentPasswordWithoutSaving() {
        when(passwordEncoder.matches("Wrong@123", "stored-hash")).thenReturn(false);

        assertError(request("Wrong@123", "Better@456", "Better@456"), ErrorCode.CURRENT_PASSWORD_INCORRECT);
        verify(userRepository, never()).save(user);
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "NOLOWERCASE@1",
            "nouppercase@1",
            "NoNumber@",
            "NoSpecial123",
            "Short1!",
            "Contains Space1!"
    })
    void rejectsEveryWeakPasswordCategory(String weakPassword) {
        when(passwordEncoder.matches("Current@123", "stored-hash")).thenReturn(true);
        assertError(request("Current@123", weakPassword, weakPassword), ErrorCode.PASSWORD_POLICY_VIOLATION);
        verify(userRepository, never()).save(user);
    }

    @Test
    void rejectsPasswordLongerThan72Characters() {
        String tooLong = "Aa1!" + "x".repeat(69);
        when(passwordEncoder.matches("Current@123", "stored-hash")).thenReturn(true);
        assertError(request("Current@123", tooLong, tooLong), ErrorCode.PASSWORD_POLICY_VIOLATION);
    }

    @Test
    void rejectsConfirmationMismatch() {
        when(passwordEncoder.matches("Current@123", "stored-hash")).thenReturn(true);
        assertError(request("Current@123", "Better@456", "Different@456"), ErrorCode.PASSWORD_CONFIRMATION_MISMATCH);
    }

    @Test
    void rejectsSamePlainCurrentAndNewPassword() {
        when(passwordEncoder.matches("Current@123", "stored-hash")).thenReturn(true);
        assertError(request("Current@123", "Current@123", "Current@123"), ErrorCode.NEW_PASSWORD_REUSED);
    }

    @Test
    void rejectsNewPasswordThatMatchesStoredHash() {
        when(passwordEncoder.matches("Current@123", "stored-hash")).thenReturn(true);
        when(passwordEncoder.matches("Better@456", "stored-hash")).thenReturn(true);
        assertError(request("Current@123", "Better@456", "Better@456"), ErrorCode.NEW_PASSWORD_REUSED);
    }

    @Test
    void rejectsOauthManagedAccount() {
        user.setAuthProvider(AuthProvider.GOOGLE);
        assertError(request("Current@123", "Better@456", "Better@456"), ErrorCode.PASSWORD_CHANGE_NOT_AVAILABLE);
        verify(passwordEncoder, never()).encode("Better@456");
    }

    private void assertError(ChangePasswordRequestDTO request, ErrorCode expected) {
        assertThatThrownBy(() -> profileService.changeCurrentPassword(request))
                .isInstanceOfSatisfying(BusinessException.class,
                        exception -> assertThat(exception.getErrorCode()).isEqualTo(expected));
    }

    private ChangePasswordRequestDTO request(String current, String next, String confirmation) {
        ChangePasswordRequestDTO request = new ChangePasswordRequestDTO();
        request.setCurrentPassword(current);
        request.setNewPassword(next);
        request.setConfirmPassword(confirmation);
        return request;
    }
}
