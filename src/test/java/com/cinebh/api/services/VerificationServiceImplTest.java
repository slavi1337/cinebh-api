package com.cinebh.api.services;

import com.cinebh.api.config.NotificationProperties;
import com.cinebh.api.config.VerificationProperties;
import com.cinebh.api.entities.User;
import com.cinebh.api.entities.VerificationCode;
import com.cinebh.api.entities.enums.VerificationCodeType;
import com.cinebh.api.repositories.VerificationCodeRepository;
import com.cinebh.api.services.impl.VerificationServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.OffsetDateTime;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class VerificationServiceImplTest {

    @Mock
    private VerificationCodeRepository verificationCodeRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private VerificationProperties verificationProperties;

    @InjectMocks
    private VerificationServiceImpl verificationService;

    @Captor
    private ArgumentCaptor<VerificationCode> codeCaptor;

    private User testUser;

    @BeforeEach
    void setUp() {
        testUser = new User();
        testUser.setId(UUID.randomUUID());
        testUser.setEmail("test@cinebh.com");
    }

    @Test
    void shouldGenerateAndSaveCodeSuccessfully() {
        when(verificationProperties.getCodeLength()).thenReturn(8);
        when(verificationProperties.getCodeTtlMinutes()).thenReturn(15);
        when(passwordEncoder.encode(any(String.class))).thenReturn("hashed-code");

        final String generatedCode = verificationService.generateAndSaveCode(testUser, VerificationCodeType.ACCOUNT_VERIFICATION);

        assertThat(generatedCode).hasSize(8).matches("\\d+");

        verify(verificationCodeRepository).invalidateAllPendingCodes(testUser.getId(), VerificationCodeType.ACCOUNT_VERIFICATION);
        verify(verificationCodeRepository).save(codeCaptor.capture());

        final VerificationCode savedCode = codeCaptor.getValue();
        assertThat(savedCode.getUser()).isEqualTo(testUser);
        assertThat(savedCode.getType()).isEqualTo(VerificationCodeType.ACCOUNT_VERIFICATION);
        assertThat(savedCode.getCodeHash()).isEqualTo("hashed-code");
        assertThat(savedCode.getIsUsed()).isFalse();
    }

    @Test
    void shouldReturnFalseWhenNoValidCodeExists() {
        when(verificationCodeRepository.findLatestValidCode(testUser.getId(), VerificationCodeType.ACCOUNT_VERIFICATION))
                .thenReturn(Optional.empty());

        final boolean result = verificationService.verifyCode(testUser, VerificationCodeType.ACCOUNT_VERIFICATION, "123456");

        assertThat(result).isFalse();
    }

    @Test
    void shouldReturnFalseAndInvalidateWhenCodeIsExpired() {
        final VerificationCode expiredCode = new VerificationCode();
        expiredCode.setExpiresAt(OffsetDateTime.now().minusMinutes(1));
        expiredCode.setIsUsed(false);

        when(verificationCodeRepository.findLatestValidCode(testUser.getId(), VerificationCodeType.ACCOUNT_VERIFICATION))
                .thenReturn(Optional.of(expiredCode));

        final boolean result = verificationService.verifyCode(testUser, VerificationCodeType.ACCOUNT_VERIFICATION, "123456");

        assertThat(result).isFalse();
        verify(verificationCodeRepository).save(codeCaptor.capture());
        assertThat(codeCaptor.getValue().getIsUsed()).isTrue();
    }

    @Test
    void shouldReturnFalseWhenCodeDoesNotMatch() {
        final VerificationCode validCode = new VerificationCode();
        validCode.setExpiresAt(OffsetDateTime.now().plusMinutes(15));
        validCode.setCodeHash("hashed-code");

        when(verificationCodeRepository.findLatestValidCode(testUser.getId(), VerificationCodeType.ACCOUNT_VERIFICATION))
                .thenReturn(Optional.of(validCode));
        when(passwordEncoder.matches("wrong-code", "hashed-code")).thenReturn(false);

        final boolean result = verificationService.verifyCode(testUser, VerificationCodeType.ACCOUNT_VERIFICATION, "wrong-code");

        assertThat(result).isFalse();
    }

    @Test
    void shouldReturnTrueAndInvalidateWhenCodeIsCorrect() {
        final VerificationCode validCode = new VerificationCode();
        validCode.setExpiresAt(OffsetDateTime.now().plusMinutes(15));
        validCode.setCodeHash("hashed-code");
        validCode.setIsUsed(false);

        when(verificationCodeRepository.findLatestValidCode(testUser.getId(), VerificationCodeType.ACCOUNT_VERIFICATION))
                .thenReturn(Optional.of(validCode));
        when(passwordEncoder.matches("123456", "hashed-code")).thenReturn(true);

        final boolean result = verificationService.verifyCode(testUser, VerificationCodeType.ACCOUNT_VERIFICATION, "123456");

        assertThat(result).isTrue();
        verify(verificationCodeRepository).save(codeCaptor.capture());
        assertThat(codeCaptor.getValue().getIsUsed()).isTrue();
    }
}
