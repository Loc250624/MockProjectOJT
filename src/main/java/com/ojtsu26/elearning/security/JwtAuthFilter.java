package com.ojtsu26.elearning.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

@Slf4j
@Component
@RequiredArgsConstructor
public class JwtAuthFilter extends OncePerRequestFilter {

    private final JwtUtils jwtUtils;
    private final CustomUserDetailsService userDetailsService;

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        try {
            String jwt = parseJwt(request);
            if (jwt == null) {
                log.debug("JWT not found: method={}, uri={}, hasAuthorizationHeader={}, hasJwtCookie={}",
                        request.getMethod(), request.getRequestURI(),
                        request.getHeader("Authorization") != null, hasJwtCookie(request));
            } else if (jwtUtils.validateJwtToken(jwt)) {
                String email = jwtUtils.getEmailFromJwtToken(jwt);

                UserDetails userDetails = userDetailsService.loadUserByUsername(email);
                if (!isUsableAccount(userDetails)) {
                    SecurityContextHolder.clearContext();
                    log.debug("JWT account rejected: method={}, uri={}, username={}",
                            request.getMethod(), request.getRequestURI(), email);
                    filterChain.doFilter(request, response);
                    return;
                }

                UsernamePasswordAuthenticationToken authentication = new UsernamePasswordAuthenticationToken(
                        userDetails, null, userDetails.getAuthorities());
                authentication.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));

                SecurityContextHolder.getContext().setAuthentication(authentication);
                log.debug("JWT authentication set: method={}, uri={}, username={}, authorities={}",
                        request.getMethod(), request.getRequestURI(), email, userDetails.getAuthorities());
            } else {
                log.debug("JWT validation failed: method={}, uri={}", request.getMethod(), request.getRequestURI());
            }
        } catch (Exception e) {
            log.warn("Cannot set user authentication: method={}, uri={}, exceptionType={}, message={}",
                    request.getMethod(), request.getRequestURI(), e.getClass().getSimpleName(), e.getMessage());
        }

        filterChain.doFilter(request, response);
    }

    private boolean isUsableAccount(UserDetails userDetails) {
        return userDetails.isEnabled()
                && userDetails.isAccountNonLocked()
                && userDetails.isAccountNonExpired()
                && userDetails.isCredentialsNonExpired();
    }

    private String parseJwt(HttpServletRequest request) {
        // First check Cookie for SSR Thymeleaf compatibility
        if (request.getCookies() != null) {
            for (Cookie cookie : request.getCookies()) {
                if ("jwt_token".equals(cookie.getName())) {
                    return cookie.getValue();
                }
            }
        }
        
        // Fallback to Header for pure API clients
        String headerAuth = request.getHeader("Authorization");
        if (StringUtils.hasText(headerAuth) && headerAuth.startsWith("Bearer ")) {
            return headerAuth.substring(7);
        }

        return null;
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
