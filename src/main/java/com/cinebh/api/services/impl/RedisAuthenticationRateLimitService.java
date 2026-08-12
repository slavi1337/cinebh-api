package com.cinebh.api.services.impl;

import com.cinebh.api.exceptions.ApiException;
import com.cinebh.api.services.AuthenticationRateLimitService;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.Locale;

@Service
@RequiredArgsConstructor
public class RedisAuthenticationRateLimitService implements AuthenticationRateLimitService {

    private static final Logger log = LoggerFactory.getLogger(RedisAuthenticationRateLimitService.class);
    private static final String FAILED_LOGIN_EMAIL_KEY_PREFIX = "auth:login:failed:";
    private static final String FAILED_LOGIN_IP_KEY_PREFIX = "auth:login:failed-ip:";
    private static final int MAX_FAILED_LOGIN_ATTEMPTS_PER_EMAIL = 5;
    private static final int MAX_FAILED_LOGIN_ATTEMPTS_PER_IP = 20;
    private static final Duration FAILED_LOGIN_WINDOW = Duration.ofMinutes(1);

    private final StringRedisTemplate redisTemplate;

    @Override
    public void assertLoginAllowed(final String email, final String clientIpAddress) {
        assertLimitAllowed(failedLoginEmailKey(email), MAX_FAILED_LOGIN_ATTEMPTS_PER_EMAIL);
        assertLimitAllowed(failedLoginIpKey(clientIpAddress), MAX_FAILED_LOGIN_ATTEMPTS_PER_IP);
    }

    @Override
    public void recordFailedLogin(final String email, final String clientIpAddress) {
        recordFailedAttempt(failedLoginEmailKey(email));
        recordFailedAttempt(failedLoginIpKey(clientIpAddress));
    }

    @Override
    public void clearFailedLoginAttempts(final String email) {
        final String key = failedLoginEmailKey(email);

        if (key == null) {
            return;
        }

        try {
            redisTemplate.delete(key);
        } catch (Exception exception) {
            log.warn("Failed to clear failed login attempts", exception);
        }
    }

    private void assertLimitAllowed(final String key, final int maxFailedAttempts) {
        if (key == null) {
            return;
        }

        try {
            final String value = redisTemplate.opsForValue().get(key);
            final int failedAttempts = value == null ? 0 : Integer.parseInt(value);

            if (failedAttempts >= maxFailedAttempts) {
                throw new ApiException(
                        "Too many failed login attempts. Please try again later.",
                        HttpStatus.TOO_MANY_REQUESTS
                );
            }
        } catch (ApiException exception) {
            throw exception;
        } catch (Exception exception) {
            log.warn("Failed to check login rate limit", exception);
        }
    }

    private void recordFailedAttempt(final String key) {
        if (key == null) {
            return;
        }

        try {
            final Long failedAttempts = redisTemplate.opsForValue().increment(key);

            if (Long.valueOf(1L).equals(failedAttempts)) {
                redisTemplate.expire(key, FAILED_LOGIN_WINDOW);
            }
        } catch (Exception exception) {
            log.warn("Failed to record failed login attempt", exception);
        }
    }

    private String failedLoginEmailKey(final String email) {
        if (email == null || email.isBlank()) {
            return null;
        }

        return FAILED_LOGIN_EMAIL_KEY_PREFIX + email.trim().toLowerCase(Locale.ROOT);
    }

    private String failedLoginIpKey(final String clientIpAddress) {
        if (clientIpAddress == null || clientIpAddress.isBlank()) {
            return null;
        }

        return FAILED_LOGIN_IP_KEY_PREFIX + clientIpAddress.trim().toLowerCase(Locale.ROOT);
    }
}
