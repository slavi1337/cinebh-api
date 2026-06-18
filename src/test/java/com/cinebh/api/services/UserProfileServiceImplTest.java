package com.cinebh.api.services;

import com.cinebh.api.dto.profile.ChangePasswordRequest;
import com.cinebh.api.dto.profile.UpdateUserProfileRequest;
import com.cinebh.api.dto.profile.UserProfileResponse;
import com.cinebh.api.dto.profile.UserProjectionResponse;
import com.cinebh.api.entities.Booking;
import com.cinebh.api.entities.City;
import com.cinebh.api.entities.Hall;
import com.cinebh.api.entities.Movie;
import com.cinebh.api.entities.Projection;
import com.cinebh.api.entities.SeatTemplate;
import com.cinebh.api.entities.User;
import com.cinebh.api.entities.Venue;
import com.cinebh.api.entities.enums.BookingStatus;
import com.cinebh.api.entities.enums.SeatType;
import com.cinebh.api.entities.enums.UserRole;
import com.cinebh.api.exceptions.ApiException;
import com.cinebh.api.mappers.BookingResponseMapper;
import com.cinebh.api.repositories.BookingRepository;
import com.cinebh.api.repositories.CityRepository;
import com.cinebh.api.repositories.UserRepository;
import com.cinebh.api.services.impl.BookingCoverImageResolver;
import com.cinebh.api.services.impl.UserProfileServiceImpl;
import com.cinebh.api.services.storage.StorageService;
import com.cinebh.api.services.storage.StoredFile;
import com.cinebh.api.utils.SecurityUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UserProfileServiceImplTest {

    private static final Clock CLOCK = Clock.fixed(
            Instant.parse("2026-06-18T10:00:00Z"),
            ZoneOffset.UTC
    );

    @Mock
    private SecurityUtils securityUtils;

    @Mock
    private UserRepository userRepository;

    @Mock
    private CityRepository cityRepository;

    @Mock
    private BookingRepository bookingRepository;

    @Mock
    private AdvancedValidationService advancedValidationService;

    @Mock
    private AddressValidationService addressValidationService;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private StorageService storageService;

    private UserProfileServiceImpl userProfileService;
    private User currentUser;

    @BeforeEach
    void setUp() {
        userProfileService = new UserProfileServiceImpl(
                securityUtils,
                userRepository,
                cityRepository,
                bookingRepository,
                advancedValidationService,
                addressValidationService,
                passwordEncoder,
                storageService,
                new BookingResponseMapper(),
                new BookingCoverImageResolver(bookingRepository),
                CLOCK
        );
        currentUser = createUser();
    }

    @Test
    void shouldReturnCurrentUserProfile() {
        final City city = createCity("Sarajevo", "Bosnia and Herzegovina");
        currentUser.setCity(city);
        currentUser.setPhone("+38761123456");

        when(securityUtils.getCurrentUser()).thenReturn(currentUser);

        final UserProfileResponse response = userProfileService.getProfile();

        assertThat(response.email()).isEqualTo(currentUser.getEmail());
        assertThat(response.cityName()).isEqualTo("Sarajevo");
        assertThat(response.country()).isEqualTo("Bosnia and Herzegovina");
        assertThat(response.phone()).isEqualTo("+38761123456");
    }

    @Test
    void shouldGroupLocationOptionsByCountry() {
        final City sarajevo = createCity("Sarajevo", "Bosnia and Herzegovina");
        final City zagreb = createCity("Zagreb", "Croatia");

        when(cityRepository.findAll(any(Sort.class))).thenReturn(List.of(sarajevo, zagreb));

        final var response = userProfileService.getLocationOptions();

        assertThat(response.countries()).hasSize(2);
        assertThat(response.countries().getFirst().country()).isEqualTo("Bosnia and Herzegovina");
        assertThat(response.countries().getFirst().cities().getFirst().name()).isEqualTo("Sarajevo");
        assertThat(response.countries().get(1).country()).isEqualTo("Croatia");
    }

    @Test
    void shouldUpdateCurrentUserProfile() {
        final UUID cityId = UUID.randomUUID();
        final City city = createCity(cityId, "Banja Luka", "Bosnia and Herzegovina");
        final UpdateUserProfileRequest request = new UpdateUserProfileRequest(
                "Slavisa",
                "Covakusic",
                "+38761123456",
                cityId,
                null
        );

        when(securityUtils.getCurrentUser()).thenReturn(currentUser);
        when(cityRepository.findById(cityId)).thenReturn(Optional.of(city));
        when(userRepository.findByPhone(request.phone())).thenReturn(Optional.empty());
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));

        final UserProfileResponse response = userProfileService.updateProfile(request);

        assertThat(response.firstName()).isEqualTo("Slavisa");
        assertThat(response.lastName()).isEqualTo("Covakusic");
        assertThat(response.phone()).isEqualTo("+38761123456");
        assertThat(response.cityName()).isEqualTo("Banja Luka");
        assertThat(currentUser.getUpdatedAt()).isEqualTo(OffsetDateTime.now(CLOCK));
        verify(advancedValidationService).validatePhone("+38761123456");
        verify(userRepository).save(currentUser);
    }

    @Test
    void shouldRejectDuplicatePhoneWhenUpdatingProfile() {
        final User otherUser = createUser();
        otherUser.setId(UUID.randomUUID());
        final UpdateUserProfileRequest request = new UpdateUserProfileRequest(
                "Slavisa",
                "Covakusic",
                "+38761123456",
                null,
                null
        );

        when(securityUtils.getCurrentUser()).thenReturn(currentUser);
        when(userRepository.findByPhone(request.phone())).thenReturn(Optional.of(otherUser));

        assertThatThrownBy(() -> userProfileService.updateProfile(request))
                .isInstanceOf(ApiException.class)
                .hasFieldOrPropertyWithValue("status", HttpStatus.BAD_REQUEST)
                .hasMessage("User DTO validation failed.");

        verify(userRepository, never()).save(any(User.class));
    }

    @Test
    void shouldUploadProfileImage() {
        final MockMultipartFile file = new MockMultipartFile(
                "file",
                "avatar.png",
                "image/png",
                "image".getBytes()
        );

        when(securityUtils.getCurrentUser()).thenReturn(currentUser);
        when(storageService.upload("profile-images", file)).thenReturn("profile-images/avatar.png");
        when(storageService.getPublicUrl("profile-images/avatar.png"))
                .thenReturn("https://cdn.cinebh.com/profile-images/avatar.png");
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));

        final UserProfileResponse response = userProfileService.uploadProfileImage(file);

        assertThat(response.profileImageUrl()).isEqualTo("https://cdn.cinebh.com/profile-images/avatar.png");
        verify(userRepository).save(currentUser);
    }

    @Test
    void shouldReturnProfileImageFromStorage() {
        currentUser.setProfileImageUrl("http://localhost:9000/cinebh/profile-images/avatar.png");
        final StoredFile storedFile = new StoredFile("image/png", "image".getBytes());

        when(securityUtils.getCurrentUser()).thenReturn(currentUser);
        when(storageService.download("profile-images/avatar.png")).thenReturn(storedFile);

        final StoredFile response = userProfileService.getProfileImage();

        assertThat(response).isEqualTo(storedFile);
        verify(storageService).download("profile-images/avatar.png");
    }

    @Test
    void shouldChangePassword() {
        final ChangePasswordRequest request = new ChangePasswordRequest(
                "OldPassword123",
                "NewPassword123",
                "NewPassword123"
        );

        when(securityUtils.getCurrentUser()).thenReturn(currentUser);
        when(passwordEncoder.matches(request.currentPassword(), currentUser.getPasswordHash())).thenReturn(true);
        when(passwordEncoder.encode(request.newPassword())).thenReturn("new-hash");

        userProfileService.changePassword(request);

        assertThat(currentUser.getPasswordHash()).isEqualTo("new-hash");
        verify(advancedValidationService).validatePasswordPwned(request.newPassword());
        verify(userRepository).save(currentUser);
    }

    @Test
    void shouldRejectPasswordChangeWhenCurrentPasswordIsWrong() {
        final ChangePasswordRequest request = new ChangePasswordRequest(
                "wrong",
                "NewPassword123",
                "NewPassword123"
        );

        when(securityUtils.getCurrentUser()).thenReturn(currentUser);
        when(passwordEncoder.matches(request.currentPassword(), currentUser.getPasswordHash())).thenReturn(false);

        assertThatThrownBy(() -> userProfileService.changePassword(request))
                .isInstanceOf(ApiException.class)
                .hasFieldOrPropertyWithValue("status", HttpStatus.BAD_REQUEST)
                .hasMessage("Current password is incorrect.");

        verify(userRepository, never()).save(any(User.class));
    }

    @Test
    void shouldReturnPurchasedUpcomingProjections() {
        final Booking booking = createPaidBooking();
        final UUID movieId = booking.getProjection().getMovie().getId();

        when(securityUtils.getCurrentUser()).thenReturn(currentUser);
        when(bookingRepository.findPaidBookingsByUserId(currentUser.getId(), OffsetDateTime.now(CLOCK), true))
                .thenReturn(List.of(booking));
        when(bookingRepository.findCoverImageUrlsByMovieIds(eq(List.of(movieId))))
                .thenReturn(Map.of(movieId, "https://cdn.cinebh.com/poster.jpg"));

        final List<UserProjectionResponse> response = userProfileService.getPurchasedProjections("upcoming");

        assertThat(response).hasSize(1);
        assertThat(response.getFirst().movieTitle()).isEqualTo("Mandalorian");
        assertThat(response.getFirst().posterImageUrl()).isEqualTo("https://cdn.cinebh.com/poster.jpg");
        assertThat(response.getFirst().seats()).extracting("row").containsExactly("A");
    }

    @Test
    void shouldDeactivateCurrentUser() {
        when(securityUtils.getCurrentUser()).thenReturn(currentUser);

        userProfileService.deactivateCurrentUser();

        assertThat(currentUser.isActive()).isFalse();
        assertThat(currentUser.getUpdatedAt()).isEqualTo(OffsetDateTime.now(CLOCK));
        verify(userRepository).save(currentUser);
    }

    private User createUser() {
        final User user = new User();
        user.setId(UUID.randomUUID());
        user.setEmail("slavisa@example.com");
        user.setFirstName("Slavisa");
        user.setLastName("Covakusic");
        user.setPasswordHash("old-hash");
        user.setRole(UserRole.CUSTOMER);
        user.setActive(true);
        return user;
    }

    private City createCity(final String name, final String country) {
        return createCity(UUID.randomUUID(), name, country);
    }

    private City createCity(final UUID id, final String name, final String country) {
        final City city = new City();
        ReflectionTestUtils.setField(city, "id", id);
        ReflectionTestUtils.setField(city, "name", name);
        ReflectionTestUtils.setField(city, "country", country);
        return city;
    }

    private Booking createPaidBooking() {
        final Movie movie = new Movie();
        ReflectionTestUtils.setField(movie, "id", UUID.randomUUID());
        ReflectionTestUtils.setField(movie, "title", "Mandalorian");
        ReflectionTestUtils.setField(movie, "pgRating", "PG 13");
        ReflectionTestUtils.setField(movie, "language", "English");
        ReflectionTestUtils.setField(movie, "durationMinutes", 117);

        final City city = createCity("Sarajevo", "Bosnia and Herzegovina");

        final Venue venue = new Venue();
        ReflectionTestUtils.setField(venue, "name", "Cinebh Sarajevo");
        ReflectionTestUtils.setField(venue, "city", city);

        final Hall hall = new Hall();
        ReflectionTestUtils.setField(hall, "name", "Hall 1");
        ReflectionTestUtils.setField(hall, "venue", venue);

        final Projection projection = new Projection();
        ReflectionTestUtils.setField(projection, "id", UUID.randomUUID());
        ReflectionTestUtils.setField(projection, "movie", movie);
        ReflectionTestUtils.setField(projection, "hall", hall);
        ReflectionTestUtils.setField(projection, "startTime", OffsetDateTime.now(CLOCK).plusDays(1));

        final SeatTemplate seatTemplate = new SeatTemplate();
        ReflectionTestUtils.setField(seatTemplate, "id", UUID.randomUUID());
        ReflectionTestUtils.setField(seatTemplate, "rowNum", "A");
        ReflectionTestUtils.setField(seatTemplate, "seatNum", "1");
        ReflectionTestUtils.setField(seatTemplate, "type", SeatType.REGULAR);

        final Booking booking = new Booking(
                currentUser,
                projection,
                OffsetDateTime.now(CLOCK).plusMinutes(5),
                OffsetDateTime.now(CLOCK)
        );
        ReflectionTestUtils.setField(booking, "status", BookingStatus.PAID);
        booking.replaceActiveSeats(List.of(seatTemplate), Map.of(SeatType.REGULAR, BigDecimal.valueOf(7)));
        return booking;
    }
}
