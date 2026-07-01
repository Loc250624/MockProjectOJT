package com.ojtsu26.elearning.service.impl;

import com.ojtsu26.elearning.model.entity.User;
import com.ojtsu26.elearning.dto.request.UserRequestDTO;
import com.ojtsu26.elearning.dto.response.UserResponseDTO;
import com.ojtsu26.elearning.mapper.UserMapper;
import com.ojtsu26.elearning.model.enums.AuthProvider;
import com.ojtsu26.elearning.model.enums.Role;
import com.ojtsu26.elearning.model.enums.UserStatus;
import com.ojtsu26.elearning.repository.UserRepository;
import com.ojtsu26.elearning.service.UserService;
import jakarta.persistence.criteria.Predicate;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
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
    public Page<UserResponseDTO> searchUsers(String keyword, Role role, UserStatus status, AuthProvider authProvider, Pageable pageable) {
        Pageable effectivePageable = pageable.getSort().isSorted()
                ? pageable
                : PageRequest.of(pageable.getPageNumber(), pageable.getPageSize(), Sort.by(Sort.Direction.DESC, "createdAt"));
        return userRepository.findAll(buildUserSearchSpecification(keyword, role, status, authProvider), effectivePageable)
                .map(userMapper::toDto);
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
                .orElseThrow(() -> new RuntimeException("User not found"));
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
}
