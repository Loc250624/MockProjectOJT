package com.ojtsu26.elearning.service;

import com.ojtsu26.elearning.dto.response.AdminPasswordResetLinkResponseDTO;

public interface PasswordResetService {
    AdminPasswordResetLinkResponseDTO createAdminResetLink(Integer userId);
    void resetPassword(String token, String password, String confirmPassword);
}
