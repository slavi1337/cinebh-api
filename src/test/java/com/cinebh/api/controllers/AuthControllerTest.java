package com.cinebh.api.controllers;

import com.cinebh.api.services.AuthService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static com.cinebh.api.support.ControllerTestUtils.expectErrorResponse;
import static com.cinebh.api.support.ControllerTestUtils.standaloneMockMvc;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class AuthControllerTest {

    private static final String SIGNUP_URL = "/api/v1/auth/signup";
    private static final String LOGIN_URL = "/api/v1/auth/login";

    @Mock
    private AuthService authService;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = standaloneMockMvc(new AuthController(authService));
    }

    @Test
    void shouldReturnBadRequestWhenSignupCityIdHasInvalidFormat() throws Exception {
        final String requestBody = """
                {
                  "email": "testuser@gmail.com",
                  "password": "TheZenica123!",
                  "firstName": "Kerim",
                  "lastName": "Awad",
                  "phone": "+38763341456",
                  "profileImageUrl": "https://placehold.co/600x400",
                  "cityId": "abc",
                  "streetAddress": "Marsala Tita 12"
                }
                """;

        expectErrorResponse(
                mockMvc.perform(post(SIGNUP_URL)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(requestBody))
                        .andExpect(status().isBadRequest()),
                "Invalid request body format.",
                400
        );
        verifyNoInteractions(authService);
    }

    @Test
    void shouldReturnBadRequestWhenLoginBodyIsEmpty() throws Exception {
        expectErrorResponse(
                mockMvc.perform(post(LOGIN_URL)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("{}"))
                        .andExpect(status().isBadRequest()),
                "Validation failed",
                400
        );
        verifyNoInteractions(authService);
    }
}
