package com.ojtsu26.elearning.service.impl;

import com.ojtsu26.elearning.model.entity.User;
import com.ojtsu26.elearning.dto.request.AdminCreateUserRequestDTO;
import com.ojtsu26.elearning.dto.request.UpdateProfileRequestDTO;
import com.ojtsu26.elearning.dto.request.UserRequestDTO;
import com.ojtsu26.elearning.dto.response.UserResponseDTO;
import com.ojtsu26.elearning.exception.BusinessException;
import com.ojtsu26.elearning.exception.ErrorCode;
import com.ojtsu26.elearning.mapper.UserMapper;
import com.ojtsu26.elearning.model.enums.AuthProvider;
import com.ojtsu26.elearning.model.enums.Role;
import com.ojtsu26.elearning.model.enums.UserStatus;
import com.ojtsu26.elearning.repository.UserRepository;
import com.ojtsu26.elearning.service.UserService;
import com.ojtsu26.elearning.validation.PasswordPolicy;
import jakarta.persistence.criteria.Predicate;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class UserServiceImpl implements UserService {

    private final UserRepository userRepository;
    private final UserMapper userMapper;
    private final PasswordEncoder passwordEncoder;

    @Override
    public List<UserResponseDTO> findAll() {
        return userRepository.findAll().stream()
                .map(userMapper::toDto)
                .collect(Collectors.toList());
    }

    @Override
    public Page<UserResponseDTO> searchUsers(String keyword, Role role, UserStatus status, AuthProvider authProvider, Pageable pageable) {
        Pageable effectivePageable = pageable.getSort().isSorted()
                ? pageable
                : PageRequest.of(pageable.getPageNumber(), pageable.getPageSize(), Sort.by(Sort.Direction.DESC, "createdAt"));
        return userRepository.findAll(buildUserSearchSpecification(keyword, role, status, authProvider), effectivePageable)
                .map(userMapper::toDto);
    }

    @Override
    public List<UserResponseDTO> findUsersForExport(String keyword, Role role, UserStatus status,
                                                     AuthProvider authProvider, Sort sort) {
        Sort effectiveSort = sort == null || sort.isUnsorted()
                ? Sort.by(Sort.Direction.DESC, "createdAt")
                : sort;
        return userRepository.findAll(buildUserSearchSpecification(keyword, role, status, authProvider), effectiveSort)
                .stream()
                .map(userMapper::toDto)
                .toList();
    }

    @Override
    @Transactional
    public UserResponseDTO createByAdmin(AdminCreateUserRequestDTO requestDTO) {
        if (!PasswordPolicy.isStrong(requestDTO.getPassword())) {
            throw new BusinessException(ErrorCode.PASSWORD_POLICY_VIOLATION);
        }
        if (!requestDTO.getPassword().equals(requestDTO.getConfirmPassword())) {
            throw new BusinessException(ErrorCode.VALIDATION_ERROR, "Passwords do not match");
        }
        if (requestDTO.getRole() != Role.TEACHER && requestDTO.getRole() != Role.ADMIN) {
            throw new BusinessException(
                    ErrorCode.VALIDATION_ERROR,
                    "Admin-created accounts can only use Teacher or Admin roles"
            );
        }

        String normalizedEmail = normalizeEmail(requestDTO.getEmail());
        if (userRepository.existsByAuthProviderAndEmailIgnoreCase(AuthProvider.LOCAL, normalizedEmail)) {
            throw new BusinessException(ErrorCode.USER_ALREADY_EXISTS);
        }

        User user = User.builder()
                .fullName(requestDTO.getFullName().trim())
                .email(normalizedEmail)
                .passwordHash(passwordEncoder.encode(requestDTO.getPassword()))
                .role(requestDTO.getRole())
                .authProvider(AuthProvider.LOCAL)
                .status(UserStatus.ACTIVE)
                .build();

        try {
            return userMapper.toDto(userRepository.saveAndFlush(user));
        } catch (DataIntegrityViolationException ex) {
            throw new BusinessException(ErrorCode.USER_ALREADY_EXISTS);
        }
    }

    private Specification<User> buildUserSearchSpecification(String keyword, Role role, UserStatus status, AuthProvider authProvider) {
        return (root, query, criteriaBuilder) -> {
            List<Predicate> predicates = new ArrayList<>();

            if (keyword != null && !keyword.isBlank()) {
                String normalizedKeyword = keyword.trim().toLowerCase();
                String likeKeyword = "%" + normalizedKeyword + "%";
                List<Predicate> keywordPredicates = new ArrayList<>();
                keywordPredicates.add(criteriaBuilder.like(criteriaBuilder.lower(root.get("fullName")), likeKeyword));
                keywordPredicates.add(criteriaBuilder.like(criteriaBuilder.lower(root.get("email")), likeKeyword));
                try {
                    Integer id = Integer.valueOf(normalizedKeyword);
                    keywordPredicates.add(criteriaBuilder.equal(root.get("id"), id));
                } catch (NumberFormatException ignored) {
                    // Non-numeric keyword only searches text fields.
                }
                predicates.add(criteriaBuilder.or(keywordPredicates.toArray(new Predicate[0])));
            }

            if (role != null) {
                predicates.add(criteriaBuilder.equal(root.get("role"), role));
            }
            if (status != null) {
                predicates.add(criteriaBuilder.equal(root.get("status"), status));
            }
            if (authProvider != null) {
                predicates.add(criteriaBuilder.equal(root.get("authProvider"), authProvider));
            }

            return criteriaBuilder.and(predicates.toArray(new Predicate[0]));
        };
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
    @Transactional
    public UserResponseDTO updateUserRole(Integer targetUserId, Role role, Integer currentAdminId) {
        if (role == null) {
            throw new BusinessException(ErrorCode.VALIDATION_ERROR, "Role is required");
        }
        User user = userRepository.findById(targetUserId)
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));
        if (user.getStatus() == UserStatus.DELETED) {
            throw new BusinessException(ErrorCode.USER_ALREADY_DELETED);
        }
        if (targetUserId != null && targetUserId.equals(currentAdminId)
                && user.getRole() == Role.ADMIN && role != Role.ADMIN) {
            throw new BusinessException(ErrorCode.CANNOT_MODIFY_OWN_ACCOUNT);
        }
        if (user.getRole() == Role.ADMIN && role != Role.ADMIN) {
            ensureNotLastActiveAdmin(user);
        }

        user.setRole(role);
        return userMapper.toDto(userRepository.save(user));
    }

    @Override
    @Transactional
    public UserResponseDTO blockUser(Integer targetUserId, Integer currentAdminId) {
        User user = getTargetUserForAdminAction(targetUserId, currentAdminId);
        if (user.getStatus() == UserStatus.DELETED) {
            throw new BusinessException(ErrorCode.USER_ALREADY_DELETED);
        }
        if (user.getStatus() == UserStatus.BLOCKED) {
            throw new BusinessException(ErrorCode.USER_ALREADY_BLOCKED);
        }
        ensureNotLastActiveAdmin(user);

        user.setStatus(UserStatus.BLOCKED);
        return userMapper.toDto(userRepository.save(user));
    }

    @Override
    @Transactional
    public UserResponseDTO unblockUser(Integer targetUserId, Integer currentAdminId) {
        User user = getTargetUserForAdminAction(targetUserId, currentAdminId);
        if (user.getStatus() == UserStatus.DELETED) {
            throw new BusinessException(ErrorCode.USER_ALREADY_DELETED);
        }
        if (user.getStatus() == UserStatus.ACTIVE) {
            throw new BusinessException(ErrorCode.USER_NOT_BLOCKED);
        }

        user.setStatus(UserStatus.ACTIVE);
        return userMapper.toDto(userRepository.save(user));
    }

    private User getTargetUserForAdminAction(Integer targetUserId, Integer currentAdminId) {
        if (targetUserId != null && targetUserId.equals(currentAdminId)) {
            throw new BusinessException(ErrorCode.CANNOT_MODIFY_OWN_ACCOUNT);
        }
        return userRepository.findById(targetUserId)
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));
    }

    @Override
    @Transactional
    public UserResponseDTO softDeleteUser(Integer targetUserId, Integer currentAdminId) {
        if (targetUserId != null && targetUserId.equals(currentAdminId)) {
            throw new BusinessException(ErrorCode.CANNOT_DELETE_OWN_ACCOUNT);
        }

        User user = userRepository.findById(targetUserId)
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));

        if (user.getStatus() != UserStatus.DELETED) {
            ensureNotLastActiveAdmin(user);
            user.setStatus(UserStatus.DELETED);
            user = userRepository.save(user);
        }

        return userMapper.toDto(user);
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
                && userRepository.existsByAuthProviderAndEmailIgnoreCaseAndIdNot(
                        user.getAuthProvider(), normalizedEmail, user.getId())) {
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

    private void ensureNotLastActiveAdmin(User user) {
        if (user.getRole() == Role.ADMIN && user.getStatus() == UserStatus.ACTIVE
                && userRepository.countByRoleAndStatus(Role.ADMIN, UserStatus.ACTIVE) <= 1) {
            throw new BusinessException(ErrorCode.CANNOT_REMOVE_LAST_ADMIN);
        }
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
