package com.cinebh.api.services;

import com.cinebh.api.entities.User;
import org.springframework.security.oauth2.core.user.OAuth2User;

public interface OAuth2AuthService {
    User findOrCreateGoogleUser(OAuth2User oauthUser);
}
