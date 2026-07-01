package com.ojtsu26.elearning.controller.api;

import com.ojtsu26.elearning.common.ApiResponse;
import com.ojtsu26.elearning.dto.response.ProfileOverviewResponseDTO;
import com.ojtsu26.elearning.service.ProfileService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/admin/profile")
@RequiredArgsConstructor
public class AdminProfileRestController {

    private final ProfileService profileService;

    @GetMapping("/overview")
    public ResponseEntity<ApiResponse<ProfileOverviewResponseDTO>> overview() {
        return ResponseEntity.ok(ApiResponse.success(profileService.getCurrentProfileOverview()));
    }
}
