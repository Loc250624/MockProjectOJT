package com.ojtsu26.elearning.config;

import com.ojtsu26.elearning.security.CustomAccessDeniedHandler;
import com.ojtsu26.elearning.security.CustomUserDetailsService;
import com.ojtsu26.elearning.security.JwtAuthEntryPoint;
import com.ojtsu26.elearning.security.JwtAuthFilter;
import com.ojtsu26.elearning.security.OAuth2LoginSuccessHandler;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
import org.springframework.http.ResponseCookie;
import org.springframework.http.HttpHeaders;
import java.time.Duration;

import java.util.List;

@Configuration
@EnableWebSecurity
@RequiredArgsConstructor
@Slf4j
public class SecurityConfig {

    private final JwtAuthFilter jwtAuthFilter;
    private final CustomUserDetailsService userDetailsService;
    private final JwtAuthEntryPoint unauthorizedHandler;
    private final CustomAccessDeniedHandler accessDeniedHandler;
    private final OAuth2LoginSuccessHandler oAuth2LoginSuccessHandler;

    @Bean
    public DaoAuthenticationProvider authenticationProvider() {
        DaoAuthenticationProvider authProvider = new DaoAuthenticationProvider();
        authProvider.setUserDetailsService(userDetailsService);
        authProvider.setPasswordEncoder(passwordEncoder());
        return authProvider;
    }

    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration authConfig) throws Exception {
        return authConfig.getAuthenticationManager();
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new PasswordEncoder() {
            private final BCryptPasswordEncoder bcrypt = new BCryptPasswordEncoder();

            @Override
            public String encode(CharSequence rawPassword) {
                return bcrypt.encode(rawPassword);
            }

            @Override
            public boolean matches(CharSequence rawPassword, String encodedPassword) {
                if (encodedPassword == null) return false;
                // If it looks like a BCrypt hash, verify with BCrypt
                if (encodedPassword.startsWith("$2a$")) {
                    return bcrypt.matches(rawPassword, encodedPassword);
                }
                // Fallback for old SQL plain-text passwords
                return encodedPassword.equals(rawPassword.toString());
            }
        };
    }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
            .csrf(AbstractHttpConfigurer::disable)
            .cors(cors -> cors.configurationSource(corsConfigurationSource()))
            .exceptionHandling(exception -> exception
                    .authenticationEntryPoint(unauthorizedHandler)
                    .accessDeniedHandler(accessDeniedHandler)
            )
            .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.IF_REQUIRED))
            .authorizeHttpRequests(auth -> auth
                .requestMatchers(HttpMethod.OPTIONS, "/**").permitAll()
                .requestMatchers(
                        "/api/auth/**",
                        "/api/payment/webhook",
                        "/auth/login",
                        "/auth/register",
                        "/oauth2/**",
                        "/login/oauth2/**",
                        "/error",
                        "/",
                        "/courses",
                        "/courses/**",
                        "/public/courses/**",
                        "/blogs",
                        "/blogs/**",
                        "/public/blogs/**",
                        "/certificates/verify/**",
                        "/api/public/certificates/verify/**",
                        "/favicon.ico"
                ).permitAll()
                .requestMatchers("/css/**", "/js/**", "/images/**", "/fonts/**").permitAll()
                .requestMatchers("/student/**", "/api/student/**").hasRole("STUDENT")
                .requestMatchers("/teacher/**", "/api/teacher/**", "/instructor/**").hasRole("TEACHER")
                .requestMatchers("/admin/**", "/api/admin/**").hasRole("ADMIN")
                .anyRequest().authenticated()
            )
            .oauth2Login(oauth2 -> oauth2
                .loginPage("/auth/login")
                .successHandler(oAuth2LoginSuccessHandler)
                .failureHandler((request, response, exception) -> {
                    logOAuth2Failure(request.getRequestURI(), exception);
                    response.sendRedirect("/auth/login?error=oauth2");
                })
            )
            .logout(logout -> logout
                .logoutUrl("/auth/logout")
                .logoutSuccessHandler((request, response, authentication) -> {
                    ResponseCookie cookie = ResponseCookie.from("jwt_token", "")
                            .httpOnly(true)
                            .path("/")
                            .maxAge(Duration.ZERO)
                            .sameSite("Lax")
                            .build();
                    response.addHeader(HttpHeaders.SET_COOKIE, cookie.toString());
                    response.sendRedirect("/");
                })
                .invalidateHttpSession(true)
                .clearAuthentication(true)
                .deleteCookies("JSESSIONID")
            )
            .authenticationProvider(authenticationProvider())
            .addFilterBefore(jwtAuthFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }

    private void logOAuth2Failure(String requestUri, AuthenticationException exception) {
        String errorCode = null;
        if (exception instanceof OAuth2AuthenticationException oauth2Exception
                && oauth2Exception.getError() != null) {
            errorCode = oauth2Exception.getError().getErrorCode();
        }

        log.error(
                "OAuth2 login failed. URI={}, type={}, oauth2ErrorCode={}, message={}",
                requestUri,
                exception.getClass().getName(),
                errorCode,
                exception.getMessage(),
                exception
        );
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();
        configuration.setAllowedOrigins(List.of(
                "http://localhost:3000",
                "http://localhost:5173",
                "http://127.0.0.1:3000",
                "http://127.0.0.1:5173"
        ));
        configuration.setAllowedMethods(List.of("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"));
        configuration.setAllowedHeaders(List.of("Authorization", "Content-Type", "Accept", "Origin", "X-Requested-With"));
        configuration.setExposedHeaders(List.of("Authorization"));
        configuration.setAllowCredentials(true);
        configuration.setMaxAge(3600L);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        return source;
    }
}
