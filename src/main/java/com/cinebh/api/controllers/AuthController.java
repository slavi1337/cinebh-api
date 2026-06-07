package com.cinebh.api.controllers;

import com.cinebh.api.dto.auth.LoginRequest;
import com.cinebh.api.dto.auth.LoginResponse;
import com.cinebh.api.dto.auth.RegisterRequest;
import com.cinebh.api.dto.auth.VerifyRequest;
import com.cinebh.api.services.AuthService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@Validated
@RequiredArgsConstructor
@RequestMapping("/api/v1/auth")
@Tag(name = "Authentication", description = "Public endpoints for user authentication and registration")
public class AuthController {

    private final AuthService authService;

    @PostMapping("/signup")
    @Operation(
            summary = "Register a new user",
            description = "Creates a new inactive user account and sends a verification code via email"
    )
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "User registered successfully, verification email sent"),
            @ApiResponse(
                    responseCode = "400",
                    description = "Invalid payload or validation failed. Possible reasons: invalid DTO format, duplicate email/phone, compromised password, " +
                            "invalid email domain (MX/TLD/Disposable), reserved keywords, or unreachable/invalid profile image URL."
            ),
            @ApiResponse(responseCode = "500", description = "Unexpected server error")
    })
    public ResponseEntity<Void> signup(
            @Valid @RequestBody final RegisterRequest registerRequest
    ) {
        authService.register(registerRequest);
        return ResponseEntity.status(HttpStatus.CREATED).build();
    }

    @PostMapping("/verify")
    @Operation(
            summary = "Verify user account",
            description = "Verifies account using the code sent via email"
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Account verified successfully"),
            @ApiResponse(responseCode = "400", description = "Invalid code, expired code, or account already verified"),
            @ApiResponse(responseCode = "404", description = "User not found"),
            @ApiResponse(responseCode = "500", description = "Unexpected server error")
    })
    public ResponseEntity<Void> verify(
            @Valid @RequestBody final VerifyRequest verifyRequest
    ) {
        authService.verify(verifyRequest);
        return ResponseEntity.ok().build();
    }

    @PostMapping("/login")
    @Operation(
            summary = "Authenticate user",
            description = "Authenticates user and sets HttpOnly JWT cookies"
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Login successful"),
            @ApiResponse(responseCode = "401", description = "Invalid credentials"),
            @ApiResponse(responseCode = "403", description = "Account not verified")
    })
    public ResponseEntity<LoginResponse> login(
            @Valid @RequestBody final LoginRequest loginRequest,
            final HttpServletResponse response
    ) {
        return ResponseEntity.ok(authService.login(loginRequest, response));
    }

    @GetMapping("/me")
    @Operation(
            summary = "Get current authenticated user",
            description = "Returns the currently authenticated user based on JWT cookie"
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Current user returned successfully"),
            @ApiResponse(responseCode = "401", description = "User not authenticated")
    })
    public ResponseEntity<LoginResponse> getCurrentUser() {
        return ResponseEntity.ok(authService.getCurrentUser());
    }

    @PostMapping("/logout")
    @Operation(
            summary = "Logout user",
            description = "Clears JWT authentication cookies"
    )
    public ResponseEntity<Void> logout(final HttpServletResponse response) {
        authService.logout(response);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/refresh")
    @Operation(
            summary = "Refresh tokens",
            description = "Issues a new access token using refresh token cookie"
    )
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "Tokens refreshed successfully"),
            @ApiResponse(responseCode = "401", description = "Invalid or missing refresh token")
    })
    public ResponseEntity<Void> refresh(final HttpServletRequest request, final HttpServletResponse response) {
        authService.refresh(request, response);
        return ResponseEntity.noContent().build();
    }
}
