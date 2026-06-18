package com.cinebh.api.services;

public interface AuthenticationRateLimitService {

    void assertLoginAllowed(String email);

    void recordFailedLogin(String email);

    void clearFailedLoginAttempts(String email);
}
