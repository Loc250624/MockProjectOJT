package com.ojtsu26.elearning.web;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.ojtsu26.elearning.service.SystemSettingService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.util.AntPathMatcher;
import org.springframework.web.servlet.HandlerInterceptor;

import java.io.IOException;
import java.util.List;
import java.util.Map;

@Component
@RequiredArgsConstructor
public class MaintenanceModeInterceptor implements HandlerInterceptor {

    private static final String ADMIN_AUTHORITY = "ROLE_ADMIN";
    private static final String MESSAGE = "LumiNa is temporarily unavailable due to maintenance.";

    private static final List<String> WHITELIST = List.of(
            "/maintenance",
            "/error",
            "/favicon.ico",
            "/css/**",
            "/js/**",
            "/images/**",
            "/img/**",
            "/fonts/**",
            "/uploads/**",
            "/webjars/**",
            "/auth/login",
            "/auth/logout",
            "/auth/forgot-password",
            "/auth/reset-password",
            "/auth/oauth2/complete",
            "/api/auth/csrf",
            "/api/auth/login",
            "/api/auth/oauth2/complete",
            "/oauth2/**",
            "/login/oauth2/**",
            "/api/payment/vnpay-ipn",
            "/api/payment/webhook",
            "/student/payment-result"
    );

    private final SystemSettingService systemSettingService;
    private final ObjectMapper objectMapper;
    private final AntPathMatcher pathMatcher = new AntPathMatcher();

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler)
            throws IOException {
        if (!systemSettingService.isMaintenanceModeEnabled()) {
            return true;
        }
        if (HttpMethod.OPTIONS.matches(request.getMethod())) {
            return true;
        }
        if (isWhitelisted(requestPath(request))) {
            return true;
        }
        if (authenticatedUserIsAdmin()) {
            return true;
        }

        if (isApiRequest(request)) {
            writeMaintenanceJson(response);
        } else {
            response.sendRedirect(request.getContextPath() + "/maintenance");
        }
        return false;
    }

    private String requestPath(HttpServletRequest request) {
        String path = request.getRequestURI();
        String contextPath = request.getContextPath();
        if (contextPath != null && !contextPath.isBlank() && path.startsWith(contextPath)) {
            path = path.substring(contextPath.length());
        }
        return path.isBlank() ? "/" : path;
    }

    private boolean isWhitelisted(String path) {
        return WHITELIST.stream().anyMatch(pattern -> pathMatcher.match(pattern, path));
    }

    private boolean authenticatedUserIsAdmin() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        return authentication != null
                && authentication.isAuthenticated()
                && authentication.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .anyMatch(ADMIN_AUTHORITY::equals);
    }

    private boolean isApiRequest(HttpServletRequest request) {
        String path = requestPath(request);
        String requestedWith = request.getHeader("X-Requested-With");
        String accept = request.getHeader(HttpHeaders.ACCEPT);
        return path.startsWith("/api/")
                || "XMLHttpRequest".equalsIgnoreCase(requestedWith)
                || (accept != null && accept.contains(MediaType.APPLICATION_JSON_VALUE));
    }

    private void writeMaintenanceJson(HttpServletResponse response) throws IOException {
        response.setStatus(HttpStatus.SERVICE_UNAVAILABLE.value());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        objectMapper.writeValue(response.getWriter(), Map.of(
                "code", HttpStatus.SERVICE_UNAVAILABLE.value(),
                "error", "MAINTENANCE_MODE",
                "message", MESSAGE
        ));
    }
}
