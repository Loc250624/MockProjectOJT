package com.ojtsu26.elearning.service.impl;

import com.ojtsu26.elearning.dto.request.UpdateProfileRequestDTO;
import com.ojtsu26.elearning.dto.response.UserResponseDTO;
import com.ojtsu26.elearning.exception.BusinessException;
import com.ojtsu26.elearning.exception.ErrorCode;
import com.ojtsu26.elearning.mapper.UserMapper;
import com.ojtsu26.elearning.model.entity.User;
import com.ojtsu26.elearning.model.enums.AuthProvider;
import com.ojtsu26.elearning.model.enums.Role;
import com.ojtsu26.elearning.model.enums.UserStatus;
import com.ojtsu26.elearning.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UserServiceImplTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private UserMapper userMapper;

    @Mock
    private PasswordEncoder passwordEncoder;

    private UserServiceImpl userService;

    @BeforeEach
    void setUp() {
        userService = new UserServiceImpl(userRepository, userMapper, passwordEncoder);
        lenient().when(userMapper.toDto(any(User.class))).thenAnswer(invocation -> toDto(invocation.getArgument(0)));
    }

    @Test
    void getCurrentProfileReturnsProfile() {
        User user = localUser();
        when(userRepository.findById(1)).thenReturn(Optional.of(user));

        UserResponseDTO profile = userService.getCurrentProfile(1);

        assertThat(profile.getId()).isEqualTo(1);
        assertThat(profile.getFullName()).isEqualTo("Alex Johnson");
        assertThat(profile.getEmail()).isEqualTo("alex@example.com");
        assertThat(profile.getRole()).isEqualTo(Role.STUDENT);
    }

    @Test
    void getCurrentProfileThrowsWhenUserNotFound() {
        when(userRepository.findById(1)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> userService.getCurrentProfile(1))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.USER_NOT_FOUND);
    }

    @Test
    void updateCurrentProfileUpdatesFullName() {
        User user = localUser();
        when(userRepository.findById(1)).thenReturn(Optional.of(user));
        when(userRepository.save(user)).thenReturn(user);

        UserResponseDTO profile = userService.updateCurrentProfile(1, request("  New Name  ", "alex@example.com"));

        assertThat(profile.getFullName()).isEqualTo("New Name");
        assertThat(user.getFullName()).isEqualTo("New Name");
    }

    @Test
    void updateCurrentProfileAllowsLocalEmailChange() {
        User user = localUser();
        when(userRepository.findById(1)).thenReturn(Optional.of(user));
        when(userRepository.existsByAuthProviderAndEmailIgnoreCaseAndIdNot(
                AuthProvider.LOCAL, "new@example.com", 1)).thenReturn(false);
        when(userRepository.save(user)).thenReturn(user);

        UserResponseDTO profile = userService.updateCurrentProfile(1, request("Alex Johnson", "  NEW@Example.COM  "));

        assertThat(profile.getEmail()).isEqualTo("new@example.com");
        assertThat(user.getEmail()).isEqualTo("new@example.com");
    }

    @Test
    void updateCurrentProfileRejectsDuplicateEmail() {
        User user = localUser();
        when(userRepository.findById(1)).thenReturn(Optional.of(user));
        when(userRepository.existsByAuthProviderAndEmailIgnoreCaseAndIdNot(
                AuthProvider.LOCAL, "taken@example.com", 1)).thenReturn(true);

        assertThatThrownBy(() -> userService.updateCurrentProfile(1, request("Alex Johnson", "taken@example.com")))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.EMAIL_ALREADY_EXISTS);

        verify(userRepository, never()).save(any(User.class));
    }

    @Test
    void updateCurrentProfileRejectsOauth2EmailChange() {
        User user = localUser();
        user.setAuthProvider(AuthProvider.GOOGLE);
        when(userRepository.findById(1)).thenReturn(Optional.of(user));

        assertThatThrownBy(() -> userService.updateCurrentProfile(1, request("Alex Johnson", "new@example.com")))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.OAUTH2_EMAIL_CANNOT_BE_CHANGED);

        verify(userRepository, never()).save(any(User.class));
    }

    @Test
    void updateCurrentProfileDoesNotChangeRoleStatusOrPasswordHash() {
        User user = localUser();
        when(userRepository.findById(1)).thenReturn(Optional.of(user));
        when(userRepository.save(user)).thenReturn(user);

        userService.updateCurrentProfile(1, request("New Name", "alex@example.com"));

        assertThat(user.getRole()).isEqualTo(Role.STUDENT);
        assertThat(user.getStatus()).isEqualTo(UserStatus.ACTIVE);
        assertThat(user.getPasswordHash()).isEqualTo("secret-hash");
    }

    @Test
    void updateCurrentProfilePreservesAvatarUrl() {
        User user = localUser();
        when(userRepository.findById(1)).thenReturn(Optional.of(user));
        when(userRepository.save(user)).thenReturn(user);

        UserResponseDTO profile = userService.updateCurrentProfile(
                1,
                request("Alex Johnson", "alex@example.com")
        );

        assertThat(profile.getAvatarUrl()).isEqualTo("https://cdn.example.com/old.png");
        assertThat(user.getAvatarUrl()).isEqualTo("https://cdn.example.com/old.png");
    }

    @Test
    void updateCurrentAvatarUpdatesAvatarUrl() {
        User user = localUser();
        when(userRepository.findById(1)).thenReturn(Optional.of(user));
        when(userRepository.save(user)).thenReturn(user);

        UserResponseDTO profile = userService.updateCurrentAvatar(1, "  /uploads/avatars/avatar.png  ");

        assertThat(profile.getAvatarUrl()).isEqualTo("/uploads/avatars/avatar.png");
        assertThat(user.getAvatarUrl()).isEqualTo("/uploads/avatars/avatar.png");
    }

    private User localUser() {
        return User.builder()
                .id(1)
                .fullName("Alex Johnson")
                .email("alex@example.com")
                .passwordHash("secret-hash")
                .avatarUrl("https://cdn.example.com/old.png")
                .role(Role.STUDENT)
                .authProvider(AuthProvider.LOCAL)
                .status(UserStatus.ACTIVE)
                .build();
    }

    private UpdateProfileRequestDTO request(String fullName, String email) {
        UpdateProfileRequestDTO request = new UpdateProfileRequestDTO();
        request.setFullName(fullName);
        request.setEmail(email);
        return request;
    }

    private UserResponseDTO toDto(User user) {
        UserResponseDTO dto = new UserResponseDTO();
        dto.setId(user.getId());
        dto.setFullName(user.getFullName());
        dto.setEmail(user.getEmail());
        dto.setAvatarUrl(user.getAvatarUrl());
        dto.setRole(user.getRole());
        dto.setAuthProvider(user.getAuthProvider());
        dto.setStatus(user.getStatus());
        dto.setCreatedAt(user.getCreatedAt());
        dto.setUpdatedAt(user.getUpdatedAt());
        return dto;
    }
}
