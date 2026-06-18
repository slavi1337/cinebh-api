package com.cinebh.api.dto.profile;

import java.util.List;

public record CountryOptionResponse(
        String country,
        List<CityOptionResponse> cities
) {
}
