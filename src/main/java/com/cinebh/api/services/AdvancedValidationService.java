package com.cinebh.api.services;

public interface AdvancedValidationService {
    void validateEmailDomain(String email);

    void validatePasswordPwned(String password);
}
