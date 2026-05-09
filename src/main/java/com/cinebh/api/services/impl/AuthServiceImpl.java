package com.cinebh.api.services.impl;

import com.cinebh.api.dto.auth.RegisterRequest;
import com.cinebh.api.dto.auth.VerifyRequest;
import com.cinebh.api.entities.User;
import com.cinebh.api.entities.enums.UserRole;
import com.cinebh.api.entities.enums.VerificationCodeType;
import com.cinebh.api.exceptions.ApiException;
import com.cinebh.api.repositories.UserRepository;
import com.cinebh.api.services.AuthService;
import com.cinebh.api.services.NotificationService;
import com.cinebh.api.services.VerificationService;
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

    @Override
    @Transactional
    public void register(final RegisterRequest request) {
        userRepository.findByEmail(request.email()).ifPresent(existingUser -> {
            if (Boolean.TRUE.equals(existingUser.getIsActive())) {
                throw new ApiException("Email is already in use.", HttpStatus.BAD_REQUEST);
            } else {
                throw new ApiException("Account already exists but is not verified. Please proceed to login to receive a new verification code.", HttpStatus.BAD_REQUEST);
            }
        });

        final User user = new User();
        user.setId(UUID.randomUUID());
        user.setEmail(request.email());
        user.setPasswordHash(passwordEncoder.encode(request.password()));
        user.setRole(UserRole.CUSTOMER);
        user.setIsActive(false);
        user.setCreatedAt(OffsetDateTime.now());

        try {
            userRepository.saveAndFlush(user);
        } catch (org.springframework.dao.DataIntegrityViolationException exception) {
            throw new ApiException("Email is already in use. If unverified, please proceed to login.", HttpStatus.BAD_REQUEST);
        }

        final String code = verificationService.generateAndSaveCode(user, VerificationCodeType.ACCOUNT_VERIFICATION);

        notificationService.sendAccountVerificationCode(user.getEmail(), null, code);
    }

    @Override
    @Transactional
    public void verify(final VerifyRequest request) {
        final User user = userRepository.findByEmail(request.email())
                .orElseThrow(() -> new ApiException("User not found.", HttpStatus.NOT_FOUND));

        if (Boolean.TRUE.equals(user.getIsActive())) {
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

        user.setIsActive(true);
        user.setUpdatedAt(OffsetDateTime.now());
        userRepository.save(user);
    }
}
