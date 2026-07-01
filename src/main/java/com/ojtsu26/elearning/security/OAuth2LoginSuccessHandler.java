package com.ojtsu26.elearning.security;

import com.ojtsu26.elearning.model.entity.User;
import com.ojtsu26.elearning.model.enums.AuthProvider;
import com.ojtsu26.elearning.model.enums.Role;
import com.ojtsu26.elearning.model.enums.UserStatus;
import com.ojtsu26.elearning.repository.UserRepository;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.time.Duration;
import java.util.Optional;

@Slf4j
@Component
@RequiredArgsConstructor
public class OAuth2LoginSuccessHandler implements AuthenticationSuccessHandler {

    private final UserRepository userRepository;
    private final JwtUtils jwtUtils;

    @Override
    public void onAuthenticationSuccess(HttpServletRequest request, HttpServletResponse response, Authentication authentication) throws IOException, ServletException {
        OAuth2AuthenticationToken authToken = (OAuth2AuthenticationToken) authentication;
        OAuth2User oAuth2User = authToken.getPrincipal();
        
        String email = oAuth2User.getAttribute("email");
        String name = oAuth2User.getAttribute("name");
        
        String registrationId = authToken.getAuthorizedClientRegistrationId().toUpperCase();
        AuthProvider authProvider = AuthProvider.valueOf(registrationId);

        Optional<User> userOptional = userRepository.findByEmail(email);
        User user;
        if (userOptional.isPresent()) {
            user = userOptional.get();
            // Automatically link accounts if they log in via OAuth2 but registered locally
            if (user.getAuthProvider() == AuthProvider.LOCAL) {
                user.setAuthProvider(authProvider);
                userRepository.save(user);
            }
        } else {
            user = User.builder()
                    .email(email)
                    .fullName(name)
                    .role(Role.STUDENT)
                    .authProvider(authProvider)
                    .status(UserStatus.ACTIVE)
                    .build();
            userRepository.save(user);
            log.info("New user registered via OAuth2: {}", email);
        }

        String jwt = jwtUtils.generateTokenFromEmail(user.getEmail());

        ResponseCookie cookie = ResponseCookie.from("jwt_token", jwt)
                .httpOnly(true)
                .path("/")
                .maxAge(Duration.ofDays(1))
                .sameSite("Lax")
                .build();
        response.addHeader(HttpHeaders.SET_COOKIE, cookie.toString());

        if (user.getRole() == Role.ADMIN) {
            response.sendRedirect("/admin/dashboard");
        } else if (user.getRole() == Role.TEACHER) {
            response.sendRedirect("/teacher/dashboard");
        } else {
            response.sendRedirect("/student/dashboard");
        }
    }
}
