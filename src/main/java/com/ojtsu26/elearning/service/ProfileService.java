package com.ojtsu26.elearning.service;

import com.ojtsu26.elearning.dto.request.ChangePasswordRequestDTO;
import com.ojtsu26.elearning.dto.request.UpdateProfileRequestDTO;
import com.ojtsu26.elearning.dto.response.ProfileOverviewResponseDTO;
import com.ojtsu26.elearning.dto.response.UserResponseDTO;
import org.springframework.web.multipart.MultipartFile;

public interface ProfileService {
    UserResponseDTO getCurrentProfile();
    ProfileOverviewResponseDTO getCurrentProfileOverview();
    UserResponseDTO updateCurrentProfile(UpdateProfileRequestDTO request);
    void changeCurrentPassword(ChangePasswordRequestDTO request);
    UserResponseDTO uploadCurrentAvatar(MultipartFile file);
    UserResponseDTO useCurrentAvatarUrl(String imageUrl);
    UserResponseDTO removeCurrentAvatar();
}
