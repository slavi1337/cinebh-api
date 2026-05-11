package com.cinebh.api.services;

public interface NotificationService {

    void sendAccountVerificationCode(String toEmail, String toName, String code);

    void sendPasswordResetCode(String toEmail, String toName, String code);
}
