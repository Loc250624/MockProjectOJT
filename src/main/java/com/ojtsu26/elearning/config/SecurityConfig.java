package com.ojtsu26.elearning.config;

import com.ojtsu26.elearning.security.CustomAccessDeniedHandler;
import com.ojtsu26.elearning.security.CustomOAuth2UserService;
import com.ojtsu26.elearning.security.CustomUserDetailsService;
import com.ojtsu26.elearning.security.CsrfCookieFilter;
import com.ojtsu26.elearning.security.JwtAuthEntryPoint;
import com.ojtsu26.elearning.security.JwtAuthFilter;
import com.ojtsu26.elearning.security.OAuth2AccountService;
import com.ojtsu26.elearning.security.OAuth2LoginSuccessHandler;
import com.ojtsu26.elearning.security.OAuth2ProviderConfigurationFilter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseCookie;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;
import org.springframework.security.oauth2.client.web.DefaultOAuth2AuthorizationRequestResolver;
import org.springframework.security.oauth2.client.web.OAuth2AuthorizationRequestRedirectFilter;
import org.springframework.security.oauth2.client.web.OAuth2AuthorizationRequestResolver;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.endpoint.OAuth2AuthorizationRequest;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.security.web.csrf.CookieCsrfTokenRepository;
import org.springframework.security.web.csrf.CsrfTokenRequestAttributeHandler;
import org.springframework.security.web.session.SessionManagementFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.time.Duration;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

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
    private final CustomOAuth2UserService customOAuth2UserService;
    private final ClientRegistrationRepository clientRegistrationRepository;
    private final OAuth2ProviderConfigurationFilter oAuth2ProviderConfigurationFilter;
    private final PasswordEncoder passwordEncoder;

    @Value("${app.cors.allowed-origins:http://localhost:3000,http://localhost:5173,http://127.0.0.1:3000,http://127.0.0.1:5173}")
    private List<String> allowedOrigins;

    @Bean
    public DaoAuthenticationProvider authenticationProvider() {
        DaoAuthenticationProvider authProvider = new DaoAuthenticationProvider();
        authProvider.setUserDetailsService(userDetailsService);
        authProvider.setPasswordEncoder(passwordEncoder);
        return authProvider;
    }

    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration authConfig) throws Exception {
        return authConfig.getAuthenticationManager();
    }

    /**
     * Custom OAuth2 authorization-request resolver.
     *
     * <p>Intercepts Google authorization requests and appends
     * {@code prompt=select_account} so that Google always shows the
     * account-chooser screen, even when the user is already signed in to a
     * Google account in their browser.  Without this parameter, Google silently
     * reuses the last session — making it impossible to switch accounts without
     * first logging out of Google entirely.</p>
     */
    @Bean
    public OAuth2AuthorizationRequestResolver oAuth2AuthorizationRequestResolver() {
        DefaultOAuth2AuthorizationRequestResolver resolver =
                new DefaultOAuth2AuthorizationRequestResolver(
                        clientRegistrationRepository,
                        "/oauth2/authorization");

        // Append prompt=select_account to every outbound authorization request.
        // Google: shows the account-chooser dialog.
        // GitHub: unknown parameter — silently ignored by GitHub.
        return new OAuth2AuthorizationRequestResolver() {
            @Override
            public OAuth2AuthorizationRequest resolve(jakarta.servlet.http.HttpServletRequest request) {
                return withGoogleAccountChooser(resolveRegistrationId(request), resolver.resolve(request));
            }

            @Override
            public OAuth2AuthorizationRequest resolve(jakarta.servlet.http.HttpServletRequest request,
                                                      String clientRegistrationId) {
                return withGoogleAccountChooser(clientRegistrationId, resolver.resolve(request, clientRegistrationId));
            }
        };
    }

    private OAuth2AuthorizationRequest withGoogleAccountChooser(String registrationId,
                                                                OAuth2AuthorizationRequest authorizationRequest) {
        if (authorizationRequest == null || !"google".equalsIgnoreCase(registrationId)) {
            return authorizationRequest;
        }

        Map<String, Object> additionalParameters =
                new LinkedHashMap<>(authorizationRequest.getAdditionalParameters());
        additionalParameters.put("prompt", "select_account");

        return OAuth2AuthorizationRequest.from(authorizationRequest)
                .additionalParameters(additionalParameters)
                .build();
    }

    private String resolveRegistrationId(jakarta.servlet.http.HttpServletRequest request) {
        String uri = request.getRequestURI();
        int lastSlash = uri.lastIndexOf('/');
        return lastSlash >= 0 ? uri.substring(lastSlash + 1) : uri;
    }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
            .csrf(csrf -> csrf
                    .csrfTokenRepository(CookieCsrfTokenRepository.withHttpOnlyFalse())
                    .csrfTokenRequestHandler(new CsrfTokenRequestAttributeHandler())
                    .requireCsrfProtectionMatcher(this::requiresTeacherCsrfProtection)
            )
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
                        "/api/ai-chatbot/chat",
                        "/api/payment/vnpay-ipn",
                        "/api/payment/webhook",
                        "/student/payment-result",
                        "/auth/login",
                        "/auth/register",
                        "/auth/oauth2/complete",
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
                        "/about/introduction",
                        "/contact",
                        "/legal/terms-of-use",
                        "/legal/privacy-policy",
                        "/support/help-center",
                        "/support/learning-guide",
                        "/support/faq",
                        "/support/payment-policy",
                        "/certificates",
                        "/certificates/verify/**",
                        "/ai-chatbot",
                        "/api/public/certificates/verify/**",
                        "/favicon.ico"
                ).permitAll()
                .requestMatchers("/css/**", "/js/**", "/images/**", "/fonts/**").permitAll()
                .requestMatchers("/admin/feedback", "/admin/feedback/**").hasRole("ADMIN")
                .requestMatchers("/student/feedback").hasRole("STUDENT")
                .requestMatchers("/student/feedback/**").denyAll()
                .requestMatchers("/student/**", "/api/student/**").hasRole("STUDENT")
                .requestMatchers("/teacher/**", "/api/teacher/**", "/instructor/**").hasRole("TEACHER")
                .requestMatchers("/admin/**", "/api/admin/**").hasRole("ADMIN")
                .anyRequest().authenticated()
            )
            .oauth2Login(oauth2 -> oauth2
                .loginPage("/auth/login")
                // Wire the custom user-info service that handles GitHub private email
                .userInfoEndpoint(userInfo -> userInfo
                    .userService(customOAuth2UserService)
                )
                // Wire the custom resolver that forces Google account-chooser every time
                .authorizationEndpoint(auth -> auth
                    .authorizationRequestResolver(oAuth2AuthorizationRequestResolver())
                )
                .successHandler(oAuth2LoginSuccessHandler)
                .failureHandler((request, response, exception) -> {
                    logOAuth2Failure(request.getRequestURI(), exception);
                    response.sendRedirect("/auth/login?error=" + resolveOAuth2FailureError(exception));
                })
            )
            .logout(logout -> logout
                .logoutUrl("/auth/logout")
                .logoutSuccessHandler((request, response, authentication) -> {
                    // Expire the JWT HTTP-Only cookie
                    ResponseCookie jwtClear = ResponseCookie.from("jwt_token", "")
                            .httpOnly(true)
                            .path("/")
                            .maxAge(Duration.ZERO)
                            .sameSite("Lax")
                            .build();
                    response.addHeader(HttpHeaders.SET_COOKIE, jwtClear.toString());

                    // Expire the session cookie (used internally by Spring OAuth2
                    // to store the PKCE state / nonce during the code exchange)
                    ResponseCookie sessionClear = ResponseCookie.from("JSESSIONID", "")
                            .httpOnly(true)
                            .path("/")
                            .maxAge(Duration.ZERO)
                            .sameSite("Lax")
                            .build();
                    response.addHeader(HttpHeaders.SET_COOKIE, sessionClear.toString());

                    response.sendRedirect("/");
                })
                .invalidateHttpSession(true)
                .clearAuthentication(true)
                .deleteCookies("JSESSIONID", "jwt_token")
            )
            .authenticationProvider(authenticationProvider())
            .addFilterAfter(new CsrfCookieFilter(), SessionManagementFilter.class)
            .addFilterBefore(oAuth2ProviderConfigurationFilter, OAuth2AuthorizationRequestRedirectFilter.class)
            .addFilterBefore(jwtAuthFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }

    private boolean requiresTeacherCsrfProtection(HttpServletRequest request) {
        String method = request.getMethod();
        if (HttpMethod.GET.matches(method) || HttpMethod.HEAD.matches(method)
                || HttpMethod.TRACE.matches(method) || HttpMethod.OPTIONS.matches(method)) {
            return false;
        }
        String path = request.getServletPath();
        if (path == null) {
            return false;
        }
        // Exclude payment webhooks/IPN from CSRF check
        if (path.startsWith("/api/payment/")) {
            return false;
        }
        return true;
    }

    private void logOAuth2Failure(String requestUri, AuthenticationException exception) {
        String errorCode = null;
        if (exception instanceof OAuth2AuthenticationException oauth2Exception
                && oauth2Exception.getError() != null) {
            errorCode = oauth2Exception.getError().getErrorCode();
        }
        log.warn(
                "OAuth2 login failed. URI={}, type={}, oauth2ErrorCode={}",
                requestUri,
                exception.getClass().getName(),
                errorCode);
    }

    private String resolveOAuth2FailureError(AuthenticationException exception) {
        if (exception instanceof OAuth2AuthenticationException oauth2Exception
                && oauth2Exception.getError() != null) {
            String errorCode = oauth2Exception.getError().getErrorCode();
            if (OAuth2AccountService.ERROR_EMAIL_NOT_FOUND.equals(errorCode)) {
                return "oauth2_no_email";
            }
            if ("access_denied".equals(errorCode)) {
                return "oauth2_cancelled";
            }
        }
        return "oauth2";
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();
        configuration.setAllowedOrigins(allowedOrigins);
        configuration.setAllowedMethods(List.of("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"));
        configuration
                .setAllowedHeaders(List.of("Authorization", "Content-Type", "Accept", "Origin", "X-Requested-With",
                        "X-CSRF-TOKEN", "X-XSRF-TOKEN"));
        configuration.setExposedHeaders(List.of("Authorization"));
        configuration.setAllowCredentials(true);
        configuration.setMaxAge(3600L);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        return source;
    }
}
