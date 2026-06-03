package com.cinebh.api.services;

public interface AddressValidationService {
    boolean isValidStreetInCity(String city, String streetAddress);
}
