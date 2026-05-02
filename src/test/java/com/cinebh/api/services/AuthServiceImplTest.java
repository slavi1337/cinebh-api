package com.cinebh.api.services;

import com.cinebh.api.dto.auth.RegisterRequest;
import com.cinebh.api.dto.auth.VerifyRequest;
import com.cinebh.api.entities.User;
import com.cinebh.api.entities.enums.VerificationCodeType;
import com.cinebh.api.exceptions.ApiException;
import com.cinebh.api.repositories.UserRepository;
import com.cinebh.api.services.impl.AuthServiceImpl;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AuthServiceImplTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private VerificationService verificationService;

    @Mock
    private NotificationService notificationService;

    @InjectMocks
    private AuthServiceImpl authService;

    @Test
    void shouldRegisterUserSuccessfully() {
        final RegisterRequest request = new RegisterRequest("test@cinebh.com", "Password123");
        when(userRepository.existsByEmail(request.email())).thenReturn(false);
        when(passwordEncoder.encode(request.password())).thenReturn("hashed-password");
        when(verificationService.generateAndSaveCode(any(User.class), eq(VerificationCodeType.ACCOUNT_VERIFICATION)))
                .thenReturn("123456");

        authService.register(request);

        final ArgumentCaptor<User> userCaptor = ArgumentCaptor.forClass(User.class);
        verify(userRepository).save(userCaptor.capture());

        final User savedUser = userCaptor.getValue();
        assertThat(savedUser.getEmail()).isEqualTo("test@cinebh.com");
        assertThat(savedUser.getIsActive()).isFalse();
        assertThat(savedUser.getPasswordHash()).isEqualTo("hashed-password");

        verify(notificationService).sendAccountVerificationCode(eq("test@cinebh.com"), eq(null), eq("123456"));
    }

    @Test
    void shouldThrowExceptionWhenEmailAlreadyExists() {
        final RegisterRequest request = new RegisterRequest("existing@cinebh.com", "Password123");
        when(userRepository.existsByEmail(request.email())).thenReturn(true);

        assertThatThrownBy(() -> authService.register(request))
                .isInstanceOf(ApiException.class)
                .hasFieldOrPropertyWithValue("status", HttpStatus.BAD_REQUEST)
                .hasMessage("Email is already in use.");
    }

    @Test
    void shouldVerifyUserSuccessfully() {
        final VerifyRequest request = new VerifyRequest("test@cinebh.com", "123456");
        final User user = new User();
        user.setEmail(request.email());
        user.setIsActive(false);

        when(userRepository.findByEmail(request.email())).thenReturn(Optional.of(user));
        when(verificationService.verifyCode(user, VerificationCodeType.ACCOUNT_VERIFICATION, "123456"))
                .thenReturn(true);

        authService.verify(request);

        assertThat(user.getIsActive()).isTrue();
        verify(userRepository).save(user);
    }

    @Test
    void shouldThrowExceptionWhenVerifyCodeIsInvalid() {
        final VerifyRequest request = new VerifyRequest("test@cinebh.com", "000000");
        final User user = new User();
        user.setEmail(request.email());
        user.setIsActive(false);

        when(userRepository.findByEmail(request.email())).thenReturn(Optional.of(user));
        when(verificationService.verifyCode(user, VerificationCodeType.ACCOUNT_VERIFICATION, "000000"))
                .thenReturn(false);

        assertThatThrownBy(() -> authService.verify(request))
                .isInstanceOf(ApiException.class)
                .hasMessage("Invalid or expired verification code.");
    }
}
