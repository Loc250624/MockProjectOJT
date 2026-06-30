package com.ojtsu26.elearning.service;

import com.ojtsu26.elearning.dto.request.LoginRequestDTO;
import com.ojtsu26.elearning.dto.request.RegisterRequestDTO;
import com.ojtsu26.elearning.dto.response.AuthResponseDTO;

public interface AuthService {
    AuthResponseDTO register(RegisterRequestDTO request);
    AuthResponseDTO login(LoginRequestDTO request);
}
