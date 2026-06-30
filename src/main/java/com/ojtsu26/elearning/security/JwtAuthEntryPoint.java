package com.ojtsu26.elearning.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.ojtsu26.elearning.common.ApiResponse;
import com.ojtsu26.elearning.exception.ErrorCode;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.stereotype.Component;

import java.io.IOException;

@Slf4j
@Component
public class JwtAuthEntryPoint implements AuthenticationEntryPoint {

    @Override
    public void commence(HttpServletRequest request, HttpServletResponse response,
                         AuthenticationException authException) throws IOException, ServletException {
        log.warn("Unauthorized error: method={}, uri={}, hasAuthorizationHeader={}, hasJwtCookie={}, message={}",
                request.getMethod(),
                request.getRequestURI(),
                request.getHeader("Authorization") != null,
                hasJwtCookie(request),
                authException.getMessage());
        
        if (request.getRequestURI().startsWith("/api/")) {
            response.setContentType("application/json");
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            ApiResponse<Void> apiResponse = ApiResponse.error(ErrorCode.UNAUTHORIZED.getCode(), ErrorCode.UNAUTHORIZED.getMessage());
            ObjectMapper mapper = new ObjectMapper();
            response.getOutputStream().println(mapper.writeValueAsString(apiResponse));
        } else {
            response.sendRedirect("/auth/login");
        }
    }

    private boolean hasJwtCookie(HttpServletRequest request) {
        if (request.getCookies() == null) {
            return false;
        }

        for (Cookie cookie : request.getCookies()) {
            if ("jwt_token".equals(cookie.getName())) {
                return true;
            }
        }
        return false;
    }
}
