package com.cinebh.api.services;

import com.cinebh.api.exceptions.ApiException;
import com.cinebh.api.services.impl.RedisAuthenticationRateLimitService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.http.HttpStatus;

import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RedisAuthenticationRateLimitServiceTest {

    private static final String EMAIL = "Test@Cinebh.com";
    private static final String REDIS_KEY = "auth:login:failed:test@cinebh.com";

    @Mock
    private StringRedisTemplate redisTemplate;

    @Mock
    private ValueOperations<String, String> valueOperations;

    private RedisAuthenticationRateLimitService rateLimitService;

    @BeforeEach
    void setUp() {
        rateLimitService = new RedisAuthenticationRateLimitService(redisTemplate);
    }

    @Test
    void shouldAllowLoginWhenFailedAttemptCountIsBelowLimit() {
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.get(REDIS_KEY)).thenReturn("4");

        rateLimitService.assertLoginAllowed(EMAIL);
    }

    @Test
    void shouldRejectLoginWhenFailedAttemptLimitIsReached() {
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.get(REDIS_KEY)).thenReturn("5");

        assertThatThrownBy(() -> rateLimitService.assertLoginAllowed(EMAIL))
                .isInstanceOf(ApiException.class)
                .hasFieldOrPropertyWithValue("status", HttpStatus.TOO_MANY_REQUESTS)
                .hasMessage("Too many failed login attempts. Please try again later.");
    }

    @Test
    void shouldExpireCounterWhenRecordingFirstFailedLogin() {
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.increment(REDIS_KEY)).thenReturn(1L);

        rateLimitService.recordFailedLogin(EMAIL);

        verify(redisTemplate).expire(REDIS_KEY, Duration.ofMinutes(1));
    }

    @Test
    void shouldClearFailedLoginAttempts() {
        rateLimitService.clearFailedLoginAttempts(EMAIL);

        verify(redisTemplate).delete(REDIS_KEY);
    }
}
