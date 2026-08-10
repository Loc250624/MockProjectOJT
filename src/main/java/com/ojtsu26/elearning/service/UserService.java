package com.ojtsu26.elearning.service;

import com.ojtsu26.elearning.dto.request.AdminCreateUserRequestDTO;
import com.ojtsu26.elearning.dto.request.UserRequestDTO;
import com.ojtsu26.elearning.dto.request.UpdateProfileRequestDTO;
import com.ojtsu26.elearning.dto.response.UserResponseDTO;
import com.ojtsu26.elearning.model.enums.AuthProvider;
import com.ojtsu26.elearning.model.enums.Role;
import com.ojtsu26.elearning.model.enums.UserStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;

import java.util.List;

public interface UserService {
    List<UserResponseDTO> findAll();
    Page<UserResponseDTO> searchUsers(String keyword, Role role, UserStatus status, AuthProvider authProvider, Pageable pageable);
    List<UserResponseDTO> findUsersForExport(String keyword, Role role, UserStatus status, AuthProvider authProvider, Sort sort);
    UserResponseDTO createByAdmin(AdminCreateUserRequestDTO requestDTO);
    UserResponseDTO findById(Integer id);
    UserResponseDTO create(UserRequestDTO requestDTO);
    UserResponseDTO update(Integer id, UserRequestDTO requestDTO);
    void delete(Integer id);
    UserResponseDTO getCurrentProfile(Integer currentUserId);
    UserResponseDTO updateCurrentProfile(Integer currentUserId, UpdateProfileRequestDTO request);
    UserResponseDTO updateCurrentAvatar(Integer currentUserId, String avatarUrl);
    UserResponseDTO updateUserRole(Integer targetUserId, Role role, Integer currentAdminId);
    UserResponseDTO blockUser(Integer targetUserId, Integer currentAdminId);
    UserResponseDTO unblockUser(Integer targetUserId, Integer currentAdminId);
    UserResponseDTO softDeleteUser(Integer targetUserId, Integer currentAdminId);
}
