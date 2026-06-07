package com.cinebh.api.services.impl;

import com.cinebh.api.dto.auth.LoginRequest;
import com.cinebh.api.dto.auth.LoginResponse;
import com.cinebh.api.dto.auth.RegisterRequest;
import com.cinebh.api.dto.auth.VerifyRequest;
import com.cinebh.api.entities.City;
import com.cinebh.api.entities.User;
import com.cinebh.api.entities.enums.UserRole;
import com.cinebh.api.entities.enums.VerificationCodeType;
import com.cinebh.api.exceptions.ApiException;
import com.cinebh.api.repositories.UserRepository;
import com.cinebh.api.repositories.CityRepository;
import com.cinebh.api.security.JwtService;
import com.cinebh.api.services.*;
import com.cinebh.api.utils.CookieUtils;
import com.cinebh.api.utils.SecurityUtils;
import io.jsonwebtoken.Claims;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class AuthServiceImpl implements AuthService {

    private final UserRepository userRepository;
    private final CityRepository cityRepository;
    private final PasswordEncoder passwordEncoder;
    private final VerificationService verificationService;
    private final NotificationService notificationService;
    private final AdvancedValidationService advancedValidationService;
    private final AddressValidationService addressValidationService;
    private final JwtService jwtService;
    private final CookieUtils cookieUtils;
    private final SecurityUtils securityUtils;

    @Override
    @Transactional
    public void register(final RegisterRequest request) {
        userRepository.findByEmail(request.email()).ifPresent(existingUser -> {
            final String message = existingUser.isActive()
                    ? "Email is already in use."
                    : "Account already exists but is not verified. Please proceed to login to receive a new verification code.";
            throw new ApiException("User DTO validation failed.", HttpStatus.BAD_REQUEST, 10001, "email", message);
        });

        advancedValidationService.validateEmailDomain(request.email());
        advancedValidationService.validatePasswordPwned(request.password());
        advancedValidationService.validatePhone(request.phone());
        advancedValidationService.validateNameNotReserved(request.firstName(), request.lastName());
        advancedValidationService.validateImageUrl(request.profileImageUrl());

        final User user = new User();
        user.setId(UUID.randomUUID());
        user.setEmail(request.email());
        user.setPasswordHash(passwordEncoder.encode(request.password()));
        user.setFirstName(request.firstName());
        user.setLastName(request.lastName());
        user.setPhone(request.phone());
        user.setProfileImageUrl(request.profileImageUrl());
        user.setStreetAddress(request.streetAddress());
        if (request.cityId() != null) {
            final City city = cityRepository.findById(request.cityId())
                    .orElseThrow(() -> new ApiException("Invalid location selected.", HttpStatus.BAD_REQUEST));
            user.setCity(city);
            if (!addressValidationService.isValidStreetInCity(city.getName(), request.streetAddress())) {
                throw new ApiException("Street does not exist in the selected city.", HttpStatus.BAD_REQUEST);
            }
        }
        user.setRole(UserRole.CUSTOMER);
        user.setActive(false);
        user.setCreatedAt(OffsetDateTime.now());

        try {
            userRepository.saveAndFlush(user);
        } catch (DataIntegrityViolationException exception) {
            throw new ApiException("User DTO validation failed.", HttpStatus.BAD_REQUEST, 10002, "email/phone", "Email or Phone number is already in use.");
        }

        final String code = verificationService.generateAndSaveCode(user, VerificationCodeType.ACCOUNT_VERIFICATION);
        notificationService.sendAccountVerificationCode(user.getEmail(), getFullName(user), code);
    }

    @Override
    @Transactional
    public void verify(final VerifyRequest request) {
        final User user = userRepository.findByEmail(request.email())
                .orElseThrow(() -> new ApiException("User not found.", HttpStatus.NOT_FOUND));

        if (user.isActive()) {
            throw new ApiException("Account is already verified.", HttpStatus.BAD_REQUEST);
        }

        final boolean isValid = verificationService.verifyCode(
                user,
                VerificationCodeType.ACCOUNT_VERIFICATION,
                request.code()
        );

        if (!isValid) {
            throw new ApiException("Invalid or expired verification code.", HttpStatus.BAD_REQUEST);
        }

        user.setActive(true);
        user.setUpdatedAt(OffsetDateTime.now());
        userRepository.save(user);
    }

    @Override
    @Transactional(noRollbackFor = ApiException.class)
    public LoginResponse login(final LoginRequest request, final HttpServletResponse response) {
        final User user = userRepository.findByEmail(request.email())
                .orElseThrow(() -> new ApiException("Invalid email or password.", HttpStatus.UNAUTHORIZED));

        if (!passwordEncoder.matches(request.password(), user.getPasswordHash())) {
            throw new ApiException("Invalid email or password.", HttpStatus.UNAUTHORIZED);
        }

        final String fullName = getFullName(user);

        if (!user.isActive()) {
            final String code = verificationService.generateAndSaveCode(user, VerificationCodeType.ACCOUNT_VERIFICATION);
            notificationService.sendAccountVerificationCode(user.getEmail(), fullName, code);
            throw new ApiException("Account is not verified. A new code has been sent to your email.", HttpStatus.FORBIDDEN);
        }

        final String accessToken = jwtService.generateAccessToken(user);
        final String refreshToken = jwtService.generateRefreshToken(user, request.rememberMe());

        cookieUtils.setTokenCookies(response, accessToken, refreshToken, request.rememberMe());

        return new LoginResponse(user.getId(), user.getEmail(), fullName, user.getRole().name());
    }

    @Override
    @Transactional(readOnly = true)
    public LoginResponse getCurrentUser() {
        final User user = securityUtils.getCurrentUser();
        return new LoginResponse(user.getId(), user.getEmail(), getFullName(user), user.getRole().name());
    }

    @Override
    public void refresh(final HttpServletRequest request, final HttpServletResponse response) {
        final String refreshToken = cookieUtils.extractCookie(request, "refresh_token")
                .orElseThrow(() -> new ApiException("Refresh token missing", HttpStatus.UNAUTHORIZED));

        try {
            final Claims claims = jwtService.extractClaims(refreshToken);
            final String email = claims.getSubject();

            final User user = userRepository.findByEmail(email)
                    .orElseThrow(() -> new ApiException("User not found.", HttpStatus.UNAUTHORIZED));

            final String newAccessToken = jwtService.generateAccessToken(user);
            cookieUtils.setAccessTokenCookie(response, newAccessToken);
        } catch (Exception e) {
            throw new ApiException("Invalid or expired refresh token.", HttpStatus.UNAUTHORIZED);
        }
    }

    @Override
    public void logout(final HttpServletResponse response) {
        cookieUtils.clearTokenCookies(response);
    }

    private String getFullName(final User user) {
        return (user.getFirstName() != null && user.getLastName() != null)
                ? user.getFirstName() + " " + user.getLastName()
                : user.getEmail().split("@")[0];
    }
}
