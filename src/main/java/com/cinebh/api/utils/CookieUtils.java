package com.cinebh.api.utils;

import com.cinebh.api.config.SecurityProperties;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseCookie;
import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.Optional;

@Component
@RequiredArgsConstructor
public final class CookieUtils {

    private static final String ACCESS_TOKEN_COOKIE = "access_token";
    private static final String REFRESH_TOKEN_COOKIE = "refresh_token";
    private static final String SESSION_COOKIE = "JSESSIONID";
    private static final String ROOT_COOKIE_PATH = "/";
    private static final String API_COOKIE_PATH = "/api/v1";

    private final SecurityProperties securityProperties;

    public void setTokenCookies(
            final HttpServletResponse response,
            final String accessToken,
            final String refreshToken,
            final boolean rememberMe
    ) {
        if (rememberMe) {
            setPersistentTokenCookies(response, accessToken, refreshToken);
            return;
        }

        setSessionTokenCookies(response, accessToken, refreshToken);
    }

    public void setAccessTokenCookie(final HttpServletResponse response, final String accessToken) {
        response.addHeader("Set-Cookie", buildSessionCookie(ACCESS_TOKEN_COOKIE, accessToken).toString());
    }

    public void clearTokenCookies(final HttpServletResponse response) {
        clearCookie(response, ACCESS_TOKEN_COOKIE);
        clearCookie(response, REFRESH_TOKEN_COOKIE);
    }

    public void clearSessionCookie(final HttpServletResponse response) {
        clearCookie(response, SESSION_COOKIE);
    }

    public void clearAuthenticationCookies(final HttpServletResponse response) {
        clearTokenCookies(response);
        clearSessionCookie(response);
    }

    public Optional<String> extractCookie(final HttpServletRequest request, final String name) {
        if (request.getCookies() == null) {
            return Optional.empty();
        }
        return Arrays.stream(request.getCookies())
                .filter(cookie -> name.equals(cookie.getName()))
                .map(Cookie::getValue)
                .findFirst();
    }

    private void setSessionTokenCookies(
            final HttpServletResponse response,
            final String accessToken,
            final String refreshToken
    ) {
        response.addHeader("Set-Cookie", buildSessionCookie(ACCESS_TOKEN_COOKIE, accessToken).toString());
        response.addHeader("Set-Cookie", buildSessionCookie(REFRESH_TOKEN_COOKIE, refreshToken).toString());
    }

    private void setPersistentTokenCookies(
            final HttpServletResponse response,
            final String accessToken,
            final String refreshToken
    ) {
        final long refreshMaxAge = securityProperties.jwt().rememberMeRefreshTokenExpirationMs() / 1000;

        response.addHeader("Set-Cookie", buildSessionCookie(ACCESS_TOKEN_COOKIE, accessToken).toString());
        response.addHeader("Set-Cookie", buildPersistentCookie(REFRESH_TOKEN_COOKIE, refreshToken, refreshMaxAge).toString());
    }

    private ResponseCookie buildSessionCookie(final String name, final String value) {
        return baseCookie(name, value, ROOT_COOKIE_PATH, true).build();
    }

    private ResponseCookie buildPersistentCookie(final String name, final String value, final long maxAgeSeconds) {
        return baseCookie(name, value, ROOT_COOKIE_PATH, true)
                .maxAge(maxAgeSeconds)
                .build();
    }

    private void clearCookie(final HttpServletResponse response, final String name) {
        response.addHeader("Set-Cookie", buildExpiredCookie(name, ROOT_COOKIE_PATH, true).toString());
        response.addHeader("Set-Cookie", buildExpiredCookie(name, ROOT_COOKIE_PATH, false).toString());
        response.addHeader("Set-Cookie", buildExpiredCookie(name, API_COOKIE_PATH, true).toString());
        response.addHeader("Set-Cookie", buildExpiredCookie(name, API_COOKIE_PATH, false).toString());
    }

    private ResponseCookie buildExpiredCookie(
            final String name,
            final String path,
            final boolean includeConfiguredDomain
    ) {
        return baseCookie(name, "", path, includeConfiguredDomain)
                .maxAge(0)
                .build();
    }

    private ResponseCookie.ResponseCookieBuilder baseCookie(
            final String name,
            final String value,
            final String path,
            final boolean includeConfiguredDomain
    ) {
        final ResponseCookie.ResponseCookieBuilder builder = ResponseCookie.from(name, value)
                .httpOnly(securityProperties.cookie().httpOnly())
                .secure(securityProperties.cookie().secure())
                .path(path)
                .sameSite(securityProperties.cookie().sameSite());

        final String configuredDomain = securityProperties.cookie().domain();
        if (includeConfiguredDomain && configuredDomain != null && !configuredDomain.isBlank()) {
            builder.domain(configuredDomain);
        }

        return builder;
    }
}
