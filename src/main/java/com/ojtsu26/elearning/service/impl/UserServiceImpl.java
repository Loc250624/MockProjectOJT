package com.ojtsu26.elearning.service.impl;

import com.ojtsu26.elearning.model.entity.User;
import com.ojtsu26.elearning.dto.request.UpdateProfileRequestDTO;
import com.ojtsu26.elearning.dto.request.UserRequestDTO;
import com.ojtsu26.elearning.dto.response.UserResponseDTO;
import com.ojtsu26.elearning.exception.BusinessException;
import com.ojtsu26.elearning.exception.ErrorCode;
import com.ojtsu26.elearning.mapper.UserMapper;
import com.ojtsu26.elearning.model.enums.AuthProvider;
import com.ojtsu26.elearning.repository.UserRepository;
import com.ojtsu26.elearning.service.UserService;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class UserServiceImpl implements UserService {

    private final UserRepository userRepository;
    private final UserMapper userMapper;

    @Override
    public List<UserResponseDTO> findAll() {
        return userRepository.findAll().stream()
                .map(userMapper::toDto)
                .collect(Collectors.toList());
    }

    @Override
    public UserResponseDTO findById(Integer id) {
        User entity = userRepository.findById(id)
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));
        return userMapper.toDto(entity);
    }

    @Override
    public UserResponseDTO create(UserRequestDTO requestDTO) {
        User entity = userMapper.toEntity(requestDTO);
        User saved = userRepository.save(entity);
        return userMapper.toDto(saved);
    }

    @Override
    public UserResponseDTO update(Integer id, UserRequestDTO requestDTO) {
        if (!userRepository.existsById(id)) {
            throw new RuntimeException("User not found");
        }
        User entity = userMapper.toEntity(requestDTO);
        entity.setId(id);
        User updated = userRepository.save(entity);
        return userMapper.toDto(updated);
    }

    @Override
    public void delete(Integer id) {
        userRepository.deleteById(id);
    }

    @Override
    public UserResponseDTO getCurrentProfile(Integer currentUserId) {
        User user = getUserOrThrow(currentUserId);
        return userMapper.toDto(user);
    }

    @Override
    @Transactional
    public UserResponseDTO updateCurrentProfile(Integer currentUserId, UpdateProfileRequestDTO request) {
        User user = getUserOrThrow(currentUserId);

        String normalizedEmail = normalizeEmail(request.getEmail());
        if (user.getAuthProvider() != AuthProvider.LOCAL
                && !normalizedEmail.equalsIgnoreCase(user.getEmail())) {
            throw new BusinessException(
                    ErrorCode.OAUTH2_EMAIL_CANNOT_BE_CHANGED,
                    "Email cua tai khoan OAuth2 duoc quan ly boi nha cung cap dang nhap"
            );
        }

        if (!normalizedEmail.equalsIgnoreCase(user.getEmail())
                && userRepository.existsByEmailIgnoreCaseAndIdNot(normalizedEmail, user.getId())) {
            throw new BusinessException(ErrorCode.EMAIL_ALREADY_EXISTS);
        }

        user.setFullName(request.getFullName().trim());
        user.setEmail(normalizedEmail);
        if (request.getAvatarUrl() != null) {
            user.setAvatarUrl(normalizeAvatarUrl(request.getAvatarUrl()));
        }

        User saved = userRepository.save(user);
        return userMapper.toDto(saved);
    }

    @Override
    @Transactional
    public UserResponseDTO updateCurrentAvatar(Integer currentUserId, String avatarUrl) {
        User user = getUserOrThrow(currentUserId);
        user.setAvatarUrl(normalizeOptionalText(avatarUrl));
        User saved = userRepository.save(user);
        return userMapper.toDto(saved);
    }

    private User getUserOrThrow(Integer id) {
        return userRepository.findById(id)
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));
    }

    private String normalizeEmail(String email) {
        return email.trim().toLowerCase(Locale.ROOT);
    }

    private String normalizeOptionalText(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return value.trim();
    }

    private String normalizeAvatarUrl(String avatarUrl) {
        String normalizedAvatarUrl = normalizeOptionalText(avatarUrl);
        if (normalizedAvatarUrl == null) {
            return null;
        }
        String lowerAvatarUrl = normalizedAvatarUrl.toLowerCase(Locale.ROOT);
        if (!lowerAvatarUrl.startsWith("http://") && !lowerAvatarUrl.startsWith("https://")) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "Avatar URL must start with http:// or https://");
        }
        return normalizedAvatarUrl;
    }
}
