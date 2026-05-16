package com.cinebh.api.utils;

import com.cinebh.api.entities.User;
import com.cinebh.api.exceptions.ApiException;
import com.cinebh.api.repositories.UserRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SecurityUtilsTest {

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private SecurityUtils securityUtils;

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void shouldReturnCurrentUserSuccessfully() {
        final String email = "test@cinebh.com";
        final User user = new User();
        user.setEmail(email);

        final Authentication auth = setupSecurityContext(email);
        when(auth.getName()).thenReturn(email);
        when(userRepository.findByEmail(email)).thenReturn(Optional.of(user));

        final User result = securityUtils.getCurrentUser();

        assertThat(result.getEmail()).isEqualTo(email);
    }

    @Test
    void shouldThrowExceptionWhenNotAuthenticated() {
        SecurityContextHolder.clearContext();

        assertThatThrownBy(() -> securityUtils.getCurrentUser())
                .isInstanceOf(ApiException.class)
                .hasMessage("User not authenticated");
    }

    @Test
    void shouldThrowExceptionForAnonymousUser() {
        setupSecurityContext("anonymousUser");

        assertThatThrownBy(() -> securityUtils.getCurrentUser())
                .isInstanceOf(ApiException.class)
                .hasMessage("User not authenticated");
    }

    private Authentication setupSecurityContext(String principal) {
        final SecurityContext securityContext = mock(SecurityContext.class);
        final Authentication authentication = mock(Authentication.class);

        when(securityContext.getAuthentication()).thenReturn(authentication);
        when(authentication.isAuthenticated()).thenReturn(true);
        when(authentication.getPrincipal()).thenReturn(principal);

        SecurityContextHolder.setContext(securityContext);
        return authentication;
    }
}
