package com.cinebh.api.services.impl;

import com.cinebh.api.dto.profile.ChangePasswordRequest;
import com.cinebh.api.dto.profile.CityOptionResponse;
import com.cinebh.api.dto.profile.CountryOptionResponse;
import com.cinebh.api.dto.profile.ProfileLocationOptionsResponse;
import com.cinebh.api.dto.profile.UpdateUserProfileRequest;
import com.cinebh.api.dto.profile.UserProfileResponse;
import com.cinebh.api.dto.profile.UserProjectionResponse;
import com.cinebh.api.entities.Booking;
import com.cinebh.api.entities.City;
import com.cinebh.api.entities.User;
import com.cinebh.api.exceptions.ApiException;
import com.cinebh.api.mappers.BookingResponseMapper;
import com.cinebh.api.repositories.BookingRepository;
import com.cinebh.api.repositories.CityRepository;
import com.cinebh.api.repositories.UserRepository;
import com.cinebh.api.services.AddressValidationService;
import com.cinebh.api.services.AdvancedValidationService;
import com.cinebh.api.services.UserProfileService;
import com.cinebh.api.services.storage.StorageService;
import com.cinebh.api.services.storage.StoredFile;
import com.cinebh.api.utils.SecurityUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.time.Clock;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class UserProfileServiceImpl implements UserProfileService {

    private static final String PROFILE_IMAGE_DIRECTORY = "profile-images";
    private static final String PAST_PROJECTION_STATUS = "past";
    private static final String UPCOMING_PROJECTION_STATUS = "upcoming";
    private static final long MAX_PROFILE_IMAGE_SIZE_BYTES = 5L * 1024 * 1024;

    private final SecurityUtils securityUtils;
    private final UserRepository userRepository;
    private final CityRepository cityRepository;
    private final BookingRepository bookingRepository;
    private final AdvancedValidationService advancedValidationService;
    private final AddressValidationService addressValidationService;
    private final PasswordEncoder passwordEncoder;
    private final StorageService storageService;
    private final BookingResponseMapper bookingResponseMapper;
    private final BookingCoverImageResolver bookingCoverImageResolver;
    private final Clock clock;

    @Override
    @Transactional(readOnly = true)
    public UserProfileResponse getProfile() {
        return toProfileResponse(securityUtils.getCurrentUser());
    }

    @Override
    @Transactional(readOnly = true)
    public ProfileLocationOptionsResponse getLocationOptions() {
        final List<City> cities = cityRepository.findAll(Sort.by(
                Sort.Order.asc("country"),
                Sort.Order.asc("name")
        ));
        final Map<String, List<CityOptionResponse>> citiesByCountry = new LinkedHashMap<>();

        cities.forEach(city -> citiesByCountry
                .computeIfAbsent(city.getCountry(), ignored -> new ArrayList<>())
                .add(new CityOptionResponse(city.getId(), city.getName())));

        final List<CountryOptionResponse> countries = citiesByCountry.entrySet()
                .stream()
                .map(entry -> new CountryOptionResponse(entry.getKey(), entry.getValue()))
                .toList();

        return new ProfileLocationOptionsResponse(countries);
    }

    @Override
    @Transactional
    public UserProfileResponse updateProfile(final UpdateUserProfileRequest request) {
        final User currentUser = securityUtils.getCurrentUser();
        final City city = findCity(request.cityId());
        final String phone = blankToNull(request.phone());
        final String streetAddress = blankToNull(request.streetAddress());

        advancedValidationService.validateNameNotReserved(request.firstName(), request.lastName());
        advancedValidationService.validatePhone(phone);
        validatePhoneIsAvailable(phone, currentUser.getId());
        validateAddress(city, streetAddress);

        currentUser.setFirstName(blankToNull(request.firstName()));
        currentUser.setLastName(blankToNull(request.lastName()));
        currentUser.setPhone(phone);
        currentUser.setCity(city);
        currentUser.setStreetAddress(streetAddress);
        currentUser.setUpdatedAt(OffsetDateTime.now(clock));

        return toProfileResponse(userRepository.save(currentUser));
    }

    @Override
    @Transactional
    public UserProfileResponse uploadProfileImage(final MultipartFile file) {
        validateProfileImage(file);

        final User currentUser = securityUtils.getCurrentUser();
        final String objectKey = storageService.upload(PROFILE_IMAGE_DIRECTORY, file);
        final String imageUrl = storageService.getPublicUrl(objectKey);

        currentUser.setProfileImageUrl(imageUrl);
        currentUser.setUpdatedAt(OffsetDateTime.now(clock));

        return toProfileResponse(userRepository.save(currentUser));
    }

    @Override
    @Transactional(readOnly = true)
    public StoredFile getProfileImage() {
        final User currentUser = securityUtils.getCurrentUser();
        final String objectKey = profileImageObjectKey(currentUser.getProfileImageUrl());

        return storageService.download(objectKey);
    }

    @Override
    @Transactional
    public void changePassword(final ChangePasswordRequest request) {
        final User currentUser = securityUtils.getCurrentUser();

        if (!passwordEncoder.matches(request.currentPassword(), currentUser.getPasswordHash())) {
            throw new ApiException("Current password is incorrect.", HttpStatus.BAD_REQUEST);
        }

        if (!Objects.equals(request.newPassword(), request.repeatNewPassword())) {
            throw new ApiException("Passwords do not match.", HttpStatus.BAD_REQUEST);
        }

        advancedValidationService.validatePasswordPwned(request.newPassword());

        currentUser.setPasswordHash(passwordEncoder.encode(request.newPassword()));
        currentUser.setUpdatedAt(OffsetDateTime.now(clock));
        userRepository.save(currentUser);
    }

    @Override
    @Transactional
    public void deactivateCurrentUser() {
        final User currentUser = securityUtils.getCurrentUser();
        currentUser.setActive(false);
        currentUser.setUpdatedAt(OffsetDateTime.now(clock));
        userRepository.save(currentUser);
    }

    @Override
    @Transactional(readOnly = true)
    public List<UserProjectionResponse> getPurchasedProjections(final String status) {
        final User currentUser = securityUtils.getCurrentUser();
        final boolean upcoming = parseProjectionStatus(status);
        final List<Booking> bookings = bookingRepository.findPaidBookingsByUserId(
                currentUser.getId(),
                OffsetDateTime.now(clock),
                upcoming
        );
        final Map<UUID, String> coverImageUrlsByMovieId =
                bookingCoverImageResolver.findCoverImageUrlsByMovieId(bookings);

        return bookings.stream()
                .map(booking -> bookingResponseMapper.toUserProjectionResponse(
                        booking,
                        coverImageUrlsByMovieId.get(booking.getProjection().getMovie().getId())
                ))
                .toList();
    }

    private boolean parseProjectionStatus(final String status) {
        if (status == null || status.isBlank() || UPCOMING_PROJECTION_STATUS.equalsIgnoreCase(status)) {
            return true;
        }

        if (PAST_PROJECTION_STATUS.equalsIgnoreCase(status)) {
            return false;
        }

        throw new ApiException("Projection status must be 'upcoming' or 'past'.", HttpStatus.BAD_REQUEST);
    }

    private void validatePhoneIsAvailable(final String phone, final UUID currentUserId) {
        if (phone == null) {
            return;
        }

        userRepository.findByPhone(phone)
                .filter(user -> !Objects.equals(user.getId(), currentUserId))
                .ifPresent(user -> {
                    throw new ApiException(
                            "User DTO validation failed.",
                            HttpStatus.BAD_REQUEST,
                            10002,
                            "phone",
                            "Phone number is already in use."
                    );
                });
    }

    private void validateAddress(final City city, final String streetAddress) {
        if (streetAddress == null) {
            return;
        }

        if (city == null) {
            throw new ApiException("City is required when street address is provided.", HttpStatus.BAD_REQUEST);
        }

        if (!addressValidationService.isValidStreetInCity(city.getName(), streetAddress)) {
            throw new ApiException("Street does not exist in the selected city.", HttpStatus.BAD_REQUEST);
        }
    }

    private void validateProfileImage(final MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new ApiException("File must not be empty", HttpStatus.BAD_REQUEST);
        }

        final String contentType = file.getContentType();
        if (contentType == null || !contentType.toLowerCase().startsWith("image/")) {
            throw new ApiException("Profile image must be an image file.", HttpStatus.BAD_REQUEST);
        }

        if (file.getSize() > MAX_PROFILE_IMAGE_SIZE_BYTES) {
            throw new ApiException("Image size must be less than 5MB.", HttpStatus.BAD_REQUEST);
        }
    }

    private City findCity(final UUID cityId) {
        if (cityId == null) {
            return null;
        }

        return cityRepository.findById(cityId)
                .orElseThrow(() -> new ApiException("Invalid location selected.", HttpStatus.BAD_REQUEST));
    }

    private String profileImageObjectKey(final String profileImageUrl) {
        if (profileImageUrl == null || profileImageUrl.isBlank()) {
            throw new ApiException("Profile image was not uploaded.", HttpStatus.NOT_FOUND);
        }

        final int objectKeyStartIndex = profileImageUrl.indexOf(PROFILE_IMAGE_DIRECTORY + "/");

        if (objectKeyStartIndex < 0) {
            throw new ApiException("Profile image is not stored in file storage.", HttpStatus.NOT_FOUND);
        }

        return profileImageUrl.substring(objectKeyStartIndex);
    }

    private String blankToNull(final String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }

    private UserProfileResponse toProfileResponse(final User user) {
        final City city = user.getCity();

        return new UserProfileResponse(
                user.getId(),
                user.getFirstName(),
                user.getLastName(),
                user.getEmail(),
                user.getPhone(),
                user.getProfileImageUrl(),
                city == null ? null : city.getId(),
                city == null ? null : city.getName(),
                city == null ? null : city.getCountry(),
                user.getStreetAddress()
        );
    }

}
