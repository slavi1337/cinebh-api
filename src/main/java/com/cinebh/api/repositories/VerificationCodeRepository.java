package com.cinebh.api.repositories;

import com.cinebh.api.entities.VerificationCode;
import com.cinebh.api.repositories.custom.VerificationCodeQueryRepository;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
public interface VerificationCodeRepository extends JpaRepository<VerificationCode, UUID>, VerificationCodeQueryRepository {
}
