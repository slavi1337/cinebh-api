package com.cinebh.api.controllers;

import com.cinebh.api.dto.profile.ChangePasswordRequest;
import com.cinebh.api.dto.profile.ProfileLocationOptionsResponse;
import com.cinebh.api.dto.profile.ProjectionHistoryStatus;
import com.cinebh.api.dto.profile.UpdateUserProfileRequest;
import com.cinebh.api.dto.profile.UserProfileResponse;
import com.cinebh.api.dto.profile.UserProjectionResponse;
import com.cinebh.api.services.AuthService;
import com.cinebh.api.services.UserProfileService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@RestController
@Validated
@RequiredArgsConstructor
@RequestMapping("/api/v1/users/me")
public class UserProfileController {

    private final UserProfileService userProfileService;
    private final AuthService authService;

    @GetMapping("/profile")
    public ResponseEntity<UserProfileResponse> getProfile() {
        return ResponseEntity.ok(userProfileService.getProfile());
    }

    @GetMapping("/profile/options")
    public ResponseEntity<ProfileLocationOptionsResponse> getLocationOptions() {
        return ResponseEntity.ok(userProfileService.getLocationOptions());
    }

    @PutMapping("/profile")
    public ResponseEntity<UserProfileResponse> updateProfile(
            @Valid @RequestBody final UpdateUserProfileRequest request
    ) {
        return ResponseEntity.ok(userProfileService.updateProfile(request));
    }

    @PostMapping(value = "/profile-image", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<UserProfileResponse> uploadProfileImage(
            @RequestPart("file") final MultipartFile file
    ) {
        return ResponseEntity.ok(userProfileService.uploadProfileImage(file));
    }

    @GetMapping("/profile-image")
    public ResponseEntity<Void> getProfileImage() {
        return ResponseEntity.status(HttpStatus.FOUND)
                .location(userProfileService.getProfileImageUri())
                .build();
    }

    @PutMapping("/password")
    public ResponseEntity<Void> changePassword(
            @Valid @RequestBody final ChangePasswordRequest request,
            final HttpServletRequest servletRequest,
            final HttpServletResponse servletResponse
    ) {
        userProfileService.changePassword(request);
        authService.logout(servletRequest, servletResponse);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/projections")
    public ResponseEntity<List<UserProjectionResponse>> getPurchasedProjections(
            @RequestParam(defaultValue = "UPCOMING") final ProjectionHistoryStatus status
    ) {
        return ResponseEntity.ok(userProfileService.getPurchasedProjections(status));
    }

    @DeleteMapping("/profile")
    public ResponseEntity<Void> deactivateProfile(
            final HttpServletRequest request,
            final HttpServletResponse response
    ) {
        userProfileService.deactivateCurrentUser();
        authService.logout(request, response);
        return ResponseEntity.noContent().build();
    }
}
