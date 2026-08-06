package com.ojtsu26.elearning.controller.api;

import com.ojtsu26.elearning.common.ApiResponse;
import com.ojtsu26.elearning.dto.request.LoginRequestDTO;
import com.ojtsu26.elearning.dto.request.OAuth2CompleteRegistrationRequestDTO;
import com.ojtsu26.elearning.dto.request.RegisterRequestDTO;
import com.ojtsu26.elearning.dto.response.AuthResponseDTO;
import com.ojtsu26.elearning.exception.BusinessException;
import com.ojtsu26.elearning.exception.ErrorCode;
import com.ojtsu26.elearning.model.entity.User;
import com.ojtsu26.elearning.security.JwtCookieService;
import com.ojtsu26.elearning.security.OAuth2AccountService;
import com.ojtsu26.elearning.security.OAuth2LoginSuccessHandler;
import com.ojtsu26.elearning.service.AuthService;
import org.springframework.security.web.csrf.CsrfToken;
import jakarta.servlet.http.HttpSession;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthRestController {

    private final AuthService authService;
    private final JwtCookieService jwtCookieService;
    private final OAuth2AccountService oAuth2AccountService;

    @GetMapping("/csrf")
    public CsrfResponse csrf(CsrfToken csrfToken) {
        return new CsrfResponse(
                csrfToken.getHeaderName(),
                csrfToken.getParameterName(),
                csrfToken.getToken()
        );
    }

    @PostMapping("/register")
    public ResponseEntity<ApiResponse<AuthResponseDTO>> register(@Valid @RequestBody RegisterRequestDTO request, HttpServletResponse response) {
        AuthResponseDTO authResponse = authService.register(request);
        jwtCookieService.addJwtCookieFromToken(response, authResponse.getToken());
        // Do not expose token in JSON body since we use HTTP-Only cookie for SSR support
        authResponse.setToken(null);
        return ResponseEntity.ok(ApiResponse.success(authResponse, "User registered successfully"));
    }

    @PostMapping("/login")
    public ResponseEntity<ApiResponse<AuthResponseDTO>> login(@Valid @RequestBody LoginRequestDTO request, HttpServletResponse response) {
        AuthResponseDTO authResponse = authService.login(request);
        jwtCookieService.addJwtCookieFromToken(response, authResponse.getToken());
        authResponse.setToken(null);
        return ResponseEntity.ok(ApiResponse.success(authResponse, "Login successful"));
    }

    @PostMapping("/oauth2/complete")
    public ResponseEntity<ApiResponse<AuthResponseDTO>> completeOAuthRegistration(
            @Valid @RequestBody OAuth2CompleteRegistrationRequestDTO request,
            HttpSession session,
            HttpServletResponse response) {

        String token = pendingToken(session);
        if (token == null) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "OAuth registration has expired. Please sign in again.");
        }

        User user = oAuth2AccountService.completePendingRegistration(token, request);
        session.removeAttribute(OAuth2LoginSuccessHandler.PENDING_OAUTH_SESSION_ATTRIBUTE);
        jwtCookieService.addJwtCookie(response, user.getId());

        AuthResponseDTO authResponse = AuthResponseDTO.builder()
                .id(user.getId())
                .email(user.getEmail())
                .fullName(user.getFullName())
                .role(user.getRole().name())
                .redirectUrl(resolveRedirectUrl(user))
                .build();
        return ResponseEntity.ok(ApiResponse.success(authResponse, "OAuth registration completed"));
    }

    private String pendingToken(HttpSession session) {
        Object value = session.getAttribute(OAuth2LoginSuccessHandler.PENDING_OAUTH_SESSION_ATTRIBUTE);
        return value instanceof String token && !token.isBlank() ? token : null;
    }

    private String resolveRedirectUrl(User user) {
        return switch (user.getRole()) {
            case STUDENT -> "/student/dashboard";
            case TEACHER -> "/teacher/dashboard";
            case ADMIN -> "/admin/dashboard";
        };
    }

    public record CsrfResponse(String headerName, String parameterName, String token) {
    }
}
