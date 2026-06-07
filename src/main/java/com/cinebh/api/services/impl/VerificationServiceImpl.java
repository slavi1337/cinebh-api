package com.cinebh.api.services.impl;

import com.cinebh.api.entities.User;
import com.cinebh.api.entities.VerificationCode;
import com.cinebh.api.entities.enums.VerificationCodeType;
import com.cinebh.api.repositories.VerificationCodeRepository;
import com.cinebh.api.services.VerificationService;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.time.OffsetDateTime;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class VerificationServiceImpl implements VerificationService {

    private static final SecureRandom SECURE_RANDOM = new SecureRandom();

    private final VerificationCodeRepository verificationCodeRepository;
    private final PasswordEncoder passwordEncoder;

    @Value("${app.verification.code-length:6}")
    private int codeLength;

    @Value("${app.verification.code-ttl-minutes:15}")
    private int codeTtlMinutes;

    @Override
    @Transactional
    public String generateAndSaveCode(final User user, final VerificationCodeType type) {
        verificationCodeRepository.invalidateAllPendingCodes(user.getId(), type);

        final String rawCode = generateRandomDigits();
        final String hashedCode = passwordEncoder.encode(rawCode);

        final VerificationCode code = new VerificationCode();
        code.setId(UUID.randomUUID());
        code.setUser(user);
        code.setType(type);
        code.setCodeHash(hashedCode);
        code.setUsed(false);
        code.setCreatedAt(OffsetDateTime.now());
        code.setExpiresAt(OffsetDateTime.now().plusMinutes(codeTtlMinutes));

        verificationCodeRepository.save(code);

        return rawCode;
    }

    @Override
    @Transactional
    public boolean verifyCode(final User user, final VerificationCodeType type, final String rawCode) {
        final Optional<VerificationCode> activeCodeOptional =
                verificationCodeRepository.findLatestValidCode(user.getId(), type);

        if (activeCodeOptional.isEmpty()) {
            return false;
        }

        final VerificationCode code = activeCodeOptional.get();

        if (code.getExpiresAt().isBefore(OffsetDateTime.now())) {
            code.setUsed(true);
            verificationCodeRepository.save(code);
            return false;
        }

        if (!passwordEncoder.matches(rawCode, code.getCodeHash())) {
            return false;
        }

        code.setUsed(true);
        verificationCodeRepository.save(code);
        return true;
    }

    private String generateRandomDigits() {
        return SECURE_RANDOM.ints(codeLength, 0, 10)
                .mapToObj(String::valueOf)
                .collect(Collectors.joining());
    }
}
