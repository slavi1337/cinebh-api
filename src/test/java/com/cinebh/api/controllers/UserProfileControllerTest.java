package com.cinebh.api.controllers;

import com.cinebh.api.dto.profile.ChangePasswordRequest;
import com.cinebh.api.dto.profile.ProfileLocationOptionsResponse;
import com.cinebh.api.dto.profile.UpdateUserProfileRequest;
import com.cinebh.api.dto.profile.UserProfileResponse;
import com.cinebh.api.services.AuthService;
import com.cinebh.api.services.UserProfileService;
import com.cinebh.api.services.storage.StoredFile;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.UUID;

import static com.cinebh.api.support.ControllerTestUtils.getJson;
import static com.cinebh.api.support.ControllerTestUtils.standaloneMockMvc;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class UserProfileControllerTest {

    private static final String PROFILE_URL = "/api/v1/users/me/profile";
    private static final String PROFILE_OPTIONS_URL = "/api/v1/users/me/profile/options";
    private static final String PROFILE_IMAGE_URL = "/api/v1/users/me/profile-image";
    private static final String PASSWORD_URL = "/api/v1/users/me/password";
    private static final String PROJECTIONS_URL = "/api/v1/users/me/projections";

    @Mock
    private UserProfileService userProfileService;

    @Mock
    private AuthService authService;

    private MockMvc mockMvc;
    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        mockMvc = standaloneMockMvc(new UserProfileController(userProfileService, authService));
        objectMapper = new ObjectMapper();
    }

    @Test
    void shouldReturnProfile() throws Exception {
        final UserProfileResponse response = profileResponse();

        when(userProfileService.getProfile()).thenReturn(response);

        mockMvc.perform(getJson(PROFILE_URL))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.email").value("slavisa@example.com"))
                .andExpect(jsonPath("$.cityName").value("Sarajevo"));

        verify(userProfileService).getProfile();
    }

    @Test
    void shouldReturnProfileLocationOptions() throws Exception {
        when(userProfileService.getLocationOptions())
                .thenReturn(new ProfileLocationOptionsResponse(List.of()));

        mockMvc.perform(getJson(PROFILE_OPTIONS_URL))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.countries").isArray());

        verify(userProfileService).getLocationOptions();
    }

    @Test
    void shouldUpdateProfile() throws Exception {
        final UpdateUserProfileRequest request = new UpdateUserProfileRequest(
                "Slavisa",
                "Covakusic",
                "+38761123456",
                UUID.randomUUID(),
                null
        );
        final UserProfileResponse response = profileResponse();

        when(userProfileService.updateProfile(any(UpdateUserProfileRequest.class))).thenReturn(response);

        mockMvc.perform(put(PROFILE_URL)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.email").value("slavisa@example.com"));

        verify(userProfileService).updateProfile(any(UpdateUserProfileRequest.class));
    }

    @Test
    void shouldUploadProfileImage() throws Exception {
        final MockMultipartFile file = new MockMultipartFile(
                "file",
                "avatar.png",
                "image/png",
                "image".getBytes()
        );

        when(userProfileService.uploadProfileImage(any(MultipartFile.class))).thenReturn(profileResponse());

        mockMvc.perform(multipart(PROFILE_IMAGE_URL).file(file))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.profileImageUrl").value("https://cdn.cinebh.com/avatar.png"));

        verify(userProfileService).uploadProfileImage(any(MultipartFile.class));
    }

    @Test
    void shouldReturnProfileImage() throws Exception {
        when(userProfileService.getProfileImage()).thenReturn(new StoredFile(
                "image/png",
                "image".getBytes()
        ));

        mockMvc.perform(getJson(PROFILE_IMAGE_URL))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.IMAGE_PNG))
                .andExpect(content().bytes("image".getBytes()));

        verify(userProfileService).getProfileImage();
    }

    @Test
    void shouldChangePassword() throws Exception {
        final ChangePasswordRequest request = new ChangePasswordRequest(
                "OldPassword123",
                "NewPassword123",
                "NewPassword123"
        );

        mockMvc.perform(put(PASSWORD_URL)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isNoContent());

        verify(userProfileService).changePassword(any(ChangePasswordRequest.class));
    }

    @Test
    void shouldReturnPurchasedProjections() throws Exception {
        when(userProfileService.getPurchasedProjections("past")).thenReturn(List.of());

        mockMvc.perform(getJson(PROJECTIONS_URL).param("status", "past"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray());

        verify(userProfileService).getPurchasedProjections("past");
    }

    @Test
    void shouldDeactivateProfileAndLogout() throws Exception {
        mockMvc.perform(delete(PROFILE_URL))
                .andExpect(status().isNoContent());

        verify(userProfileService).deactivateCurrentUser();
        verify(authService).logout(any(HttpServletRequest.class), any(HttpServletResponse.class));
    }

    private UserProfileResponse profileResponse() {
        return new UserProfileResponse(
                UUID.randomUUID(),
                "Slavisa",
                "Covakusic",
                "slavisa@example.com",
                "+38761123456",
                "https://cdn.cinebh.com/avatar.png",
                UUID.randomUUID(),
                "Sarajevo",
                "Bosnia and Herzegovina",
                null
        );
    }
}
