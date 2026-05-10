package com.cinebh.api.services.impl;

import com.cinebh.api.dto.auth.LoginRequest;
import com.cinebh.api.dto.auth.LoginResponse;
import com.cinebh.api.dto.auth.RegisterRequest;
import com.cinebh.api.dto.auth.VerifyRequest;
import com.cinebh.api.entities.User;
import com.cinebh.api.entities.enums.UserRole;
import com.cinebh.api.entities.enums.VerificationCodeType;
import com.cinebh.api.exceptions.ApiException;
import com.cinebh.api.repositories.UserRepository;
import com.cinebh.api.security.JwtService;
import com.cinebh.api.services.AdvancedValidationService;
import com.cinebh.api.services.AuthService;
import com.cinebh.api.services.NotificationService;
import com.cinebh.api.services.VerificationService;
import com.cinebh.api.utils.CookieUtils;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
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
    private final PasswordEncoder passwordEncoder;
    private final VerificationService verificationService;
    private final NotificationService notificationService;
    private final AdvancedValidationService advancedValidationService;
    private final JwtService jwtService;
    private final CookieUtils cookieUtils;

    @Override
    @Transactional
    public void register(final RegisterRequest request) {
        userRepository.findByEmail(request.email()).ifPresent(existingUser -> {
            final String message = existingUser.isActive()
                    ? "Email is already in use."
                    : "Account already exists but is not verified. Please proceed to login to receive a new verification code.";
            throw new ApiException(message, HttpStatus.BAD_REQUEST);
        });

        advancedValidationService.validateEmailDomain(request.email());
        advancedValidationService.validatePasswordPwned(request.password());

        final User user = new User();
        user.setId(UUID.randomUUID());
        user.setEmail(request.email());
        user.setPasswordHash(passwordEncoder.encode(request.password()));
        user.setRole(UserRole.CUSTOMER);
        user.setActive(false);
        user.setCreatedAt(OffsetDateTime.now());

        try {
            userRepository.saveAndFlush(user);
        } catch (org.springframework.dao.DataIntegrityViolationException exception) {
            throw new ApiException("Email is already in use. If unverified, please proceed to login.", HttpStatus.BAD_REQUEST);
        }

        final String code = verificationService.generateAndSaveCode(user, VerificationCodeType.ACCOUNT_VERIFICATION);
        notificationService.sendAccountVerificationCode(user.getEmail(), user.getEmail().split("@")[0], code);
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

        if (!user.isActive()) {
            final String code = verificationService.generateAndSaveCode(user, VerificationCodeType.ACCOUNT_VERIFICATION);
            notificationService.sendAccountVerificationCode(user.getEmail(), user.getEmail().split("@")[0], code);
            throw new ApiException("Account is not verified. A new code has been sent to your email.", HttpStatus.FORBIDDEN);
        }

        final String accessToken = jwtService.generateAccessToken(user);
        final String refreshToken = jwtService.generateRefreshToken(user);

        cookieUtils.setTokenCookies(response, accessToken, refreshToken);

        final String fullName = (user.getFirstName() != null && user.getLastName() != null)
                ? user.getFirstName() + " " + user.getLastName()
                : user.getEmail().split("@")[0];

        return new LoginResponse(user.getId(), user.getEmail(), fullName, user.getRole().name());
    }

    @Override
    @Transactional(readOnly = true)
    public void refresh(final HttpServletRequest request, final HttpServletResponse response) {
        final String refreshToken = cookieUtils.extractCookie(request, "refresh_token")
                .orElseThrow(() -> new ApiException("Refresh token missing", HttpStatus.UNAUTHORIZED));

        if (!jwtService.isTokenValid(refreshToken)) {
            throw new ApiException("Invalid or expired refresh token.", HttpStatus.UNAUTHORIZED);
        }

        final String email = jwtService.extractEmail(refreshToken);
        final User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new ApiException("User not found.", HttpStatus.UNAUTHORIZED));

        final String newAccessToken = jwtService.generateAccessToken(user);
        cookieUtils.setTokenCookies(response, newAccessToken, refreshToken);
    }

    @Override
    public void logout(final HttpServletResponse response) {
        cookieUtils.clearTokenCookies(response);
    }
}
