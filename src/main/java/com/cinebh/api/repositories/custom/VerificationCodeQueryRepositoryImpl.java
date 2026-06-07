package com.cinebh.api.repositories.custom;

import com.cinebh.api.entities.QVerificationCode;
import com.cinebh.api.entities.VerificationCode;
import com.cinebh.api.entities.enums.VerificationCodeType;
import com.querydsl.jpa.impl.JPAQueryFactory;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
@RequiredArgsConstructor
public class VerificationCodeQueryRepositoryImpl implements VerificationCodeQueryRepository {

    private final JPAQueryFactory queryFactory;
    private final QVerificationCode verificationCode = QVerificationCode.verificationCode;

    @Override
    public Optional<VerificationCode> findLatestValidCode(final UUID userId, final VerificationCodeType type) {
        return Optional.ofNullable(
                queryFactory.selectFrom(verificationCode)
                        .where(verificationCode.user.id.eq(userId)
                                .and(verificationCode.type.eq(type))
                                .and(verificationCode.isUsed.isFalse()))
                        .orderBy(verificationCode.createdAt.desc())
                        .fetchFirst()
        );
    }

    @Override
    public void invalidateAllPendingCodes(final UUID userId, final VerificationCodeType type) {
        queryFactory.update(verificationCode)
                .set(verificationCode.isUsed, true)
                .where(verificationCode.user.id.eq(userId)
                        .and(verificationCode.type.eq(type))
                        .and(verificationCode.isUsed.isFalse()))
                .execute();
    }
}
