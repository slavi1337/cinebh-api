package com.cinebh.api.repositories.custom;

import com.cinebh.api.entities.VerificationCode;
import com.cinebh.api.entities.enums.VerificationCodeType;

import java.util.Optional;
import java.util.UUID;

public interface VerificationCodeQueryRepository {

    Optional<VerificationCode> findLatestValidCode(UUID userId, VerificationCodeType type);

    void invalidateAllPendingCodes(UUID userId, VerificationCodeType type);
}
