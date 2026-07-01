package com.ojtsu26.elearning.service;

import com.ojtsu26.elearning.dto.request.UserRequestDTO;
import com.ojtsu26.elearning.dto.response.UserResponseDTO;
import com.ojtsu26.elearning.model.enums.AuthProvider;
import com.ojtsu26.elearning.model.enums.Role;
import com.ojtsu26.elearning.model.enums.UserStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;

public interface UserService {
    List<UserResponseDTO> findAll();
    Page<UserResponseDTO> searchUsers(String keyword, Role role, UserStatus status, AuthProvider authProvider, Pageable pageable);
    UserResponseDTO findById(Integer id);
    UserResponseDTO create(UserRequestDTO requestDTO);
    UserResponseDTO update(Integer id, UserRequestDTO requestDTO);
    UserResponseDTO blockUser(Integer targetUserId, Integer currentAdminId);
    UserResponseDTO unblockUser(Integer targetUserId, Integer currentAdminId);
    UserResponseDTO softDeleteUser(Integer targetUserId, Integer currentAdminId);
}
