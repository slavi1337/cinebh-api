package com.cinebh.api.security;

import com.cinebh.api.config.SecurityProperties;
import com.cinebh.api.entities.User;
import com.cinebh.api.entities.enums.UserRole;
import com.cinebh.api.security.impl.JwtServiceImpl;
import io.jsonwebtoken.Claims;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class JwtServiceImplTest {

    @Mock
    private SecurityProperties securityProperties;

    @Mock
    private SecurityProperties.Jwt jwtProperties;

    @InjectMocks
    private JwtServiceImpl jwtService;

    private User testUser;

    @BeforeEach
    void setUp() {
        when(securityProperties.jwt()).thenReturn(jwtProperties);
        when(jwtProperties.secret()).thenReturn("v9y$B&E)H@McQfTjWnZr4u7x!A%C*F-JaNdTgUkXp2s5v8y/B?E(G+KbPeShVmYq");

        testUser = new User();
        testUser.setEmail("test@cinebh.com");
        testUser.setRole(UserRole.CUSTOMER);
    }

    @Test
    void shouldGenerateValidAccessToken() {
        when(jwtProperties.accessTokenExpirationMs()).thenReturn(3600000L);

        final String token = jwtService.generateAccessToken(testUser);

        assertThat(token).isNotNull();
        assertThat(jwtService.extractEmail(token)).isEqualTo(testUser.getEmail());
        assertThat(jwtService.isTokenValid(token)).isTrue();

        final Claims claims = jwtService.extractClaims(token);
        assertThat(claims.get("role")).isEqualTo("CUSTOMER");
    }

    @Test
    void shouldGenerateValidRefreshToken() {
        when(jwtProperties.refreshTokenExpirationMs()).thenReturn(86400000L);

        final String token = jwtService.generateRefreshToken(testUser);

        assertThat(token).isNotNull();
        assertThat(jwtService.extractEmail(token)).isEqualTo(testUser.getEmail());
        assertThat(jwtService.isTokenValid(token)).isTrue();
    }
}
