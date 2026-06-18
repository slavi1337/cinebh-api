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
    private static final String FAILED_LOGIN_KEY_PREFIX = "auth:login:failed:";
    private static final int MAX_FAILED_LOGIN_ATTEMPTS = 5;
    private static final Duration FAILED_LOGIN_WINDOW = Duration.ofMinutes(1);

    private final StringRedisTemplate redisTemplate;

    @Override
    public void assertLoginAllowed(final String email) {
        final String key = failedLoginKey(email);

        if (key == null) {
            return;
        }

        try {
            final String value = redisTemplate.opsForValue().get(key);
            final int failedAttempts = value == null ? 0 : Integer.parseInt(value);

            if (failedAttempts >= MAX_FAILED_LOGIN_ATTEMPTS) {
                throw new ApiException(
                        "Too many failed login attempts. Please try again later.",
                        HttpStatus.TOO_MANY_REQUESTS
                );
            }
        } catch (ApiException exception) {
            throw exception;
        } catch (Exception exception) {
            log.warn("Failed to check login rate limit for email={}", email, exception);
        }
    }

    @Override
    public void recordFailedLogin(final String email) {
        final String key = failedLoginKey(email);

        if (key == null) {
            return;
        }

        try {
            final Long failedAttempts = redisTemplate.opsForValue().increment(key);

            if (Long.valueOf(1L).equals(failedAttempts)) {
                redisTemplate.expire(key, FAILED_LOGIN_WINDOW);
            }
        } catch (Exception exception) {
            log.warn("Failed to record failed login attempt for email={}", email, exception);
        }
    }

    @Override
    public void clearFailedLoginAttempts(final String email) {
        final String key = failedLoginKey(email);

        if (key == null) {
            return;
        }

        try {
            redisTemplate.delete(key);
        } catch (Exception exception) {
            log.warn("Failed to clear failed login attempts for email={}", email, exception);
        }
    }

    private String failedLoginKey(final String email) {
        if (email == null || email.isBlank()) {
            return null;
        }

        return FAILED_LOGIN_KEY_PREFIX + email.trim().toLowerCase(Locale.ROOT);
    }
}
