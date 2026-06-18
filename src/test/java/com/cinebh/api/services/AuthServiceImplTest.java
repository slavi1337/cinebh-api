package com.cinebh.api.services;

import com.cinebh.api.dto.auth.LoginRequest;
import com.cinebh.api.dto.auth.LoginResponse;
import com.cinebh.api.dto.auth.RegisterRequest;
import com.cinebh.api.dto.auth.VerifyRequest;
import com.cinebh.api.entities.City;
import com.cinebh.api.entities.User;
import com.cinebh.api.entities.enums.UserRole;
import com.cinebh.api.entities.enums.VerificationCodeType;
import com.cinebh.api.exceptions.ApiException;
import com.cinebh.api.repositories.CityRepository;
import com.cinebh.api.repositories.UserRepository;
import com.cinebh.api.security.JwtService;
import com.cinebh.api.services.impl.AuthServiceImpl;
import com.cinebh.api.utils.CookieUtils;
import io.jsonwebtoken.Claims;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.TestingAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AuthServiceImplTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private CityRepository cityRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private VerificationService verificationService;

    @Mock
    private NotificationService notificationService;

    @Mock
    private AdvancedValidationService advancedValidationService;

    @Mock
    private AddressValidationService addressValidationService;

    @Mock
    private JwtService jwtService;

    @Mock
    private CookieUtils cookieUtils;

    @Mock
    private AuthenticationRateLimitService authenticationRateLimitService;

    @InjectMocks
    private AuthServiceImpl authService;

    private User testUser;

    @BeforeEach
    void setUp() {
        testUser = new User();
        testUser.setId(UUID.randomUUID());
        testUser.setEmail("tes213t@gmail.com");
        testUser.setPasswordHash("hashed-password");
        testUser.setRole(UserRole.CUSTOMER);
        testUser.setActive(true);
    }

    private RegisterRequest createRegisterRequest(String email, UUID cityId) {
        return new RegisterRequest(
                email,
                "Password123912831280398",
                "Slavisa",
                "Tester",
                "+38761123456",
                "https://placehold.co/600x400",
                cityId,
                "Ferhadija 1"
        );
    }

    @Test
    void shouldRegisterUserSuccessfully() {
        final UUID cityId = UUID.randomUUID();
        final RegisterRequest request = createRegisterRequest("tes213t@gmail.com", cityId);
        final City city = new City();

        when(cityRepository.findById(cityId)).thenReturn(Optional.of(city));
        when(addressValidationService.isValidStreetInCity(city.getName(), request.streetAddress())).thenReturn(true);
        when(userRepository.findByEmail(request.email())).thenReturn(Optional.empty());
        when(passwordEncoder.encode(request.password())).thenReturn("hashed-password");
        when(verificationService.generateAndSaveCode(any(User.class), eq(VerificationCodeType.ACCOUNT_VERIFICATION)))
                .thenReturn("123456");

        authService.register(request);

        final ArgumentCaptor<User> userCaptor = ArgumentCaptor.forClass(User.class);
        verify(userRepository).saveAndFlush(userCaptor.capture());

        final User savedUser = userCaptor.getValue();
        assertThat(savedUser.getEmail()).isEqualTo("tes213t@gmail.com");
        assertThat(savedUser.isActive()).isFalse();
        assertThat(savedUser.getPasswordHash()).isEqualTo("hashed-password");
        assertThat(savedUser.getCity()).isEqualTo(city);

        verify(notificationService).sendAccountVerificationCode(eq("tes213t@gmail.com"), any(), eq("123456"));
    }

    @Test
    void shouldThrowExceptionWhenActiveEmailAlreadyExists() {
        final RegisterRequest request = createRegisterRequest("existing@cinebh.com", null);
        final User existingUser = new User();
        existingUser.setActive(true);

        when(userRepository.findByEmail(request.email())).thenReturn(Optional.of(existingUser));

        assertThatThrownBy(() -> authService.register(request))
                .isInstanceOf(ApiException.class)
                .hasFieldOrPropertyWithValue("status", HttpStatus.BAD_REQUEST)
                .hasMessage("User DTO validation failed.");
    }

    @Test
    void shouldThrowExceptionWithSpecificMessageWhenInactiveEmailExists() {
        final RegisterRequest request = createRegisterRequest("inactive@cinebh.com", null);
        final User existingUser = new User();
        existingUser.setActive(false);

        when(userRepository.findByEmail(request.email())).thenReturn(Optional.of(existingUser));

        assertThatThrownBy(() -> authService.register(request))
                .isInstanceOf(ApiException.class)
                .hasFieldOrPropertyWithValue("status", HttpStatus.BAD_REQUEST)
                .hasMessage("User DTO validation failed.");
    }

    @Test
    void shouldLoginSuccessfully() {
        final LoginRequest request = new LoginRequest("test@cinebh.com", "Password123", false);
        final HttpServletResponse response = mock(HttpServletResponse.class);

        when(userRepository.findByEmail(request.email())).thenReturn(Optional.of(testUser));
        when(passwordEncoder.matches(request.password(), testUser.getPasswordHash())).thenReturn(true);
        when(jwtService.generateAccessToken(testUser)).thenReturn("access-token");
        when(jwtService.generateRefreshToken(testUser, false)).thenReturn("refresh-token");

        final LoginResponse loginResponse = authService.login(request, response);

        assertThat(loginResponse.email()).isEqualTo(testUser.getEmail());
        verify(authenticationRateLimitService).clearFailedLoginAttempts(request.email());
        verify(cookieUtils).setTokenCookies(response, "access-token", "refresh-token", false);
    }

    @Test
    void shouldRecordFailedLoginWhenPasswordIsInvalid() {
        final LoginRequest request = new LoginRequest("test@cinebh.com", "WrongPassword123", false);
        final HttpServletResponse response = mock(HttpServletResponse.class);

        when(userRepository.findByEmail(request.email())).thenReturn(Optional.of(testUser));
        when(passwordEncoder.matches(request.password(), testUser.getPasswordHash())).thenReturn(false);

        assertThatThrownBy(() -> authService.login(request, response))
                .isInstanceOf(ApiException.class)
                .hasFieldOrPropertyWithValue("status", HttpStatus.UNAUTHORIZED)
                .hasMessage("Invalid email or password.");

        verify(authenticationRateLimitService).recordFailedLogin(request.email());
    }

    @Test
    void shouldRecordFailedLoginWhenEmailDoesNotExist() {
        final LoginRequest request = new LoginRequest("missing@cinebh.com", "Password123", false);
        final HttpServletResponse response = mock(HttpServletResponse.class);

        when(userRepository.findByEmail(request.email())).thenReturn(Optional.empty());

        assertThatThrownBy(() -> authService.login(request, response))
                .isInstanceOf(ApiException.class)
                .hasFieldOrPropertyWithValue("status", HttpStatus.UNAUTHORIZED)
                .hasMessage("Invalid email or password.");

        verify(authenticationRateLimitService).recordFailedLogin(request.email());
    }

    @Test
    void shouldRejectLoginWhenRateLimitIsExceeded() {
        final LoginRequest request = new LoginRequest("test@cinebh.com", "Password123", false);
        final HttpServletResponse response = mock(HttpServletResponse.class);

        doThrow(new ApiException(
                "Too many failed login attempts. Please try again later.",
                HttpStatus.TOO_MANY_REQUESTS
        )).when(authenticationRateLimitService).assertLoginAllowed(request.email());

        assertThatThrownBy(() -> authService.login(request, response))
                .isInstanceOf(ApiException.class)
                .hasFieldOrPropertyWithValue("status", HttpStatus.TOO_MANY_REQUESTS)
                .hasMessage("Too many failed login attempts. Please try again later.");

        verifyNoInteractions(userRepository, passwordEncoder, jwtService, cookieUtils);
    }

    @Test
    void shouldThrowExceptionAndSendNewCodeWhenLoginInactiveUser() {
        final LoginRequest request = new LoginRequest("test@cinebh.com", "Password123", false);
        testUser.setActive(false);
        final HttpServletResponse response = mock(HttpServletResponse.class);

        when(userRepository.findByEmail(request.email())).thenReturn(Optional.of(testUser));
        when(passwordEncoder.matches(request.password(), testUser.getPasswordHash())).thenReturn(true);
        when(verificationService.generateAndSaveCode(testUser, VerificationCodeType.ACCOUNT_VERIFICATION)).thenReturn("123456");

        assertThatThrownBy(() -> authService.login(request, response))
                .isInstanceOf(ApiException.class)
                .hasFieldOrPropertyWithValue("status", HttpStatus.FORBIDDEN)
                .hasMessageContaining("Account is not verified");

        verify(notificationService).sendAccountVerificationCode(eq(testUser.getEmail()), any(), eq("123456"));
    }

    @Test
    void shouldRefreshTokensSuccessfully() {
        final String refreshToken = "valid-refresh-token";
        final String email = "test@cinebh.com";
        final User user = new User();
        user.setEmail(email);
        user.setActive(true);
        final Claims claims = mock(Claims.class);

        final HttpServletRequest request = mock(HttpServletRequest.class);
        final HttpServletResponse response = mock(HttpServletResponse.class);

        when(cookieUtils.extractCookie(request, "refresh_token")).thenReturn(Optional.of(refreshToken));

        when(jwtService.extractClaims(refreshToken)).thenReturn(claims);
        when(claims.getSubject()).thenReturn(email);

        when(userRepository.findByEmail(email)).thenReturn(Optional.of(user));
        when(jwtService.generateAccessToken(user)).thenReturn("new-access-token");

        authService.refresh(request, response);

        verify(cookieUtils).setAccessTokenCookie(response, "new-access-token");
    }

    @Test
    void shouldThrowExceptionWhenRefreshingInactiveUser() {
        final String refreshToken = "valid-refresh-token";
        final String email = "inactive@cinebh.com";
        final User user = new User();
        user.setEmail(email);
        user.setActive(false);
        final Claims claims = mock(Claims.class);

        final HttpServletRequest request = mock(HttpServletRequest.class);
        final HttpServletResponse response = mock(HttpServletResponse.class);

        when(cookieUtils.extractCookie(request, "refresh_token")).thenReturn(Optional.of(refreshToken));
        when(jwtService.extractClaims(refreshToken)).thenReturn(claims);
        when(claims.getSubject()).thenReturn(email);
        when(userRepository.findByEmail(email)).thenReturn(Optional.of(user));

        assertThatThrownBy(() -> authService.refresh(request, response))
                .isInstanceOf(ApiException.class)
                .hasFieldOrPropertyWithValue("status", HttpStatus.UNAUTHORIZED)
                .hasMessage("Invalid or expired refresh token.");
    }

    @Test
    void shouldLoginSuccessfullyWithRememberMe() {
        final LoginRequest request = new LoginRequest("test@cinebh.com", "Password123", true);
        final HttpServletResponse response = mock(HttpServletResponse.class);

        when(userRepository.findByEmail(request.email())).thenReturn(Optional.of(testUser));
        when(passwordEncoder.matches(request.password(), testUser.getPasswordHash())).thenReturn(true);
        when(jwtService.generateAccessToken(testUser)).thenReturn("access-token");
        when(jwtService.generateRefreshToken(testUser, true)).thenReturn("refresh-token");

        final LoginResponse loginResponse = authService.login(request, response);

        assertThat(loginResponse.email()).isEqualTo(testUser.getEmail());
        verify(authenticationRateLimitService).clearFailedLoginAttempts(request.email());
        verify(cookieUtils).setTokenCookies(response, "access-token", "refresh-token", true);
    }

    @Test
    void shouldLogoutSuccessfully() {
        final HttpServletRequest request = mock(HttpServletRequest.class);
        final HttpServletResponse response = mock(HttpServletResponse.class);
        final HttpSession session = mock(HttpSession.class);
        SecurityContextHolder.getContext().setAuthentication(
                new TestingAuthenticationToken("test@cinebh.com", null)
        );
        when(request.getSession(false)).thenReturn(session);

        authService.logout(request, response);

        verify(cookieUtils).clearAuthenticationCookies(response);
        verify(session).invalidate();
        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
    }

    @Test
    void shouldVerifyAccountSuccessfully() {
        final VerifyRequest request = new VerifyRequest("test@cinebh.com", "123456");
        testUser.setActive(false);

        when(userRepository.findByEmail(request.email())).thenReturn(Optional.of(testUser));
        when(verificationService.verifyCode(testUser, VerificationCodeType.ACCOUNT_VERIFICATION, "123456")).thenReturn(true);

        authService.verify(request);

        assertThat(testUser.isActive()).isTrue();
        verify(userRepository).save(testUser);
    }

    @Test
    void shouldThrowExceptionWhenVerifyCodeIsInvalid() {
        final VerifyRequest request = new VerifyRequest("test@cinebh.com", "000000");
        testUser.setActive(false);

        when(userRepository.findByEmail(request.email())).thenReturn(Optional.of(testUser));
        when(verificationService.verifyCode(testUser, VerificationCodeType.ACCOUNT_VERIFICATION, "000000"))
                .thenReturn(false);

        assertThatThrownBy(() -> authService.verify(request))
                .isInstanceOf(ApiException.class)
                .hasMessage("Invalid or expired verification code.");
    }
}
