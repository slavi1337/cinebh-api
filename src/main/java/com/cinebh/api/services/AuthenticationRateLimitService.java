package com.cinebh.api.services;

public interface AuthenticationRateLimitService {

    void assertLoginAllowed(String email, String clientIpAddress);

    void recordFailedLogin(String email, String clientIpAddress);

    void clearFailedLoginAttempts(String email);
}
