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
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RedisAuthenticationRateLimitServiceTest {

    private static final String EMAIL = "Test@Cinebh.com";
    private static final String CLIENT_IP_ADDRESS = "203.0.113.10";
    private static final String EMAIL_REDIS_KEY = "auth:login:failed:test@cinebh.com";
    private static final String IP_REDIS_KEY = "auth:login:failed-ip:203.0.113.10";

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
        when(valueOperations.get(EMAIL_REDIS_KEY)).thenReturn("4");
        when(valueOperations.get(IP_REDIS_KEY)).thenReturn("19");

        rateLimitService.assertLoginAllowed(EMAIL, CLIENT_IP_ADDRESS);
    }

    @Test
    void shouldRejectLoginWhenEmailLimitIsReached() {
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.get(EMAIL_REDIS_KEY)).thenReturn("5");

        assertThatThrownBy(() -> rateLimitService.assertLoginAllowed(EMAIL, CLIENT_IP_ADDRESS))
                .isInstanceOf(ApiException.class)
                .hasFieldOrPropertyWithValue("status", HttpStatus.TOO_MANY_REQUESTS)
                .hasMessage("Too many failed login attempts. Please try again later.");
    }

    @Test
    void shouldRejectLoginWhenIpLimitIsReached() {
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.get(EMAIL_REDIS_KEY)).thenReturn("4");
        when(valueOperations.get(IP_REDIS_KEY)).thenReturn("20");

        assertThatThrownBy(() -> rateLimitService.assertLoginAllowed(EMAIL, CLIENT_IP_ADDRESS))
                .isInstanceOf(ApiException.class)
                .hasFieldOrPropertyWithValue("status", HttpStatus.TOO_MANY_REQUESTS)
                .hasMessage("Too many failed login attempts. Please try again later.");
    }

    @Test
    void shouldExpireBothCountersWhenRecordingFirstFailedLogin() {
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.increment(EMAIL_REDIS_KEY)).thenReturn(1L);
        when(valueOperations.increment(IP_REDIS_KEY)).thenReturn(1L);

        rateLimitService.recordFailedLogin(EMAIL, CLIENT_IP_ADDRESS);

        verify(redisTemplate).expire(EMAIL_REDIS_KEY, Duration.ofMinutes(1));
        verify(redisTemplate).expire(IP_REDIS_KEY, Duration.ofMinutes(1));
    }

    @Test
    void shouldClearOnlyEmailFailedLoginAttempts() {
        rateLimitService.clearFailedLoginAttempts(EMAIL);

        verify(redisTemplate).delete(EMAIL_REDIS_KEY);
        verify(redisTemplate, never()).delete(IP_REDIS_KEY);
    }
}
