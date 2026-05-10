package com.cinebh.api.security.impl;

import com.cinebh.api.config.SecurityProperties;
import com.cinebh.api.entities.User;
import com.cinebh.api.security.JwtService;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class JwtServiceImpl implements JwtService {

    private final SecurityProperties securityProperties;

    @Override
    public String generateAccessToken(final User user) {
        return buildToken(
                Map.of("role", user.getRole().name()),
                user.getEmail(),
                securityProperties.getJwt().getAccessTokenExpirationMs()
        );
    }

    @Override
    public String generateRefreshToken(final User user) {
        return buildToken(Map.of(), user.getEmail(), securityProperties.getJwt().getRefreshTokenExpirationMs());
    }

    @Override
    public String extractEmail(final String token) {
        return extractClaims(token).getSubject();
    }

    @Override
    public boolean isTokenValid(final String token) {
        try {
            return !extractClaims(token).getExpiration().before(new Date());
        } catch (Exception e) {
            return false;
        }
    }

    @Override
    public Claims extractClaims(final String token) {
        return Jwts.parser()
                .verifyWith(getSigningKey())
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    private String buildToken(final Map<String, Object> extraClaims, final String subject, final long expiration) {
        return Jwts.builder()
                .claims(extraClaims)
                .subject(subject)
                .issuedAt(new Date(System.currentTimeMillis()))
                .expiration(new Date(System.currentTimeMillis() + expiration))
                .signWith(getSigningKey())
                .compact();
    }

    private SecretKey getSigningKey() {
        final byte[] keyBytes = securityProperties.getJwt().getSecret().getBytes(StandardCharsets.UTF_8);
        return Keys.hmacShaKeyFor(keyBytes);
    }
}
