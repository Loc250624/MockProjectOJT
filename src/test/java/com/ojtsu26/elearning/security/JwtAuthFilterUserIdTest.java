package com.ojtsu26.elearning.security;

import jakarta.servlet.FilterChain;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class JwtAuthFilterUserIdTest {

    @AfterEach
    void clearContext() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void filterLoadsTheExactInternalUserIdFromToken() throws Exception {
        JwtUtils jwtUtils = mock(JwtUtils.class);
        CustomUserDetailsService userDetailsService = mock(CustomUserDetailsService.class);
        UserDetails expectedPrincipal = mock(UserDetails.class);
        when(expectedPrincipal.isEnabled()).thenReturn(true);
        when(expectedPrincipal.isAccountNonLocked()).thenReturn(true);
        when(expectedPrincipal.isAccountNonExpired()).thenReturn(true);
        when(expectedPrincipal.isCredentialsNonExpired()).thenReturn(true);
        when(expectedPrincipal.getAuthorities()).thenReturn(List.of());
        when(jwtUtils.validateJwtToken("token-for-github-user")).thenReturn(true);
        when(jwtUtils.getUserIdFromJwtToken("token-for-github-user")).thenReturn(202);
        when(userDetailsService.loadUserById(202)).thenReturn(expectedPrincipal);

        JwtAuthFilter filter = new JwtAuthFilter(jwtUtils, userDetailsService);
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("Authorization", "Bearer token-for-github-user");
        filter.doFilterInternal(request, new MockHttpServletResponse(), mock(FilterChain.class));

        assertThat(SecurityContextHolder.getContext().getAuthentication().getPrincipal())
                .isSameAs(expectedPrincipal);
        verify(userDetailsService).loadUserById(202);
    }
}
