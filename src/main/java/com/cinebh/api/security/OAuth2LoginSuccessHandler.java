package com.cinebh.api.security;

import com.cinebh.api.config.FrontendProperties;
import com.cinebh.api.entities.User;
import com.cinebh.api.security.JwtService;
import com.cinebh.api.services.OAuth2AuthService;
import com.cinebh.api.utils.CookieUtils;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;

@Component
@RequiredArgsConstructor
public class OAuth2LoginSuccessHandler implements AuthenticationSuccessHandler {

    private final OAuth2AuthService oauth2AuthService;
    private final JwtService jwtService;
    private final CookieUtils cookieUtils;
    private final FrontendProperties frontendProperties;

    @Override
    public void onAuthenticationSuccess(
            final HttpServletRequest request,
            final HttpServletResponse response,
            final Authentication authentication
    ) throws IOException, ServletException {
        final OAuth2User oauthUser = (OAuth2User) authentication.getPrincipal();
        final User user = oauth2AuthService.findOrCreateGoogleUser(oauthUser);

        final String accessToken = jwtService.generateAccessToken(user);
        final String refreshToken = jwtService.generateRefreshToken(user, true);

        cookieUtils.setTokenCookies(response, accessToken, refreshToken, true);

        response.sendRedirect(frontendProperties.baseUrl() + "/?auth=google-success");
    }
}
