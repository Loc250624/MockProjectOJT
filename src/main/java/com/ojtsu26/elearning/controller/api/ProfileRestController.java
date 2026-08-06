package com.ojtsu26.elearning.controller.api;

import com.ojtsu26.elearning.common.ApiResponse;
import com.ojtsu26.elearning.dto.request.AvatarUrlRequestDTO;
import com.ojtsu26.elearning.dto.request.ChangePasswordRequestDTO;
import com.ojtsu26.elearning.dto.request.UpdateProfileRequestDTO;
import com.ojtsu26.elearning.dto.response.ProfileOverviewResponseDTO;
import com.ojtsu26.elearning.dto.response.UserResponseDTO;
import com.ojtsu26.elearning.model.enums.AuthProvider;
import com.ojtsu26.elearning.security.CustomUserDetails;
import com.ojtsu26.elearning.security.JwtCookieService;
import com.ojtsu26.elearning.service.ProfileService;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.util.Objects;

@RestController
@RequestMapping("/api/profile")
@RequiredArgsConstructor
public class ProfileRestController {

    private final ProfileService profileService;
    private final JwtCookieService jwtCookieService;

    @GetMapping
    public ResponseEntity<ApiResponse<UserResponseDTO>> getProfile() {
        return ResponseEntity.ok(ApiResponse.success(profileService.getCurrentProfile()));
    }

    @GetMapping("/overview")
    public ResponseEntity<ApiResponse<ProfileOverviewResponseDTO>> getProfileOverview() {
        return ResponseEntity.ok(ApiResponse.success(profileService.getCurrentProfileOverview()));
    }

    @PutMapping
    public ResponseEntity<ApiResponse<UserResponseDTO>> updateProfile(
            @AuthenticationPrincipal CustomUserDetails currentUser,
            @Valid @RequestBody UpdateProfileRequestDTO request,
            HttpServletResponse response) {
        String previousEmail = currentUser.getUsername();
        AuthProvider authProvider = currentUser.getUser().getAuthProvider();
        UserResponseDTO updatedProfile = profileService.updateCurrentProfile(request);

        if (authProvider == AuthProvider.LOCAL && !Objects.equals(previousEmail, updatedProfile.getEmail())) {
            jwtCookieService.addJwtCookie(response, updatedProfile.getId());
        }

        return ResponseEntity.ok(ApiResponse.success(updatedProfile, "Profile updated successfully"));
    }

    @PutMapping("/password")
    public ResponseEntity<ApiResponse<Void>> changePassword(
            @Valid @RequestBody ChangePasswordRequestDTO request) {
        profileService.changeCurrentPassword(request);
        return ResponseEntity.ok(ApiResponse.success(null, "Password changed successfully"));
    }

    @PostMapping(value = "/avatar/upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ApiResponse<UserResponseDTO>> uploadAvatar(@RequestParam("file") MultipartFile file) {
        return ResponseEntity.ok(ApiResponse.success(profileService.uploadCurrentAvatar(file), "Avatar updated successfully"));
    }

    @PostMapping("/avatar/url")
    public ResponseEntity<ApiResponse<UserResponseDTO>> useAvatarUrl(@Valid @RequestBody AvatarUrlRequestDTO request) {
        return ResponseEntity.ok(ApiResponse.success(profileService.useCurrentAvatarUrl(request.getImageUrl()), "Avatar updated successfully"));
    }

    @DeleteMapping("/avatar")
    public ResponseEntity<ApiResponse<UserResponseDTO>> removeAvatar() {
        return ResponseEntity.ok(ApiResponse.success(profileService.removeCurrentAvatar(), "Avatar removed successfully"));
    }
}
