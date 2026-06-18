package com.cinebh.api.services;

import com.cinebh.api.dto.profile.ChangePasswordRequest;
import com.cinebh.api.dto.profile.ProfileLocationOptionsResponse;
import com.cinebh.api.dto.profile.UpdateUserProfileRequest;
import com.cinebh.api.dto.profile.UserProfileResponse;
import com.cinebh.api.dto.profile.UserProjectionResponse;
import com.cinebh.api.services.storage.StoredFile;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

public interface UserProfileService {

    UserProfileResponse getProfile();

    ProfileLocationOptionsResponse getLocationOptions();

    UserProfileResponse updateProfile(UpdateUserProfileRequest request);

    UserProfileResponse uploadProfileImage(MultipartFile file);

    StoredFile getProfileImage();

    void changePassword(ChangePasswordRequest request);

    void deactivateCurrentUser();

    List<UserProjectionResponse> getPurchasedProjections(String status);
}
