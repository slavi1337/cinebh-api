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

    private final SecurityProperties securityProperties;

    public void setTokenCookies(
            final HttpServletResponse response,
            final String accessToken,
            final String refreshToken
    ) {
        final long accessMaxAge = securityProperties.jwt().accessTokenExpirationMs() / 1000;
        final long refreshMaxAge = securityProperties.jwt().refreshTokenExpirationMs() / 1000;

        response.addHeader("Set-Cookie", buildCookie("access_token", accessToken, accessMaxAge).toString());
        response.addHeader("Set-Cookie", buildCookie("refresh_token", refreshToken, refreshMaxAge).toString());
    }

    public void clearTokenCookies(final HttpServletResponse response) {
        response.addHeader("Set-Cookie", buildCookie("access_token", "", 0).toString());
        response.addHeader("Set-Cookie", buildCookie("refresh_token", "", 0).toString());
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

    private ResponseCookie buildCookie(final String name, final String value, final long maxAgeSeconds) {
        return ResponseCookie.from(name, value)
                .httpOnly(securityProperties.cookie().httpOnly())
                .secure(securityProperties.cookie().secure())
                .path("/")
                .domain(securityProperties.cookie().domain())
                .maxAge(maxAgeSeconds)
                .sameSite(securityProperties.cookie().sameSite())
                .build();
    }
}
