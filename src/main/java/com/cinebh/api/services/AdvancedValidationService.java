package com.cinebh.api.services;

public interface AdvancedValidationService {
    void validateEmailDomain(String email);

    void validatePasswordPwned(String password);

    void validatePhone(String phone);

    void validateNameNotReserved(String firstName, String lastName);

    void validateImageUrl(String imageUrl);
}
