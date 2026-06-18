package com.cinebh.api.utils;

import com.cinebh.api.config.SecurityProperties;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
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
    void shouldSetSessionTokenCookiesWhenRememberMeIsFalse() {
        final HttpServletResponse response = mock(HttpServletResponse.class);

        stubCookieSettings();

        cookieUtils.setTokenCookies(response, "access-val", "refresh-val", false);

        final List<String> cookieHeaders = captureCookieHeaders(response, 2);

        assertThat(cookieHeaders).anyMatch(header -> header.contains("access_token=access-val"));
        assertThat(cookieHeaders).anyMatch(header -> header.contains("refresh_token=refresh-val"));
        assertThat(cookieHeaders).anyMatch(header -> header.contains("HttpOnly"));
        assertThat(cookieHeaders).anyMatch(header -> header.contains("Secure"));
        assertThat(cookieHeaders).noneMatch(header -> header.contains("Max-Age"));
    }

    @Test
    void shouldSetPersistentTokenCookiesWhenRememberMeIsTrue() {
        final HttpServletResponse response = mock(HttpServletResponse.class);

        stubCookieSettings();
        when(securityProperties.jwt()).thenReturn(jwtProperties);
        when(jwtProperties.rememberMeRefreshTokenExpirationMs()).thenReturn(2592000000L);

        cookieUtils.setTokenCookies(response, "access-val", "refresh-val", true);

        final List<String> cookieHeaders = captureCookieHeaders(response, 2);

        assertThat(cookieHeaders).anyMatch(header ->
                header.contains("access_token=access-val") && !header.contains("Max-Age")
        );
        assertThat(cookieHeaders).anyMatch(header ->
                header.contains("refresh_token=refresh-val") && header.contains("Max-Age=2592000")
        );
        assertThat(cookieHeaders).anyMatch(header -> header.contains("HttpOnly"));
        assertThat(cookieHeaders).anyMatch(header -> header.contains("Secure"));
    }

    @Test
    void shouldSetAccessTokenCookieAsSessionCookie() {
        final HttpServletResponse response = mock(HttpServletResponse.class);

        stubCookieSettings();

        cookieUtils.setAccessTokenCookie(response, "new-access-val");

        final List<String> cookieHeaders = captureCookieHeaders(response, 1);

        assertThat(cookieHeaders).anyMatch(header -> header.contains("access_token=new-access-val"));
        assertThat(cookieHeaders).noneMatch(header -> header.contains("Max-Age"));
    }

    @Test
    void shouldClearTokenCookiesSuccessfully() {
        final HttpServletResponse response = mock(HttpServletResponse.class);

        stubCookieSettings();

        cookieUtils.clearTokenCookies(response);

        final List<String> cookieHeaders = captureCookieHeaders(response, 8);

        assertThat(cookieHeaders).filteredOn(header ->
                header.contains("access_token=") && header.contains("Max-Age=0")
        ).hasSize(4);
        assertThat(cookieHeaders).filteredOn(header ->
                header.contains("refresh_token=") && header.contains("Max-Age=0")
        ).hasSize(4);
        assertThat(cookieHeaders).anyMatch(header -> header.contains("Domain=localhost"));
        assertThat(cookieHeaders).anyMatch(header -> !header.contains("Domain="));
        assertThat(cookieHeaders).anyMatch(header -> header.contains("Path=/api/v1"));
        assertThat(cookieHeaders).anyMatch(header -> header.contains("Path=/;"));
    }

    @Test
    void shouldClearAuthenticationCookiesSuccessfully() {
        final HttpServletResponse response = mock(HttpServletResponse.class);

        stubCookieSettings();

        cookieUtils.clearAuthenticationCookies(response);

        final List<String> cookieHeaders = captureCookieHeaders(response, 12);

        assertThat(cookieHeaders).filteredOn(header ->
                header.contains("access_token=") && header.contains("Max-Age=0")
        ).hasSize(4);
        assertThat(cookieHeaders).filteredOn(header ->
                header.contains("refresh_token=") && header.contains("Max-Age=0")
        ).hasSize(4);
        assertThat(cookieHeaders).filteredOn(header ->
                header.contains("JSESSIONID=") && header.contains("Max-Age=0")
        ).hasSize(4);
    }

    @Test
    void shouldExtractCookieSuccessfully() {
        final HttpServletRequest request = mock(HttpServletRequest.class);
        final Cookie[] cookies = {new Cookie("access_token", "test-token")};
        when(request.getCookies()).thenReturn(cookies);

        final Optional<String> result = cookieUtils.extractCookie(request, "access_token");

        assertThat(result).isPresent().contains("test-token");
    }

    @Test
    void shouldReturnEmptyOptionalWhenCookieDoesNotExist() {
        final HttpServletRequest request = mock(HttpServletRequest.class);
        final Cookie[] cookies = {new Cookie("refresh_token", "test-token")};

        when(request.getCookies()).thenReturn(cookies);

        final Optional<String> result = cookieUtils.extractCookie(request, "access_token");

        assertThat(result).isEmpty();
    }

    @Test
    void shouldReturnEmptyOptionalWhenRequestHasNoCookies() {
        final HttpServletRequest request = mock(HttpServletRequest.class);

        when(request.getCookies()).thenReturn(null);

        final Optional<String> result = cookieUtils.extractCookie(request, "access_token");

        assertThat(result).isEmpty();
    }

    private void stubCookieSettings() {
        when(securityProperties.cookie()).thenReturn(cookieProperties);
        when(cookieProperties.domain()).thenReturn("localhost");
        when(cookieProperties.httpOnly()).thenReturn(true);
        when(cookieProperties.secure()).thenReturn(true);
        when(cookieProperties.sameSite()).thenReturn("None");
    }

    private List<String> captureCookieHeaders(final HttpServletResponse response, final int expectedInvocations) {
        final ArgumentCaptor<String> cookieHeaderCaptor = ArgumentCaptor.forClass(String.class);

        verify(response, times(expectedInvocations))
                .addHeader(eq("Set-Cookie"), cookieHeaderCaptor.capture());

        return cookieHeaderCaptor.getAllValues();
    }
}
