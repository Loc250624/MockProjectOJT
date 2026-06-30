package com.ojtsu26.elearning.controller.api;

import com.ojtsu26.elearning.common.ApiResponse;
import com.ojtsu26.elearning.dto.request.LoginRequestDTO;
import com.ojtsu26.elearning.dto.request.RegisterRequestDTO;
import com.ojtsu26.elearning.dto.response.AuthResponseDTO;
import com.ojtsu26.elearning.service.AuthService;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.Duration;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthRestController {

    private final AuthService authService;

    @PostMapping("/register")
    public ResponseEntity<ApiResponse<AuthResponseDTO>> register(@Valid @RequestBody RegisterRequestDTO request, HttpServletResponse response) {
        AuthResponseDTO authResponse = authService.register(request);
        setJwtCookie(response, authResponse.getToken());
        // Do not expose token in JSON body since we use HTTP-Only cookie for SSR support
        authResponse.setToken(null);
        return ResponseEntity.ok(ApiResponse.success(authResponse, "User registered successfully"));
    }

    @PostMapping("/login")
    public ResponseEntity<ApiResponse<AuthResponseDTO>> login(@Valid @RequestBody LoginRequestDTO request, HttpServletResponse response) {
        AuthResponseDTO authResponse = authService.login(request);
        setJwtCookie(response, authResponse.getToken());
        authResponse.setToken(null);
        return ResponseEntity.ok(ApiResponse.success(authResponse, "Login successful"));
    }

    @PostMapping("/logout")
    public ResponseEntity<ApiResponse<Void>> logout(HttpServletResponse response) {
        ResponseCookie cookie = ResponseCookie.from("jwt_token", "")
                .httpOnly(true)
                .path("/")
                .maxAge(Duration.ZERO)
                .sameSite("Lax")
                .build();
        response.addHeader(HttpHeaders.SET_COOKIE, cookie.toString());
        return ResponseEntity.ok(ApiResponse.success(null, "Logout successful"));
    }

    private void setJwtCookie(HttpServletResponse response, String token) {
        ResponseCookie cookie = ResponseCookie.from("jwt_token", token)
                .httpOnly(true)
                .path("/")
                .maxAge(Duration.ofDays(1))
                .sameSite("Lax")
                .build();
        response.addHeader(HttpHeaders.SET_COOKIE, cookie.toString());
    }
}
