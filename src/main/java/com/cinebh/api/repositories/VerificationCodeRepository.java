package com.cinebh.api.repositories;

import com.cinebh.api.entities.VerificationCode;
import com.cinebh.api.entities.enums.VerificationCodeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface VerificationCodeRepository extends JpaRepository<VerificationCode, UUID> {

    @Query("""
            SELECT vc FROM VerificationCode vc 
            WHERE vc.user.id = :userId 
            AND vc.type = :type 
            AND vc.isUsed = false 
            ORDER BY vc.createdAt DESC 
            LIMIT 1
            """)
    Optional<VerificationCode> findLatestValidCode(
            @Param("userId") UUID userId,
            @Param("type") VerificationCodeType type
    );

    @Modifying
    @Query("""
            UPDATE VerificationCode vc 
            SET vc.isUsed = true 
            WHERE vc.user.id = :userId AND vc.type = :type AND vc.isUsed = false
            """)
    void invalidateAllPendingCodes(
            @Param("userId") UUID userId,
            @Param("type") VerificationCodeType type
    );
}
