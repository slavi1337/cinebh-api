package com.cinebh.api.dto.auth;

import java.util.UUID;

public record LoginResponse(
        UUID id,
        String email,
        String fullName,
        String role
) {
}
