package com.cinebh.api.security;

import com.cinebh.api.entities.User;
import io.jsonwebtoken.Claims;

public interface JwtService {
    String generateAccessToken(User user);

    String generateRefreshToken(User user);

    String generateRefreshToken(User user, boolean rememberMe);

    Claims extractClaims(String token);

    boolean isTokenValid(String token);

    String extractEmail(String token);
}
