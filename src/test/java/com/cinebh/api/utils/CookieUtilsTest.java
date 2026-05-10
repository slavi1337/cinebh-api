package com.cinebh.api.utils;

import com.cinebh.api.config.SecurityProperties;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.contains;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CookieUtilsTest {

    @Mock
    private SecurityProperties securityProperties;

    @Mock
    private SecurityProperties.Jwt jwtProperties;

    @Mock
    private SecurityProperties.Cookie cookieProperties;

    @InjectMocks
    private CookieUtils cookieUtils;

    @Test
    void shouldSetTokenCookiesSuccessfully() {
        final HttpServletResponse response = mock(HttpServletResponse.class);

        stubCookieSettings();
        when(securityProperties.getJwt()).thenReturn(jwtProperties);
        when(jwtProperties.getAccessTokenExpirationMs()).thenReturn(900000L);
        when(jwtProperties.getRefreshTokenExpirationMs()).thenReturn(604800000L);

        cookieUtils.setTokenCookies(response, "access-val", "refresh-val");

        verify(response, atLeastOnce()).addHeader(eq("Set-Cookie"), contains("access_token=access-val"));
        verify(response, atLeastOnce()).addHeader(eq("Set-Cookie"), contains("refresh_token=refresh-val"));
        verify(response, atLeastOnce()).addHeader(eq("Set-Cookie"), contains("HttpOnly"));
    }

    @Test
    void shouldClearTokenCookiesSuccessfully() {
        final HttpServletResponse response = mock(HttpServletResponse.class);

        stubCookieSettings();

        cookieUtils.clearTokenCookies(response);

        verify(response, atLeastOnce()).addHeader(eq("Set-Cookie"), contains("access_token=;"));
        verify(response, atLeastOnce()).addHeader(eq("Set-Cookie"), contains("Max-Age=0"));
    }

    @Test
    void shouldExtractCookieSuccessfully() {
        final HttpServletRequest request = mock(HttpServletRequest.class);
        final Cookie[] cookies = {new Cookie("access_token", "test-token")};
        when(request.getCookies()).thenReturn(cookies);

        final Optional<String> result = cookieUtils.extractCookie(request, "access_token");

        assertThat(result).isPresent().contains("test-token");
    }

    private void stubCookieSettings() {
        when(securityProperties.getCookie()).thenReturn(cookieProperties);
        when(cookieProperties.getDomain()).thenReturn("localhost");
        when(cookieProperties.isHttpOnly()).thenReturn(true);
        when(cookieProperties.isSecure()).thenReturn(true);
        when(cookieProperties.getSameSite()).thenReturn("None");
    }
}
