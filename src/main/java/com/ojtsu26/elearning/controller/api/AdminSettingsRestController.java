package com.ojtsu26.elearning.controller.api;

import com.ojtsu26.elearning.common.ApiResponse;
import com.ojtsu26.elearning.dto.request.SystemSettingUpdateRequestDTO;
import com.ojtsu26.elearning.dto.request.SystemSettingsBulkUpdateRequestDTO;
import com.ojtsu26.elearning.dto.response.SystemSettingResponseDTO;
import com.ojtsu26.elearning.security.CustomUserDetails;
import com.ojtsu26.elearning.service.SystemSettingService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/admin/settings")
@RequiredArgsConstructor
public class AdminSettingsRestController {

    private final SystemSettingService systemSettingService;

    @GetMapping
    public ResponseEntity<ApiResponse<List<SystemSettingResponseDTO>>> getSettings() {
        return ResponseEntity.ok(ApiResponse.success(systemSettingService.findAll()));
    }

    @PutMapping("/{key}")
    public ResponseEntity<ApiResponse<SystemSettingResponseDTO>> updateSetting(
            @PathVariable String key,
            @Valid @RequestBody SystemSettingUpdateRequestDTO request,
            @AuthenticationPrincipal CustomUserDetails currentUser) {
        SystemSettingResponseDTO setting = systemSettingService.update(
                key,
                request.getValue(),
                currentUser.getUser().getId());
        return ResponseEntity.ok(ApiResponse.success(setting, "Setting updated successfully"));
    }

    @PostMapping("/bulk")
    public ResponseEntity<ApiResponse<List<SystemSettingResponseDTO>>> updateSettings(
            @Valid @RequestBody SystemSettingsBulkUpdateRequestDTO request,
            @AuthenticationPrincipal CustomUserDetails currentUser) {
        List<SystemSettingResponseDTO> settings = systemSettingService.updateBulk(
                request.getValues(),
                currentUser.getUser().getId());
        return ResponseEntity.ok(ApiResponse.success(settings, "Settings updated successfully"));
    }
}
