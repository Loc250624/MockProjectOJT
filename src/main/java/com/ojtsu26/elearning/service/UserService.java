package com.ojtsu26.elearning.service;

import com.ojtsu26.elearning.dto.request.UserRequestDTO;
import com.ojtsu26.elearning.dto.request.UpdateProfileRequestDTO;
import com.ojtsu26.elearning.dto.response.UserResponseDTO;
import java.util.List;

public interface UserService {
    List<UserResponseDTO> findAll();
    UserResponseDTO findById(Integer id);
    UserResponseDTO create(UserRequestDTO requestDTO);
    UserResponseDTO update(Integer id, UserRequestDTO requestDTO);
    void delete(Integer id);
    UserResponseDTO getCurrentProfile(Integer currentUserId);
    UserResponseDTO updateCurrentProfile(Integer currentUserId, UpdateProfileRequestDTO request);
    UserResponseDTO updateCurrentAvatar(Integer currentUserId, String avatarUrl);
}
