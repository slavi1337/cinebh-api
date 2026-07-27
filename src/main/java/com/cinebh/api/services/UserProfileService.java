package com.cinebh.api.services;

import com.cinebh.api.dto.profile.ChangePasswordRequest;
import com.cinebh.api.dto.profile.ProfileLocationOptionsResponse;
import com.cinebh.api.dto.profile.ProjectionHistoryStatus;
import com.cinebh.api.dto.profile.UpdateUserProfileRequest;
import com.cinebh.api.dto.profile.UserProfileResponse;
import com.cinebh.api.dto.profile.UserProjectionResponse;
import org.springframework.web.multipart.MultipartFile;

import java.net.URI;
import java.util.List;

public interface UserProfileService {

    UserProfileResponse getProfile();

    ProfileLocationOptionsResponse getLocationOptions();

    UserProfileResponse updateProfile(UpdateUserProfileRequest request);

    UserProfileResponse uploadProfileImage(MultipartFile file);

    URI getProfileImageUri();

    void changePassword(ChangePasswordRequest request);

    void deactivateCurrentUser();

    List<UserProjectionResponse> getPurchasedProjections(ProjectionHistoryStatus status);
}
