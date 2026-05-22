package com.cinebh.api.services.impl;

import com.cinebh.api.entities.User;
import com.cinebh.api.entities.enums.OAuthProvider;
import com.cinebh.api.entities.enums.UserRole;
import com.cinebh.api.exceptions.ApiException;
import com.cinebh.api.repositories.UserRepository;
import com.cinebh.api.services.OAuth2AuthService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class OAuth2AuthServiceImpl implements OAuth2AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    @Override
    @Transactional
    public User findOrCreateGoogleUser(final OAuth2User oauthUser) {
        final String googleId = oauthUser.getAttribute("sub");
        final String email = oauthUser.getAttribute("email");
        final String firstName = oauthUser.getAttribute("given_name");
        final String lastName = oauthUser.getAttribute("family_name");
        final String picture = oauthUser.getAttribute("picture");

        if (googleId == null || email == null) {
            throw new ApiException("Google account does not provide required profile information.", HttpStatus.BAD_REQUEST);
        }

        return userRepository.findByEmail(email.toLowerCase())
                .map(existingUser -> updateExistingUser(existingUser, googleId, firstName, lastName, picture))
                .orElseGet(() -> createGoogleUser(email, firstName, lastName, picture, googleId));
    }

    private User createGoogleUser(
            final String email,
            final String firstName,
            final String lastName,
            final String picture,
            final String googleId
    ) {
        final User user = new User();

        user.setId(UUID.randomUUID());
        user.setEmail(email.toLowerCase());
        user.setFirstName(firstName);
        user.setLastName(lastName);
        user.setProfileImageUrl(picture);
        user.setPasswordHash(passwordEncoder.encode(UUID.randomUUID().toString()));
        user.setOauthProvider(OAuthProvider.GOOGLE);
        user.setOauthProviderId(googleId);
        user.setRole(UserRole.CUSTOMER);
        user.setActive(true);
        user.setCreatedAt(OffsetDateTime.now());

        return userRepository.save(user);
    }

    private User updateExistingUser(
            final User user,
            final String googleId,
            final String firstName,
            final String lastName,
            final String picture
    ) {
        if (user.getOauthProvider() != null && user.getOauthProviderId() != null
                && !OAuthProvider.GOOGLE.equals(user.getOauthProvider())) {
            throw new ApiException("This email is already linked with another login provider.", HttpStatus.CONFLICT);
        }

        user.setOauthProvider(OAuthProvider.GOOGLE);
        user.setOauthProviderId(googleId);
        user.setActive(true);

        if (isBlank(user.getFirstName()) && !isBlank(firstName)) {
            user.setFirstName(firstName);
        }

        if (isBlank(user.getLastName()) && !isBlank(lastName)) {
            user.setLastName(lastName);
        }

        if (isBlank(user.getProfileImageUrl()) && !isBlank(picture)) {
            user.setProfileImageUrl(picture);
        }

        user.setUpdatedAt(OffsetDateTime.now());

        return userRepository.save(user);
    }

    private boolean isBlank(final String value) {
        return value == null || value.isBlank();
    }
}
