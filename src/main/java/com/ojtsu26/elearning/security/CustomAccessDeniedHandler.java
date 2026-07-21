package com.ojtsu26.elearning.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.ojtsu26.elearning.common.ApiResponse;
import com.ojtsu26.elearning.exception.ErrorCode;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.security.web.csrf.InvalidCsrfTokenException;
import org.springframework.security.web.csrf.MissingCsrfTokenException;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;

@Slf4j
@Component
@RequiredArgsConstructor
public class CustomAccessDeniedHandler implements AccessDeniedHandler {

    private final ObjectMapper objectMapper;

    @Override
    public void handle(HttpServletRequest request, HttpServletResponse response, AccessDeniedException accessDeniedException) throws IOException, ServletException {
        String code = csrfCode(accessDeniedException);
        log.warn("Access denied: code={}, method={}, path={}", code, request.getMethod(), request.getRequestURI());

        if (code != null) {
            if (isJsonRequest(request)) {
                response.setContentType(MediaType.APPLICATION_JSON_VALUE);
                response.setStatus(HttpServletResponse.SC_FORBIDDEN);
                objectMapper.writeValue(response.getOutputStream(), csrfBody(code, request.getRequestURI()));
            } else {
                response.sendRedirect("/");
            }
        } else if (isJsonRequest(request)) {
            response.setContentType(MediaType.APPLICATION_JSON_VALUE);
            response.setStatus(HttpServletResponse.SC_FORBIDDEN);
            ApiResponse<Void> apiResponse = ApiResponse.error(ErrorCode.ACCESS_DENIED.getCode(), ErrorCode.ACCESS_DENIED.getMessage());
            objectMapper.writeValue(response.getOutputStream(), apiResponse);
        } else {
            response.sendRedirect("/");
        }
    }

    private boolean isJsonRequest(HttpServletRequest request) {
        String uri = request.getRequestURI();
        if (uri == null) {
            return false;
        }
        if (uri.equals("/student/feedback") || uri.startsWith("/auth/") || uri.equals("/login") || uri.equals("/register")) {
            return false;
        }
        if (uri.startsWith("/api/") || uri.contains("/courses/") || uri.contains("/lessons/")) {
            return true;
        }
        String accept = request.getHeader("Accept");
        if (accept != null && accept.contains("application/json")) {
            return true;
        }
        return false;
    }

    private String csrfCode(AccessDeniedException exception) {
        if (exception instanceof MissingCsrfTokenException) {
            return "CSRF_TOKEN_MISSING";
        }
        if (exception instanceof InvalidCsrfTokenException) {
            return "CSRF_TOKEN_INVALID";
        }
        return null;
    }

    private Map<String, Object> csrfBody(String code, String path) {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("timestamp", Instant.now().toString());
        body.put("status", HttpServletResponse.SC_FORBIDDEN);
        body.put("code", code);
        body.put("message", "CSRF token is missing or invalid. Refresh the token and retry once.");
        body.put("path", path);
        return body;
    }
}
