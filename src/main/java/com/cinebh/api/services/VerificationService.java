package com.cinebh.api.services;

import com.cinebh.api.entities.User;
import com.cinebh.api.entities.enums.VerificationCodeType;

public interface VerificationService {

    String generateAndSaveCode(User user, VerificationCodeType type);

    boolean verifyCode(User user, VerificationCodeType type, String rawCode);
}
